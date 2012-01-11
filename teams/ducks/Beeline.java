package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Beeline extends Navigation {
	
	private final int minDistance;

	public Beeline(BaseRobot myBR, int distance) {
		super(myBR);
		minDistance = distance;
	}
	
	public void navigateTo(MapLocation target) throws GameActionException {
		if (rc.isMovementActive()) {
			return;
		}
		Direction dir = br.currLoc.directionTo(target);
		int distance = br.currLoc.distanceSquaredTo(target);
		if (dir == Direction.OMNI) {
			return;
		}
		if (br.currDir != dir) {
			rc.setDirection(dir);
		} else if (distance < minDistance) {
			if (rc.canMove(br.currDir.opposite())) {
				rc.moveBackward();
			}
		} else {
			if (rc.canMove(br.currDir)){
				rc.moveForward();
			}
		}
	}
}
