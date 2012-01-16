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

public class ArchonStrategy extends StrategyRobot {
	
	private int splitTime;
	private Direction splitDirection;
	
	private int soldiersSpawned;
	
	private int rallyPriority;
	private Direction bearing;
	private MapLocation explorationTarget;
	private MapLocation objective;
	
	private int timeUntilBroadcast;
	private boolean initialized;
	private boolean splitDone;
//	private RobotInfo closestEnemy;
	private int soldiersToSpawn;
	
	public ArchonStrategy(RobotController myRC) {
		super(myRC, RobotState.INITIALIZE);
	}
	
//	@Override
//	public void run() throws GameActionException {
//		processTransitions();
//		execute();
//	}
	
	
	public RobotState processTransitions(RobotState state) throws GameActionException {
		switch (state) {
		case INITIALIZE:
		{
			if (initialized)
				return RobotState.SPLIT;
		} break;
		case SPLIT:
		{
			if (splitDone)
				return (RobotState.SPAWN_SOLDIERS);
			
		} break;
		case SPAWN_SOLDIERS:
		{
			if (soldiersSpawned >= soldiersToSpawn) {
				// rally units
				sendRally();
				// change state
				if (shouldDefend()) {
					return (RobotState.DEFEND);
				} else if (shouldTower()) {
					return (RobotState.GOTO_POWER_CORE);
				} else {
					return (RobotState.RUSH);
				}
			}
		} break;
		case RUSH:
		{
			// make soldier if enough flux
			if (currFlux > RobotType.SOLDIER.spawnCost) {
				return (RobotState.SPAWN_SOLDIERS);
				
			} else if (dc.getClosestEnemy() != null) {
				// chase enemy if in range
				return (RobotState.CHASE);
			}
			
			objective = explorationTarget;
		} break;
		case DEFEND:
		{
			// make soldier if enough flux
			if (currFlux > RobotType.SOLDIER.spawnCost) {
				return (RobotState.SPAWN_SOLDIERS);
				
			} else if (dc.getClosestEnemy() != null) {
				// chase enemy if in range
				return (RobotState.CHASE);
			}
		} break;
		case CHASE:
		{
			// make soldier if enough flux
			if (currFlux == myMaxFlux)
				return (RobotState.SPAWN_SOLDIERS);
				
			// go back to previous state if no enemy in range
			if (dc.getClosestEnemy() == null) {
				if (shouldDefend()) {
					return (RobotState.DEFEND);
				} else if (shouldTower()) {
					return (RobotState.GOTO_POWER_CORE);
				} else {
					return (RobotState.RUSH);
				}
			}
		} break;
		case GOTO_POWER_CORE:
		{
			// if enemy is nearby, kite it
			if (dc.getClosestEnemy() != null) {
				return (RobotState.CHASE);
			}
		} break;
		default:
			break;
		}
		return state;
	}
	
	public void prepareTransition(RobotState newstate, RobotState oldstate)
	{
		try
		{
			switch (newstate)
			{
			case SPLIT:
			{
				splitTime = 0;
				splitDone = false;
			} break;
			case DEFEND:
			{
				// set objective
				objective = rc.sensePowerCore().getLocation();
			} break;
			case RUSH:
			{
				// set objective
				objective = explorationTarget;
			} break;
			case CHASE:
			{
//				closestEnemy = dc.getClosestEnemy();
			} break;
			case SPAWN_SOLDIERS:
			{
				soldiersSpawned = 0;
				switch (oldstate)
				{
				case SPLIT: 
					soldiersToSpawn = Constants.SOLDIERS_PER_ARCHON; break;
				case RUSH:
					soldiersToSpawn = 1; break;
				case DEFEND:
					soldiersToSpawn = 1; break;
				default:
					soldiersToSpawn = 1; break;
				}
			}
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void execute(RobotState state) throws GameActionException {
			switch (state) {
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
	public void processMessage(char msgType, StringBuilder sb) throws GameActionException {
		int[] msgInts;
		RobotState currstate = getCurrentState();
		//MapLocation[] msgLocs;
		switch(msgType) {
			case 'r':
				if (currstate == RobotState.SPAWN_SOLDIERS) {
					if (shouldDefend()) {
						gotoState(RobotState.DEFEND);
					} else {
						gotoState(RobotState.RUSH);
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
		objective = explorationTarget;
		// set broadcast time
		timeUntilBroadcast = 0;
		initialized = true;
	}
	
	public void split() throws GameActionException {
		if (splitDone) return;
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
			splitDone = true;
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
			splitDone = true;
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
		// distribute flux
		distributeFlux();
	}
	
	public void defend() throws GameActionException {
		// move backwards towards bearing
		moonwalkTowards(objective);
		// reset bearing if necessary
		setBearing();
		// distribute flux
		distributeFlux();
		// send rally
		sendRally();
	}
	
	public void rush() throws GameActionException {
		// move backwards towards bearing
		moonwalkTowards(objective);
		// reset bearing if necessary
		setBearing();
		// distribute flux
		distributeFlux();
		// send rally
		sendRally();
	}
	
	public void chase() throws GameActionException {
		RobotInfo closestEnemy = dc.getClosestEnemy();
		if (closestEnemy == null) return;
		// set objective
		rallyPriority++;
		objective = closestEnemy.location;
		// kite enemy
		kite(closestEnemy.location);
		// distribute flux
		distributeFlux();
		// send rally
		sendRally();
	}
	
	public void gotoPowerCore() throws GameActionException {
		// get next power core
		MapLocation powerCore = getNextPowerCore();
		// move backwards towards power core if far away
		int distance = currLoc.distanceSquaredTo(powerCore);
		if (distance > 2) {
			moonwalkTowards(powerCore);
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
		objective = powerCore;
		sendRally();
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
			bearing = bearing.rotateLeft().rotateLeft();
			explorationTarget = currLoc.add(bearing, GameConstants.MAP_MAX_HEIGHT);
			rallyPriority++;
		}
	}
	
	private void moonwalkTowards(MapLocation target) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// sense tiles
		mc.senseAllTiles();
		// move towards target
		nav.setDestination(target);
		nav.prepare();
		Direction dir = nav.navigateToDestination();
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
			if (currDir != dir.opposite()) {
				rc.setDirection(dir.opposite());
			} else if (rc.canMove(currDir.opposite())) {
				rc.moveBackward();
			}
		}
	}
	
	private void kite(MapLocation target) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// turn in direction of target
		Direction[] targetDirs = new Direction[] {
				currLoc.directionTo(target),
				currLoc.directionTo(target).rotateLeft(),
				currLoc.directionTo(target).rotateRight()
		};
		for (Direction d : targetDirs) {
			if (rc.canMove(d.opposite())) {
				if (currDir != d) {
					rc.setDirection(d);
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
				return;
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
					if (rInfo.type == RobotType.TOWER || rInfo.type == RobotType.ARCHON) {
						continue;
					}
					if (rInfo.flux <
							Constants.MIN_UNIT_FLUX_RATIO * rInfo.type.maxFlux) {
						double fluxToTransfer = Math.min(
								Constants.MIN_UNIT_FLUX_RATIO * rInfo.type.maxFlux - rInfo.flux,
								currFlux - Constants.MIN_ARCHON_FLUX);
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
	
	private void sendRally() throws GameActionException {
		if (--timeUntilBroadcast <= 0) {
			io.sendShorts("#sr", new int[] {rallyPriority, objective.x, objective.y});
			io.sendShorts("#ar", new int[] {rallyPriority, bearing.ordinal()});
			timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
		}
	}
	
	private void sendBackOff() throws GameActionException {
		io.sendMapLoc("#sb", currLoc);
	}
}
