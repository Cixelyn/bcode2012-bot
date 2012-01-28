package ypstrategyframework;

import battlecode.common.Direction;
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
	
	public abstract Direction navigateTo(
			MapLocation destination) throws GameActionException;

	public Direction getOutOfCurrentSquare() {
		boolean[] moveable = br.dc.getMovableDirections();
		if (moveable[br.currDir.ordinal()]) return br.currDir;
		else if (moveable[br.currDir.opposite().ordinal()]) return br.currDir.opposite();
		else 
			for (int x=0; x<8; x++)
				if (moveable[x]) return Constants.directions[x];
		
		return Direction.NONE;
	}
}
