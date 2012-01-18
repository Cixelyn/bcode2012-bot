package ducks;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * SoldierRobotJV
 * 
 * @author jven
 */
public class SoldierRobotJV extends BaseRobot {
	
	/**
	 * TODO(jven): Move this stuff out of here
	 * Constants used by the Soldier.
	 */
	private static class SoldierConstants {
		public static final double MIN_SOLDIER_FLUX = 1.0;
		public static final int SWARM_RADIUS = 16;
	}
	
	/**
	 * TODO(jven): Move this stuff out of here
	 * All possible states for the Soldier. See the private void methods for
	 * each state for details.
	 */
	private enum SoldierState {
		INITIALIZE,
		WAIT_FOR_RALLY,
		RUSH,
		ENGAGE,
		LOW_FLUX
	}
	
	/** The current state of the Soldier. */
	private SoldierState curState;
	
	/** Whether the Soldier is done initializing. */
	private boolean initialized;
	
	/** Whether the Soldier has been rallied. */
	private boolean rallied = false;
	
	/** The objective of the Soldier. */
	private MapLocation objective;

	public SoldierRobotJV(RobotController myRC) {
		super(myRC);
		// set initial state
		curState = SoldierState.INITIALIZE;
	}

	@Override
	public void run() throws GameActionException {
		// transition to a new state if necessary
		curState = getNextState();
		// TODO(jven): debugging
		rc.setIndicatorString(0, myType + " - " + curState);
		// execute
		switch (curState) {
			case INITIALIZE:
				initialize();
				break;
			case WAIT_FOR_RALLY:
				waitForRally();
				break;
			case RUSH:
				rush();
				break;
			case ENGAGE:
				engage();
				break;
			case LOW_FLUX:
				return;
			default:
				// we got g'd
				rc.suicide();
				break;
		}
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb)
			throws GameActionException {
		switch (msgType) {
			case RALLY:
				rallied = true;
				objective = BroadcastSystem.decodeMapLoc(sb);
				break;
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	/** Returns the state the Soldier should execute this turn, using the current
	 * state.
	 * @modifies Must not modify the state of the Soldier in any way!
	 * @returns The state to execute this turn.
	 */
	private SoldierState getNextState() throws GameActionException {
		// if we're low on flux, power down
		if (rc.getFlux() < SoldierConstants.MIN_SOLDIER_FLUX) {
			return SoldierState.LOW_FLUX;
		}
		// check if we should transition
		switch (curState) {
			case INITIALIZE:
				// if we're done initializing, start spinning
				if (initialized) {
					return SoldierState.WAIT_FOR_RALLY;
				}
				break;
			case WAIT_FOR_RALLY:
				if (rallied) {
					return SoldierState.RUSH;
				}
				break;
			case RUSH:
				// if an enemy is nearby, engage
				if (dc.getClosestEnemy() != null) {
					return SoldierState.ENGAGE;
				}
				break;
			case ENGAGE:
				// if no enemy nearby, go back to rushing
				if (dc.getClosestEnemy() == null) {
					return SoldierState.RUSH;
				}
				break;
			case LOW_FLUX:
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
	 * Initializes the Soldier. The Soldier should only ever enter this state
	 * when he is born
	 */
	private void initialize() throws GameActionException {
		// initialize navigation system
		nav.setNavigationMode(NavigationMode.BUG);
		nav.setDestination(myHome);
		// initialize broadcast system
		io.setChannels(new BroadcastChannel[] {BroadcastChannel.ALL, BroadcastChannel.SOLDIERS});
		// avoid power nodes
		micro.toggleAvoidPowerNodes(true);
		// set an initial objective
		objective = myHome;
		// done initializing
		initialized = true;
	}

	/**
	 * Wait for rally. The Soldier should only ever enter this state right after
	 * initialization.
	 */
	private void waitForRally() throws GameActionException {
		// spin around!
		micro.setHoldPositionMode();
		micro.attackMove();
	}
	
	/**
	 * Rush the enemy. The Soldier can enter this state at any time.
	 */
	private void rush() throws GameActionException {
		// set micro mode
		micro.setSwarmMode(SoldierConstants.SWARM_RADIUS);
		// set objective
		micro.setObjective(objective);
		// swarm towards objective
		micro.attackMove();
	}
	
	/**
	 * Engage the enemy forces. The Soldier should only enter this state if an
	 * enemy is nearby.
	 */
	private void engage() throws GameActionException {
		// abort if no enemy nearby
		if (dc.getClosestEnemy() == null) {
			return;
		}
		// set micro mode
		micro.setChargeMode();
		// set objective
		micro.setObjective(dc.getClosestEnemy().location);
		// kite enemy
		micro.attackMove();
		// distribute flux
		fbs.manageFlux();
		// send rally
		rally.broadcastRally();
	}
}
