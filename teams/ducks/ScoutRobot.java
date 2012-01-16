package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class ScoutRobot extends StrategyRobot {

	public ScoutRobot(RobotController myRC) {
		super(myRC, RobotState.FOLLOW);
		io.setAddresses(new String[] {"#x"});
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
		if (currFlux < Constants.MIN_ROBOT_FLUX) {
			return;
		}
		switch (state) {
			case FOLLOW:
				follow();
				break;
			default:
				break;
		}
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
			case 'd':
				int[] deadEnemyArchonIDs = Radio.decodeShorts(sb);
				for (int id : deadEnemyArchonIDs) {
					enemyArchonInfo.reportEnemyArchonKill(id);
				}
			default:
				super.processMessage(msgType, sb);
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
		if (!rc.isMovementActive()) {
			Direction dir = currLoc.directionTo(closestArchon);
			if (dir != Direction.OMNI) {
				if (currDir != dir) {
					rc.setDirection(dir);
				} else if (rc.canMove(currDir)) {
					rc.moveForward();
				}
			}
		}
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
		return damagedUnits >= 1;
	}

}
