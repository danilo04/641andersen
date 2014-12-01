package edu.iastate.coms641;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import soot.Body;
import soot.Context;
import soot.EntryPoints;
import soot.G;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.ClassConstant;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.options.Options;

/**
 * @author Danilo Dominguez Perez
 * Implemented from: The Complexity of Andersen's Analysis in Practice by Sridharan and Fink
 *
 */
public class AndersenPointsTo implements PointsToAnalysis {
	enum VariableType {
		LOCAL, STATICREF, OBJECTFIELD
	}

	interface Variable {
		public VariableType getVariableType();

		public Type getSootType();
	}

	//TODO check putting final to see if the JVM treats this class as a value class
	static class ObjectFieldVariable implements Variable {
		Node obj;
		SootField f;

		public ObjectFieldVariable(Node obj, SootField f) {
			this.obj = obj;
			this.f = f;
		}

		public Node getNode() {
			return obj;
		}

		@Override
		public Type getSootType() {
			return f.getType();
		}
		
		public SootField getField() {
			return f;
		}

		@Override
		public VariableType getVariableType() {
			return VariableType.OBJECTFIELD;
		}
		
		@Override
		public String toString() {
			return "Node: " + obj + ", field: " + f;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((obj == null) ? 0 : obj.hashCode());
			result = prime * result + ((f == null) ? 0 : f.hashCode());

			return result;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null)
				return false;
			if (!(o instanceof ObjectFieldVariable))
				return false;
			ObjectFieldVariable other = (ObjectFieldVariable) o;
			if (obj == null) {
				if (other.obj != null)
					return false;
			} else if (!obj.equals(other.obj))
				return false;

			if (f == null) {
				if (other.f != null)
					return false;
			} else if (!f.equals(other.f))
				return false;

			return true;
		}
	}

	static class LocalVariable implements Variable {
		Local local;

		public LocalVariable(Local local) {
			if (local.getNumber() == 0) {
				Scene.v().getLocalNumberer().add(local);
			}
			this.local = local;
		}

		@Override
		public Type getSootType() {
			return local.getType();
		}

		@Override
		public VariableType getVariableType() {
			return VariableType.LOCAL;
		}

		public Local getLocal() {
			return local;
		}
		
		@Override
		public String toString() {
			return "Local: " + local;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((local == null) ? 0 : local.hashCode());

			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof LocalVariable))
				return false;
			LocalVariable other = (LocalVariable) obj;
			if (local == null) {
				if (other.local != null)
					return false;
			} else if (!local.equals(other.local))
				return false;

			return true;
		}
	}

	static class StaticRefVariable implements Variable {
		StaticFieldRef ref;

		public StaticRefVariable(StaticFieldRef ref) {
			this.ref = ref;
		}

		@Override
		public Type getSootType() {
			return ref.getType();
		}

		@Override
		public VariableType getVariableType() {
			return VariableType.STATICREF;
		}

		public StaticFieldRef getRef() {
			return ref;
		}
		
		@Override
		public String toString() {
			return "StaticRef: " + ref;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((ref == null) ? 0 : ref.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof StaticRefVariable))
				return false;
			StaticRefVariable other = (StaticRefVariable) obj;
			if (ref == null) {
				if (other.ref != null)
					return false;
			} else if (!ref.equals(other.ref))
				return false;

			return true;
		}
	}

	private final CallGraphBuilder cgb;
	//private ReachableMethods reachables;
	private Map<Variable, HashNodePointsToSet> ptDelta = new HashMap<Variable, HashNodePointsToSet>();
	private Map<Variable, HashNodePointsToSet> pt = new HashMap<Variable, HashNodePointsToSet>();
	private Set<AssignStmt> newStmts = new HashSet<AssignStmt>();
	private Map<Variable, Set<Variable>> flowG = new HashMap<Variable, Set<Variable>>();
	private Map<Local, Set<AssignStmt>> localToLoads = new HashMap<Local, Set<AssignStmt>>();
	private Map<Local, Set<AssignStmt>> localToStores = new HashMap<Local, Set<AssignStmt>>();
	private Map<Value, Variable> valueToVariableCache = new HashMap<Value, Variable>();
	private Set<IdentityStmt> thisRefs = new HashSet<IdentityStmt>();
	private Set<IdentityStmt> paramRefs = new HashSet<IdentityStmt>();
	private Set<AssignStmt> stringConstants = new HashSet<AssignStmt>();
	private Set<AssignStmt> classConstants = new HashSet<AssignStmt>();

	public AndersenPointsTo() {
		// start with a simple callgraph analysis
		cgb = new CallGraphBuilder(DumbPointerAnalysis.v());
		
	}

	public void build() {
		long time = -System.currentTimeMillis();
		prepareCallgraph();
		long cgTime = time + System.currentTimeMillis();
		G.v().out.println("[Andersen] Built CG with dump pointer analysis: " + cgTime);
		
		for (SootClass c : Scene.v().getClasses()) {
			handleClass(c);
		}
		
		long gTime = time + System.currentTimeMillis() - cgTime;
		G.v().out.println("[Andersen] Create flow graph G and finding statements of interest: " + gTime);

		buildPointsToSets();
		long pTime = time + System.currentTimeMillis() - cgTime - gTime;
		G.v().out.println("[Andersen] Building pointsTo sets: " + pTime);

		Scene.v().setPointsToAnalysis(this);
	}

	private void prepareCallgraph() {
		cgb.build();	
		ArrayList<MethodOrMethodContext> methods = new ArrayList<MethodOrMethodContext>();
		for (SootMethod m : EntryPoints.v().all()) {
			methods.add(m);
		}
		
	   // reachables = cgb.reachables();
	}

	private void buildPointsToSets() {
		Queue<Variable> worklist = new LinkedList<Variable>();
		// create new fresh objects for each new instance
		for (AssignStmt newStmt : newStmts) {
			Variable x = getVariable(newStmt.getLeftOp());
			worklist.add(x);
			newFreshPtDelta(x, worklist);
		}

		// I already created the flow graph G

		// compute the transitive closure
		while (!worklist.isEmpty()) {
			Variable n = worklist.poll();

			// compute the diffProp for each edge from n --> n' in G
			if (flowG.containsKey(n)) {
				for (Variable np : flowG.get(n)) {
					diffProp(ptDelta.get(n), np, worklist);
				}
			}

			// if n represents a local x
			if (n.getVariableType() == VariableType.LOCAL) {
				// compute the stores
				Local x = ((LocalVariable) n).getLocal();
				if (localToStores.containsKey(x)) {
					for (AssignStmt xfy : localToStores.get(x)) {
						SootField f = ((InstanceFieldRef) xfy.getLeftOp()).getField();
						Variable y = getVariable(xfy.getRightOp());
						for (Node oi : ptDelta.get(n).getNodes()) {
							ObjectFieldVariable of = getObjectFieldVariable(oi, f);
							if (!inFlowG(y, of)) {
								addEdgeFlowG(y, of);
								diffProp(pt.get(y), of, worklist);
							}
						}
					}
				}

				// compute the loads
				if (localToLoads.containsKey(x)) {
					for (AssignStmt yxf : localToLoads.get(x)) {
						SootField f = ((InstanceFieldRef) yxf.getRightOp()).getField();
						Variable y = getVariable(yxf.getLeftOp());
						for (Node oi : ptDelta.get(n).getNodes()) {
							ObjectFieldVariable of = getObjectFieldVariable(oi, f);
							if (!inFlowG(of, y)) {
								addEdgeFlowG(of, y);
								diffProp(pt.get(of), y, worklist);
							}
						}
					}
				}
			}

			// update points To sets
			HashNodePointsToSet newPtSet = HashNodePointsToSet.copy(pt.get(n));
			newPtSet.unionMutable(ptDelta.get(n));
			pt.put(n, newPtSet);
			ptDelta.put(n, HashNodePointsToSet.empty());
		}
		
		handleStringConstants();
		handleClassConstants();

		ptDelta = null;
		flowG = null;
		localToLoads = null;
		localToStores = null;
		stringConstants = null;
		classConstants = null;
		thisRefs = null;
		paramRefs = null;
	}

	private void handleClassConstants() {
		for (AssignStmt a : classConstants) {
			if (a.getLeftOp() instanceof InstanceFieldRef) {
				InstanceFieldRef fieldRef = (InstanceFieldRef) a.getLeftOp();
				Variable baseVar = getVariable(fieldRef.getBase());
				if (pt.containsKey(baseVar)) {
					for (Node obj : pt.get(baseVar).getNodes()) {
						Variable x = getObjectFieldVariable(obj, fieldRef.getField());
						HashNodePointsToSet set = pt.containsKey(x) ? pt.get(x) : HashNodePointsToSet.empty();
						set.addString(((ClassConstant) a.getRightOp()).value);
						pt.put(x, set);
					}
				}
			} else {
				Variable x = getVariable(a.getLeftOp());
				HashNodePointsToSet set = pt.containsKey(x) ? pt.get(x) : HashNodePointsToSet.empty();
				set.addString(((ClassConstant) a.getRightOp()).value);
				pt.put(x, set);
			}
		}
	}

	private void handleStringConstants() {
		for (AssignStmt a : stringConstants) {
			if (a.getLeftOp() instanceof InstanceFieldRef) {
				InstanceFieldRef fieldRef = (InstanceFieldRef) a.getLeftOp();
				Variable baseVar = getVariable(fieldRef.getBase());
				if (pt.containsKey(baseVar)) {
					for (Node obj : pt.get(baseVar).getNodes()) {
						Variable x = getObjectFieldVariable(obj, fieldRef.getField());
						HashNodePointsToSet set = pt.containsKey(x) ? pt.get(x) : HashNodePointsToSet.empty();
						set.addString(((StringConstant) a.getRightOp()).value);
						pt.put(x, set);
					}
				}
			} else {
				Variable x = getVariable(a.getLeftOp());
				HashNodePointsToSet set = pt.containsKey(x) ? pt.get(x) : HashNodePointsToSet.empty();
				set.addString(((StringConstant) a.getRightOp()).value);
				pt.put(x, set);
			}
		}
		
	}

	private boolean inFlowG(Variable src, Variable dest) {
		if (!flowG.containsKey(src))
			return false;
		return flowG.get(src).contains(dest);
	}

	private void diffProp(HashNodePointsToSet srcSet, Variable n, Queue<Variable> worklist) {
		HashNodePointsToSet oldPtDeltaSet = ptDelta.get(n);
		// in this case null and empty are equal
		if (oldPtDeltaSet == null) {
			// avoid the error in which an empty set is different from a null set
			oldPtDeltaSet = HashNodePointsToSet.empty();
			ptDelta.put(n, oldPtDeltaSet);
		}
		// ptDelta(n) = ptDelta(n) U (srcDest - pt(n))
		HashNodePointsToSet newPtDeltaSet = oldPtDeltaSet.union(HashNodePointsToSet.copy(srcSet).diff(pt.get(n)));		
		
		// if ptDetal(n) has changed
		if (ptDelta.get(n) == null || !newPtDeltaSet.equals(oldPtDeltaSet)) {
			ptDelta.put(n, newPtDeltaSet);
			worklist.add(n);
		}
	}

	/**
	 * Updates the ptDelta of the variable a new fresh object
	 * @param worklist 
	 */
	private void newFreshPtDelta(Variable x, Queue<Variable> worklist) {
		if (!ptDelta.containsKey(x)) {
			ptDelta.put(x, HashNodePointsToSet.empty());
		}
		
		// ptDelta(x) = ptDelta(x) U { oi }, where oi is fresh
		HashNodePointsToSet objs = ptDelta.get(x);
		Node node = new Node(SymbolicObject.fresh(), x.getSootType());
		objs.add(node);
		
		// add the edges for the this references
		for (IdentityStmt i : thisRefs) {
			ThisRef ref = (ThisRef) i.getRightOp();
			Type thisType = ref.getType();
			if (thisType.equals(x.getSootType())) {
				Variable thisVar = getVariable(i.getLeftOp());
				if (!ptDelta.containsKey(thisVar)) {
					ptDelta.put(thisVar, HashNodePointsToSet.empty());
				}
				
				ptDelta.get(thisVar).add(node);
				
				// the algorithm needs *this* references in the worklist, otherwise
				// they would not be reachable
				worklist.add(thisVar);
			}
		}
	}

	private void handleClass(SootClass c) {
		for (SootMethod m : c.getMethods()) {
			if (m.isConcrete() && !m.isPhantom()) {
				// TODO for now go to all the methods
				if (m.hasActiveBody() /*&& reachables.contains(m)*/) {
					handleMethod(m);
				} else {
					m.retrieveActiveBody();
					// TODO just handle reachable methods
					if (m.hasActiveBody() /*&& reachables.contains(m)*/) {
						handleMethod(m);
					}
				}
			} 
		}
	}

	private void handleMethod(SootMethod m) {
		if (Options.v().verbose()) {
			G.v().out.println("[Andersen] Handling method: " + m);
		}
		
		Body b = m.retrieveActiveBody();
		for (Unit u : b.getUnits()) {
			Stmt s = (Stmt) u;
			handleStmt(s);
		}
	}

	private void handleStmt(Stmt s) {
		if (s instanceof AssignStmt) {
			AssignStmt a = (AssignStmt) s;
			if (a.getRightOp() instanceof NewExpr || 
					a.getRightOp() instanceof NewArrayExpr) {
				// TODO handle || a.getRightOp() instanceof NewMultiArrayExpr
				newStmts.add(a);				
			} else if ((a.getLeftOp() instanceof Local || a.getLeftOp() instanceof StaticFieldRef)
					&& (a.getRightOp() instanceof Local || a.getRightOp() instanceof StaticFieldRef)) {
				addEdgeFlowG(getVariable(a.getRightOp()),
						getVariable(a.getLeftOp()));
			} else if (a.getLeftOp() instanceof InstanceFieldRef && a.getRightOp() instanceof Local) {
				Value base = ((InstanceFieldRef) a.getLeftOp()).getBase();
				Local baseStore = (Local) base;
				if (!localToStores.containsKey(baseStore)) {
					localToStores.put(baseStore, new HashSet<AssignStmt>());
				}
				Set<AssignStmt> stores = localToStores.get(baseStore);
				stores.add(a);
			} else if (a.getRightOp() instanceof InstanceFieldRef && a.getLeftOp() instanceof Local) {
				Value base = ((InstanceFieldRef) a.getRightOp()).getBase();
				Local baseStore = (Local) base;
				if (!localToLoads.containsKey(baseStore)) {
					localToLoads.put(baseStore, new HashSet<AssignStmt>());
				}
				Set<AssignStmt> loads = localToLoads.get(baseStore);
				loads.add(a);
			} else if (a.getRightOp() instanceof StringConstant) {
				stringConstants.add(a);
			} else if (a.getRightOp() instanceof ClassConstant) {
				classConstants.add(a);
			}
		} else if (s instanceof IdentityStmt) {
			IdentityStmt i = (IdentityStmt) s;
			if (i.getRightOp() instanceof ThisRef) {
				thisRefs.add(i);
			} else if (i.getRightOp() instanceof ParameterRef) {
				paramRefs.add(i);
			}
		}
	}

	private void addEdgeFlowG(Variable src, Variable dest) {
		if (!flowG.containsKey(src)) {
			flowG.put(src, new HashSet<Variable>());
		}
		Set<Variable> dests = flowG.get(src);
		dests.add(dest);
	}

	private Variable getVariable(Value op) {
		if (valueToVariableCache.containsKey(op))
			return valueToVariableCache.get(op);
		Variable newVar = createVariable(op);
		valueToVariableCache.put(op, newVar);
		return newVar;
	}

	private Variable createVariable(Value op) {
		if (op instanceof Local)
			return new LocalVariable((Local) op);
		else if (op instanceof StaticFieldRef)
			return new StaticRefVariable((StaticFieldRef) op);
		throw new InvalidParameterException("Value is not a local or static ref, it is: " + op.getClass());
	}

	private ObjectFieldVariable getObjectFieldVariable(Node obj, SootField f) {
		return new ObjectFieldVariable(obj, f);
	}

	@Override
	public PointsToSet reachingObjects(Local l) {
		Variable var = getVariable(l);
		if (var != null && pt.containsKey(var)) {
			return pt.get(var);
		}
		return HashNodePointsToSet.emptyRef();
	}

	@Override
	public PointsToSet reachingObjects(SootField f) {
		HashNodePointsToSet set = HashNodePointsToSet.empty();
		for (Variable x: pt.keySet()) {
			if (x instanceof ObjectFieldVariable) {
				if (((ObjectFieldVariable) x).getField().equals(f)) {
					set.unionMutable(pt.get(x));
				}
			}
		}
		
		return set;
	}

	@Override
	public PointsToSet reachingObjects(Context c, Local l) {
		return reachingObjects(l);
	}

	@Override
	public PointsToSet reachingObjects(PointsToSet ps, SootField f) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PointsToSet reachingObjects(Local l, SootField f) {
		Variable x = getVariable(l);
		if (x != null) {
			HashNodePointsToSet reaching = HashNodePointsToSet.empty();
			if (pt.containsKey(x)) {
				for (Node oi : pt.get(x).getNodes()) {
					ObjectFieldVariable obj = new ObjectFieldVariable(oi, f);
					if (pt.containsKey(obj)) {
						for (Node of : pt.get(obj).getNodes()) {
							reaching.add(of);
						}
					}
					
				}
				return reaching;
			}
		}
		
		return HashNodePointsToSet.emptyRef();
	}

	@Override
	public PointsToSet reachingObjects(Context c, Local l, SootField f) {
		return reachingObjects(l, f);
	}

	@Override
	public PointsToSet reachingObjectsOfArrayElement(PointsToSet ps) {
		throw new UnsupportedOperationException();
	}

	static class SymbolicObject {
		private static int counter = 0;

		public static int fresh() {
			return ++counter;
		}
	}

}
