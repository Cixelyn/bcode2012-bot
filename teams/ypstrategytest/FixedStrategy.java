package ypstrategytest;


public abstract class FixedStrategy extends Strategy {
	
	final StrategyElement[] baseStrategy;
	StrategyQueue baseQueue;
	StrategyQueue baseRest;
	StrategyElement[] roundStrategy;
	StrategyElement[] roundStrategyBackup;
//	StrategyElement[] newRoundStrategy;
	int baseIndex;
//	int newRoundIndex;
	int newRoundSize;
//	int roundIndex;
	int roundSize;
	StrategyElement best;
	int bestindex;
	
	private static final StrategyElement WORST_STRATEGY = new StrategyElement(null, -999, 0, 0);
	private static final int ARRAYLEN = StrategyConstants.MAX_ROUND_STRATEGY_ELEMENTS+StrategyConstants.MAX_NEWROUND_STRATEGY_ELEMENTS;
	
	public FixedStrategy(StrategyElement[] base) {
		baseStrategy = base;
		roundStrategy = new StrategyElement[ARRAYLEN];
		roundStrategyBackup = new StrategyElement[ARRAYLEN];
//		newRoundStrategy = new StrategyElement[StrategyConstants.MAX_NEWROUND_STRATEGY_ELEMENTS];
		baseIndex = 0;
//		roundIndex = 0;
		roundSize = 0;
//		newRoundIndex = 0;
		newRoundSize = ARRAYLEN-1;
		best = WORST_STRATEGY;
		bestindex = -1;
	}

	@Override
	public void initForRound(int round) {
//		newRoundIndex = 0;
		newRoundSize = 0;
		cleanUp(round);
		StrategyElement temp;
		
		for (; baseIndex<baseStrategy.length; baseIndex++)
		{
			temp = baseStrategy[baseIndex];
			if (temp.startRound > round) break;
			if (temp.endRound < round) break;
			roundStrategy[roundSize++] = temp;
			if (temp.priority>best.priority)
			{
				best = temp;
				bestindex = roundSize;
			}
//			System.out.println("adding strategy "+temp);
		}
		System.arraycopy(roundStrategy, 0, roundStrategyBackup, 0, roundSize);
//		System.out.println("total strategies: "+roundSize);
	}
	
	public void cleanUp(int round)
	{
		int oldrs = roundSize;
		StrategyElement temp;
		StrategyElement[] newround = new StrategyElement[ARRAYLEN];
		
		roundSize = 0;
		best = WORST_STRATEGY;
		for (int x=0; x<oldrs; x++)
		{
			temp = roundStrategyBackup[x];
			if (temp==null || temp.endRound < round) continue;
			if (temp.priority>best.priority)
			{
				best = temp;
				bestindex = roundSize;
			}
			newround[roundSize++] = temp;
			
		}
		roundStrategy = newround;
	}
	
	@Override
	public void addForRound(StrategyElement elt) 
	{
		roundStrategy[--newRoundSize] = elt;
		if (elt.priority>best.priority)
		{
			best = elt;
			bestindex = newRoundSize;
		}
	}
	
	private void findBestStrategy()
	{
		int bestp = -999;
		StrategyElement beststrat = WORST_STRATEGY;
		int besti = -1;
		StrategyElement temp;
		for (int x=newRoundSize; x<ARRAYLEN; x++)
		{
			temp = roundStrategy[x];
			if (temp!=null && temp.priority>bestp)
			{
				bestp = temp.priority;
				besti = x;
				beststrat = temp;
			}
		}
		for (int x=0; x<roundSize; x++)
		{
			temp = roundStrategy[x];
			if (temp!=null && temp.priority>bestp)
			{
				bestp = temp.priority;
				besti = x;
				beststrat = temp;
			}
		}
		best = beststrat;
		bestindex = besti;
	}
	
	@Override
	public void act() 
	{
		StrategyElement exec;
		while (true)
		{
//			by the time we act, we should already have a best strategy
			exec = best;
			best = WORST_STRATEGY;
			roundStrategy[bestindex] = null;
			if (execute(exec.action)) return;
			
			findBestStrategy();
			if (best==WORST_STRATEGY) return;
		}
	}
}
