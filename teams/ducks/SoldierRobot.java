package ducks;

import ducks.Debug.Owner;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class SoldierRobot extends StrategyRobot {
	
	private boolean initialized;
	
	public SoldierRobot(RobotController myRC) {
		super(myRC, RobotState.INITIALIZE);
		initialized = false;
	}


	@Override
	public RobotState processTransitions(RobotState state)
			throws GameActionException {
		
		switch (state) {
			case INITIALIZE:
				if (initialized) {
					return RobotState.HOLD_POSITION;
				}
				break;
			default:
				break;
		}
		return state;
	}

	@Override
	public void prepareTransition(RobotState newstate, RobotState oldstate)
			throws GameActionException {
		switch (newstate) {
			case INITIALIZE:
				initialized = false;
				break;
			case HOLD_POSITION:
				// set micro mode
				mi.setHoldPositionMode();
				// set flux management mode
				fm.setBatteryMode();
				break;
			default:
				break;
		}
		
	}

	@Override
	public void execute(RobotState state) throws GameActionException {
		// TODO(jven): use hibernation engine instead
		// power down if not enough flux
		if (currFlux < Constants.MIN_ROBOT_FLUX) {
			debug.setIndicatorString(0, "" + myType + " - LOW FLUX", Owner.ALL);
			return;
		}
		switch (state) {
			case INITIALIZE:
				initialize();
				break;
			case HOLD_POSITION:
				holdPosition();
				break;
			default:
				break;
		}
	}
	
	@Override
	public void processMessage(
			char msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
			case 'd':
				eai.reportEnemyArchonKills(Radio.decodeShorts(sb));
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	public void initialize() throws GameActionException {
		// set navigation mode
		nav.setNavigationMode(NavigationMode.BUG);
		// set radio addresses
		io.setAddresses(new String[] {"#x", "#s"});
		// sense all
		mc.senseAll();
		// done
		initialized = true;
	}
	
	public void holdPosition() throws GameActionException {
		// hold position
		mi.attackMove();
		// distribute flux
		fm.manageFlux();
		// send dead enemy archon info
		eai.sendDeadEnemyArchonIDs();
	}
}
