package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ScoutRobotJV2 extends BaseRobot {
	
//	private static class MyConstants {
//	}

	public ScoutRobotJV2(RobotController myRC) throws GameActionException {
		super(myRC);
	}

	@Override
	public void run() throws GameActionException {
		// suicide if you're out of flux
		if (rc.getFlux() < 0.5) {
			rc.suicide();
		}
		// scan
		radar.scan(false, true);
		// attack if you can
		if (!rc.isAttackActive() && radar.closestEnemy != null &&
				rc.canAttackSquare(radar.closestEnemy.location)) {
			rc.attackSquare(radar.closestEnemy.location,
					radar.closestEnemy.robot.getRobotLevel());
		}
	}
	
	@Override
	public void processMessage(
			BroadcastType type, StringBuilder sb) throws GameActionException {
		switch (type) {
			case MAP_EDGES:
				ses.receiveMapEdges(BroadcastSystem.decodeUShorts(sb));
				break;
			case MAP_FRAGMENTS:
				ses.receiveMapFragment(BroadcastSystem.decodeInts(sb));
				break;
			case POWERNODE_FRAGMENTS:
				ses.receivePowerNodeFragment(BroadcastSystem.decodeInts(sb));
				break;
			default:
				super.processMessage(type, sb);
		}
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		if (radar.closestEnemy == null) {
			return new MoveInfo(
					curLoc.directionTo(mc.guessEnemyPowerCoreLocation()), false);
		} else {
			Direction dirToEnemy = curLoc.directionTo(radar.closestEnemy.location);
			if (radar.closestEnemyDist <= 8) {
				return new MoveInfo(dirToEnemy.opposite(), true);
			} else if (curLoc.add(dirToEnemy).distanceSquaredTo(
					radar.closestEnemy.location) > 8) {
				return new MoveInfo(dirToEnemy, false);
			} else {
				return null;
			}
		}
	}
	
	@Override
	public void useExtraBytecodes() {
		// TODO(jven): check we didn't go over bytecodes within the method
		// share exploration
		if (Clock.getBytecodesLeft() > 3000 && Math.random() < 0.3) {
			ses.broadcastMapFragment();
		}
		if (Clock.getBytecodesLeft() > 1000 && Math.random() < 0.3) {
			ses.broadcastPowerNodeFragment();
		}
		if (Clock.getBytecodesLeft() > 1000 && Math.random() < 0.3) {
			ses.broadcastMapEdges();
		}
		// process shared exploration
		while (Clock.getBytecodesLeft() > 7000) {
			mc.extractUpdatedPackedDataStep();
		}
	}
}
