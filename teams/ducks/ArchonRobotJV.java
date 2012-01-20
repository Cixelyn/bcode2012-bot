package ducks;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ArchonRobotJV extends BaseRobot {
	
	private enum StrategyState {
		SPLIT,
		RUSH
	}
	
	private enum BehaviorState {
		SWARM,
		ENGAGE,
		CHASE
	}
	
	private static class MyConstants {
		public static final int ROUND_TO_STOP_SPLIT = 20;
		public static final int SPLIT_DISTANCE = 20;
	}
	
	private StrategyState strategy;
	private BehaviorState behavior;

	public ArchonRobotJV(RobotController myRC) throws GameActionException {
		super(myRC);
		// set initial states
		strategy = StrategyState.SPLIT;
		behavior = BehaviorState.SWARM;
		// set navigation mode
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		nav.setDestination(myHome);
	}

	@Override
	public void run() throws GameActionException {
		// check strategy state
		if (curRound > MyConstants.ROUND_TO_STOP_SPLIT) {
			strategy = StrategyState.RUSH;
		}
		// set target
		if (strategy == StrategyState.RUSH) {
			nav.setDestination(mc.guessEnemyPowerCoreLocation());
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
		if (strategy == StrategyState.SPLIT) {
			// move away from home
			return new MoveInfo(myHome.directionTo(birthplace), false);
		} else if (strategy == StrategyState.RUSH) {
			// check if i should build a scout
			if (rc.getFlux() >= RobotType.SCOUT.spawnCost +
					0.4 * RobotType.SCOUT.maxFlux) {
				if (rc.senseTerrainTile(curLocInFront) != TerrainTile.OFF_MAP &&
						rc.senseObjectAtLocation(
						curLocInFront, RobotLevel.IN_AIR) == null) {
					return new MoveInfo(RobotType.SCOUT, curDir);
				}
			}
			// move towards target
			MapLocation closestArchon = dc.getClosestArchon();
			if (closestArchon != null) {
				int distance = curLoc.distanceSquaredTo(closestArchon);
				if (Math.random() < 0.6 * (MyConstants.SPLIT_DISTANCE - distance) /
						MyConstants.SPLIT_DISTANCE) {
					return new MoveInfo(
							curLoc.directionTo(dc.getClosestArchon()).opposite(), false);
				}
			}
			return new MoveInfo(nav.navigateToDestination(), true);
		} else {
			return null;
		}
	}
	
	@Override
	public void useExtraBytecodes() {
		// TODO(jven): check we didn't go over bytecodes within the method
		// think about where to move
		while (Clock.getBytecodesLeft() > 3000) {
			nav.prepare();
		}
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
		while (Clock.getBytecodesLeft() > 500) {
			mc.extractUpdatedPackedDataStep();
		}
	}
}
