package ypstrategytest;


public abstract class Strategy {
	
	/**
	 * initialize the strategy for the given round number
	 */
	public abstract void initForRound(int round);
	
	/**
	 * acts for the given round based on the strategy and execute
	 */
	public abstract void act();
	
	/**
	 * maps StrategyEnum objects to function calls.
	 * Returns true if act() should end after this call
	 * (if it returns false, act() will try to execute more actions by priority)
	 */
	public abstract boolean execute(StrategyEnum strategy);
	
	/**
	 * adds the given strategy element to the current round of things to consider
	 */
	public abstract void addForRound(StrategyElement elt);
}
