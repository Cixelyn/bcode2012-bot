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
		MAKE_SCOUTS
	}
	
	private static class MyConstants {
		public static final int ROUND_TO_STOP_SPLIT = 20;
	}
	
	private StrategyState strategy;
	
	int numScouts;

	public ArchonRobotJV(RobotController myRC) throws GameActionException {
		super(myRC);
		// set initial states
		strategy = StrategyState.SPLIT;
		// set flux balance mode
		fbs.setPoolMode();
		// set broadcast channels
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.ARCHONS,
				BroadcastChannel.EXPLORERS
		});
	}

	@Override
	public void run() throws GameActionException {
		// scan
		radar.scan(true, true);
		// check strategy state
		if (curRound < MyConstants.ROUND_TO_STOP_SPLIT) {
			strategy = StrategyState.SPLIT;
		} else {
			strategy = StrategyState.MAKE_SCOUTS;
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
		switch (strategy) {
			case SPLIT:
				return new MoveInfo(myHome.directionTo(birthplace), false);
			case MAKE_SCOUTS:
				if (rc.getFlux() >= RobotType.SCOUT.spawnCost +
						RobotType.SCOUT.maxFlux) {
					if (rc.senseTerrainTile(curLocInFront) != TerrainTile.OFF_MAP &&
							rc.senseObjectAtLocation(
							curLocInFront, RobotLevel.IN_AIR) == null) {
						return new MoveInfo(RobotType.SCOUT, curDir);
					} else {
						return new MoveInfo(curDir.rotateLeft());
					}
				} else {
					return new MoveInfo(curLoc.directionTo(
							mc.guessEnemyPowerCoreLocation()));
				}
			default:
				return null;
		}
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
		super.useExtraBytecodes();
		// share exploration
		if (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 3000 && Math.random() < 0.3) {
			ses.broadcastMapFragment();
		}
		if (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 1000 && Math.random() < 0.3) {
			ses.broadcastPowerNodeFragment();
		}
		if (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 1000 && Math.random() < 0.3) {
			ses.broadcastMapEdges();
		}
		// process shared exploration
		while (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 1000) {
			mc.extractUpdatedPackedDataStep();
		}
	}
}
