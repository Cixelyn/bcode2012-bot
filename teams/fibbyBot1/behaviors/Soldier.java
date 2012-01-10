package fibbyBot1.behaviors;

import battlecode.common.*;

public class Soldier extends Unit{
	
	public Soldier(RobotController RC) {
		super(RC);
	}
	
	public void run() throws GameActionException {
		// set rally
		this.setRally();
		// attack command
		MapLocation enemyLoc = null;
		if (!myRC.isAttackActive()) {
			RobotInfo nearbyEnemyInfo = this.util.senseNearbyEnemy();
			if (nearbyEnemyInfo != null) {
				enemyLoc = nearbyEnemyInfo.location;
			}
		}
		// movement commands
		if (!myRC.isMovementActive()) {
			if (enemyLoc != null) {
				this.micro(enemyLoc);
			} else {
				this.util.navigate(this.target);
			}
		}
	}
	
	public void micro(MapLocation enemyLoc) throws GameActionException {
		Direction dir = myRC.getLocation().directionTo(enemyLoc);
		if (myRC.getDirection() != dir) {
			myRC.setDirection(dir);
		} else if (
				myRC.getLocation().distanceSquaredTo(enemyLoc) <=
				myRC.getType().attackRadiusMaxSquared) {
			if (myRC.canMove(dir.opposite())) {
				myRC.moveBackward();
			}
		} else {
			if (myRC.canMove(dir)) {
				myRC.moveForward();
			}
		}
	}
}
