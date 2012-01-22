package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ScoutRobotJV extends BaseRobot {
	
	/** The possible behaviors for the Scout. */
	private enum BehaviorState {
		WAIT_FOR_FLUX,
		FIND_ENEMY,
		REPORT_ENEMY,
		PET,
		EXPLORE
	}
	
	/** Defines the shape of a space filling curve, used to explore the map
	 * starting from some corner.
	 */
	private static final int[][] SCOUT_PATTERN = new int[][] {
		{0,0},{1,0},{1,1},{0,1},{0,2},{1,2},{2,2},{2,1},{2,0},{3,0},{3,1},{3,2},
		{3,3},{2,3},{1,3},{0,3},{0,4},{1,4},{2,4},{3,4},{4,4},{4,3},{4,2},{4,1},
		{4,0},{5,0},{5,1},{5,2},{5,3},{5,4},{5,5},{4,5},{3,5},{2,5},{1,5},{0,5},
		{0,6},{1,6},{2,6},{3,6},{4,6},{5,6},{6,6},{6,5},{6,4},{6,3},{6,2},{6,1},
		{6,0},{7,0},{7,1},{7,2},{7,3},{7,4},{7,5},{7,6},{7,7},{6,7},{5,7},{4,7},
		{3,7},{2,7},{1,7},{0,7},{0,8},{1,8},{2,8},{3,8},{4,8},{5,8},{6,8},{7,8},
		{8,8},{8,7},{8,6},{8,5},{8,4},{8,3},{8,2},{8,1},{8,0},{9,0},{9,1},{9,2},
		{9,3},{9,4},{9,5},{9,6},{9,7},{9,8},{9,9},{8,9},{7,9},{6,9},{5,9},{4,9},
		{3,9},{2,9},{1,9},{0,9},{0,10},{1,10},{2,10},{3,10},{4,10},{5,10},{6,10},
		{7,10},{8,10},{9,10},{10,10},{10,9},{10,8},{10,7},{10,6},{10,5},{10,4},
		{10,3},{10,2},{10,1},{10,0},{11,0},{11,1},{11,2},{11,3},{11,4},{11,5},
		{11,6},{11,7},{11,8},{11,9},{11,10},{11,11},{10,11},{9,11},{8,11},{7,11},
		{6,11},{5,11},{4,11},{3,11},{2,11},{1,11},{0,11},{0,12},{1,12},{2,12},
		{3,12},{4,12},{5,12},{6,12},{7,12},{8,12},{9,12},{10,12},{11,12},{12,12},
		{12,11},{12,10},{12,9},{12,8},{12,7},{12,6},{12,5},{12,4},{12,3},{12,2},
		{12,1},{12,0}};
	
	/** Round to stop exploring, regardless of whether you're done. */
	private static final int ROUND_TO_STOP_EXPLORING = 1000;
	
	/** The Scout's current position in SCOUT_PATTERN. When this equals
	 * SCOUT_PATTERN.length - 1, the Scout knows the whole map.
	 */
	private int scoutPatternIdx;
	
	/** The Scout's current behavior. */
	private BehaviorState behavior;
	
	/** The Scout's current objective. */
	private MapLocation objective;
	
	public ScoutRobotJV(RobotController myRC) throws GameActionException {
		super(myRC);
		// set initial state
		behavior = BehaviorState.WAIT_FOR_FLUX;
		// set broadcast channels
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.SCOUTS
		});
		// set navigation mode
		nav.setNavigationMode(NavigationMode.GREEDY);
	}

	/**
	 * In terms of bytecodes, this run method takes, as a conservative estimate,
	 * 600 + cost of scan, which with a lot of enemies can get to 600 + 6000.
	 */
	@Override
	public void run() throws GameActionException {
		dbg.setIndicatorString('j',0, "Behavior state: " + behavior);
		dbg.setIndicatorString('j',1, "Scout pattern idx: " + scoutPatternIdx);
		// suicide if not enough flux
		if (rc.getFlux() < 3.0) {
			rc.suicide();
		}
		// scan
		radar.scan(true, true);
		// switch states if necessary
		switch (behavior) {
			case WAIT_FOR_FLUX:
				if (rc.getFlux() > myMaxFlux - 10) {
					behavior = BehaviorState.FIND_ENEMY;
				}
				break;
			case FIND_ENEMY:
				if (radar.closestEnemy != null) {
					behavior = BehaviorState.REPORT_ENEMY;
				}
				break;
			case REPORT_ENEMY:
				break;
			case PET:
				break;
			case EXPLORE:
				if (curRound >= ROUND_TO_STOP_EXPLORING || scoutPatternIdx >=
						SCOUT_PATTERN.length - 1) {
					behavior = BehaviorState.PET;
				}
				break;
			default:
				break;
		}
		// set flux balance mode
		switch (behavior) {
			case WAIT_FOR_FLUX:
			case FIND_ENEMY:
			case REPORT_ENEMY:
				fbs.disable();
				break;
			case PET:
				fbs.setPoolMode();
				break;
			case EXPLORE:
				fbs.setBatteryMode();
				break;
			default:
				break;
		}
		// set objective based on behavior
		switch (behavior) {
			case WAIT_FOR_FLUX:
				// go to nearest archon
				objective = dc.getClosestArchon();
				break;
			case FIND_ENEMY:
				objective = mc.guessEnemyPowerCoreLocation();
				break;
			case REPORT_ENEMY:
			case PET:
				// go to nearest archon
				objective = dc.getClosestArchon();
				break;
			case EXPLORE:
				// get an unexplored location
				objective = getExplorationTarget();
				break;
			default:
				break;
		}
		// attack if you can
		if (!rc.isAttackActive() && radar.closestEnemy != null &&
				rc.canAttackSquare(radar.closestEnemy.location)) {
			rc.attackSquare(radar.closestEnemy.location,
					radar.closestEnemy.robot.getRobotLevel());
		}
		// heal if you should
		if ((curEnergon < myMaxEnergon || radar.numAllyDamaged > 0) &&
				Math.random() < 0.3) {
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
			/* more aggressive kiting code
			Direction dir = curLoc.directionTo(radar.getEnemySwarmCenter());
			int dist = (int)radar.closestEnemyDist;
			// if we're too close, back up
			if (dist <= 5) {
				return new MoveInfo(dir.opposite(), true);
			}
			// if we're in safe range, face target
			if (dist <= 25) {
				return new MoveInfo(dir);
			}
			// we're far away, move to target
			return new MoveInfo(dir, false);
			*/
			return new MoveInfo(curLoc.directionTo(
					radar.getEnemySwarmCenter()).opposite(), true);
		} else {
			// if no enemy is nearby, go to objective
			if (objective != null) {
				return new MoveInfo(curLoc.directionTo(objective), false);
			} else {
				return new MoveInfo(nav.navigateCompletelyRandomly(), false);
			}
		}
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
		super.useExtraBytecodes();
		// share exploration with archons
		if (behavior == BehaviorState.PET) {
			if (curRound == Clock.getRoundNum() &&
					Clock.getBytecodesLeft() > 4000 && Math.random() < 0.05 /
					(radar.numAllyRobots + 1)) {
				ses.broadcastMapFragment();
			}
			if (curRound == Clock.getRoundNum() &&
					Clock.getBytecodesLeft() > 2000 && Math.random() < 0.05 /
					(radar.numAllyRobots + 1)) {
				ses.broadcastPowerNodeFragment();
			}
			if (curRound == Clock.getRoundNum() &&
					Clock.getBytecodesLeft() > 2000 && Math.random() < 0.05 /
					(radar.numAllyRobots + 1)) {
				ses.broadcastMapEdges();
			}
		}
		// process shared exploration
		while (curRound == Clock.getRoundNum() &&
				Clock.getBytecodesLeft() > 5000 &&
				!mc.extractUpdatedPackedDataStep()) {
		}
	}
	
	/**
	 * Get a location to explore.
	 */
	private MapLocation getExplorationTarget() {
		Direction d;
		int sx, sy, mx, my;
		switch (myID % 4) {
			case 0:
				d = Direction.NORTH_WEST;
				sx = mc.edgeXMin;
				sy = mc.edgeYMin;
				mx = 1;
				my = 1;
				break;
			case 1:
				d = Direction.NORTH_EAST;
				sx = mc.edgeXMax;
				sy = mc.edgeYMin;
				mx = -1;
				my = 1;
				break;
			case 2:
				d = Direction.SOUTH_WEST;
				sx = mc.edgeXMin;
				sy = mc.edgeYMax;
				mx = 1;
				my = -1;
				break;
			case 3:
			default:
				d = Direction.SOUTH_EAST;
				sx = mc.edgeXMax;
				sy = mc.edgeYMax;
				mx = -1;
				my = -1;
				break;
		}
		if (sx * sy == 0) {
			// i don't know where the corner is
			return myHome.add(d, 100);
		}
		// making new map locations is very costly so advance your scout pattern by
		// at most 3 per turn
		// TODO(jven): might be much cheaper to access mc.sensed and check map
		// edges directly
		int numIterations = 0;
		for (int idx = scoutPatternIdx; idx < SCOUT_PATTERN.length &&
				numIterations++ < 3; idx++) {
			int[] coords = SCOUT_PATTERN[idx];
			MapLocation loc = new MapLocation(
					mc.cacheToWorldX(sx + mx * (1 + coords[0]) * 7),
					mc.cacheToWorldY(sy + my * (1 + coords[1]) * 7));
			if (!mc.isOffMap(loc) && !mc.isSensed(loc)) {
				return loc;
			} else {
				scoutPatternIdx++;
			}
		}
		return null;
	}
}
