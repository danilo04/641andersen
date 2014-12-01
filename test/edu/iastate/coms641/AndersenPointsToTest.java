package edu.iastate.coms641;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import soot.Body;
import soot.G;
import soot.IntegerType;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;

public class AndersenPointsToTest {	
	private static String SOOT_CLASS_PATH = ".:benchmarks/javatests.jar";
	private static Map<String, String> methodToTest = new HashMap<String, String>();
	@Rule public TestName name = new TestName();

	@BeforeClass
	public static void prepareTests() {
		methodToTest.put("test1", "edu.iastate.coms641.tests.Test1");
	}
	
 
	public void runSoot(String testName) {
		Utils.setupSoot(SOOT_CLASS_PATH, testName, "cg.andersen");
	}

	@Before
	public void setUp() throws Exception {
		if (methodToTest.containsKey(name.getMethodName())) {
			runSoot(methodToTest.get(name.getMethodName()));
		}

	}

	@After
	public void tearDown() throws Exception {
		G.v();
		G.reset();
	}

	@Test
	public void test1() {
		PointsToAnalysis pointsTo = Scene.v().getPointsToAnalysis();
		SootClass test1Class = Scene.v().loadClassAndSupport(methodToTest.get("test1"));
		SootMethod test1Main = test1Class.getMethod("void main(java.lang.String[])");
		Body bTest1 = test1Main.getActiveBody();
		InstanceFieldRef test1 = null;
		InstanceFieldRef test2 = null;
		InstanceFieldRef test3 = null;
		for (Unit u : bTest1.getUnits()) {
			if (u instanceof AssignStmt) {
				AssignStmt a = (AssignStmt) u;
				if (a.getRightOp() instanceof InstanceFieldRef && 
						a.getLeftOp() instanceof Local) {
					Local left = (Local) a.getLeftOp();
					if (left.getType() instanceof IntegerType) {
						if (test1 == null) {
							test1 = (InstanceFieldRef) a.getRightOp();
						} else if (test2 == null) {
							test2 = (InstanceFieldRef) a.getRightOp();
						} else {
							test3 = (InstanceFieldRef) a.getRightOp();
						}
					}	
				}
			}
		}
		
		PointsToSet setTest1 = pointsTo.reachingObjects((Local)test1.getBase());
		PointsToSet setTest2 = pointsTo.reachingObjects((Local)test2.getBase());
		PointsToSet setTest3 = pointsTo.reachingObjects((Local)test3.getBase());
		
		String test1Name = test1.getBase().toString();
		String test2Name = test2.getBase().toString();
		String test3Name = test3.getBase().toString();
		
		if ((test1Name.equals("r2") && test2Name.equals("r5")) || 
				(test1Name.equals("r5") && test2Name.equals("r2"))) {
			assertTrue(setTest1.hasNonEmptyIntersection(setTest2));
		} else {
			assertFalse(setTest1.hasNonEmptyIntersection(setTest2));
		}
		
		if ((test1Name.equals("r2") && test3Name.equals("r5")) || 
				(test3Name.equals("r5") && test3Name.equals("r2"))) {
			assertTrue(setTest1.hasNonEmptyIntersection(setTest3));
		} else {
			assertFalse(setTest1.hasNonEmptyIntersection(setTest3));
		}
		
		if ((test3Name.equals("r2") && test2Name.equals("r5")) || 
				(test3Name.equals("r5") && test2Name.equals("r2"))) {
			assertTrue(setTest3.hasNonEmptyIntersection(setTest2));
		} else {
			assertFalse(setTest3.hasNonEmptyIntersection(setTest2));
		}
	
	}
	

}
