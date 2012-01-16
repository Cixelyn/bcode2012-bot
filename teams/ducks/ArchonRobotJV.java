package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ArchonRobotJV extends BaseRobot {
	
	private int splitTime;
	private Direction splitDirection;
	
	private int soldiersSpawned;
	
	private int rallyPriority;
	private Direction bearing;
	private MapLocation explorationTarget;
	
	private int timeUntilBroadcast;
	
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
			case DEFEND:
				defend();
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
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb) {
		int[] msgInts;
		//MapLocation[] msgLocs;
		switch(msgType) {
			case 'r':
				if (currState == RobotState.SPAWN_SOLDIERS) {
					if (shouldDefend()) {
						currState = RobotState.DEFEND;
					} else {
						currState = RobotState.RUSH;
					}
				}
				msgInts = Radio.decodeShorts(sb);
				int msgRallyPriority = msgInts[0];
				Direction msgBearing = Direction.values()[msgInts[1]];
				if (msgRallyPriority > rallyPriority) {
					bearing = msgBearing;
					explorationTarget = currLoc.add(
							bearing, GameConstants.MAP_MAX_HEIGHT);
					rallyPriority = msgRallyPriority;
				}
				break;
			case 'd':
				int[] deadEnemyArchonIDs = Radio.decodeShorts(sb);
				for (int id : deadEnemyArchonIDs) {
					enemyArchonInfo.reportEnemyArchonKill(id);
				}
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	public void initialize() throws GameActionException {
		// set nav mode
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		// set radio addresses
		io.setAddresses(new String[] {"#x", "#a"});
		// set split direction, bearing, target
		splitDirection = rc.sensePowerCore().getLocation().directionTo(currLoc);
		rallyPriority = 0;
		bearing = rc.sensePowerCore().getLocation().directionTo(
				dc.getCapturablePowerCores()[0]);
		explorationTarget = currLoc.add(bearing, GameConstants.MAP_MAX_HEIGHT);
		// set broadcast time
		timeUntilBroadcast = 0;
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
			// rally units
			sendRally(explorationTarget);
			// change state
			if (shouldTower()) {
				currState = RobotState.GOTO_POWER_CORE;
			} else if (shouldDefend()) {
				currState = RobotState.DEFEND;
			} else {
				currState = RobotState.RUSH;
			}
		}
		// distribute flux
		distributeFlux();
	}
	
	public void defend() throws GameActionException {
		// set objective
		mi.setObjective(rc.sensePowerCore().getLocation());
		mi.setMoonwalkMode();
		// make soldier if enough flux
		if (currFlux > RobotType.SOLDIER.spawnCost) {
			currState = RobotState.SPAWN_SOLDIERS;
			return;
		}
		// moonwalk towards bearing
		mi.attackMove();
		// reset bearing if necessary
		setBearing();
		// distribute flux
		distributeFlux();
		// chase enemy if in range
		if (dc.getClosestEnemy() != null) {
			currState = RobotState.CHASE;
		}
		// send rally
		sendRally(currLoc);
	}
	
	public void rush() throws GameActionException {
		// set objective
		mi.setObjective(explorationTarget);
		mi.setMoonwalkMode();
		// make soldier if enough flux
		if (currFlux > RobotType.SOLDIER.spawnCost) {
			currState = RobotState.SPAWN_SOLDIERS;
			return;
		}
		// move backwards towards bearing
		mi.attackMove();
		// reset bearing if necessary
		setBearing();
		// distribute flux
		distributeFlux();
		// chase enemy if in range
		if (dc.getClosestEnemy() != null) {
			currState = RobotState.CHASE;
		}
		// send rally
		sendRally(explorationTarget);
	}
	
	public void chase() throws GameActionException {
		// increase rally priority
		rallyPriority++;
		// go back to previous state if no enemy in range
		RobotInfo closestEnemy = dc.getClosestEnemy();
		if (closestEnemy == null) {
			if (shouldTower()) {
				currState = RobotState.GOTO_POWER_CORE;
			} else if (shouldDefend()) {
				currState = RobotState.DEFEND;
			} else {
				currState = RobotState.RUSH;
			}
			return;
		}
		// set objective
		mi.setObjective(closestEnemy.location);
		mi.setKiteMode(Constants.ARCHON_SAFETY_RANGE);
		// kite enemy
		mi.attackMove();
		// distribute flux
		distributeFlux();
		// send rally
		sendRally(closestEnemy.location);
	}
	
	public void gotoPowerCore() throws GameActionException {
		// get next power core
		MapLocation powerCore = getNextPowerCore();
		// set objective
		mi.setObjective(powerCore);
		mi.setMoonwalkMode();
		int distance = currLoc.distanceSquaredTo(powerCore);
		if (distance > 2) {
			// moonwalk towards power core if far away
			mi.attackMove();
		} else if (distance > 0 && distance <= 2) {
			// build tower if in range
			spawnUnitInDir(RobotType.TOWER, currLoc.directionTo(powerCore));
		} else {
			// i'm on top, get out of the way
			boolean movedOut = false;
			for (Direction d : Direction.values()) {
				if (d == Direction.OMNI || d == Direction.NONE) {
					continue;
				}
				if (rc.canMove(d)) {
					movedOut = true;
					if (currDir != d) {
						rc.setDirection(d);
					} else {
						rc.moveForward();
					}
				}
			}
			if (!movedOut) {
				sendBackOff();
			}
		}
		// distribute flux if we're not adjacent to tower or if we have too much
		if (distance > 2 || currFlux == myType.maxFlux) {
			distributeFlux();
		}
		// send rally
		sendRally(powerCore);
		// if enemy is nearby, kite it
		if (dc.getClosestEnemy() != null) {
			currState = RobotState.CHASE;
		}
	}
	
	private void setBearing() throws GameActionException {
		// if an appropriate off map tile is found, change bearing
		int range;
		if (bearing.isDiagonal()) {
			range = 4;
		} else {
			range = 6;
		}
		if (rc.senseTerrainTile(currLoc.add(bearing, range)) ==
				TerrainTile.OFF_MAP) {
			bearing = bearing.rotateLeft().rotateLeft().rotateLeft();
			explorationTarget = currLoc.add(bearing, GameConstants.MAP_MAX_HEIGHT);
			rallyPriority++;
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
		// wait if unit is in the way, tell him to move
		if (dc.getAdjacentGameObject(dir, type.level) != null) {
			sendBackOff();
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
				if (this.currFlux < Constants.MIN_ROBOT_FLUX) {
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
					if (rInfo.type == RobotType.TOWER || rInfo.type == RobotType.ARCHON) {
						continue;
					}
					if (rInfo.flux <
							0.75 * rInfo.type.maxFlux) {
						double fluxToTransfer = Math.min(
								0.75 * rInfo.type.maxFlux - rInfo.flux,
								currFlux - Constants.MIN_ROBOT_FLUX);
						if (fluxToTransfer > 0) {
							// if we throw an exception, our info is stale, so abort
							try {
								rc.transferFlux(
										rInfo.location, rInfo.robot.getRobotLevel(), fluxToTransfer);
								currFlux -= fluxToTransfer;
							} catch (GameActionException e) {
								return;
							}
						}
					}
				}
			}
		}
	}
	
	private MapLocation getNextPowerCore() {
		try {
			return dc.getClosestCapturablePowerCore();
		} catch (GameActionException e) {
			return null;
		}
		/*
		// get furthest power core from main
		int furthestDistance = -1;
		MapLocation furthestPowerCore = null;
		for (MapLocation powerCore : dc.getCapturablePowerCores()) {
			int distance = rc.sensePowerCore().getLocation().distanceSquaredTo(
					powerCore);
			if (distance > furthestDistance) {
				furthestPowerCore = powerCore;
				furthestDistance = distance;
			}
		}
		return furthestPowerCore;
		*/
	}
	
	private boolean shouldDefend() {
		try {
			return (enemyArchonInfo.getNumEnemyArchons() > 0 &&
					currLoc.equals(dc.getAlliedArchons()[0]));
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean shouldTower() throws GameActionException {
		int numAlliedArchons = dc.getAlliedArchons().length;
		int numEnemyArchons = enemyArchonInfo.getNumEnemyArchons();
		for (int idx = 0; idx < numAlliedArchons; idx++) {
			if (currLoc.equals(dc.getAlliedArchons()[idx])) {
				return idx >= GameConstants.NUMBER_OF_ARCHONS -
						Constants.NUM_ARCHONS_TO_TOWER[numEnemyArchons];
			}
		}
		return false;
	}
	
	private void sendRally(MapLocation target) throws GameActionException {
		if (--timeUntilBroadcast <= 0) {
			io.sendShorts("#sr", new int[] {
					rallyPriority, target.x, target.y});
			io.sendShorts("#ar", new int[] {
					rallyPriority, bearing.ordinal()});
			timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
		}
	}
	
	private void sendBackOff() throws GameActionException {
		io.sendMapLoc("#sb", currLoc);
	}
}
