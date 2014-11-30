package edu.iastate.coms641;

import java.util.HashSet;
import java.util.Set;

import soot.PointsToSet;
import soot.Type;
import soot.jimple.ClassConstant;

class HashNodePointsToSet implements PointsToSet {
	private Set<Node> nodes;
	private Set<String> possibleStrings = new HashSet<String>();
	private Set<ClassConstant> possibleClassConstants = new HashSet<ClassConstant>();
	private static HashNodePointsToSet emptyRefSet = new EmptyHashNodePointsToSet();
	
	private HashNodePointsToSet() {
		nodes = new HashSet<Node>();
	}
	
	public HashNodePointsToSet(HashNodePointsToSet other) {
		if (other != null) {
			nodes = new HashSet<Node>(other.nodes);
		} else {
			nodes = new HashSet<Node>();
		}
	}
	
	public static HashNodePointsToSet empty() {
		return new HashNodePointsToSet();
	}
	
	public static HashNodePointsToSet emptyRef() {
		return emptyRefSet;
	}
	
	public static HashNodePointsToSet copy(HashNodePointsToSet other) {
		return new HashNodePointsToSet(other);
	}
	
	void add(Node node) {
		nodes.add(node);
	}
	
	void addString(String str) {
		possibleStrings.add(str);
	}
	
	void addClassConstant(ClassConstant c) {
		possibleClassConstants.add(c);
	}
	
	public HashNodePointsToSet union(HashNodePointsToSet other) {
		HashNodePointsToSet newSet = copy(this);
		if (other != null) {
			newSet.nodes.addAll(other.nodes);
		}
		
		return newSet;
	}
	
	public void unionMutable(HashNodePointsToSet other) {
		if (other != null) {
			nodes.addAll(other.nodes);
		}
	}
	
	public HashNodePointsToSet diff(HashNodePointsToSet other) {
		HashNodePointsToSet newSet = copy(this);
		if (other != null) {
			newSet.nodes.removeAll(other.nodes);
		}
		
		return newSet;
	}
	
	public Set<Node> getNodes() {
		return nodes;
	}
	
	@Override
	public boolean hasNonEmptyIntersection(PointsToSet other) {
		if (other instanceof HashNodePointsToSet) {
			HashNodePointsToSet otherHash = (HashNodePointsToSet) other;
			Set<Node> checkSet = new HashSet<Node>(nodes);
			checkSet.retainAll(otherHash.nodes);
			return !checkSet.isEmpty();
		}
		return false;
	}

	@Override
	public boolean isEmpty() {
		return nodes.isEmpty();
	}

	@Override
	public Set<ClassConstant> possibleClassConstants() {
		return possibleClassConstants;
	}

	@Override
	public Set<String> possibleStringConstants() {
		return possibleStrings;
	}

	@Override
	public Set<Type> possibleTypes() {
		Set<Type> typesSet = new HashSet<Type>();
		for (Node node : nodes) {
			typesSet.add(node.getVariableType());
		}
		
		return typesSet;
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("Nodes: " + nodes.toString());
		if (!possibleStrings.isEmpty()) {
			str.append(", Possible Strings" + possibleStrings);
		}
		if (!possibleClassConstants.isEmpty()) {
			str.append(", Possible ClassConstants" + possibleClassConstants);
		}
		
		return str.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
		result = prime
				* result
				+ ((possibleClassConstants == null) ? 0
						: possibleClassConstants.hashCode());
		result = prime * result
				+ ((possibleStrings == null) ? 0 : possibleStrings.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HashNodePointsToSet other = (HashNodePointsToSet) obj;
		if (nodes == null) {
			if (other.nodes != null)
				return false;
		} else if (!nodes.equals(other.nodes))
			return false;
		if (possibleClassConstants == null) {
			if (other.possibleClassConstants != null)
				return false;
		} else if (!possibleClassConstants.equals(other.possibleClassConstants))
			return false;
		if (possibleStrings == null) {
			if (other.possibleStrings != null)
				return false;
		} else if (!possibleStrings.equals(other.possibleStrings))
			return false;
		return true;
	}



	static class EmptyHashNodePointsToSet extends HashNodePointsToSet {
		void add(Node node) {
			throw new UnsupportedOperationException("Immutable set");
		}
		
		
		public void unionMutable(HashNodePointsToSet other) {
			throw new UnsupportedOperationException("Immutable set");
		}
		
	}
	
}
