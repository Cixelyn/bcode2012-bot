package ypstrategytest;

import com.sun.org.apache.xml.internal.utils.StopParseException;

public class StrategyElement  {
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