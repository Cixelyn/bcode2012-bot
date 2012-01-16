package ypstrategytest;

public class StrategyPart {
	public final StrategyEnum strategy;
	public final int priority;
	
	public StrategyPart(StrategyEnum strategy, int priority) {
		this.strategy = strategy;
		this.priority = priority;
	}
	
	@Override
	public String toString() {
		return priority+" "+strategy;
	}
}
