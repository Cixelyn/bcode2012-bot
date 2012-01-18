package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;

public class SoldierRobotHT extends BaseRobot {
	boolean checkedBehind = false;
	
	public SoldierRobotHT(RobotController myRC) {
		super(myRC);
		nav.setNavigationMode(NavigationMode.GREEDY);
		io.setChannels(new BroadcastChannel[] {BroadcastChannel.ALL, BroadcastChannel.SOLDIERS});
		fbs.setBattleMode();
	}

	@Override
	public void run() throws GameActionException {
		MapLocation closestArchon = dc.getClosestArchon();
		nav.setDestination(closestArchon);
		
		if(!rc.isAttackActive()) {
			RobotInfo closestEnemy = dc.getClosestEnemy();
			if (closestEnemy != null && rc.canAttackSquare(closestEnemy.location)) {
				rc.attackSquare(closestEnemy.location, closestEnemy.robot.getRobotLevel());
			}
		} 
	}
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) {
		int[] data = null;
		switch(msgType) {
		case MAP_EDGES:
			data = BroadcastSystem.decodeUShorts(sb);
			ses.receiveMapEdges(data);
			break;
		case MAP_FRAGMENTS:
			data = BroadcastSystem.decodeInts(sb);
			ses.receiveMapFragment(data);
			break;
		case POWERNODE_FRAGMENTS:
			data = BroadcastSystem.decodeInts(sb);
			ses.receivePowerNodeFragment(data);
			break;
		} 
	}
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		if(rc.getFlux()<1) return null;
		if(!checkedBehind) {
			checkedBehind = true;
			return new MoveInfo(curDir.opposite());
		} else {
			Direction dir = nav.navigateToDestination();
			if(dir==null) return null;
			MapLocation loc = curLoc.add(dir);
			if(rc.canSenseSquare(loc) && rc.senseObjectAtLocation(loc, RobotLevel.POWER_NODE)!=null)
				return null;
			return new MoveInfo(dir, false);
		}
	}
}
