package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class SoldierRobot extends BaseRobot {

	public SoldierRobot(RobotController myRC) {
		super(myRC);
		this.currState = RobotState.DIZZY;
	}

	@Override
	public void run() throws GameActionException {
		switch (this.currState) {
			case DIZZY:
				dizzy();
				break;
			case MICRO:
				micro();
				break;
			default:
				break;
		}
	}
	
	private void dizzy() throws GameActionException {
		// if enemy in range, micro... spin otherwise
		boolean enemySighted = false;
		for (Robot r : dc.getNearbyRobots()) {
			// TODO(jven): consider towers not adjacent to one of ours
			if (r.getTeam() != myTeam) {
				enemySighted = true;
				break;
			}
		}
		if (enemySighted) {
			currState = RobotState.MICRO;
		} else if (!rc.isMovementActive()) {
			rc.setDirection(currDir.rotateLeft().rotateLeft().rotateLeft());
		}
	}
	
	private void micro() throws GameActionException {
		int closestDistance = (
				GameConstants.MAP_MAX_HEIGHT + GameConstants.MAP_MAX_WIDTH);
		RobotInfo closestEnemy = null;
		for (Robot r : dc.getNearbyRobots()) {
			if (r.getTeam() != myTeam) {
				// TODO(jven): don't shoot at towers not adjacent to one of ours
				RobotInfo rInfo = rc.senseRobotInfo(r);
				int distance = currLoc.distanceSquaredTo(rInfo.location);
				if (distance < closestDistance) {
					closestDistance = distance;
					closestEnemy = rInfo;
				}
			}
		}
		// change state and return if no enemy in sight
		if (closestEnemy == null) {
			currState = RobotState.DIZZY;
			return;
		}
		// shoot if possible
		if (!rc.isAttackActive() && rc.canAttackSquare(closestEnemy.location)) {
			rc.attackSquare(
					closestEnemy.location, closestEnemy.robot.getRobotLevel());
		}
		// dance if possible
		if (!rc.isMovementActive()) {
			Direction dir = currLoc.directionTo(closestEnemy.location);
			if (currDir != dir) {
				rc.setDirection(currDir);
			} else if (closestDistance <= rc.getType().attackRadiusMaxSquared) {
				if (rc.canMove(currDir.opposite())) {
					rc.moveBackward();
				}
			} else {
				if (rc.canMove(currDir)) {
					rc.moveForward();
				}
			}
		}
	}

}
