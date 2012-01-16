package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class ArchonRobotYP extends BaseRobot {

	public ArchonRobotYP(RobotController myRC) {
		super(myRC);
		nodes[0] = rc.sensePowerCore().getLocation();
	}

	public void run() throws GameActionException {
		runYPBUGCODE(); if (true) return;
	}

	void runYPBUGCODE() throws GameActionException
	{
//		System.out.println("ducks");
//		rc.setDirection(currDir.rotateLeft());
//		rc.setIndicatorString(0, "round "+currRound);
		
//		rc.yield();
//		rc.setIndicatorString(2, "no add");
		rc.setIndicatorString(2, "target "+nodes[nodeindex]);
		
		if (nodes[nodeindex]==null) //reset
		{
			System.out.println("reset");
			nodeindex = 0;
			nodesize = 1;
			nodes[0] = rc.sensePowerCore().getLocation();
		}
		
		MapLocation cur = rc.getLocation();
		if (cur.isAdjacentTo(nodes[nodeindex]))
		{
			rc.setIndicatorString(0, "round "+currRound+" a"+1);
			System.out.println("next to "+nodes[nodeindex]);
			GameObject go = rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.ON_GROUND);
			RobotInfo ri;
			boolean done = false;
			if (go==null){
				while (rc.isMovementActive()) rc.yield();
				rc.setDirection(cur.directionTo(nodes[nodeindex]));
				Direction ddd = cur.directionTo(nodes[nodeindex]);
				System.out.println("facing "+nodes[nodeindex]);
				while (rc.getFlux() < RobotType.TOWER.spawnCost) rc.yield();
				System.out.println("has flux for "+nodes[nodeindex]);
				
				rc.setIndicatorString(0, "round "+currRound+" a"+2);
				while ((!rc.canMove(ddd)) || rc.isMovementActive()) {
					go = rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.ON_GROUND);
					if (go!=null)
					{
						ri = rc.senseRobotInfo((Robot) go);
						if (ri.type == RobotType.TOWER)
						{
							done = true;
							break;
						} 
					}
					rc.yield();
				}
				if (!done)
				{
					System.out.println("spawning "+nodes[nodeindex]);
					rc.spawn(RobotType.TOWER);
//					PowerNode pn = (PowerNode)rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.POWER_NODE);
//					MapLocation[] neighbors = pn.neighbors();
//					addNewLocs(neighbors);
				}
			}
			rc.setIndicatorString(0, "round "+currRound+" a"+3);
			PowerNode pn = (PowerNode)rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.POWER_NODE);
			MapLocation[] neighbors = pn.neighbors();
			addNewLocs(neighbors);
			nodeindex++;
		} else if (rc.canSenseSquare(nodes[nodeindex]))
		{
			rc.setIndicatorString(0, "round "+currRound+" a"+4);
			GameObject go = rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.ON_GROUND);
			if (go!=null)
			{
				PowerNode pn = (PowerNode)rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.POWER_NODE);
				MapLocation[] neighbors = pn.neighbors();
				addNewLocs(neighbors);
				nodeindex++;
			}
		}
		
		if (nodeindex<nodesize && !rc.isMovementActive())
		{

			// TODO(jven): i made your bug vars private in BlindBug, move these
			// indicator strings there if you still need them
			//rc.setIndicatorString(2, "target "+nodes[nodeindex]);
			//rc.setIndicatorString(0, "round "+currRound+" a"+5);
			int bytecode = Clock.getBytecodeNum();
			//rc.setIndicatorString(1, "before move "+bytecode+" cur "+currLoc);
//			rc.setIndicatorString(1, "start:"+bugStart+" end:"+bugTarget+" cw:"+bugCW+" cur:"+currLoc+" obs:"+bugObs);
			
			nav.setNavigationMode(NavigationMode.BUG);
			nav.setDestination(nodes[nodeindex]);
			Direction d = nav.navigateToDestination();
			//Direction d = nv.navigateTo(nodes[nodeindex]);
			if (currDir == d)
				rc.moveForward();
			else
				rc.setDirection(d);
			
			bytecode = Clock.getBytecodeNum()-bytecode;
//			rc.setIndicatorString(1, "move used "+bytecode);
//			rc.setIndicatorString(0, "end:"+bugStart+" end:"+bugTarget+" cw:"+bugCW+" cur:"+currLoc+" obs:"+bugObs+" move used "+bytecode);
		}
//		rc.setIndicatorString(0, "round "+currRound+" a"+6);
		
	}
	
	
	MapLocation[] nodes = new MapLocation[GameConstants.MAX_POWER_NODES+2];
	int nodesize = 0;
	int nodeindex = 0;
	void addNewLocs(MapLocation[] locs)
	{
		int bytecode = Clock.getBytecodeNum();
		boolean add = false;
		for (int x=0; x<locs.length; x++)
		{
			MapLocation m = locs[x];
			add = true;
			for (int y=0; y<nodesize; y++)
			{
				if (nodes[y].x==m.x&&nodes[y].y==m.y)
				{
					add = false;
					break;
				}
			}
			if (add)
				nodes[nodesize++] = m;
		}
		System.out.println("added, now has "+nodesize+" nodes");
		bytecode = Clock.getBytecodeNum()-bytecode;
		rc.setIndicatorString(2, "adding new loc used "+bytecode+" next "+nodes[nodesize-1]);
	}
}
