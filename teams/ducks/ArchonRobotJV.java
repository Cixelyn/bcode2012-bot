package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

/**
 * ArchonRobotJV, aka MODULAR_BOT
 * 
 * @author jven
 */
public class ArchonRobotJV extends BaseRobot {
	
	/**
	 * TODO(jven): Move this stuff out of here
	 * Constants used by the Archon.
	 */
	private static class ArchonConstants {
		public static final int ROUND_TO_STOP_EXPLORING = 100;
		public static final int HOME_RADIUS = 9;
		public static final int ARCHON_SPLIT_DISTANCE = 16;
		public static final int NUM_INITIAL_SOLDIERS = 5;
		public static final double MIN_ARCHON_FLUX = 5;
		public static final int ARCHON_ENGAGE_DISTANCE = 26;
	}
	
	/**
	 * TODO(jven): Move this stuff out of here
	 * All possible states for the Archon. See the private void methods for
	 * each state for details.
	 */
	private enum ArchonState {
		INITIALIZE,
		EXPLORE,
		RETURN_HOME,
		SPLIT,
		BUILD_INITIAL_ARMY,
		RUSH,
		CAP,
		ENGAGE
	}
	
	/** The current state of the Archon. */
	private ArchonState curState;
	
	/** Whether the Archon is done initializing. */
	private boolean initialized;
	
	/** Whether the Archon has been rallied. */
	private boolean rallied = false;
	
	/** The number of soldiers currently built during the build_initial_army
	 * state.
	 */
	private int initialSoldiersBuilt;

	public ArchonRobotJV(RobotController myRC) {
		super(myRC);
		// set initial state
		curState = ArchonState.INITIALIZE;
	}

	@Override
	public void run() throws GameActionException {
		// TODO(jven): temporary
		if (directionToSenseIn != null) {
			mc.senseAfterMove(directionToSenseIn);
			directionToSenseIn = null;
		}
		// transition to a new state if necessary
		curState = getNextState();
		// TODO(jven): debugging
		rc.setIndicatorString(0, myType + " - " + curState);
		// execute
		switch (curState) {
			case INITIALIZE:
				initialize();
				break;
			case EXPLORE:
				explore();
				break;
			case RETURN_HOME:
				returnHome();
				break;
			case SPLIT:
				split();
				break;
			case BUILD_INITIAL_ARMY:
				buildInitialArmy();
				break;
			case RUSH:
				rush();
				break;
			case CAP:
				cap();
				break;
			case ENGAGE:
				engage();
				break;
			default:
				// we got g'd
				rc.suicide();
				break;
		}
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb)
			throws GameActionException {
		switch (msgType) {
			case 'e':
				ses.receiveMapEdges(BroadcastSystem.decodeShorts(sb));
				break;
			case 'm':
				ses.receiveMapFragment(BroadcastSystem.decodeInts(sb));
				break;
			case 'p':
				ses.receivePowerNodeFragment(BroadcastSystem.decodeInts(sb));
				break;
			case 'r':
				rallied = true;
				break;
			default:
				break;
		}
	}
	
	/** Returns the state the Archon should execute this turn, using the current
	 * state.
	 * @modifies Must not modify the state of the Archon in any way!
	 * @returns The state to execute this turn.
	 */
	private ArchonState getNextState() throws GameActionException {
		// check if we should transition
		switch (curState) {
			case INITIALIZE:
				// if we're done initializing, start exploring
				if (initialized) {
					return ArchonState.EXPLORE;
				}
				break;
			case EXPLORE:
				// if we're done exploring, go home
				if (curRound >= ArchonConstants.ROUND_TO_STOP_EXPLORING) {
					return ArchonState.RETURN_HOME;
				}
				break;
			case RETURN_HOME:
				// consider ourselves home if we're close enough to our main
				if (curLoc.distanceSquaredTo(myHome) <= ArchonConstants.HOME_RADIUS) {
					return ArchonState.SPLIT;
				}
				break;
			case SPLIT:
				// stop splitting if we're far enough away from the closest archon
				// or if our flux is full (meaning that we're taking way too long)
				if (dc.getClosestArchon() == null ||
						curLoc.distanceSquaredTo(dc.getClosestArchon()) >=
						ArchonConstants.ARCHON_SPLIT_DISTANCE ||
						rc.getFlux() == myMaxFlux) {
					return ArchonState.BUILD_INITIAL_ARMY;
				}
				break;
			case BUILD_INITIAL_ARMY:
				// if we've built enough soldiers or an enemy is nearby, start
				// the rush
				if (rallied ||
						initialSoldiersBuilt >= ArchonConstants.NUM_INITIAL_SOLDIERS &&
						rc.getFlux() >= ArchonConstants.MIN_ARCHON_FLUX ||
						dc.getClosestEnemy() != null) {
					rallied = true;
					return ArchonState.RUSH;
				}
				break;
			case RUSH:
				// if an enemy is nearby, engage
				if (dc.getClosestEnemy() != null) {
					return ArchonState.ENGAGE;
				}
				// if we've seen the enemy base, start tower capping
				if (mc.getEnemyPowerCoreLocation() != null) {
					return ArchonState.CAP;
				}
				break;
			case CAP:
				// if we've never seen the enemy base, rush
				if (mc.getEnemyPowerCoreLocation() == null) {
					return ArchonState.RUSH;
				}
				// if an enemy is nearby, engage
				if (dc.getClosestEnemy() != null) {
					return ArchonState.ENGAGE;
				}
				break;
			case ENGAGE:
				// if no enemy nearby, go back to rushing or tower capping
				if (dc.getClosestEnemy() == null) {
					if (mc.getEnemyPowerCoreLocation() == null) {
						return ArchonState.RUSH;
					} else {
						return ArchonState.CAP;
					}
				}
				break;
			default:
				// we got g'd
				rc.suicide();
				break;
		}
		// if we didn't transition, stay in the current state
		return curState;
	}
	
	/**
	 * Initializes the Archon. The Archon should only ever enter this state once:
	 * at the beginning of the game.
	 */
	private void initialize() throws GameActionException {
		// initialize navigation system
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		nav.setDestination(myHome);
		// initialize broadcast system
		io.setAddresses(new String[] {"#x", "#a", "#e"});
		// initialize map cache
		mc.senseAll();
		// done initializing
		initialized = true;
	}
	
	/**
	 * Explore the map! The Archon should only enter this state at the start of
	 * the game, after initialization.
	 */
	private void explore() throws GameActionException {
		// set micro mode
		micro.setNormalMode();
		micro.toggleAvoidPowerNodes(false);
		// set objective for some place far away from home, in a different direction
		// from the other Archons
		micro.setObjective(myHome.add(myHome.directionTo(birthplace),
				GameConstants.MAP_MAX_HEIGHT));
		// TODO(jven): explore more haphazardly to fill in gaps
		// tangent bug to destination
		micro.attackMove();
	}
	
	/**
	 * Return home. This state can be called at any time.
	 */
	private void returnHome() throws GameActionException {
		// set micro mode
		micro.setNormalMode();
		micro.toggleAvoidPowerNodes(false);
		// set objective for home
		micro.setObjective(myHome);
		// tangent bug to home
		micro.attackMove();
	}
	
	/**
	 * Try to get away from other archons. This state can be called at any time.
	 */
	private void split() throws GameActionException {
		// set micro mode
		micro.setKiteMode(ArchonConstants.ARCHON_SPLIT_DISTANCE);
		micro.toggleAvoidPowerNodes(false);
		// set objective for closest archon
		micro.setObjective(dc.getClosestArchon());
		// kite closest archon
		micro.attackMove();
	}
	
	/**
	 * Build up our initial army. This state should only be called after the
	 * initial split.
	 */
	private void buildInitialArmy() throws GameActionException {
		// try to build a soldier
		for (Direction d : Direction.values()) {
			if (d == Direction.OMNI || d == Direction.NONE) {
				continue;
			}
			if (spawnUnitInDir(RobotType.SOLDIER, d)) {
				initialSoldiersBuilt++;
				break;
			}
		}
		// distribute flux
		fbs.setBattleMode();
		fbs.manageFlux();
	}
	
	/**
	 * Rush across the map and pwn some n00bs.
	 */
	private void rush() throws GameActionException {
		// set micro mode
		micro.setMoonwalkMode();
		micro.toggleAvoidPowerNodes(false);
		// set objective
		micro.setObjective(rally.getCurrentObjective());
		// try to build a soldier
		for (Direction d : Direction.values()) {
			if (d == Direction.OMNI || d == Direction.NONE) {
				continue;
			}
			if (spawnUnitInDir(RobotType.SOLDIER, d)) {
				break;
			}
		}
		// go to objective
		micro.attackMove();
		// distribute flux
		fbs.manageFlux();
		// send rally
		rally.broadcastRally();
	}
	
	/**
	 * Build towers and G him.
	 */
	private void cap() throws GameActionException {
		// set micro mode
		micro.setNormalMode();
		micro.toggleAvoidPowerNodes(true);
		// set objective
		MapLocation powerNode = mc.guessBestPowerNodeToCapture();
		micro.setObjective(powerNode);
		int distance = curLoc.distanceSquaredTo(powerNode);
		// build tower if close enough, otherwise move towards power node
		if (distance == 0) {
			// TODO(jven): back up
		} else if (distance <= 2) {
			spawnUnitInDir(RobotType.TOWER, curLoc.directionTo(powerNode));
		} else {
			micro.attackMove();
		}
	}
	
	/**
	 * Engage the enemy forces.
	 */
	private void engage() throws GameActionException {
		// abort if no enemy nearby
		if (dc.getClosestEnemy() == null) {
			return;
		}
		// set micro mode
		micro.setKiteMode(ArchonConstants.ARCHON_ENGAGE_DISTANCE);
		micro.toggleAvoidPowerNodes(true);
		// set objective
		micro.setObjective(dc.getClosestEnemy().location);
		// try to build a soldier in front of me
		spawnUnitInDir(RobotType.SOLDIER, curDir);
		// kite enemy
		micro.attackMove();
		// distribute flux
		fbs.manageFlux();
		// send rally
		rally.broadcastRally();
	}
	
	/**
	 * Tries to spawn a unit of the given type in the given direction. Returns
	 * true if successful, false otherwise. Will turn in direction if not facing
	 * the right way.
	 * @param type The type of robot to spawn
	 * @param dir The direction to spawn the unit
	 * @modifies Might modify direction and flux
	 * @return Whether the method was successful
	 */
	private boolean spawnUnitInDir(
			RobotType type, Direction dir) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {	
			return false;
		}
		// return if not enough flux
		if (rc.getFlux() < type.spawnCost) {
			return false;
		}
		// return if terrain tile is bad
		if ((type.level == RobotLevel.ON_GROUND &&
				!rc.canMove(dir)) ||
				(type.level == RobotLevel.IN_AIR &&
				rc.senseTerrainTile(curLoc.add(dir)) == TerrainTile.OFF_MAP)) {
			return false;
		}
		// return if unit is in the way
		if (dc.getAdjacentGameObject(dir, type.level) != null) {
			return false;
		}
		// turn in direction to spawn
		if (curDir != dir) {
			rc.setDirection(dir);
			return false;
		}
		// spawn unit
		rc.spawn(type);
		return true;
	}
}
