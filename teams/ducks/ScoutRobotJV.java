package ducks;

import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class ScoutRobotJV extends StrategyRobot {

	public ScoutRobotJV(RobotController myRC) {
		super(myRC, RobotState.HEAL);
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
		// power down if not enough flux
		if (currFlux < Constants.POWER_DOWN_FLUX) {
			return;
		}
		switch (state) {
			case HEAL:
				heal();
				break;
			default:
				break;
		}
	}
	
	public void heal() throws GameActionException {
		// spin around
		if (!rc.isMovementActive()) {
			rc.setDirection(currDir.rotateRight().rotateRight().rotateRight());
		}
		// check if there are enough damaged units
		int damagedUnits = 0;
		for (Robot r : dc.getNearbyRobots()) {
			if (r.getTeam() == myTeam) {
				RobotInfo rInfo = rc.senseRobotInfo(r);
				if (!rInfo.regen && rInfo.energon < rInfo.type.maxEnergon) {
					damagedUnits++;
				}
			}
		}
		// regenerate if necessary
		if (damagedUnits >= Constants.MIN_DAMAGED_UNITS_TO_REGEN) {
			rc.regenerate();
		}
		// attack closest enemy
		attackClosestEnemy();
	}
	
	private void attackClosestEnemy() throws GameActionException {
		// wait if attack is active
		if (rc.isAttackActive()) {
			return;
		}
		// see if enemy in range
		RobotInfo closestEnemy = dc.getClosestEnemy();
		if (closestEnemy != null && rc.canAttackSquare(closestEnemy.location)) {
			rc.attackSquare(
					closestEnemy.location, closestEnemy.robot.getRobotLevel());
		}
	}
}
