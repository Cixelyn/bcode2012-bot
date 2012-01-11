package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.RobotController;

public class ScoutRobot extends BaseRobot {
	
	public MapLocation target;

	public ScoutRobot(RobotController myRC) {
		super(myRC);
		this.currState = RobotState.FOLLOW;
		this.target = rc.getLocation().add(-20, -50);
	}

	@Override
	public void run() throws GameActionException {
		switch (currState) {
			case FOLLOW:
				follow();
				break;
			default:
				break;
		}
	}
	
	private void follow() throws GameActionException {
		if (rc.isMovementActive()) {
			return;
		}
		// get closest archon
		int closestDistance = (
				GameConstants.MAP_MAX_HEIGHT + GameConstants.MAP_MAX_WIDTH);
		MapLocation closestArchon = currLoc;
		for (MapLocation archon : dc.getAlliedArchons()) {
			int distance = currLoc.distanceSquaredTo(archon);
			if (distance < closestDistance) {
				closestDistance = distance;
				closestArchon = archon;
			}
		}
		this.target = closestArchon;
		// go to target
		Direction dir = currLoc.directionTo(target);
		if (dir == Direction.OMNI) {
			return;
		} else if (currDir == dir) {
			if (rc.canMove(currDir)) {
				rc.moveForward();
			}
		} else {
			rc.setDirection(dir);
		}
	}
}
