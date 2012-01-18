package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldierRobotHT extends BaseRobot {
	boolean checkedBehind = false;
	boolean aboutToMove = false;
	Direction computedMoveDir = null;
	int turnsStuck = 0;
	
	public SoldierRobotHT(RobotController myRC) {
		super(myRC);
		nav.setNavigationMode(NavigationMode.GREEDY);
		fbs.setBattleMode();
	}

	@Override
	public void run() throws GameActionException {
		MapLocation closestArchon = dc.getClosestArchon();
		nav.setDestination(closestArchon);
		
		if(!rc.isMovementActive() && rc.getFlux()>0.4) {
			if(aboutToMove)  {
				if(rc.canMove(curDir)) {
					if(mc.isPowerNode(curLocInFront)) {
						if(rc.canMove(curDir.opposite())) {
							rc.moveBackward();
							checkedBehind = false;
							aboutToMove = false;
						}
					} else {
						rc.moveForward();
						checkedBehind = false;
						aboutToMove = false;
					}
				} else {
					Direction dir = nav.wiggleToMovableDirection(computedMoveDir);
					if(dir!=null) {
						rc.setDirection(dir);
					} else {
						turnsStuck++;
						if(turnsStuck>10) {
							rc.setDirection(nav.navigateCompletelyRandomly());
						}
					}
				}
			} else if(!checkedBehind) {
				rc.setDirection(curDir.opposite());
				checkedBehind = true;
			} else {
				computedMoveDir = nav.navigateToDestination();
				if(computedMoveDir!=null) {
					Direction dir = nav.wiggleToMovableDirection(computedMoveDir);
					if(dir!=null) {
						rc.setDirection(dir);
						turnsStuck = 0;
						aboutToMove = true;
					}
				}
			}
		}
		
		if(!rc.isAttackActive()) {
			RobotInfo closestEnemy = dc.getClosestEnemy();
			if (closestEnemy != null && rc.canAttackSquare(closestEnemy.location)) {
				rc.attackSquare(closestEnemy.location, closestEnemy.robot.getRobotLevel());
			}
		} 
	}
	

}
