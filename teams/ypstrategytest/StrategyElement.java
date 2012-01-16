package ypstrategytest;

import java.io.Serializable;


public class StrategyElement  implements Serializable {
	private static final long serialVersionUID = 3795294744889692516L;
	public final StrategyEnum action;
	public final int priority;
	public final int startRound;
	public final int endRound;
	public StrategyElement(StrategyEnum act, int priority, int start, int end) 
	{
		this.action = act;
		this.priority = priority;
		this.startRound = start;
		this.endRound = end;
	}
	
	@Override
	public String toString() {
		return priority+" "+action+" "+startRound+" "+endRound;
	}
}
