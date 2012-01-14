package ducks;

import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class ScorcherRobot extends BaseRobot {

	public ScorcherRobot(RobotController myRC) {
		super(myRC);
		currState = RobotState.INITIALIZE;
	}

	@Override
	public void run() throws GameActionException {
		switch (currState) {
			case INITIALIZE:
				initialize();
				break;
			case POWER_SAVE:
				powerSave();
				break;
			default:
				break;
		}
	}
	
	public void initialize() throws GameActionException {
		currState = RobotState.POWER_SAVE;
		powerSave();
	}
	
	public void powerSave() throws GameActionException {
		// attack closest enemy if possible
		attackGround();
		// spin
		if (!rc.isMovementActive()) {
			rc.setDirection(currDir.rotateLeft().rotateLeft().rotateLeft());
		}
	}
	
	private void attackGround() throws GameActionException {
		if (rc.isAttackActive()) {
			return;
		}
		boolean shouldAttack = false;
		for (Robot r : dc.getNearbyRobots()) {
			if (r.getTeam() != myTeam) {
				shouldAttack = true;
			} else {
				// ally unit in range, don't attack
				return;
			}
		}
		if (shouldAttack) {
			rc.attackSquare(null, null);
		}
	}

}
