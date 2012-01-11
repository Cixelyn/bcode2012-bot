package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class ScoutRobot extends BaseRobot {

	public ScoutRobot(RobotController myRC) {
		super(myRC);
		nv = new Beeline(this, 0, false);
		currState = RobotState.FOLLOW;
	}

	@Override
	public void run() throws GameActionException {
		// TODO(jven): use processMessage
		for (Message m : rc.getAllMessages()) {
			if (m.strings != null && m.strings.length == 1) {
				if (m.strings[0] == "archon") {
					for (int i = 2; i < m.ints.length; i++) {
						enemyArchonInfo.reportEnemyArchonKill(m.ints[i]);
					}
				} else if (m.strings[0] == "soldier") {
					for (int i = 0; i < m.ints.length; i++) {
						enemyArchonInfo.reportEnemyArchonKill(m.ints[i]);
					}
				}
			}
		}
		// power down if not enough flux
		if (currFlux < Constants.POWER_DOWN_FLUX) {
			return;
		}
		switch (currState) {
			case FOLLOW:
				follow();
				break;
			default:
				break;
		}
	}
	
	private void follow() throws GameActionException {
		if (rc.isMovementActive()) {
			return;
		}
		// get closest archon
		int closestDistance = Integer.MAX_VALUE;
		MapLocation closestArchon = currLoc;
		for (MapLocation archon : dc.getAlliedArchons()) {
			int distance = currLoc.distanceSquaredTo(archon);
			if (distance < closestDistance) {
				closestDistance = distance;
				closestArchon = archon;
			}
		}
		// go to target
		nv.navigateTo(closestArchon);
		// regenerate if necessary
		if (shouldRegenerate()) {
			rc.regenerate();
		}
	}
	
	private boolean shouldRegenerate() throws GameActionException {
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
		return damagedUnits >= Constants.MIN_DAMAGED_UNITS_TO_REGEN;
	}
}
