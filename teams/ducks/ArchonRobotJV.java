package ducks;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class ArchonRobotJV extends BaseRobot {
	
	private enum ArchonState {
		INITIALIZE,
		DIZZY
	}
	
	private ArchonState curState;
	
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
			case DIZZY:
				dizzy();
				break;
			default:
				// we got g'd
				rc.suicide();
				break;
		}
	}
	
	private ArchonState getNextState() throws GameActionException {
		// check if we should transition
		switch (curState) {
			case INITIALIZE:
				if (initialized) {
					return ArchonState.DIZZY;
				}
		}
		// if we didn't transition, stay in the current state
		return curState;
	}
	
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
	
	private void dizzy() throws GameActionException {
		// set micro mode
		mi.setHoldPositionMode();
		// spin around!
		mi.attackMove();
	}

}
