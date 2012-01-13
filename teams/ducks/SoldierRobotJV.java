package ducks;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class SoldierRobotJV extends BaseRobot {

	public SoldierRobotJV(RobotController myRC) {
		super(myRC);
		currState = RobotState.INITIALIZE;
	}

	@Override
	public void run() throws GameActionException {
		// power down if not enough flux
		if (currFlux < Constants.POWER_DOWN_FLUX) {
			return;
		}
		switch (currState) {
			case INITIALIZE:
				initialize();
				break;
			case POWER_SAVE:
				powerSave();
				break;
			case RUSH:
				rush();
				break;
			default:
				break;
		}
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb) {
		switch(msgType) {
			case 'w':
				if (currState == RobotState.POWER_SAVE) {
					currState = RobotState.RUSH;
				}
				break;
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	public void initialize() throws GameActionException {
		// set nav mode
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		// set radio addresses
		io.setAddresses(new String[] {"#x", "#s"});
		// go to power save mode
		currState = RobotState.POWER_SAVE;
		powerSave();
	}
	
	public void powerSave() throws GameActionException {
		// spin
		if (!rc.isMovementActive()) {
			rc.setDirection(currDir.rotateLeft().rotateLeft().rotateLeft());
		}
	}
	
	public void rush() throws GameActionException {
		rc.suicide();
	}
}
