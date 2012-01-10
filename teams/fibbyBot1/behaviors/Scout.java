package fibbyBot1.behaviors;

import fibbyBot1.Constants;
import battlecode.common.*;

public class Scout extends Unit{
	
	public Scout(RobotController RC) {
		super(RC);
	}

	public void run() throws GameActionException {
		// attack command
		MapLocation enemyLoc = null;
		if (!myRC.isAttackActive()) {
			RobotInfo nearbyEnemyInfo = this.util.senseNearbyEnemy();
			if (nearbyEnemyInfo != null) {
				enemyLoc = nearbyEnemyInfo.location;
			}
		}
		// heal if enemies nearby
		if (enemyLoc != null) {
			double p = Math.random();
			if (p < Constants.REGENERATE_PROBABILITY) {
				this.myRC.regenerate();
			}
		}
		// get location of nearby archon
		for (Message m : this.myRC.getAllMessages()) {
			if (m.ints[0] == 1337) {
				this.target = m.locations[1];
			}
		}
		// gogogo
		if (this.target == null) {
			return;
		}
		if (!this.myRC.isMovementActive()) {
			Direction dir = this.myRC.getLocation().directionTo(this.target);
			if (dir == Direction.OMNI) {
				return;
			} else if (this.myRC.getDirection() != dir) {
				this.myRC.setDirection(dir);
			} else {
				if (this.myRC.canMove(dir)) {
					this.myRC.moveForward();
				}
			}
		}
	}
}
