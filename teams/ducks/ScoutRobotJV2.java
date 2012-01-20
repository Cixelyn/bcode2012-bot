package ducks;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ScoutRobotJV2 extends BaseRobot {
	
	private enum StrategyState {
		RUSH,
	}
	
	private enum BehaviorState {
		CHARGE,
		KITE
	}
	
//	private static class MyConstants {
//	}
	
	private StrategyState strategy;
	private BehaviorState behavior;

	public ScoutRobotJV2(RobotController myRC) throws GameActionException {
		super(myRC);
		// set initial states
		strategy = StrategyState.RUSH;
		behavior = BehaviorState.CHARGE;
	}

	@Override
	public void run() throws GameActionException {
	}
	
	@Override
	public void processMessage(
			BroadcastType type, StringBuilder sb) throws GameActionException {
		switch (type) {
			case MAP_EDGES:
				ses.receiveMapEdges(BroadcastSystem.decodeUShorts(sb));
				break;
			case MAP_FRAGMENTS:
				ses.receiveMapFragment(BroadcastSystem.decodeInts(sb));
				break;
			case POWERNODE_FRAGMENTS:
				ses.receivePowerNodeFragment(BroadcastSystem.decodeInts(sb));
				break;
			default:
				super.processMessage(type, sb);
		}
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		return null;
	}
	
	@Override
	public void useExtraBytecodes() {
		// TODO(jven): check we didn't go over bytecodes within the method
		// share exploration
		if (Clock.getBytecodesLeft() > 3000 && Math.random() < 0.3) {
			ses.broadcastMapFragment();
		}
		if (Clock.getBytecodesLeft() > 1000 && Math.random() < 0.3) {
			ses.broadcastPowerNodeFragment();
		}
		if (Clock.getBytecodesLeft() > 1000 && Math.random() < 0.3) {
			ses.broadcastMapEdges();
		}
		// process shared exploration
		while (Clock.getBytecodesLeft() > 7000) {
			mc.extractUpdatedPackedDataStep();
		}
	}
}
