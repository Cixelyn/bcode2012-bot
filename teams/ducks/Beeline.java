package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Beeline extends Navigation {
	
	private final int targetDistance;
	private final boolean moveBackwards;

	public Beeline(BaseRobot myBR, int targetDistance, boolean moveBackwards) {
		super(myBR);
		this.targetDistance = targetDistance;
		this.moveBackwards = moveBackwards;
	}
	
	public Direction navigateTo(MapLocation target) throws GameActionException {
		if (rc.isMovementActive()) {
			return Direction.NONE;
		}
		Direction dir = br.currLoc.directionTo(target);
		int distance = br.currLoc.distanceSquaredTo(target);
		if (dir == Direction.OMNI) {
			return Direction.OMNI;
		}
		if (br.currDir != dir) {
//			rc.setDirection(dir);
			return dir;
		} else if (distance < targetDistance) {
			if (moveBackwards && rc.canMove(br.currDir.opposite())) {
//				rc.moveBackward();
				return br.currDir.opposite();
			}
		} else {
			if (rc.canMove(br.currDir)){
//				rc.moveForward();
				return br.currDir;
			}
		}
		return Direction.NONE;
	}
}
