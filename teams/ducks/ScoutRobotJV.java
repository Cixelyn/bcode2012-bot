package ducks;


import battlecode.common.GameActionException;
import battlecode.common.RobotController;

/**
 * ScoutRobotJV
 * 
 * @author jven
 */
public class ScoutRobotJV extends BaseRobot {
	
	/**
	 * TODO(jven): Move this stuff out of here
	 * Constants used by the Scout.
	 */
//	private static class ScoutConstants {
//	}
	
	/**
	 * TODO(jven): Move this stuff out of here
	 * All possible states for the Scout. See the private void methods for
	 * each state for details.
	 */
	private enum ScoutState {
		INITIALIZE,
		WIRE
	}
	
	/** The current state of the Scout. */
	private ScoutState curState;
	
	/** Whether the Scout is done initializing. */
	private boolean initialized;

	public ScoutRobotJV(RobotController myRC) {
		super(myRC);
		// set initial state
		curState = ScoutState.INITIALIZE;
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
			case WIRE:
				wire();
				break;
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
			case MAP_EDGES:
				ses.receiveMapEdges(BroadcastSystem.decodeUShorts(sb));
				break;
			case MAP_FRAGMENTS:
				ses.receiveMapFragment(BroadcastSystem.decodeInts(sb));
				break;
			case POWERNODE_FRAGMENTS:
				ses.receivePowerNodeFragment(BroadcastSystem.decodeInts(sb));
				break;
			case WIRE_REQUEST:
				sws.broadcastWireAccept(BroadcastSystem.decodeShort(sb));
				break;
			case WIRE_CONFIRM:
				sws.rebroadcastWireConfirm(BroadcastSystem.decodeUShorts(sb));
				break;
			default:
				this.processMessage(msgType, sb);
				break;
		}
	}
	
	/** Returns the state the Scout should execute this turn, using the current
	 * state.
	 * @modifies Must not modify the state of the Scout in any way!
	 * @returns The state to execute this turn.
	 */
	private ScoutState getNextState() throws GameActionException {
		// check if we should transition
		switch (curState) {
			case INITIALIZE:
				// if we're done initializing, start exploring
				if (initialized) {
					return ScoutState.WIRE;
				}
				break;
			case WIRE:
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
	 * Initializes the Scout. The Scout should only ever enter this state once:
	 * when it is born
	 */
	private void initialize() throws GameActionException {
		// set navigation mode
		nav.setNavigationMode(NavigationMode.GREEDY);
		// initialize broadcast system
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.SCOUTS,
				BroadcastChannel.EXPLORERS
		});
		// initialize map cache
		mc.senseAll();
		// done initializing
		initialized = true;
	}
	
	/**
	 * Form a wire.
	 */
	private void wire() throws GameActionException {
		// set micro mode
		micro.setChargeMode();
		if (sws.isOnWire()) {
			// go to my wire location
			micro.setObjective(sws.getMyWireLocation());
			micro.attackMove();
		} else {
			// go to closest archon
			if (dc.getClosestArchon() != null) {
				micro.setObjective(myHome);
				micro.attackMove();
			}
		}
	}
}
