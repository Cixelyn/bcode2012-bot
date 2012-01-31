package brutalai;

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
		CAP,
		ESCORT
	}
	
	private static class MyConstants {
		public static final int ROUND_TO_STOP_SPLIT = 20;
	}
	
	private StrategyState strategy;
	
	//private RobotType nextUnit;

	public ArchonRobotJV(RobotController myRC) throws GameActionException {
		super(myRC);
		// set initial states
		strategy = StrategyState.SPLIT;
		// set broadcast channels
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.ARCHONS,
				BroadcastChannel.EXPLORERS
		});
		// set navigation mode
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
	}

	@Override
	public void run() throws GameActionException {
		// scan
		radar.scan(true, true);
		// check strategy state
		if (curRound < MyConstants.ROUND_TO_STOP_SPLIT) {
			strategy = StrategyState.SPLIT;
		} else if (curLoc.equals(dc.getAlliedArchons()[0])) {
			strategy = StrategyState.CAP;
		} else {
			strategy = StrategyState.ESCORT;
		}
		// set destination and flux balance mode
		switch (strategy) {
			case SPLIT:
				fbs.disable();
				break;
			case CAP:
				nav.setDestination(mc.guessBestPowerNodeToCapture());
				fbs.setPoolMode();
				break;
			case ESCORT:
				nav.setDestination(dc.getAlliedArchons()[0]);
				fbs.setPoolMode();
				break;
			default:
				break;
		}
		// broadcast objective for scorchers
		io.sendMapLoc(BroadcastChannel.ALL, BroadcastType.RALLY,
				mc.guessBestPowerNodeToCapture());
		// indicator strings
		dbg.setIndicatorString('j', 0, "ARCHON - " + strategy);
	}
	
	@Override
	public void processMessage(
			BroadcastType type, StringBuilder sb) throws GameActionException {
		switch (type) {
			case INITIAL_REPORT:
				int[] initialReport = BroadcastSystem.decodeUShorts(sb);
				int initialReportTime = initialReport[0];
				MapLocation initialReportLoc = new MapLocation(
						initialReport[0], initialReport[1]);
				dbg.println('j', "Scouts report: Enemy approaching from " +
						initialReportLoc + " as of round " + initialReportTime);
				io.sendUShort(BroadcastChannel.SCOUTS,
						BroadcastType.INITIAL_REPORT_ACK, 0);
				break;
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
		if (radar.closestEnemy != null && radar.closestEnemyDist < 25) {
			return new MoveInfo(curLoc.directionTo(
					radar.closestEnemy.location).opposite(), true);
		}
		switch (strategy) {
			case SPLIT:
				return new MoveInfo(myHome.directionTo(birthplace), false);
			case CAP:
				if (curLoc.isAdjacentTo(nav.getDestination())) {
					if (rc.getFlux() >= 200 && rc.senseObjectAtLocation(
							nav.getDestination(), RobotLevel.ON_GROUND) == null) {
						return new MoveInfo(RobotType.TOWER, curLoc.directionTo(
								nav.getDestination()));
					} else {
						return null;
					}
				} else if (rc.getFlux() >= (Clock.getRoundNum() < 150 ? 110 : 299) &&
						rc.senseTerrainTile(curLocInFront) != TerrainTile.OFF_MAP &&
						rc.senseObjectAtLocation(
						curLocInFront, RobotLevel.IN_AIR) == null) {
					return new MoveInfo(RobotType.SCOUT, curDir);
				} else {
					return new MoveInfo(nav.navigateToDestination(), false);
				}
			case ESCORT:
				if (rc.getFlux() >= 290) {
					if (rc.canMove(curDir)) {
						if (Util.randDouble() < 0.5) {
							return new MoveInfo(RobotType.SCORCHER, curDir);
						} else {
							return new MoveInfo(RobotType.SOLDIER, curDir);
						}
					} else {
						return new MoveInfo(curDir.rotateLeft());
					}
				} else if (dc.getClosestArchon() != null &&
						curLoc.distanceSquaredTo(dc.getClosestArchon()) < 16 &&
						Util.randDouble() < 0.3) {
					return new MoveInfo(curLoc.directionTo(
							dc.getClosestArchon()).opposite(), false);
				} else {
					return new MoveInfo(nav.navigateToDestination(), false);
				}
			default:
				return null;
		}
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
		super.useExtraBytecodes();
		// prepare
		if (curRound == Clock.getRoundNum() && Clock.getBytecodesLeft() > 5000) {
			nav.prepare();
		}
		// share exploration
		if (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 3000 && Util.randDouble() < 0.2) {
			ses.broadcastMapFragment();
		}
		if (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 1000 && Util.randDouble() < 0.2) {
			ses.broadcastPowerNodeFragment();
		}
		if (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 1000 && Util.randDouble() < 0.2) {
			ses.broadcastMapEdges();
		}
		// process shared exploration
		while (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 1000 &&
				!mc.extractUpdatedPackedDataStep()) {
		}
	}
}
