package ducks;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class SoldierRobotJVBackdoor extends BaseRobot {

	private MapLocation objective;
	
	public SoldierRobotJVBackdoor(RobotController myRC)
			throws GameActionException {
		super(myRC);
		// set broadcast channels
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
		});
		// set flux balance mode
		fbs.setPoolMode();
		// set navigation mode
		nav.setNavigationMode(NavigationMode.BUG);
		// set initial objective
		objective = myHome;
	}

	@Override
	public void run() throws GameActionException {
		// suicide if not enough flux
		if (rc.getFlux() < 2.0) {
			rc.suicide();
		}
		// scan
		radar.scan(true, true);
		// set destination
		nav.setDestination(objective);
		// attack if you can
		if (!rc.isAttackActive() && radar.closestEnemy != null &&
				rc.canAttackSquare(radar.closestEnemy.location)) {
			rc.attackSquare(radar.closestEnemy.location,
					radar.closestEnemy.robot.getRobotLevel());
		}
	}
	
	@Override
	public void processMessage(
			BroadcastType type, StringBuilder sb) throws GameActionException {
		switch (type) {
			case RALLY:
				objective = BroadcastSystem.decodeMapLoc(sb);
				break;
			default:
				super.processMessage(type, sb);
		}
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		if (curLoc.distanceSquaredTo(objective) < 4) {
			return new MoveInfo(curLoc.directionTo(objective));
		} else {
			return new MoveInfo(nav.navigateToDestination(), false);
		}
	}

}
