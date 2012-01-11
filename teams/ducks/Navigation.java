package ducks;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class Navigation {
	
	BaseRobot br;
	RobotController rc;
	
	public Navigation(BaseRobot myBR) {
		br = myBR;
		rc = myBR.rc;
	}
	
	public abstract void navigateTo(
			MapLocation destination) throws GameActionException;
}
