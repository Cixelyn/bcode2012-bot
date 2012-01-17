package ducks;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

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
		public static int ROUND_TO_STOP_EXPLORING = 100;
		public static int HOME_RADIUS = 9;
		public static int ARCHON_SPLIT_DISTANCE = 16;
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
		BUILD_INITIAL_ARMY
	}
	
	/** The current state of the Archon. */
	private ArchonState curState;
	
	/** Whether the Archon is done initializing. */
	private boolean initialized;

	public ArchonRobotJV(RobotController myRC) {
		super(myRC);
		// set initial state
		curState = ArchonState.INITIALIZE;
	}

	@Override
	public void run() throws GameActionException {
		// transition to a new state if necessary
		curState = getNextState();
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
			default:
				// we got g'd
				rc.suicide();
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
		io.setAddresses(new String[] {"#x", "#a"});
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
		mi.setNormalMode();
		// set objective for some place far away from home, in a different direction
		// from the other Archons
		mi.setObjective(myHome.add(myHome.directionTo(birthplace),
				GameConstants.MAP_MAX_HEIGHT));
		// TODO(jven): explore more haphazardly to fill in gaps
		// tangent bug to destination
		mi.attackMove();
	}
	
	/**
	 * Return home. This state can be called at any time.
	 */
	private void returnHome() throws GameActionException {
		// set micro mode
		mi.setNormalMode();
		// set objective for home
		mi.setObjective(myHome);
		// tangent bug to home
		mi.attackMove();
	}
	
	/**
	 * Try to get away from other archons. This state can be called at any time.
	 */
	private void split() throws GameActionException {
		// set micro mode
		mi.setKiteMode(ArchonConstants.ARCHON_SPLIT_DISTANCE);
		// set objective for closest archon
		mi.setObjective(dc.getClosestArchon());
		// kite closest archon
		mi.attackMove();
	}
	
	/**
	 * Build up our initial army. This state should only be called after the
	 * initial split.
	 */
	private void buildInitialArmy() throws GameActionException {
		
	}

}
