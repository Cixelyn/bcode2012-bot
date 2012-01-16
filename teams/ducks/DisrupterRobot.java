package ducks;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class DisrupterRobot extends StrategyRobot {

	public DisrupterRobot(RobotController myRC) {
		super(myRC, RobotState.DIZZY);
	}

	@Override
	public RobotState processTransitions(RobotState state)
			throws GameActionException {
		return state;
	}

	@Override
	public void prepareTransition(RobotState newstate, RobotState oldstate)
			throws GameActionException {
	}

	@Override
	public void execute(RobotState state) throws GameActionException {
		switch (state) {
		case DIZZY:
			this.dizzy();
			break;
		default:
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
