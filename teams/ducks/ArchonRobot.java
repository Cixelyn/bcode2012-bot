package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ArchonRobot extends BaseRobot {
	
	RobotType unitToSpawn;

	public ArchonRobot(RobotController myRC) {
		super(myRC);
		this.currState = RobotState.EXPLORE;
		this.unitToSpawn = this.getSpawnType();
	}

	public void run() throws GameActionException{
		this.rc.setIndicatorString(0, "" + this.currState);
		this.rc.setIndicatorString(1, "I want to spawn a " + this.unitToSpawn);
		switch (this.currState) {
			case EXPLORE:
				this.explore();
				break;
			case SPAWN_UNIT:
				this.spawnUnit();
				break;
			default:
				this.rc.setIndicatorString(2, "RobotState." + this.currState +
						" not implemented.");
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
		// if we have enough flux, spawn unit
		if (this.currFlux > this.unitToSpawn.spawnCost + 20) {
			this.currState = RobotState.SPAWN_UNIT;
		}
	}
	
	private void spawnUnit() throws GameActionException {
		if (this.rc.isMovementActive()) {
			return;
		}
		// see if i can make the unit in front of me
		// TODO(jven): get terrain tile from data cache
		TerrainTile tt = this.rc.senseTerrainTile(this.currLocInFront);
		if (tt != TerrainTile.OFF_MAP &&
				!(this.unitToSpawn.level == RobotLevel.ON_GROUND &&
				tt == TerrainTile.VOID)) {
			GameObject obj = this.dc.getAdjacentGameObject(
					this.currDir, this.unitToSpawn.level);
			if (obj == null) {
				// spawn unit, set spawn type
				this.rc.spawn(this.unitToSpawn);
				this.unitToSpawn = this.getSpawnType();
				this.currState = RobotState.EXPLORE;
				return;
			}
		}
		// look for a direction to spawn
		for (Direction d : Direction.values()) {
			if (d == Direction.OMNI || d == Direction.NONE){
				continue;
			}
			tt = this.rc.senseTerrainTile(this.currLoc.add(d));
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
		this.rc.setIndicatorString(2, "AHHH");
	}
	
	private RobotType getSpawnType() {
		double p = (Math.random() * this.currRound);
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
