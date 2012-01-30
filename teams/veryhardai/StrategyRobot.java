package veryhardai;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class StrategyRobot extends BaseRobot {

	private RobotState currState;
	
	public StrategyRobot(RobotController myRC, RobotState initialState) throws GameActionException {
		super(myRC);
		currState = initialState;
	}

	@Override
	public void run() throws GameActionException {
		gotoState(processTransitions(currState));
		execute(currState);
	}

	/**
	 * Returns the current Robot's state
	 * @return
	 */
	public RobotState getCurrentState() {
		return currState;
	}
	
	/**
	 * returns which state to transition to.
	 * (return the same state if not transitioning
	 * Use this to setup the normal state transition diagram
	 */
	public abstract RobotState processTransitions(RobotState state) throws GameActionException;

	
	
	/**
	 * ONLY CALL WHEN PROCESSING MESSAGES OTHERWISE YOU GET G'ED
	 * Because decoupling is impossible for 
	 * @param newstate - state you want to switch into
	 * @throws GameActionException
	 */
	public void gotoState(RobotState newstate) throws GameActionException {
		if (newstate!=currState)
		{
			prepareTransition(newstate, currState);
			currState = newstate;
		}
	}

	
	/**
	 * set up fields for transitionoing from oldstate to newstate
	 */
	public abstract void prepareTransition(RobotState newstate, RobotState oldstate) throws GameActionException;
	
	/**
	 * execute the behavior for the given state
	 */
	public abstract void execute(RobotState state) throws GameActionException;
}
