package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ArchonRobot extends BaseRobot {
	
	RobotType unitToSpawn;
	
	int timeUntilBroadcast = BROADCAST_FREQUENCY;

	public ArchonRobot(RobotController myRC) {
		super(myRC);
		this.currState = RobotState.EXPLORE;
		this.unitToSpawn = this.getSpawnType();
	}

	public void run() throws GameActionException {
		switch (this.currState) {
			case EXPLORE:
				explore();
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
	
	private void explore() throws GameActionException {
		// move around
		if (!this.rc.isMovementActive()) {
			if (this.rc.canMove(this.currDir)) {
				this.rc.moveForward();
			} else {
				this.rc.setDirection(this.currDir.rotateRight());
			}
		}
		// distribute flux
		this.distributeFlux();
		// check for adjacent towers
		// TODO(jven): use sensor and nav to towers
		MapLocation adjacentPowerCore = null;
		for (MapLocation powerCore : dc.getCapturablePowerCores()) {
			if (currLoc.distanceSquaredTo(powerCore) <= 2) {
				adjacentPowerCore = powerCore;
				break;
			}
		}
		if (adjacentPowerCore != null) {
			currState = RobotState.BUILD_TOWER;
			return;
		} else if (this.currFlux > this.unitToSpawn.spawnCost + MIN_UNIT_FLUX) {
			// make units if we have enough flux
			this.currState = RobotState.SPAWN_UNIT;
		}
	}
	
	private void spawnUnit() throws GameActionException {
		// check if we have enough flux
		if (currFlux < unitToSpawn.spawnCost + MIN_UNIT_FLUX) {
			currState = RobotState.EXPLORE;
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
				currState = RobotState.EXPLORE;
				return;
			}
		}
		// look for a direction to spawn
		for (Direction d : Direction.values()) {
			if (d == Direction.OMNI || d == Direction.NONE){
				continue;
			}
			tt = this.dc.getAdjacentTerrainTile(d);
			if (tt == TerrainTile.OFF_MAP || (
					this.unitToSpawn.level == RobotLevel.ON_GROUND &&
					tt == TerrainTile.VOID)) {
				continue;
			}
			GameObject obj = this.dc.getAdjacentGameObject(
					d, this.unitToSpawn.level);
			if (obj == null) {
				this.rc.setDirection(d);
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
			currState = RobotState.EXPLORE;
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
				currState = RobotState.EXPLORE;
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
				if (this.currFlux < MIN_ARCHON_FLUX) {
					break;
				}
				GameObject obj = this.dc.getAdjacentGameObject(d, level);
				if (obj instanceof Robot && obj.getTeam() == this.myTeam) {
					// TODO(jven): data cache this?
					RobotInfo rInfo = this.rc.senseRobotInfo((Robot)obj);
					if (rInfo.flux < MIN_UNIT_FLUX) {
						double fluxToTransfer = Math.min(
								MIN_UNIT_FLUX - rInfo.flux, currFlux - MIN_ARCHON_FLUX);
						if (fluxToTransfer > 0) {
							this.rc.transferFlux(
									rInfo.location,
									rInfo.robot.getRobotLevel(),
									fluxToTransfer);
						}
						this.currFlux -= fluxToTransfer;
					}
				}
			}
		}
	}
	
	private RobotType getSpawnType() {
		double p = (Math.random() * this.currRound * this.rc.getRobot().getID());
		p = p - (int)p;
		if (p < 0.3) {
			return RobotType.SOLDIER;
		} else if (p < 0.6) {
			return RobotType.SCOUT;
		} else if (p < 0.8) {
			return RobotType.DISRUPTER;
		} else {
			return RobotType.SCORCHER;
		}
	}

}
