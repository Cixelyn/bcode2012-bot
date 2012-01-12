package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ArchonRobot extends BaseRobot {
	
	private Direction bearing;
	private RobotType unitToSpawn;
	private int timeUntilBroadcast;
	private int targetPriority;

	public ArchonRobot(RobotController myRC) {
		super(myRC);
		nv = new BlindBug(this);
		currState = RobotState.RUSH;
		unitToSpawn = getSpawnType();
		bearing = Constants.INITIAL_BEARING;
		timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
		io.setAddresses(new String[] {"#x", "#a"});
	}

	@Override
	public void run() throws GameActionException {
		switch (currState) {
			case RUSH:
				rush();
				break;
			case CHASE:
				chase();
				break;
			case GOTO_POWER_CORE:
				gotoPowerCore();
				break;
			case SPAWN_UNIT:
				spawnUnit();
				break;
			case BUILD_TOWER:
				buildTower();
				break;
			default:
				break;
		}
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb) {
		switch(msgType) {
			case 'r':
				int[] msgInts = Radio.decodeInts(sb);
				int msgTargetPriority = msgInts[0];
				int msgBearingOrdinal = msgInts[1];
				if (msgTargetPriority > targetPriority) {
					bearing = Direction.values()[msgBearingOrdinal];
					targetPriority = msgTargetPriority;
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
	
	private void rush() throws GameActionException {
		// after a certain number of time, start taking towers
		if (currRound > Constants.TOWER_ROUNDS) {
			currState = RobotState.GOTO_POWER_CORE;
			return;
		}
		// scan for enemies
		for (Robot r : dc.getNearbyRobots()) {
			if (r.getTeam() != myTeam) {
				RobotInfo rInfo = rc.senseRobotInfo(r);
				if (rInfo.type == RobotType.TOWER && !isTowerTargetable(rInfo)) {
					continue;
				}
				currState = RobotState.CHASE;
				return;
			}
		}
		// reset bearing if necessary
		int range;
		if (bearing.isDiagonal()) {
			range = 4;
		} else {
			range = 6;
		}
		if (rc.senseTerrainTile(currLoc.add(bearing, range)) ==
				TerrainTile.OFF_MAP) {
			if (currRound < Constants.CIRCLE_MAP_ROUNDS) {
				bearing = bearing.rotateRight().rotateRight().rotateRight();
			} else {
				if (bearing.isDiagonal()) {
					bearing = bearing.rotateRight();
				} else {
					bearing = bearing.rotateRight().rotateRight();
				}
			}
			targetPriority++;
		}
		// move towards bearing if possible
		MapLocation target = currLoc.add(bearing,
				GameConstants.MAP_MAX_HEIGHT + GameConstants.MAP_MAX_WIDTH);
		if (!rc.isMovementActive()) {
			Direction dir = nv.navigateTo(target);
			if (dir != Direction.OMNI && dir != Direction.NONE) {
				if (currDir != dir.opposite()) {
					rc.setDirection(dir.opposite());
				} else {
					if (rc.canMove(currDir.opposite())) {
						rc.moveBackward();
					}
				}
			}
		}
		// broadcast target if necessary
		if (--timeUntilBroadcast <= 0) {
			sendArchonMessage(target);
			timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
		}
		// distribute flux
		this.distributeFlux();
		// make units
		if (this.currFlux > this.unitToSpawn.spawnCost + Constants.MIN_UNIT_FLUX) {
			this.currState = RobotState.SPAWN_UNIT;
		}
	}
	
	public void chase() throws GameActionException {
		// get closest enemy
		int closestDistance = Integer.MAX_VALUE;
		RobotInfo closestEnemy = null;
		for (Robot r : dc.getNearbyRobots()) {
			if (r.getTeam() != myTeam) {
				RobotInfo rInfo = rc.senseRobotInfo(r);
				if (rInfo.type == RobotType.TOWER && !isTowerTargetable(rInfo)) {
					continue;
				}
				int distance = currLoc.distanceSquaredTo(rInfo.location);
				if (distance < closestDistance) {
					closestEnemy = rInfo;
					closestDistance = distance;
				}
			}
		}
		// go back to rushing if no enemies in range
		if (closestEnemy == null) {
			currState = RobotState.RUSH;
			return;
		}
		// broadcast target if necessary, with increased priority
		targetPriority++;
		if (--timeUntilBroadcast <= 0) {
			sendArchonMessage(closestEnemy.location);
			timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
		}
		// try to stay at safe range
		if (!rc.isMovementActive()) {
			Direction dir = currLoc.directionTo(closestEnemy.location);
			int distance = currLoc.distanceSquaredTo(closestEnemy.location);
			if (dir != Direction.OMNI) {
				if (currDir != dir) {
					rc.setDirection(dir);
				} else if (distance < Constants.ARCHON_SAFETY_RANGE) {
					if (rc.canMove(currDir.opposite())) {
						rc.moveBackward();
					}
				} else {
					if (rc.canMove(currDir)){
						rc.moveForward();
					}
				}
			}
		}
		// distribute flux
		this.distributeFlux();
	}
	
	private void gotoPowerCore() throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// get closest capturable power core
		int closestDistance = Integer.MAX_VALUE;
		MapLocation closestPowerCore = null;
		for (MapLocation powerCore : dc.getCapturablePowerCores()) {
			int distance = currLoc.distanceSquaredTo(powerCore);
			if (distance < closestDistance) {
				closestPowerCore = powerCore;
				closestDistance = distance;
			}
		}
		if (closestPowerCore != null) {
			if (closestDistance > 2) {
				nv.navigateTo(closestPowerCore);
			} else {
				currState = RobotState.BUILD_TOWER;
			}
		} else {
			// TODO(jven): handle case where no open power cores left
		}
		// distribute flux
		this.distributeFlux();
	}
	
	private void spawnUnit() throws GameActionException {
		// check if we have enough flux
		if (currFlux < unitToSpawn.spawnCost + Constants.MIN_UNIT_FLUX) {
			currState = RobotState.RUSH;
			return;
		}
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// see if i can make the unit in front of me
		TerrainTile tt = dc.getAdjacentTerrainTile(currDir);
		if (tt != TerrainTile.OFF_MAP &&
				!(unitToSpawn.level == RobotLevel.ON_GROUND &&
				tt == TerrainTile.VOID)) {
			GameObject obj = dc.getAdjacentGameObject(
					currDir, unitToSpawn.level);
			if (obj == null) {
				// spawn unit, set spawn type
				rc.spawn(unitToSpawn);
				unitToSpawn = getSpawnType();
				currState = RobotState.RUSH;
				return;
			}
		}
		// look for a direction to spawn
		for (Direction d : Direction.values()) {
			if (d == Direction.OMNI || d == Direction.NONE){
				continue;
			}
			tt = dc.getAdjacentTerrainTile(d);
			if (tt == TerrainTile.OFF_MAP || (
					unitToSpawn.level == RobotLevel.ON_GROUND &&
					tt == TerrainTile.VOID)) {
				continue;
			}
			GameObject obj = dc.getAdjacentGameObject(d, unitToSpawn.level);
			if (obj == null) {
				rc.setDirection(d);
				return;
			}
		}
	}
	
	private void buildTower() throws GameActionException {
		// make sure an untaken power core is next to me
		MapLocation adjacentPowerCore = null;
		for (MapLocation powerCore : dc.getCapturablePowerCores()) {
			if (currLoc.distanceSquaredTo(powerCore) <= 2) {
				adjacentPowerCore = powerCore;
				break;
			}
		}
		if (adjacentPowerCore == null) {
			currState = RobotState.GOTO_POWER_CORE;
			return;
		}
		// wait until we have enough flux
		if (currFlux < RobotType.TOWER.spawnCost) {
			return;
		}
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		Direction dir = currLoc.directionTo(adjacentPowerCore);
		// back up if on top of it
		if (dir == Direction.OMNI) {
			if (rc.canMove(currDir.opposite())) {
				rc.moveBackward();
			} else {
				for (Direction d : Direction.values()) {
					if (d == Direction.OMNI || d == Direction.NONE) {
						continue;
					}
					// TODO(jven): dc
					if (rc.canMove(d)) {
						rc.setDirection(d.opposite());
						break;
					}
				}
			}
		} else if (currDir != dir) {
			// turn to power core if necessary, then spawn tower if possible
			rc.setDirection(dir);
		} else {
			GameObject obj = dc.getAdjacentGameObject(
					currDir, RobotType.TOWER.level);
			if (obj == null) {
				rc.spawn(RobotType.TOWER);
				currState = RobotState.GOTO_POWER_CORE;
			}
		}
	}
	
	private void distributeFlux() throws GameActionException {
		// check all directions around you, ground and air
		for (Direction d : Direction.values()) {
			if (d == Direction.NONE) {
				continue;
			}
			for (RobotLevel level : RobotLevel.values()) {
				if (level == RobotLevel.POWER_NODE) {
					continue;
				}
				if (d == Direction.OMNI && level == RobotLevel.IN_AIR) {
					continue;
				}
				if (this.currFlux < Constants.MIN_ARCHON_FLUX) {
					break;
				}
				GameObject obj = dc.getAdjacentGameObject(d, level);
				if (obj instanceof Robot && obj.getTeam() == myTeam) {
					// TODO(jven): data cache this?
					RobotInfo rInfo = rc.senseRobotInfo((Robot)obj);
					if (rInfo.flux < Constants.MIN_UNIT_FLUX) {
						double fluxToTransfer = Math.min(
								Constants.MIN_UNIT_FLUX - rInfo.flux,
								currFlux - Constants.MIN_ARCHON_FLUX);
						if (fluxToTransfer > 0) {
							rc.transferFlux(
									rInfo.location, rInfo.robot.getRobotLevel(), fluxToTransfer);
						}
						currFlux -= fluxToTransfer;
					}
				}
			}
		}
	}
	
	private RobotType getSpawnType() {
		double p = (Math.random() * currRound * rc.getRobot().getID());
		p = p - (int)p;
		if (p < 0.7) {
			return RobotType.SOLDIER;
		} else {
			return RobotType.SCOUT;
		}
	}
	
	private void sendArchonMessage(
			MapLocation target) throws GameActionException {
		io.sendInts("#sr", new int[] {targetPriority, target.x, target.y});
		io.sendInts("#ar", new int[] {targetPriority, bearing.ordinal()});
	}
	
	private boolean isTowerTargetable(
			RobotInfo tower) throws GameActionException {
		// don't shoot at enemy towers not connected to one of ours
		PowerNode pn = (PowerNode)rc.senseObjectAtLocation(
				tower.location, RobotLevel.POWER_NODE);
		if (pn == null) {
			return false;
		}
		for (PowerNode myPN : dc.getAlliedPowerNodes()) {
			if (!rc.senseConnected(myPN)) {
				continue;
			}
			for (MapLocation loc : pn.neighbors()) {
				if (myPN.getLocation().equals(loc)) {
					return true;
				}
			}
		}
		return false;
	}
}
