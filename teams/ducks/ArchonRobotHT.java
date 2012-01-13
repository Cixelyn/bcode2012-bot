package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class ArchonRobotHT extends BaseRobot{
	boolean aboutToMove = false;
	public ArchonRobotHT(RobotController myRC) {
		super(myRC);
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
	}
	
	@Override
	public void run() throws GameActionException {
		if(!currLoc.equals(rc.senseAlliedArchons()[0])) return;
		mc.senseAllTiles();
		MapLocation target = rc.senseCapturablePowerNodes()[0];
		
		if(!rc.isMovementActive()) {
			Direction dir;
			if(aboutToMove) {
				dir = currDir;
			} else {
				dir = nav.navigateTo(target);
				rc.setIndicatorString(2, dir.toString());
				if(dir==Direction.OMNI) dir = Direction.NORTH;
			}
			for(int i=0; i<30 && !rc.canMove(dir); i++) {
				if(Math.random()<0.5)
					dir = dir.rotateLeft();
				else
					dir = dir.rotateRight();
			}
			if(rc.canMove(dir)) {
				if(dir==currDir) {
					if(currLocInFront.equals(target)) {
						if(currFlux>210 && rc.canMove(currDir)) {
							rc.spawn(RobotType.TOWER);
						}
					} else {
						rc.moveForward();
						aboutToMove = false;
					}
				} else {
					rc.setDirection(dir);
					aboutToMove = true;
				}
			}
		
		}
	}
}
