package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldierRobotJV extends BaseRobot {
	
	private int rallyPriority;
	private MapLocation objective;
	
	private int timeUntilBroadcast;

	public SoldierRobotJV(RobotController myRC) {
		super(myRC);
		currState = RobotState.INITIALIZE;
	}

	@Override
	public void run() throws GameActionException {
		// power down if not enough flux
		if (currFlux < Constants.POWER_DOWN_FLUX) {
			return;
		}
		switch (currState) {
			case INITIALIZE:
				initialize();
				break;
			case POWER_SAVE:
				powerSave();
				break;
			case RUSH:
				rush();
				break;
			case CHASE:
				chase();
				break;
			default:
				break;
		}
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb) {
		int[] msgInts;
		//MapLocation[] msgLocs;
		switch(msgType) {
			case 'r':
				if (currState == RobotState.POWER_SAVE) {
					currState = RobotState.RUSH;
				}
				msgInts = Radio.decodeInts(sb);
				int msgRallyPriority = msgInts[0];
				MapLocation msgObjective = new MapLocation(msgInts[1], msgInts[2]);
				if (msgRallyPriority > rallyPriority) {
					objective = msgObjective;
					rallyPriority = msgRallyPriority;
				}
				break;
			case 'd':
				int[] deadEnemyArchonIDs = Radio.decodeInts(sb);
				for (int id : deadEnemyArchonIDs) {
					enemyArchonInfo.reportEnemyArchonKill(id);
				}
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	public void initialize() throws GameActionException {
		// set nav mode
		nav.setNavigationMode(NavigationMode.BUG);
		// set radio addresses
		io.setAddresses(new String[] {"#x", "#s"});
		// set initial objective
		rallyPriority = -1;
		objective = currLoc.add(
				rc.sensePowerCore().getLocation().directionTo(
				dc.getCapturablePowerCores()[0]), GameConstants.MAP_MAX_HEIGHT);
		// set broadcast time
		timeUntilBroadcast = 0;
		// go to power save mode
		currState = RobotState.POWER_SAVE;
		powerSave();
	}
	
	public void powerSave() throws GameActionException {
		// attack closest enemy if possible
		attackClosestEnemy();
		// spin
		if (!rc.isMovementActive()) {
			rc.setDirection(currDir.rotateLeft().rotateLeft().rotateLeft());
		}
	}
	
	public void rush() throws GameActionException {
		//rc.suicide();
		// swarm towards objective
		swarmTowards(objective);
		// chase enemy if in range
		if (dc.getClosestEnemy() != null) {
			currState = RobotState.CHASE;
		}
		// send dead enemy archon IDs
		sendDeadEnemyArchonIDs();
	}
	
	public void chase() throws GameActionException {
		// go back to previous state if no enemy in range
		RobotInfo closestEnemy = dc.getClosestEnemy();
		if (closestEnemy == null) {
			currState = RobotState.RUSH;
			return;
		}
		// attack closest enemy
		attackClosestEnemy();
		// charge enemy
		charge(closestEnemy.location);
		// send dead enemy archon IDs
		sendDeadEnemyArchonIDs();
	}
	
	private void attackClosestEnemy() throws GameActionException {
		// wait if attack is active
		if (rc.isAttackActive()) {
			return;
		}
		// see if enemy in range
		RobotInfo closestEnemy = dc.getClosestEnemy();
		if (closestEnemy != null && rc.canAttackSquare(closestEnemy.location)) {
			rc.attackSquare(
					closestEnemy.location, closestEnemy.robot.getRobotLevel());
			if (closestEnemy.type == RobotType.ARCHON &&
					closestEnemy.energon <= myType.attackPower) {
				enemyArchonInfo.reportEnemyArchonKill(closestEnemy.robot.getID());
			}
		}
	}
	
	private void swarmTowards(MapLocation target) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// step towards closest archon if too far away
		MapLocation closestArchon = dc.getClosestArchon();
		if (closestArchon != null &&
				currLoc.distanceSquaredTo(closestArchon) > Constants.MAX_SWARM_RADIUS) {
			target = closestArchon;
		}
		// sense tiles
		mc.senseAllTiles();
		// move towards target
		Direction dir = nav.navigateTo(target);
		if (dir != Direction.OMNI && dir != Direction.NONE) {
			// TODO(jven): wiggle code should not be here
			for (int tries = 0; tries < Constants.WIGGLE_TIMEOUT; tries++) {
				if (!rc.canMove(dir)) {
					if (Math.random() < 0.5) {
						dir = dir.rotateLeft();
					} else {
						dir = dir.rotateRight();
					}
				} else {
					break;
				}
			}
			if (!rc.canMove(dir)) {
				return;
			}
			// end wiggle code
			if (currDir != dir) {
				rc.setDirection(dir);
			} else if (rc.canMove(currDir)) {
				rc.moveForward();
			}
		}
	}
	
	private void charge(MapLocation target) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		/*
		// turn in direction of target
		Direction[] targetDirs = new Direction[] {
				currLoc.directionTo(target),
				currLoc.directionTo(target).rotateLeft(),
				currLoc.directionTo(target).rotateRight()
		};
		// move towards target
		for (Direction d : targetDirs) {
			if (rc.canMove(d.opposite())) {
				if (rc.canMove(currDir)) {
					rc.moveForward();
				}
				return;
			}
		}
		*/
		Direction dir = currLoc.directionTo(target);
		if (currDir != dir) {
			rc.setDirection(dir);
		} else if (rc.canMove(dir)) {
			rc.moveForward();
		}
	}
	
	private void sendDeadEnemyArchonIDs() throws GameActionException {
		if (--timeUntilBroadcast <= 0) {
			io.sendInts("#xd", enemyArchonInfo.getDeadEnemyArchonIDs());
			timeUntilBroadcast = Constants.SOLDIER_BROADCAST_FREQUENCY;
		}
	}
}
