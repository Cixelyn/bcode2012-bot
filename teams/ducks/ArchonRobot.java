package ducks;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class ArchonRobot extends BaseRobot{

	public ArchonRobot(RobotController myRC) {
		super(myRC);
	}

	public void run() throws GameActionException{
		System.out.println("ducks");
		rc.setDirection(currDir.rotateLeft());
	}

}
