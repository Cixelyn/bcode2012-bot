package ducks;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class SoldierRobotCL extends BaseRobot {
	
	private enum BehaviorState { HIBERNATE, DEFEND, RAPE }


	final HibernationSystem hbs;
	BehaviorState behavior;
	
	public SoldierRobotCL(RobotController myRC) throws GameActionException {
		super(myRC);
		
		hbs = new HibernationSystem(this);
		
		io.addChannel(BroadcastChannel.ALL);
		io.addChannel(BroadcastChannel.SOLDIERS);
		fbs.setBattleMode();
	}

	@Override
	public void run() throws GameActionException {
		rc.setIndicatorString(0,behavior.toString());
		
		
		switch(behavior) {
		case HIBERNATE:
			break;
		case DEFEND:
			break;
		case RAPE:
			break;
		}
		

	}
	


}
