package ducks;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class ScoutRobot extends BaseRobot {

	public ScoutRobot(RobotController myRC) {
		super(myRC);
		nv = new Beeline(this, 0);
		currState = RobotState.FOLLOW;
	}

	@Override
	public void run() throws GameActionException {
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
