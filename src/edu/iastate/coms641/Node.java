package edu.iastate.coms641;

import soot.Type;

class Node {
	int obj;
	Type type;

	public Node(int obj, Type type) {
		this.obj = obj;
		this.type = type;
	}

	public int getObject() {
		return obj;
	}

	public Type getVariableType() {
		return type;
	}
	
	@Override
	public String toString() {
		return type.toString() + ": " + obj;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + obj;
		result = prime * result + ((type == null) ? 0 : type.hashCode());

		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (!(o instanceof Node))
			return false;
		Node other = (Node) o;
		if (obj != other.obj)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;

		return true;
	}
}