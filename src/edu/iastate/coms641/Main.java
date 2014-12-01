package edu.iastate.coms641;

import java.text.NumberFormat;
import java.util.Arrays;

import soot.Body;
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

public class Main {
	static String sootClassPath;
	static String mainClass;

	public static void main(String[] args) {
		System.out.println("Args: " + Arrays.asList(args));
		parseArgs(args);
		if (sootClassPath == null || mainClass == null) {
			usage();
			System.exit(1);
		}
		
		Utils.setupSoot(sootClassPath, mainClass, "cg.spark");
		
		System.out.println(Scene.v().getMainClass());
		PointsToAnalysis pointsTo = Scene.v().getPointsToAnalysis();
		System.out.println("PointsToAnalysis class:" + pointsTo.getClass());
		SootClass test1Class = Scene.v().loadClassAndSupport("edu.iastate.coms641.tests.Test1");
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
		
		System.out.println("Testing values");
		PointsToSet setTest1 = pointsTo.reachingObjects((Local)test1.getBase());
		PointsToSet setTest2 = pointsTo.reachingObjects((Local)test2.getBase());
		PointsToSet setTest3 = pointsTo.reachingObjects((Local)test3.getBase());
		if (setTest1.hasNonEmptyIntersection(setTest2)) {
			System.out.println("Test1: " + test1 + " and test2: " + test2 + " may be alias");
		} else {
			System.out.println("Test1: " + test1 + " and test2: " + test2 + " are not aliases");
		}
		
		if (setTest1.hasNonEmptyIntersection(setTest3)) {
			System.out.println("Test1: " + test1 + " and test3: " + test3 + " may be alias");
		} else {
			System.out.println("Test1: " + test1 + " and test3: " + test3 + " are not aliases");
		}
		
		if (setTest2.hasNonEmptyIntersection(setTest3)) {
			System.out.println("Test2: " + test2 + " and test3: " + test3 + " may be alias");
		} else {
			System.out.println("Test2: " + test2 + " and test3: " + test3 + " are not aliases");
		}
		
		memoryUsage();
		System.out.println("Finished execution.");
	}
	
	static void memoryUsage() {
		Runtime runtime = Runtime.getRuntime();

	    NumberFormat format = NumberFormat.getInstance();

	    StringBuilder sb = new StringBuilder();
	    long maxMemory = runtime.maxMemory();
	    long allocatedMemory = runtime.totalMemory();
	    long freeMemory = runtime.freeMemory();

	    sb.append("free memory: " + format.format((freeMemory / 1024) / 1024) + "mb\n");
	    sb.append("allocated memory: " + format.format((allocatedMemory / 1024) / 1024) + "mb\n");
	    sb.append("max memory: " + format.format((maxMemory / 1024) / 1024) + "mb\n");
	    sb.append("used memory: " + format.format(((allocatedMemory - freeMemory) / 1024) / 1024) + "mb\n");
	    sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "mb\n");
	    System.out.println(sb.toString());
	}
	
	static void usage() {
		System.out.println("Usage: java edu.iastate.coms641.Main -soot-class-path <class-path> -main-class <main-class-name>");
	}
	
	static void parseArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-soot-class-path")) {
				sootClassPath = args[++i];
			} else if (arg.equals("-main-class")) {
				mainClass = args[++i];
			}
		}
	} 
	
}
