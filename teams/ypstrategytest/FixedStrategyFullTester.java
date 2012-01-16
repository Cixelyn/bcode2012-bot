package ypstrategytest;

public class FixedStrategyFullTester extends FixedStrategyFull
{

	public FixedStrategyFullTester(StrategyElement[] base)
	{
		super(base);
	}
	
	private static final StrategyElement[] STRATEGY = new StrategyElement[] {
//		set up strategy here
		new StrategyElement(StrategyConstants.values[0], 2, 	0, 		1000),
		new StrategyElement(StrategyConstants.values[1], 5, 	0, 		500),
		new StrategyElement(StrategyConstants.values[2], 7, 	300,	650),
		new StrategyElement(StrategyConstants.values[3], 1, 	450, 	500),
		new StrategyElement(StrategyConstants.values[4], 4, 	600, 	700),
		new StrategyElement(StrategyConstants.values[5], 3, 	600, 	1200),
		new StrategyElement(StrategyConstants.values[6], 9, 	630, 	900),
		new StrategyElement(StrategyConstants.values[7], 4, 	750, 	845),
		new StrategyElement(StrategyConstants.values[8], 3, 	850, 	1200),
		new StrategyElement(StrategyConstants.values[9], 2, 	1000, 	1500),
		new StrategyElement(StrategyConstants.values[0], 2, 	1000, 	2000),
		new StrategyElement(StrategyConstants.values[1], 5, 	1000, 	1500),
		new StrategyElement(StrategyConstants.values[2], 7, 	1300,	1650),
		new StrategyElement(StrategyConstants.values[3], 1, 	1450, 	1500),
		new StrategyElement(StrategyConstants.values[4], 4, 	1600, 	1700),
		new StrategyElement(StrategyConstants.values[5], 3, 	1600, 	2200),
		new StrategyElement(StrategyConstants.values[6], 9, 	1630, 	1900),
		new StrategyElement(StrategyConstants.values[7], 4, 	1750, 	1845),
		new StrategyElement(StrategyConstants.values[8], 3, 	1850, 	2200),
		new StrategyElement(StrategyConstants.values[9], 2, 	2000, 	2500),
	};
	public FixedStrategyFullTester() {
		super(STRATEGY);
	}

	@Override
	public boolean execute(StrategyEnum strategy)
	{
		printStrategy(strategy);
		return strategy!=StrategyEnum.RUN;
	}
	
	public void printStrategy(StrategyEnum strategy)
	{
		System.out.println("executing "+strategy);
	}
}
