package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;

public class SoldierRobotHT extends BaseRobot {
	boolean checkedBehind = false;
	int keepTargetTurns;
	MapLocation target;
	MapLocation previousTarget;
	
	public SoldierRobotHT(RobotController myRC) {
		super(myRC);
		
		keepTargetTurns = -1;
		nav.setNavigationMode(NavigationMode.GREEDY);
		io.setChannels(new BroadcastChannel[] {BroadcastChannel.ALL, BroadcastChannel.SOLDIERS});
		fbs.setBattleMode();
	}

	@Override
	public void run() throws GameActionException {
		
		fbs.manageFlux();
		
		if(!rc.isAttackActive()) {
			RobotInfo closestEnemy = dc.getClosestEnemy();
			if(closestEnemy != null) {
				target = closestEnemy.location;
				nav.setNavigationMode(NavigationMode.GREEDY);
				keepTargetTurns = 20;
			}
			if (closestEnemy != null && rc.canAttackSquare(closestEnemy.location)) {
				rc.attackSquare(closestEnemy.location, closestEnemy.robot.getRobotLevel());
			}
		} 
	}
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		if(rc.getFlux()<1) return null;
		keepTargetTurns--;
		if(keepTargetTurns < 0) {
			target = dc.getClosestArchon();
			if(target.distanceSquaredTo(curLoc)>49) {
				nav.setNavigationMode(NavigationMode.BUG);
			} else {
				nav.setNavigationMode(NavigationMode.GREEDY);
				if(rc.canSenseSquare(target)) {
					RobotInfo ri = rc.senseRobotInfo((Robot)rc.senseObjectAtLocation(
							target, RobotLevel.ON_GROUND));
					target = target.add(ri.direction, 5);
					keepTargetTurns = 15;
				} 
			}
		}
		if(nav.getNavigationMode()!=NavigationMode.BUG || target.distanceSquaredTo(previousTarget)>25)
			nav.setDestination(target);
		previousTarget = target;
		
		
		if(keepTargetTurns>0 && curLoc.distanceSquaredTo(target) <= 2) {
			return new MoveInfo(curLoc.directionTo(target).opposite(), true);
		}
		if(!checkedBehind) {
			checkedBehind = true;
			return new MoveInfo(curDir.opposite());
		} else {
			Direction dir = nav.navigateToDestination();
			if(dir==null) return null;
			MapLocation loc = curLoc.add(dir);
			if(rc.canSenseSquare(loc) && rc.senseObjectAtLocation(loc, RobotLevel.POWER_NODE)!=null)
				return null;
			checkedBehind = false;
			return new MoveInfo(dir, false);
		}
	}
}
