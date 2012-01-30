package fibbyBot2;

import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class ScorcherRobot extends BaseRobot {

	public ScorcherRobot(RobotController myRC) {
		super(myRC);
		currState = RobotState.ATTACK_GROUND;
	}

	@Override
	public void run() throws GameActionException {
		switch (currState) {
			case ATTACK_GROUND:
				attackGround();
				break;
			default:
				break;
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
