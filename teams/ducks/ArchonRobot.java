package ducks;

import battlecode.common.Clock;
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
	
	RobotType unitToSpawn;
	
	int timeUntilBroadcast = BROADCAST_FREQUENCY;

	public ArchonRobot(RobotController myRC) {
		super(myRC);
		this.currState = RobotState.GOTO_POWER_CORE;
		this.unitToSpawn = this.getSpawnType();
	}

	public void run() throws GameActionException {
		// runYPBUGCODE(); if (true) return;
		switch (this.currState) {
			case EXPLORE:
				explore();
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
	
	private void gotoPowerCore() throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// get closest capturable power core
		int closestDistance = (
				GameConstants.MAP_MAX_HEIGHT * GameConstants.MAP_MAX_WIDTH);
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
				blindBug(closestPowerCore);
			} else {
				currState = RobotState.BUILD_TOWER;
			}
		} else {
			rc.setIndicatorString(2, "???");
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
	

	/*
	 * REMOVE THE BELOW FOR ACTUAL CODE
	 */
	void runYPBUGCODE() throws GameActionException
	{
//		System.out.println("ducks");
//		rc.setDirection(currDir.rotateLeft());
//		rc.setIndicatorString(0, "round "+currRound);
		
//		rc.yield();
//		rc.setIndicatorString(2, "no add");
		rc.setIndicatorString(2, "target "+nodes[nodeindex]);
		
		if (nodes[nodeindex]==null) //reset
		{
			System.out.println("reset");
			nodeindex = 0;
			nodesize = 1;
			nodes[0] = rc.sensePowerCore().getLocation();
		}
		
		MapLocation cur = rc.getLocation();
		if (cur.isAdjacentTo(nodes[nodeindex]))
		{
			rc.setIndicatorString(0, "round "+currRound+" a"+1);
			System.out.println("next to "+nodes[nodeindex]);
			GameObject go = rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.ON_GROUND);
			RobotInfo ri;
			boolean done = false;
			if (go==null){
				while (rc.isMovementActive()) rc.yield();
				rc.setDirection(cur.directionTo(nodes[nodeindex]));
				Direction ddd = cur.directionTo(nodes[nodeindex]);
				System.out.println("facing "+nodes[nodeindex]);
				while (rc.getFlux() < RobotType.TOWER.spawnCost) rc.yield();
				System.out.println("has flux for "+nodes[nodeindex]);
				
				rc.setIndicatorString(0, "round "+currRound+" a"+2);
				while ((!rc.canMove(ddd)) || rc.isMovementActive()) {
					go = rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.ON_GROUND);
					if (go!=null)
					{
						ri = rc.senseRobotInfo((Robot) go);
						if (ri.type == RobotType.TOWER)
						{
							done = true;
							break;
						} 
					}
					rc.yield();
				}
				if (!done)
				{
					System.out.println("spawning "+nodes[nodeindex]);
					rc.spawn(RobotType.TOWER);
//					PowerNode pn = (PowerNode)rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.POWER_NODE);
//					MapLocation[] neighbors = pn.neighbors();
//					addNewLocs(neighbors);
				}
			}
			rc.setIndicatorString(0, "round "+currRound+" a"+3);
			PowerNode pn = (PowerNode)rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.POWER_NODE);
			MapLocation[] neighbors = pn.neighbors();
			addNewLocs(neighbors);
			nodeindex++;
		} else if (rc.canSenseSquare(nodes[nodeindex]))
		{
			rc.setIndicatorString(0, "round "+currRound+" a"+4);
			GameObject go = rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.ON_GROUND);
			if (go!=null)
			{
				PowerNode pn = (PowerNode)rc.senseObjectAtLocation(nodes[nodeindex], RobotLevel.POWER_NODE);
				MapLocation[] neighbors = pn.neighbors();
				addNewLocs(neighbors);
				nodeindex++;
			}
		}
		
		if (nodeindex<nodesize && !rc.isMovementActive())
		{

			rc.setIndicatorString(2, "target "+nodes[nodeindex]);
			rc.setIndicatorString(0, "round "+currRound+" a"+5);
			int bytecode = Clock.getBytecodeNum();
			rc.setIndicatorString(1, "before move "+bytecode+" cur "+currLoc);
			rc.setIndicatorString(1, "start:"+bugStart+" end:"+bugTarget+" cw:"+bugCW+" cur:"+currLoc+" obs:"+bugObs);
			blindBug(nodes[nodeindex]);
			bytecode = Clock.getBytecodeNum()-bytecode;
//			rc.setIndicatorString(1, "move used "+bytecode);
			rc.setIndicatorString(0, "end:"+bugStart+" end:"+bugTarget+" cw:"+bugCW+" cur:"+currLoc+" obs:"+bugObs+" move used "+bytecode);
		}
//		rc.setIndicatorString(0, "round "+currRound+" a"+6);
		
	}
	
	
	MapLocation[] nodes = new MapLocation[GameConstants.MAX_POWER_NODES+2];
	int nodesize = 0;
	int nodeindex = 0;
	void addNewLocs(MapLocation[] locs)
	{
		int bytecode = Clock.getBytecodeNum();
		boolean add = false;
		for (int x=0; x<locs.length; x++)
		{
			MapLocation m = locs[x];
			add = true;
			for (int y=0; y<nodesize; y++)
			{
				if (nodes[y].x==m.x&&nodes[y].y==m.y)
				{
					add = false;
					break;
				}
			}
			if (add)
				nodes[nodesize++] = m;
		}
		System.out.println("added, now has "+nodesize+" nodes");
		bytecode = Clock.getBytecodeNum()-bytecode;
		rc.setIndicatorString(2, "adding new loc used "+bytecode);
	}
}
