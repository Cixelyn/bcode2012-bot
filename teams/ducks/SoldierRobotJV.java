package ducks;

import battlecode.common.GameActionException;
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
	}
	
	/**
	 * TODO(jven): Move this stuff out of here
	 * All possible states for the Soldier. See the private void methods for
	 * each state for details.
	 */
	private enum SoldierState {
		INITIALIZE,
		DIZZY,
		SUICIDE,
		LOW_FLUX
	}
	
	/** The current state of the Soldier. */
	private SoldierState curState;
	
	/** Whether the Soldier is done initializing. */
	private boolean initialized;
	
	/** Whether the Soldier has been rallied. */
	private boolean rallied = false;

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
			case DIZZY:
				dizzy();
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
	public void processMessage(char msgType, StringBuilder sb)
			throws GameActionException {
		switch (msgType) {
			case 'r':
				rallied = true;
				break;
			default:
				break;
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
					return SoldierState.DIZZY;
				}
				break;
			case DIZZY:
				if (rallied) {
					return SoldierState.SUICIDE;
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
	 * Initializes the Soldier. The Soldier should only ever enter this state
	 * when he is born
	 */
	private void initialize() throws GameActionException {
		// initialize navigation system
		nav.setNavigationMode(NavigationMode.BUG);
		nav.setDestination(myHome);
		// initialize broadcast system
		io.setAddresses(new String[] {"#x", "#s"});
		// done initializing
		initialized = true;
	}

	/**
	 * Weeeeeee!!!!
	 */
	private void dizzy() throws GameActionException {
		// spin around!
		micro.setHoldPositionMode();
		micro.attackMove();
	}

}
