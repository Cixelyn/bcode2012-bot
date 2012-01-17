package ducks;

import ducks.Debug.Owner;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class ScoutRobot extends StrategyRobot {
	
	private final HibernationSystem hbe;

	public ScoutRobot(RobotController myRC) {
		super(myRC, RobotState.DEFEND_BASE);
		mi.setObjective(myHome);
		mi.setChargeMode();
		
		hbe = new HibernationSystem(this);
		io.setAddresses(new String[]{"#d"});
	}

	@Override
	public RobotState processTransitions(RobotState state)
			throws GameActionException {
		return state;
	}

	@Override
	public void prepareTransition(RobotState newstate, RobotState oldstate)
			throws GameActionException {
		return;
	}

	@Override
	public void execute(RobotState state) throws GameActionException {
		mi.attackMove();
		ur.scan(true, false);
		if(ur.numAllyDamaged >= 2) {
			rc.regenerate();
		}
		debug.setIndicatorString(2, Integer.toString(ur.numAllyDamaged), Owner.YP);
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
			case 'z':
			{	
				hbe.run();
			}
			
		}
		
	}
}
