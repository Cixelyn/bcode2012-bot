package brutalai;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class FunBot extends BaseRobot {

	MapLocation target;
	int aid=-1;
	int spawned;
	
	static final int[][][] lol = new int[][][]
	{
	{	{ 12,  6,  3,  2,  1},
		{ 11,  0,  0,  0,  0},
		{ 10,  7, 13,  5,  4},
		{  9,  0,  0,  0,  0},
		{  8,  0,  0,  0,  0},
	},
	{	{ 13,  0,  0,  0,  1},
		{ 12,  0,  0,  0,  2},
		{ 11,  0,  0,  0,  3},
		{ 10,  0,  0,  0,  4},
		{  9,  8,  7,  6,  5},
	},
	{	{  8,  0,  0,  0,  1},
		{ 12,  7,  0,  0,  2},
		{ 11,  0, 13,  0,  3},
		{ 10,  0,  0,  6,  4},
		{  9,  0,  0,  0,  5},
	},
	{	{  0, 10,  3,  2,  1},
		{ 14,  0,  0,  0,  0},
		{ 13,  0, 15,  7,  6},
		{ 12,  0,  0,  0,  5},
		{  0, 11,  9,  8,  4},
	},
//	{	{  0,  1,  3,  2,  0},
//		{ 11,  0,  0,  0,  0},
//		{ 10,  0, 12,  6,  0},
//		{  9,  0,  0,  5,  0},
//		{  0,  8,  7,  4,  0},
//	},
	{	{  9, 10, 11,  6,  5},
		{ 15,  0, 16,  0,  4},
		{ 14,  0, 17,  0,  3},
		{ 13,  0,  8,  0,  2},
		{ 12,  0,  7,  0,  1},
	},
	{	{  7, 12, 11, 10,  9},
		{  0,  0,  0,  8,  0},
		{  0,  0, 13,  0,  0},
		{  0,  6,  0,  0,  0},
		{  5,  4,  3,  2,  1},
	},
	};
	
	static final int[][] lol2 = new int[][]
	{
		{ -36, -2, 0},
		{ -30, -2, 0},
		{ -24, -2, 0},
		{ -18, -2, 0},
		{ -12, -2, 0},
		{  -6, -2, 0},
	};
	
	public FunBot(RobotController myRC) throws GameActionException
	{
		super(myRC);
		nav.setNavigationMode(NavigationMode.GREEDY);
		fbs.setPoolMode();
	}

	@Override
	public void run() throws GameActionException
	{
		switch (myType)
		{
		case ARCHON:
			lolzors();
			break;
		case SCOUT:
			flyingducks();
			fbs.manageFlux();
			break;
		}
	}
	
	public void lolzors() throws GameActionException
	{
		if (target==null)
		{
			aid = 0;
			MapLocation[] locs = dc.getAlliedArchons();
			while (!curLoc.equals(locs[aid])) aid++;
			target = myHome.add(lol2[aid][0], lol2[aid][1]);
			for (int y=0; y<lol[aid].length; y++)
				for (int x=0; x<lol[aid][y].length; x++)
					if (lol[aid][y][x]>spawned) spawned = lol[aid][y][x];
			fbs.setPoolMode();
		}
		rc.setIndicatorString(0, ""+spawned);
		if (rc.getFlux()>80) fbs.manageFlux();
		if (spawned==0)
		{
			fbs.manageFlux();
			if (curRound > 2000) rc.suicide();
		}
	}
	
	public void flyingducks() throws GameActionException
	{
		if (aid==-1)
		{
			aid = 0;
			MapLocation[] locs = dc.getAlliedArchons();
			while (!curLoc.isAdjacentTo(locs[aid])) aid++;
			int xx = locs[aid].x;
			int yy = locs[aid].y;
			int count = 1;
			Robot[] gos = rc.senseNearbyGameObjects(Robot.class);
			for (int x=0; x<gos.length; x++)
			{
				RobotInfo ri = rc.senseRobotInfo(gos[x]);
				if (ri.team!=myTeam) continue;
				if (ri.type!=myType) continue;
				if (ri.location.x<xx-1 || ri.location.x>xx+3) continue;
				if (ri.location.y<yy-1 || ri.location.y>yy+3) continue;
				count++;
			}
			rc.setIndicatorString(0, ""+aid);
			rc.setIndicatorString(1, ""+count);
			int x = 0; xx = 0;
			int y = 0; yy = 0;
			for (y=0; y<lol[aid].length; y++)
				for (x=0; x<lol[aid][y].length; x++)
					if (lol[aid][y][x]==count)
					{
						yy = y;
						xx = x;
						break;
					}
			
			if (lol[aid][yy][xx]!=count)
				rc.suicide();
			target = locs[aid].add(xx, yy).add(Direction.NORTH_WEST);
			
			rc.setIndicatorString(2, ""+target);
		}
//		if (curLoc.equals(target)) (new HibernationSystem(this)).run();
		if (curLoc.equals(target)) while (true) {
			while (rc.isMovementActive()) rc.yield();
			while (true) { rc.setDirection(rc.getDirection().rotateRight()); rc.yield(); }
		}
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException
	{
		switch (myType)
		{
		case ARCHON:
			if (target==null) return null;
			else if (!curLoc.equals(target)) return new MoveInfo(curLoc.directionTo(target), false);
			else
			{
				if (curDir!=Direction.SOUTH_EAST) return new MoveInfo(Direction.SOUTH_EAST);
				if (spawned == 0) return null;
				if (rc.getFlux()>RobotType.SCOUT.spawnCost+10) {
					 spawned--;
					 return new MoveInfo(RobotType.SCOUT, curDir);
				}
			}
			break;
		case SCOUT:
			if (target==null || curLoc.equals(target)) return new MoveInfo(Direction.NORTH);
			return new MoveInfo(curLoc.directionTo(target),false);
		}
		return null;
	}
}
