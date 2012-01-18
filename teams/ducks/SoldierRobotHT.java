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
		io.setAddresses(new String[] {"#x", "#s"});
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
	public void processMessage(char msgType, StringBuilder sb) {
		int[] data = null;
		if(msgType=='e') {
			data = BroadcastSystem.decodeShorts(sb);
			ses.receiveMapEdges(data);
		} else if(msgType=='m') {
			data = BroadcastSystem.decodeInts(sb);
			ses.receiveMapFragment(data);
		} else if(msgType=='p') {
			data = BroadcastSystem.decodeInts(sb);
			ses.receivePowerNodeFragment(data);
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
