package ducks;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ScoutRobotJV extends BaseRobot {
	
	private MapLocation objective;
	
	public ScoutRobotJV(RobotController myRC) throws GameActionException {
		super(myRC);
	}

	@Override
	public void run() throws GameActionException {
		// scan
		radar.scan(true, true);
		// set objective
		objective = mc.guessEnemyPowerCoreLocation();
		// attack if you can
		if (!rc.isAttackActive() && radar.closestEnemy != null &&
				rc.canAttackSquare(radar.closestEnemy.location)) {
			rc.attackSquare(radar.closestEnemy.location,
					radar.closestEnemy.robot.getRobotLevel());
		}
		// heal if you should
		if (radar.numAllyDamaged > 0) {
			rc.regenerate();
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
		if (radar.closestEnemy != null) {
			// if an enemy is nearby, stay in (8, 18] range
			if (radar.closestEnemyDist <= 8) {
				return new MoveInfo(curLoc.directionTo(objective).opposite(), true);
			} else if (radar.closestEnemyDist <= 18) {
				return new MoveInfo(curLoc.directionTo(objective));
			} else {
				return new MoveInfo(curLoc.directionTo(objective), false);
			}
		} else {
			// if no enemy is nearby, go to objective
			return new MoveInfo(curLoc.directionTo(objective), false);
		}
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
		// TODO(jven): check we didn't go over bytecodes within the method
		// share exploration
		super.useExtraBytecodes();
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
