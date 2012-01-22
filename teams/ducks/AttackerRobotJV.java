package ducks;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class AttackerRobotJV extends BaseRobot {
	
	/** The possible behaviors for the Attacker. */
	private enum BehaviorState {
		RAPE
	}
	
	private BehaviorState behavior;
	private MapLocation objective;
	private int objectiveTime = -1;
	private final double movementProbability;
	
	public AttackerRobotJV(RobotController myRC) throws GameActionException {
		super(myRC);
		// set initial state
		behavior = BehaviorState.RAPE;
		// set broadcast channels
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL
		});
		// set navigation mode
		nav.setNavigationMode(NavigationMode.BUG);
		// probability of moving
		movementProbability = myType == RobotType.SCORCHER ? 0.03 : 0.7;
	}

	@Override
	public void run() throws GameActionException {
		rc.setIndicatorString(0, "ATTACKER - " + behavior);
		// suicide if not enough flux
		if (rc.getFlux() < 3.0) {
			rc.suicide();
		}
		// scan
		radar.scan(true, true);
		// switch states if necessary
		switch (behavior) {
			case RAPE:
				break;
			default:
				break;
		}
		// set flux balance mode
		switch (behavior) {
			case RAPE:
				fbs.setPoolMode();
				break;
			default:
				break;
		}
		// set objective to nearby enemies
		switch (behavior) {
			case RAPE:
				if (radar.closestEnemy != null) {
					objective = radar.closestEnemy.location;
					objectiveTime = curRound;
				}
				break;
			default:
				break;
		}
		// set navigation destination
		switch (behavior) {
			case RAPE:
				if (objectiveTime != -1 && curRound - objectiveTime < 30) {
					nav.setDestination(objective);
				} else if (dc.getClosestArchon() != null) {
					nav.setDestination(dc.getClosestArchon());
				} else {
					nav.setDestination(myHome);
				}
				break;
			default:
				break;
		}
		// attack if you should
		if (!rc.isAttackActive()) {
			if (myType == RobotType.SCORCHER &&
					radar.numEnemyRobots > radar.numAllyRobots) {
				rc.attackSquare(null, null);
			} else if (myType == RobotType.SOLDIER && radar.closestEnemy != null &&
					rc.canAttackSquare(radar.closestEnemy.location)) {
				rc.attackSquare(radar.closestEnemy.location,
						radar.closestEnemy.robot.getRobotLevel());
			}
		}
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
		if (radar.closestEnemy != null) {
			if (myType == RobotType.SCORCHER) {
				return new MoveInfo(curLoc.directionTo(
						radar.closestEnemy.location).opposite(), true);
			} else {
				return new MoveInfo(curLoc.directionTo(
						radar.closestEnemy.location), false);
			}
		} else {
			if (Math.random() < movementProbability) {
				return new MoveInfo(nav.navigateToDestination(), false);
			} else {
				return new MoveInfo(curDir.opposite());
			}
		}
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
		super.useExtraBytecodes();
	}
}
