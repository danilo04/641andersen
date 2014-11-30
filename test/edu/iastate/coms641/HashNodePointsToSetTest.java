package edu.iastate.coms641;

import static org.junit.Assert.*;

import org.junit.Test;

import soot.jimple.StringConstant;

public class HashNodePointsToSetTest {
	Node node1 = new Node(1, StringConstant.v("test1").getType());
	Node node2 = new Node(2, StringConstant.v("test2").getType());
	Node node3 = new Node(3, StringConstant.v("test3").getType());
	Node node4 = new Node(4, StringConstant.v("test4").getType());
	Node node5 = new Node(5, StringConstant.v("test5").getType());
	Node node6 = new Node(6, StringConstant.v("test6").getType());
	Node node7 = new Node(7, StringConstant.v("test7").getType());
	Node node8 = new Node(8, StringConstant.v("test8").getType());

	@Test
	public void testCopyInmutable() {
		HashNodePointsToSet test1 = HashNodePointsToSet.empty();
		test1.add(new Node(1, StringConstant.v("test1").getType()));
		HashNodePointsToSet test12 = HashNodePointsToSet.copy(test1);
		test12.add(new Node(2, StringConstant.v("test1").getType()));
		
		assertEquals(1, test1.getNodes().size());
		assertEquals(2, test12.getNodes().size());
		
		test1.add(new Node(3, StringConstant.v("test1").getType()));
		assertEquals(2, test1.getNodes().size());
		assertEquals(2, test12.getNodes().size());
	}
	
	@Test
	public void testUnion() {
		HashNodePointsToSet test1 = HashNodePointsToSet.empty();
		test1.add(node1);
		test1.add(node2);
		
		HashNodePointsToSet test12 = HashNodePointsToSet.copy(test1);
		test12.add(node1);
		test12.add(node2);
		test12.add(node3);
		test12.add(node4);
		test12.add(node5);
		
		test1.add(node6);
		test1.add(node7);
		test12.add(node8);
		
		HashNodePointsToSet result = test1.union(test12);
		HashNodePointsToSet expected = HashNodePointsToSet.empty();
		expected.add(node1);
		expected.add(node2);
		expected.add(node3);
		expected.add(node4);
		expected.add(node5);
		expected.add(node6);
		expected.add(node7);
		expected.add(node8);
		assertEquals(expected, result);
		
		result = test12.union(test1);
		assertEquals(expected, result);
		
		test1 = HashNodePointsToSet.empty();
		test1.add(node1);
		test1.add(node2);
		test12 = HashNodePointsToSet.empty();
		test12.add(node4);
		test12.add(node6);
		
		result = test1.union(test12);
		expected = HashNodePointsToSet.empty();
		expected.add(node1);
		expected.add(node2);
		expected.add(node4);
		expected.add(node6);
		assertEquals(expected, result);
	}
	
	@Test
	public void testUnionEmpty() {
		HashNodePointsToSet test1 = HashNodePointsToSet.empty();
		test1.add(node1);
		test1.add(node2);
		
		HashNodePointsToSet result = test1.union(null);
		
		HashNodePointsToSet expected = HashNodePointsToSet.empty();
		expected.add(node1);
		expected.add(node2);
		assertEquals(expected, result);
		
		result = test1.union(HashNodePointsToSet.empty());
		assertEquals(expected, result);
	}
	
	@Test
	public void testUnionMutable() {
		HashNodePointsToSet test1 = HashNodePointsToSet.empty();
		test1.add(node1);
		test1.add(node2);
		test1.add(node6);
		test1.add(node7);
		
		HashNodePointsToSet test12 = HashNodePointsToSet.empty();
		test12.add(node3);
		test12.add(node4);
		test12.add(node5);
		test12.add(node8);
		
		test1.unionMutable(test12);
		
		HashNodePointsToSet expected = HashNodePointsToSet.empty();
		expected.add(node1);
		expected.add(node2);
		expected.add(node3);
		expected.add(node4);
		expected.add(node5);
		expected.add(node6);
		expected.add(node7);
		expected.add(node8);
		assertEquals(expected, test1);
		
		test1 = HashNodePointsToSet.empty();
		test1.add(node1);
		test1.add(node2);
		test12 = HashNodePointsToSet.empty();
		test12.add(node4);
		test12.add(node6);
		
		test12.unionMutable(test1);
		
		expected = HashNodePointsToSet.empty();
		expected.add(node1);
		expected.add(node2);
		expected.add(node4);
		expected.add(node6);
		assertEquals(expected, test12);
	}
	
	@Test
	public void testUnionMutableEmpty() {
		HashNodePointsToSet test1 = HashNodePointsToSet.empty();
		test1.add(node1);
		test1.add(node2);
		
		test1.unionMutable(null);
		
		HashNodePointsToSet expected = HashNodePointsToSet.empty();
		expected.add(node1);
		expected.add(node2);
		assertEquals(expected, test1);
		
		test1.unionMutable(HashNodePointsToSet.empty());
		assertEquals(expected, test1);
	}
	
	@Test
	public void testDiff() {
		HashNodePointsToSet test1 = HashNodePointsToSet.empty();
		test1.add(node1);
		test1.add(node2);
		test1.add(node6);
		test1.add(node7);
		
		HashNodePointsToSet test12 = HashNodePointsToSet.empty();
		test12.add(node1);
		test12.add(node6);
		test12.add(node4);
		test12.add(node5);
		test12.add(node8);
		
		HashNodePointsToSet expected = HashNodePointsToSet.empty();
		expected.add(node2);
		expected.add(node7);
		
		HashNodePointsToSet result = test1.diff(test12);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testDiffEmpty() {
		HashNodePointsToSet test1 = HashNodePointsToSet.empty();
		test1.add(node1);
		test1.add(node2);
		
		HashNodePointsToSet test12 = HashNodePointsToSet.empty();
		HashNodePointsToSet expected = HashNodePointsToSet.empty();
		expected.add(node1);
		expected.add(node2);
		
		HashNodePointsToSet result = test1.diff(test12);
		
		assertEquals(expected, result);
		
		result = test1.diff(null);
		assertEquals(expected, result);
		assertEquals(2, test1.getNodes().size());
	}
	
	

}
