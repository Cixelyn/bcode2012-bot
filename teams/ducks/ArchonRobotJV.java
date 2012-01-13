package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ArchonRobotJV extends BaseRobot {
	
	private boolean isLeader;
	private int splitTime;
	private Direction splitDirection;
	private int soldiersSpawned;
	private Direction bearing;
	private MapLocation explorationTarget;
	
	public ArchonRobotJV(RobotController myRC) {
		super(myRC);
		currState = RobotState.INITIALIZE;
	}
	
	@Override
	public void run() throws GameActionException {
		switch (currState) {
			case INITIALIZE:
				initialize();
				break;
			case SPLIT:
				split();
				break;
			case SPAWN_SOLDIERS:
				spawnSoldiers();
				break;
			case RUSH:
				rush();
				break;
			case CHASE:
				chase();
				break;
			case GOTO_POWER_CORE:
				gotoPowerCore();
				break;
			default:
				break;
		}
		rc.setIndicatorString(1, "" + bearing);
	}
	
	public void initialize() throws GameActionException {
		// set nav mode
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		// determine leader
		// TODO(jven): check for leader in other states in case leader dies
		if (currLoc.equals(dc.getAlliedArchons()[0])) {
			isLeader = true;
		}
		// set split direction, bearing, target
		splitDirection = rc.sensePowerCore().getLocation().directionTo(currLoc);
		bearing = rc.sensePowerCore().getLocation().directionTo(
				dc.getCapturablePowerCores()[0]);
		explorationTarget = currLoc.add(bearing, 100);
		// split
		currState = RobotState.SPLIT;
		split();
	}
	
	public void split() throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// if not facing away from power core, set direction and wait
		if (currDir != splitDirection) {
			rc.setDirection(splitDirection);
			return;
		}
		// if we can't move away from the power core, start spawning soldiers
		if (!rc.canMove(splitDirection)) {
			rc.setDirection(bearing);
			currState = RobotState.SPAWN_SOLDIERS;
			return;
		}
		boolean keepGoing = false;
		// if we're too close to another archon, keep going
		if (currLoc.distanceSquaredTo(dc.getClosestArchon()) <
				Constants.SPLIT_DISTANCE) {
			keepGoing = true;
		}
		// if there's not enough room to build units, keep going
		int numOpenSquares = 0;
		for (boolean canMove : dc.getMovableDirections()) {
			if (canMove) {
				numOpenSquares++;
			}
		}
		if (numOpenSquares < Constants.SOLDIERS_PER_ARCHON) {
			keepGoing = true;
		}
		// if we time out, stop splitting
		if (splitTime++ > Constants.MAX_SPLIT_TIME) {
			keepGoing = false;
		}
		// keep splitting if necessary
		if (keepGoing) {
			rc.moveForward();
		} else {
			rc.setDirection(bearing);
			currState = RobotState.SPAWN_SOLDIERS;
		}
	}
	
	public void spawnSoldiers() throws GameActionException {
		// build soldiers in an available direction
		Direction[] spawnDirs = new Direction[] {
				bearing,
				bearing.rotateLeft(),
				bearing.rotateRight(),
				bearing.rotateLeft().rotateLeft(),
				bearing.rotateRight().rotateRight(),
				bearing.rotateLeft().rotateLeft().rotateLeft(),
				bearing.rotateRight().rotateRight().rotateRight(),
				bearing.opposite()
		};
		for (Direction d : spawnDirs) {
			// TODO(jven): use data cache
			if (rc.canMove(d)) {
				if (spawnUnitInDir(RobotType.SOLDIER, d)) {
					soldiersSpawned++;
				}
				break;
			}
		}
		if (soldiersSpawned >= Constants.SOLDIERS_PER_ARCHON) {
			// wake up units
			wakeUpSoldiers();
			// change state
			if (isLeader) {
				currState = RobotState.GOTO_POWER_CORE;
			} else {
				currState = RobotState.RUSH;
			}
		}
		// distribute flux
		distributeFlux();
	}
	
	public void rush() throws GameActionException {
		// move backwards towards bearing
		moonwalkTowards(explorationTarget);
		// reset bearing if necessary
		int range;
		if (bearing.isDiagonal()) {
			range = 4;
		} else {
			range = 6;
		}
		if (rc.senseTerrainTile(currLoc.add(bearing, range)) ==
				TerrainTile.OFF_MAP) {
			bearing = bearing.rotateLeft().rotateLeft();
			explorationTarget = currLoc.add(bearing, 100);
		}
		// distribute flux
		distributeFlux();
		// chase enemy if in range
		if (dc.getClosestEnemy() != null) {
			currState = RobotState.CHASE;
		}
	}
	
	public void chase() throws GameActionException {
		// go back to previous state if no enemy in range
		RobotInfo closestEnemy = dc.getClosestEnemy();
		if (closestEnemy == null) {
			if (isLeader) {
				currState = RobotState.GOTO_POWER_CORE;
			} else {
				currState = RobotState.RUSH;
			}
			return;
		}
		// kite enemy
		kite(closestEnemy.location);
		// distribute flux
		distributeFlux();
	}
	
	public void gotoPowerCore() throws GameActionException {
		// get a power core
		MapLocation powerCore = dc.getCapturablePowerCores()[0];
		// move backwards towards power core if far away
		if (currLoc.distanceSquaredTo(powerCore) > 2) {
			moonwalkTowards(powerCore);
		} else {
			// build tower if in range
			spawnUnitInDir(RobotType.TOWER, currLoc.directionTo(powerCore));
		}
		// distribute flux
		distributeFlux();
	}
	
	private void moonwalkTowards(MapLocation target) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// sense tiles
		mc.senseAllTiles();
		// move towards target
		Direction dir = nav.navigateTo(target);
		if (dir != Direction.OMNI && dir != Direction.NONE) {
			/*
			Direction[] wiggleDirs = new Direction[] {
					dir,
					dir.rotateLeft(),
					dir.rotateRight(),
					dir.rotateLeft().rotateLeft(),
					dir.rotateRight().rotateRight(),
					dir.rotateLeft().rotateLeft().rotateLeft(),
					dir.rotateRight().rotateRight().rotateRight()
			};
			for (Direction d : wiggleDirs) {
				if (rc.canMove(d)) {
					if (currDir != d.opposite()) {
						rc.setDirection(d.opposite());
					} else {
						rc.moveBackward();
					}
					return;
				}
			}
			*/
			for (int tries = 0; tries < 256; tries++) {
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
			if (rc.canMove(dir)) {
				if (currDir != dir.opposite()) {
					rc.setDirection(dir.opposite());
				} else {
					rc.moveBackward();
				}
			}
		}
	}
	
	private void kite(MapLocation target) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// turn in direction of target
		Direction dir = currLoc.directionTo(target);
		if (currDir != dir) {
			rc.setDirection(dir);
			return;
		}
		// stay at distance
		int distance = currLoc.distanceSquaredTo(target);
		if (distance < Constants.ARCHON_SAFETY_RANGE) {
			if (rc.canMove(currDir.opposite())) {
				rc.moveBackward();
			}
		} else {
			if (rc.canMove(currDir)) {
				rc.moveForward();
			}
		}
	}
	
	private boolean spawnUnitInDir(
			RobotType type, Direction dir) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {	
			return false;
		}
		// turn in direction to spawn
		if (currDir != dir) {
			rc.setDirection(dir);
			return false;
		}
		// wait if not enough flux
		if (currFlux < type.spawnCost) {
			return false;
		}
		// wait if unit is in the way
		if (dc.getAdjacentGameObject(dir, type.level) != null) {
			return false;
		}
		// spawn unit
		rc.spawn(type);
		currFlux -= type.spawnCost;
		return true;
	}
	
	private void distributeFlux() throws GameActionException {
		// check all directions around you, ground and air
		for (Direction d : Direction.values()) {
			// ignore none direction
			if (d == Direction.NONE) {
				continue;
			}
			for (RobotLevel level : RobotLevel.values()) {
				// if we don't have flux to give, abort
				if (this.currFlux < Constants.MIN_ARCHON_FLUX) {
					break;
				}
				// ignore power node level
				if (level == RobotLevel.POWER_NODE) {
					continue;
				}
				// can't give flux to yourself, silly!
				if (d == Direction.OMNI && level == RobotLevel.ON_GROUND) {
					continue;
				}
				GameObject obj = dc.getAdjacentGameObject(d, level);
				if (obj instanceof Robot && obj.getTeam() == myTeam) {
					// TODO(jven): data cache this?
					RobotInfo rInfo = rc.senseRobotInfo((Robot)obj);
					// don't give flux to towers
					if (rInfo.type == RobotType.TOWER) {
						continue;
					}
					if (rInfo.flux <
							Constants.MIN_UNIT_FLUX_RATIO * rInfo.type.maxFlux) {
						double fluxToTransfer = Math.min(
								Constants.MIN_UNIT_FLUX_RATIO * rInfo.type.maxFlux - rInfo.flux,
								currFlux - Constants.MIN_ARCHON_FLUX);
						if (fluxToTransfer > 0) {
							rc.transferFlux(
									rInfo.location, rInfo.robot.getRobotLevel(), fluxToTransfer);
							currFlux -= fluxToTransfer;
						}
					}
				}
			}
		}
	}
	
	private void wakeUpSoldiers() throws GameActionException {
		io.sendInt("#sw", 0);
	}
}
