package ducks;

import ducks.Debug.Owner;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public abstract class StrategyRobotExtended extends BaseRobot {

	private final static int MAX_STATE_STACK_SIZE = 10;
	
	private final RobotState[] stateStack;
	private int stack_size;
	private RobotState curState;
	
	public StrategyRobotExtended(RobotController myRC, RobotState initialState) {
		super(myRC);
		stateStack = new RobotState[MAX_STATE_STACK_SIZE];
		stack_size = 0;
		curState = initialState;
	}

	@Override
	public void run() throws GameActionException {

		// show state of robot
		debug.setIndicatorString(0,"" + myType + " - " + curState, Owner.ALL);

		initializeForRound();
		
		gotoState(processTransitions(curState));
		execute(curState);
	}
	
	
	/**
	 * Pushes the given state over the current state. 
	 * Saves the current state to the state stack.
	 * Should only be called to push a different state than the current state,
	 * on successful push, will call gotoState(newState)
	 */
	protected void pushState(RobotState newState) throws GameActionException
	{
		if (curState == newState)
		{
			debug.println("Tried to push same state: "+newState);
		} else
		{
			stateStack[stack_size++] = curState;
			gotoState(newState);
		}
	}
	
	/**
	 * Pops the last state from the stateStack and calls gotoState() on it
	 */
	protected void popState() throws GameActionException
	{
		if (stack_size == 0)
		{
			debug.println("Tried to pop empty stack, curstate: "+curState);
		} else
		{
			gotoState(stateStack[--stack_size]);
		}
	}


	/**
	 * Returns the current Robot's state
	 * @return
	 */
	public RobotState getCurrentState() {
		return curState;
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
		if (newstate!=curState)
		{
			prepareTransition(newstate, curState);
			curState = newstate;
		}
	}

	/**
	 * do any special things you need to do at the beginning at a new round
	 */
	public abstract void initializeForRound();
	
	/**
	 * set up fields for transitionoing from oldstate to newstate
	 */
	public abstract void prepareTransition(RobotState newstate, RobotState oldstate) throws GameActionException;
	
	/**
	 * execute the behavior for the given state
	 */
	public abstract void execute(RobotState state) throws GameActionException;
}
