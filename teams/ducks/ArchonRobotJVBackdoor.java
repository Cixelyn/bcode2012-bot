package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ArchonRobotJVBackdoor extends BaseRobot {
	
	private MapLocation objective;
	
	public ArchonRobotJVBackdoor(
			RobotController myRC) throws GameActionException {
		super(myRC);
		
		// set broadcast channels
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.ARCHONS,
				BroadcastChannel.EXPLORERS
		});
		
		// set flux balance mode
		fbs.setPoolMode();
		
		// set navigation mode
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		
		// set initial objective
		objective = myHome;
	}

	@Override
	public void run() throws GameActionException {
		// scan
		radar.scan(true, true);
		
		// set objective
//		objective = getNextBackdoorPowerNode();
		objective = mc.guessBestPowerNodeToCapture();
		nav.setDestination(objective);
		
		// broadcast objective
		io.sendMapLoc(BroadcastChannel.DISRUPTERS, BroadcastType.RALLY,
				objective);
	}
	
	@Override
	public void processMessage(BroadcastType type,
			StringBuilder sb) throws GameActionException {
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
				break;
		}
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		// if you have enough flux, make a disrupter...
		if (rc.getFlux() == myMaxFlux || radar.closestEnemy != null &&
				rc.getFlux() > RobotType.DISRUPTER.spawnCost + 15) {
			if (rc.canMove(curDir)) {
				return new MoveInfo(RobotType.DISRUPTER, curDir);
			} else {
				return new MoveInfo(curDir.rotateLeft());
			}
			
		// ...otherwise, if a non-tower enemy is nearby, kite it
		} else if (radar.closestEnemy != null) {
			Direction dir = curLoc.directionTo(radar.closestEnemy.location);
			if (radar.closestEnemy.type == RobotType.TOWER) {
				return new MoveInfo(dir);
			} else {
				return new MoveInfo(dir.opposite(), true);
			}
		
		// ...otherwise, go to target
		} else {
			int distance = curLoc.distanceSquaredTo(objective);
			if (distance == 0) {
				return new MoveInfo(nav.navigateCompletelyRandomly(), false);
			} else if (distance <= 2) {
				Direction dir = curLoc.directionTo(objective);
				if (rc.getFlux() > RobotType.TOWER.spawnCost && rc.canMove(dir)) {
					return new MoveInfo(RobotType.TOWER, dir);
				} else {
					return null;
				}
			} else {
				// with some probability, split from other archons
				if (dc.getClosestArchon() != null &&
						curLoc.distanceSquaredTo(dc.getClosestArchon()) < 16 &&
						Math.random() < 0.3) {
					return new MoveInfo(curLoc.directionTo(
							dc.getClosestArchon()).opposite(), true);
				} else {
					return new MoveInfo(nav.navigateToDestination(), true);
				}
			}
		}
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
		// prepare
		if (curRound == Clock.getRoundNum() && Clock.getBytecodesLeft() > 5000) {
			nav.prepare();
		}
		
		// share exploration
		if (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 3000 && Math.random() < 0.2) {
			ses.broadcastMapFragment();
		}
		if (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 1000 && Math.random() < 0.2) {
			ses.broadcastPowerNodeFragment();
		}
		if (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 1000 && Math.random() < 0.2) {
			ses.broadcastMapEdges();
		}
		
		// flux and messaging
		super.useExtraBytecodes();
		
		// process shared exploration
		while (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 1000 &&
				!mc.extractUpdatedPackedDataStep()) {
		}
	}
	
	/**
	 * Returns a caputurable power node far from the center but close to the
	 * Archon.
	 * @return The MapLocation of a power node to take.
	 */
	/*
	private MapLocation getNextBackdoorPowerNode() {
		// initialize some stuffs
		MapLocation center = new MapLocation(
				(myHome.x + mc.guessEnemyPowerCoreLocation().x) / 2,
				(myHome.y + mc.guessEnemyPowerCoreLocation().y) / 2);
		MapLocation bestPowerNode = null;
		int bestHeuristic = Integer.MAX_VALUE;
		
		// get a tower close to me but far from the center
		for (MapLocation powerNode : dc.getCapturablePowerCores()) {
			// if it's the enemy core, take it
			if (powerNode.equals(mc.getEnemyPowerCoreLocation())) {
				return powerNode;
			}
			int heuristic = curLoc.distanceSquaredTo(powerNode) -
					2 * center.distanceSquaredTo(powerNode);
			if (heuristic < bestHeuristic) {
				bestPowerNode = powerNode;
				bestHeuristic = heuristic;
			}
		}
		return bestPowerNode;
	}
	*/
}
