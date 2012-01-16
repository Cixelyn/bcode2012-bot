package ducks;

import ducks.Debug.Owner;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class StrategyRobot extends BaseRobot {

	private RobotState currState;
	
	public StrategyRobot(RobotController myRC, RobotState initialState) {
		super(myRC);
		currState = initialState;
	}

	@Override
	public void run() throws GameActionException {

		// show state of robot
		debug.setIndicatorString(0,"" + myType + " - " + currState, Owner.ALL);
		
		gotoState(processTransitions(currState));
		execute(currState);
	}
	
	public RobotState getCurrentState() {
		return currState;
	}
	
	/**
	 * returns which state to transition to.
	 * (return the same state if not transitioning)
	 */
	public abstract RobotState processTransitions(RobotState state) throws GameActionException;
	
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
