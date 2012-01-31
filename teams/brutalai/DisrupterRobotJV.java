package brutalai;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class DisrupterRobotJV extends BaseRobot {
	
	private MapLocation objective;
	private int objectiveTime = -1;
	
	public DisrupterRobotJV(RobotController myRC) throws GameActionException {
		super(myRC);
		
		// set broadcast channels
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.DISRUPTERS
		});
		
		// set navigation mode
		nav.setNavigationMode(NavigationMode.BUG);
	}

	@Override
	public void run() throws GameActionException {
		// suicide if not enough flux
		if (rc.getFlux() < 3.0) {
			rc.suicide();
		}
		
		// scan
		radar.scan(true, true);
		
		// set flux balance mode
		fbs.setPoolMode();
		
		// set objective to nearby enemies
		if (radar.closestEnemy != null) {
			objective = radar.closestEnemy.location;
			objectiveTime = curRound;
		}
		
		// set navigation destination
		if (objectiveTime != -1 && curRound - objectiveTime < 30) {
			nav.setDestination(objective);
		} else if (dc.getClosestArchon() != null) {
			nav.setDestination(dc.getClosestArchon());
		} else {
			nav.setDestination(myHome);
		}
		
		// attack if you should
		if (!rc.isAttackActive() && radar.closestEnemy != null &&
				rc.canAttackSquare(radar.closestEnemy.location)) {
			rc.attackSquare(radar.closestEnemy.location,
					radar.closestEnemy.robot.getRobotLevel());
		}
		
		// indicator strings
		dbg.setIndicatorString('j', 0, "sup");
	}
	
	@Override
	public void processMessage(
			BroadcastType type, StringBuilder sb) throws GameActionException {
		switch (type) {
			case RALLY:
				objective = BroadcastSystem.decodeMapLoc(sb);
				objectiveTime = curRound;
				break;
			default:
				super.processMessage(type, sb);
		}
	}

	@Override
	public MoveInfo computeNextMove() {
		// if you see an enemy, kite it if it's a non-tower...
		if (radar.closestEnemy != null) {
			Direction dir = curLoc.directionTo(radar.closestEnemy.location);
			if (radar.closestEnemy.type == RobotType.TOWER) {
				return new MoveInfo(dir);
			} else {
				return new MoveInfo(dir.opposite(), true);
			}
			
		// ...otherwise, if you're far from your target, move towards it
		// CONSERVATIVELY
		} else if (curLoc.distanceSquaredTo(nav.getDestination()) > 8) {
			Direction dir = nav.navigateToDestination();
			if (dir != null && dir != Direction.NONE && dir != Direction.OMNI) {
				if (rc.canMove(dir)) {
					return new MoveInfo(dir, false);
				} else {
					return new MoveInfo(dir);
				}
			} else {
				return null;
			}
			
		// ...otherwise, if you're at a good distance from the target, spin around
		} else if (curLoc.distanceSquaredTo(nav.getDestination()) > 2) {
			return new MoveInfo(curDir.rotateLeft().rotateLeft().rotateLeft());
			
		// ...otherwise, if you're too close to the target, get away from it
		} else {
			return new MoveInfo(nav.navigateRandomly(myHome), true);
		}
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
		super.useExtraBytecodes();
	}
	
}
