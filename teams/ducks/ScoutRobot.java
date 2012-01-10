package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
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
				rc.setIndicatorString(2, "RobotState." + currState +
						" not implemented.");
				break;
		}
	}
	
	private void follow() throws GameActionException {
		if (rc.isMovementActive()) {
			return;
		}
		Direction dir = currLoc.directionTo(target);
		if (currDir == dir) {
			if (rc.canMove(currDir)) {
				rc.moveForward();
			}
		} else {
			rc.setDirection(dir);
		}
	}
}
