package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ArchonRobotYP extends BaseRobot{
	private enum StrategyState {
		/** Initial split. */
		SPLIT,
		/** Seek and destroy towards a target. */
		RUSH, 
		/** Hold a position. */
		DEFEND,
		/** Take power nodes. */
		CAP,
	}
	private enum BehaviorState {
		/** No enemies to deal with. */
		SWARM,
		/** Run away from enemy forces. */
		RETREAT, 
		/** Fight the enemy forces. Micro, maybe kite. */
		BATTLE, 
		/** Track enemy's last position and keep following them. */
		CHASE,
	}
	int myArchonID;
	
	/** round we are releasing our lock */
	int roundLockTarget;
	int roundStartWakeupMode;
	MapLocation target;
	Direction targetDir;
	StrategyState strategy;
	BehaviorState behavior;
	MapLocation previousWakeupTarget;
	MapLocation enemySpottedTarget;
	
	static final int RETREAT_RADIUS = 4;
	static final int RETREAT_DISTANCE = 6;
	static final int CHASE_COMPUTE_RADIUS = 7;
	MapLocation lastPowerNodeGuess;
	
	
	public ArchonRobotYP(RobotController myRC) throws GameActionException {
		super(myRC);
		
		roundLockTarget = -Integer.MAX_VALUE;
		// compute archon ID
		MapLocation[] alliedArchons = dc.getAlliedArchons();
		for(int i=alliedArchons.length; --i>=0; ) {
			if(alliedArchons[i].equals(curLoc)) {
				myArchonID = i;
				break;
			}
		}
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.ARCHONS,
				BroadcastChannel.EXPLORERS
		});
		fbs.setPoolMode();
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		strategy = StrategyState.SPLIT;
		behavior = BehaviorState.BATTLE;
		enemySpottedTarget = null;
		lastPowerNodeGuess = null;
	}
	
	@Override
	public void run() throws GameActionException {
		
	}
	
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
//		switch(msgType) {
//		case SWARM_TARGET:
//			int[] shorts = BroadcastSystem.decodeUShorts(sb);
//			if(shorts[0]==2 && enemySpottedTarget==null) {
//				enemySpottedTarget = new MapLocation(shorts[1], shorts[2]);
//			}
//			break;
//		case MAP_EDGES:
//			ses.receiveMapEdges(BroadcastSystem.decodeUShorts(sb));
//			break;
//		case MAP_FRAGMENTS:
//			ses.receiveMapFragment(BroadcastSystem.decodeInts(sb));
//			break;
//		case POWERNODE_FRAGMENTS:
//			ses.receivePowerNodeFragment(BroadcastSystem.decodeInts(sb));
//			break;
//		case INITIAL_REPORT:
//			int[] data = BroadcastSystem.decodeUShorts(sb);
//			//int initialReportTime = data[0];
//			MapLocation initialReportLoc = new MapLocation(data[1], data[2]);
//			enemySpottedTarget = initialReportLoc;
//			io.sendUShort(BroadcastChannel.SCOUTS, BroadcastType.INITIAL_REPORT_ACK, 0);
//			break;
//		default:
//			super.processMessage(msgType, sb);
//		} 
	}
	
	
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		
		if (curRound < 36)
		{
			if (curLoc.distanceSquaredTo(myHome)<16)
				return new MoveInfo(myHome.directionTo(curLoc), true);
		}
		
		MapLocation tar = null;
		switch (myArchonID)
		{
		case 0: tar = myHome.add(Direction.EAST); break;
		case 1: tar = myHome.add(Direction.NORTH_EAST); break;
		case 2: tar = myHome.add(Direction.SOUTH_EAST); break;
		case 3: tar = myHome.add(Direction.WEST); break;
		case 4: tar = myHome.add(Direction.NORTH_WEST); break;
		case 5: tar = myHome.add(Direction.SOUTH_WEST); break;
		}
		nav.setDestination(tar);
		
		if (curLoc.equals(tar))
		{
			Direction sdir = null;
			switch (myArchonID)
			{
			case 0: sdir = Direction.EAST; break;
			case 1: sdir = Direction.NORTH; break;
			case 2: sdir = Direction.SOUTH; break;
			case 3: sdir = Direction.WEST; break;
			case 4: sdir = Direction.NORTH; break;
			case 5: sdir = Direction.SOUTH; break;
			}
			
			if (curDir != (sdir))
				return new MoveInfo(sdir);
			if (rc.canMove(curDir) && rc.getFlux()>270)
				return new MoveInfo(RobotType.SCORCHER, curDir);
			return null;
		}
		
		while (rc.getFlux()<270) rc.yield();
		resetClock();
		
		return new MoveInfo(nav.navigateToDestination(),true);
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
//		if(Clock.getRoundNum()%6==myArchonID) {
//			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>5000)
//				ses.broadcastMapFragment();
//			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3000)
//				ses.broadcastPowerNodeFragment();
//			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>2000) 
//				ses.broadcastMapEdges();
//		}
		super.useExtraBytecodes();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3000) 
			nav.prepare();
//		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1000) 
//			mc.extractUpdatedPackedDataStep();
	}
}
