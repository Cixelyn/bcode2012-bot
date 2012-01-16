package ducks;

import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class ScorcherRobot extends StrategyRobot {

	public ScorcherRobot(RobotController myRC) {
		super(myRC, RobotState.INITIALIZE);
	}
	
	@Override
	public RobotState processTransitions(RobotState state)
			throws GameActionException {
		
		switch (state) {
		case INITIALIZE:
		{
			return RobotState.POWER_SAVE;
		}
		}
		
		return state;
	}

	@Override
	public void prepareTransition(RobotState newstate, RobotState oldstate)
			throws GameActionException {
	}

	@Override
	public void execute(RobotState state) throws GameActionException {
		switch (state) {
		case POWER_SAVE:
			powerSave();
			break;
		default:
			break;
	}	
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
				//return;
			}
		}
		if (shouldAttack) {
			rc.attackSquare(null, null);
		}
	}


}
