package ducks;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class DisrupterRobot extends BaseRobot {

	public DisrupterRobot(RobotController myRC) {
		super(myRC);
		this.currState = RobotState.DIZZY;
	}

	@Override
	public void run() throws GameActionException {
		switch (this.currState) {
			case DIZZY:
				this.dizzy();
				break;
			default:
				this.rc.setIndicatorString(2, "RobotState." + this.currState +
						" not implemented.");
				break;
		}
	}
	
	private void dizzy() throws GameActionException {
		if (this.rc.isMovementActive()) {
			return;
		}
		this.rc.setDirection(this.currDir.rotateLeft().rotateLeft().rotateLeft());
	}

}
