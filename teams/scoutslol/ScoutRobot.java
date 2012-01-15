package scoutslol;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ScoutRobot extends BaseRobot {

	private int rallyPriority;
	private Direction rushDirection;
	private MapLocation explorationTarget;
	private MapLocation objective;
	
	public ScoutRobot(RobotController myRC) {
		super(myRC);
		currState = RobotState.INITIALIZE;
	}

	@Override
	public void run() throws GameActionException {
		if (currFlux < Constants.POWER_DOWN_FLUX) {
			return;
		}
		switch (currState) {
			case INITIALIZE:
				initialize();
				break;
			case WAIT_FOR_FLUX:
				waitForFlux();
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
		switch(msgType) {
			case 'r':
				msgInts = Radio.decodeInts(sb);
				int msgRallyPriority = msgInts[0];
				Direction msgRushDirection = Direction.values()[msgInts[1]];
				if (msgRallyPriority > rallyPriority) {
					rushDirection = msgRushDirection;
					explorationTarget = currLoc.add(
							rushDirection, GameConstants.MAP_MAX_HEIGHT);
					objective = new MapLocation(msgInts[2], msgInts[3]);
					rallyPriority = msgRallyPriority;
				}
				break;
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	public void initialize() throws GameActionException {
		// initially unsure
		rallyPriority = 0;
		// set addresses
		io.setAddresses(new String[] {"#x"});
		// get rush direction
		boolean up = rc.senseTerrainTile(currLoc.add(Direction.NORTH, 6)) ==
				TerrainTile.OFF_MAP;
		boolean right = rc.senseTerrainTile(currLoc.add(Direction.EAST, 6)) ==
				TerrainTile.OFF_MAP;
		boolean down = rc.senseTerrainTile(currLoc.add(Direction.SOUTH, 6)) ==
				TerrainTile.OFF_MAP;
		boolean left = rc.senseTerrainTile(currLoc.add(Direction.WEST, 6)) ==
				TerrainTile.OFF_MAP;
		if (up && right) {
			rushDirection = Direction.SOUTH_WEST;
			rallyPriority = 2;
		} else if (right && down) {
			rushDirection = Direction.NORTH_WEST;
			rallyPriority = 2;
		} else if (down && left) {
			rushDirection = Direction.NORTH_EAST;
			rallyPriority = 2;
		} else if (left && up) {
			rushDirection = Direction.SOUTH_EAST;
			rallyPriority = 2;
		} else if (up) {
			rushDirection = Direction.SOUTH;
			rallyPriority = 1;
		} else if (right) {
			rushDirection = Direction.WEST;
			rallyPriority = 1;
		} else if (down) {
			rushDirection = Direction.NORTH;
			rallyPriority = 1;
		} else if (left) {
			rushDirection = Direction.EAST;
			rallyPriority = 1;
		} else {
			rushDirection = Constants.INITIAL_BEARING;
		}
		explorationTarget = currLoc.add(
				rushDirection, GameConstants.MAP_MAX_HEIGHT);
		objective = explorationTarget;
		// communicate rush direction
		sendRushDirection();
		// wait for flux
		currState = RobotState.WAIT_FOR_FLUX;
	}
	
	public void waitForFlux() throws GameActionException {
		if (currFlux >= 35) {
			currState = RobotState.RUSH;
		}
	}
	
	public void rush() throws GameActionException {
		// set objective
		objective = explorationTarget;
		// move backwards towards bearing
		kite(objective);
		// reset bearing if necessary
		setBearing();
		// chase enemy if in range
		if (dc.getClosestEnemy() != null) {
			currState = RobotState.CHASE;
		}
		// send rush direction
		sendRushDirection();
	}
	
	public void chase() throws GameActionException {
		// go back to rushing if no enemy in range
		RobotInfo closestEnemy = dc.getClosestEnemy();
		if (closestEnemy == null) {
			currState = RobotState.RUSH;
			return;
		}
		// set objective
		rallyPriority++;
		objective = closestEnemy.location;
		// attack enemy
		attackClosestEnemy();
		// kite enemy
		kite(closestEnemy.location);
		// send rush direction
		sendRushDirection();
	}
	
	private void setBearing() throws GameActionException {
		// if an appropriate off map tile is found, change bearing
		int range;
		if (rushDirection.isDiagonal()) {
			range = 3;
		} else {
			range = 5;
		}
		if (rc.senseTerrainTile(currLoc.add(rushDirection, range)) ==
				TerrainTile.OFF_MAP) {
			rushDirection = rushDirection.rotateLeft().rotateLeft();
			explorationTarget = currLoc.add(
					rushDirection, GameConstants.MAP_MAX_HEIGHT);
			rallyPriority++;
		}
	}
	
	private void kite(MapLocation target) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// turn in direction of target
		Direction dirToTarget = currLoc.directionTo(target);
		if (dirToTarget == Direction.OMNI) {
			return;
		}
		Direction[] targetDirs = new Direction[] {
				dirToTarget,
				dirToTarget.rotateLeft(),
				dirToTarget.rotateRight()
		};
		for (Direction d : targetDirs) {
			if (rc.canMove(d.opposite())) {
				if (currDir != d) {
					rc.setDirection(d);
					return;
				}
				// stay at distance
				int distance = currLoc.distanceSquaredTo(target);
				if (distance < myType.attackRadiusMaxSquared) {
					if (rc.canMove(currDir.opposite())) {
						rc.moveBackward();
					}
				} else {
					if (rc.canMove(currDir)) {
						rc.moveForward();
					}
				}
				return;
			}
		}
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
		}
	}
	
	private boolean shouldRegenerate() throws GameActionException {
		// check if there are enough damaged units
		int damagedUnits = 0;
		for (Robot r : dc.getNearbyRobots()) {
			if (r.getTeam() == myTeam) {
				RobotInfo rInfo = rc.senseRobotInfo(r);
				if (!rInfo.regen && rInfo.energon < rInfo.type.maxEnergon) {
					damagedUnits++;
				}
			}
		}
		return damagedUnits >= Constants.MIN_DAMAGED_UNITS_TO_REGEN;
	}
	
	private void sendRushDirection() throws GameActionException {
		io.sendInts("#xr", new int[] {rallyPriority, rushDirection.ordinal(),
				objective.x, objective.y});
	}
}
