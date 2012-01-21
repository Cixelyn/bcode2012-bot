package ypstrategytest;

import battlecode.common.Clock;
import battlecode.common.RobotController;

public class RobotPlayer {
	RobotController rc;
	
	static StrategyQueue sq;
	static StrategyElement[] sa;
	
	static StrategyElement[] ba;
	
	
	public static void dostuff(RobotController rc) throws Exception
	{
		if (!rc.getLocation().equals(rc.senseAlliedArchons()[0])) rc.suicide();
		
		
		TestClass.run();
		rc.resign();
		
		int bytecode;
		
		genStrategyElts();
		StrategyHeap sh = new StrategyHeap();
		int extra = 7;
		bytecode = Clock.getBytecodeNum();
		System.out.println(" took: "+(Clock.getBytecodeNum()-bytecode-extra));
		
		bytecode = Clock.getBytecodeNum();
		int a = 5;
		StrategyElement see = sa[a];
		System.out.println(" took: "+(Clock.getBytecodeNum()-bytecode-extra));
		
		bytecode = Clock.getBytecodeNum();
		boolean larger = see.priority >= sa[a].priority;
		System.out.println(" took: "+(Clock.getBytecodeNum()-bytecode-extra));
		if (larger);
		
		bytecode = Clock.getBytecodeNum();
		int[] priorities = new int[sa.length];
		for (int x=0; x<priorities.length; x++)
			priorities[x] = sa[x].priority;
		System.out.println(" took: "+(Clock.getBytecodeNum()-bytecode-extra));
		
		bytecode = Clock.getBytecodeNum();
		a = 6;
		a = priorities[a];
		System.out.println(" took: "+(Clock.getBytecodeNum()-bytecode-extra));
		
		int a1 = 2;
		int a2 = 3;
		bytecode = Clock.getBytecodeNum();
		priorities[a1] = priorities[a2];
		System.out.println(" took: "+(Clock.getBytecodeNum()-bytecode-extra));
		
		bytecode = Clock.getBytecodeNum();
		StrategyElement[] ses = new StrategyElement[100];
		System.out.println(" took: "+(Clock.getBytecodeNum()-bytecode-extra));
		if (ses.length>9);
		bytecode = Clock.getBytecodeNum();
		System.arraycopy(priorities, a1, priorities, a2, 1);
		System.out.println(" took: "+(Clock.getBytecodeNum()-bytecode-extra));
		
		bytecode = Clock.getBytecodeNum();
		for (int x=0; x<sa.length; x++)
		{
			int b = Clock.getBytecodeNum();
			sh.insert(sa[x]);
			System.out.println("insert took: "+(Clock.getBytecodeNum()-b));
		}
		System.out.println("all inserts took: "+(Clock.getBytecodeNum()-bytecode-extra));
		
		bytecode = Clock.getBytecodeNum();
		for (int x=0; x<sa.length; x++)
		{
			int b = Clock.getBytecodeNum();
			StrategyElement se = sh.pop();
			System.out.println("poped "+se.priority);
			System.out.println("pop took: "+(Clock.getBytecodeNum()-b));
		}
		System.out.println("all pops took: "+(Clock.getBytecodeNum()-bytecode-extra));
		
		
		rc.yield();
		bytecode = Clock.getBytecodeNum();
		StrategyFull s = new FixedStrategyFullTester();
		System.out.println("strategy init took: "+(Clock.getBytecodeNum()-bytecode-extra));
		
		int t1,t2,t3;
		long totalcost = 0;
		for (int x=0; x<250; x++)
		{
			rc.yield();
			t3 = 0;
			t1 = Clock.getBytecodeNum();
			s.initForRound(x*10);
			t2 = Clock.getBytecodeNum();
			t3+=(t2-t1);
			System.out.println("init took: "+(t2-t1));
			t1 = Clock.getBytecodeNum();
			s.act();
			t2 = Clock.getBytecodeNum();
			t3+=(t2-t1);
			System.out.println("act took: "+(t2-t1));
			System.out.println("total: "+t3);
			totalcost += t3;
		}
		System.out.println("total bytecode: "+totalcost);
	}
	
	public static void genStrategyElts()
	{
		int bc = Clock.getBytecodeNum();
		sa = new StrategyElement[] {
			new StrategyElement(StrategyConstants.values[0], 2, 	0, 		1000),
			new StrategyElement(StrategyConstants.values[1], 5, 	0, 		500),
			new StrategyElement(StrategyConstants.values[2], 7, 	300,	650),
			new StrategyElement(StrategyConstants.values[3], 1, 	450, 	500),
			new StrategyElement(StrategyConstants.values[4], 4, 	600, 	700),
			new StrategyElement(StrategyConstants.values[5], 3, 	600, 	1200),
			new StrategyElement(StrategyConstants.values[6], 9, 	630, 	900),
			new StrategyElement(StrategyConstants.values[7], 2, 	1000, 	1500),
		};
		System.out.println("gen array: "+(Clock.getBytecodeNum()-bc));
		bc = Clock.getBytecodeNum();
		sq = new StrategyQueue(sa);
		System.out.println("gen queue: "+(Clock.getBytecodeNum()-bc));
	}
	
	static StrategyElement[] baseStrategy;
	static StrategyQueue baseQueue;
	static StrategyQueue baseRest;
	static StrategyElement[] roundStrategy;
	static StrategyElement[] newRoundStrategy;
	static int baseIndex;
	static int roundIndex;
	static int roundSize;
	static int newRoundIndex;
	static int newRoundSize;
	
	public static void prepareInitialVarsA()
	{
		baseStrategy = sa;
		roundStrategy = new StrategyElement[StrategyConstants.MAX_ROUND_STRATEGY_ELEMENTS];
		newRoundStrategy = new StrategyElement[StrategyConstants.MAX_NEWROUND_STRATEGY_ELEMENTS];
		baseIndex = 0;
		roundIndex = 0;
		roundSize = 0;
		newRoundIndex = 0;
		newRoundSize = 0;
	}

	public static void run(RobotController rc) {
		try {
			dostuff(rc);
			rc.resign();
		} catch (Exception e) {
			System.out.println("caught exception:");
			e.printStackTrace();
			rc.resign();
		}
	}
}
