package ducks;

import battlecode.common.*;

public class DataCache {
	
	RobotController rc;
	MapLocation[] alliedArchons;
	int alliedArchonsTime = -1;
	
	boolean[] isAdjacentGameObjectGroundCached;
	boolean[] isAdjacentGameObjectAirCached;
	GameObject[] adjacentGameObjectsGround =
			new GameObject[Direction.values().length];
	GameObject[] adjacentGameObjectsAir =
			new GameObject[Direction.values().length];
	int adjacentGameObjectsTime = -1;

	boolean[] isAdjacentTerrainTileCached;
	TerrainTile[] adjacentTerrainTiles =
			new TerrainTile[Direction.values().length];
	int adjacentTerrainTilesTime = -1;
	
	Robot[] nearbyRobots;
	int nearbyRobotsTime = -1;
	
	MapLocation[] capturablePowerCores;
	int capturablePowerCoresTime = -1;
	
	public DataCache(RobotController myRC) {
		this.rc = myRC;
	}

	
	public MapLocation[] getAlliedArchons() {
		
		if(Clock.getRoundNum() > alliedArchonsTime) {
			alliedArchons = rc.senseAlliedArchons();
		}
		
		return alliedArchons;
		
	}
	
	public GameObject getAdjacentGameObject(
			Direction d, RobotLevel level) throws GameActionException {
		int currRoundNum = Clock.getRoundNum();
		if (currRoundNum > adjacentGameObjectsTime) {
			isAdjacentGameObjectGroundCached =
					new boolean[Direction.values().length];
			isAdjacentGameObjectAirCached =
					new boolean[Direction.values().length];
			adjacentGameObjectsTime = currRoundNum;
		}
		if (level == RobotLevel.ON_GROUND) {
			if (isAdjacentGameObjectGroundCached[d.ordinal()]) {
				return adjacentGameObjectsGround[d.ordinal()];
			} else {
				GameObject obj = this.rc.senseObjectAtLocation(
						this.rc.getLocation().add(d), level);
				adjacentGameObjectsGround[d.ordinal()] = obj;
				isAdjacentGameObjectGroundCached[d.ordinal()] = true;
				return obj;
			}
		} else if (level == RobotLevel.IN_AIR){
			if (isAdjacentGameObjectAirCached[d.ordinal()]) {
				return adjacentGameObjectsAir[d.ordinal()];
			} else {
				GameObject obj = this.rc.senseObjectAtLocation(
						this.rc.getLocation().add(d), level);
				adjacentGameObjectsAir[d.ordinal()] = obj;
				isAdjacentGameObjectAirCached[d.ordinal()] = true;
				return obj;
			}
		} else {
			return null;
		}
	}
	
	public TerrainTile getAdjacentTerrainTile(
			Direction d) throws GameActionException {
		int currRoundNum = Clock.getRoundNum();
		if (currRoundNum > adjacentTerrainTilesTime) {
			isAdjacentTerrainTileCached =
					new boolean[Direction.values().length];
			adjacentTerrainTilesTime = currRoundNum;
		}
		if (isAdjacentTerrainTileCached[d.ordinal()]) {
			return adjacentTerrainTiles[d.ordinal()];
		} else {
			TerrainTile tt = this.rc.senseTerrainTile(this.rc.getLocation().add(d));
			this.adjacentTerrainTiles[d.ordinal()] = tt;
			isAdjacentTerrainTileCached[d.ordinal()] = true;
			return tt;
		}
	}
	
	public Robot[] getNearbyRobots() {
		int currRoundNum = Clock.getRoundNum();
		if (currRoundNum > nearbyRobotsTime) {
			nearbyRobots = null;
			nearbyRobotsTime = currRoundNum;
		}
		if (nearbyRobots == null) {
			nearbyRobots = this.rc.senseNearbyGameObjects(Robot.class);
		}
		return nearbyRobots;
	}
	
	public MapLocation[] getCapturablePowerCores() {
		int currRoundNum = Clock.getRoundNum();
		if (currRoundNum > capturablePowerCoresTime) {
			capturablePowerCores = null;
			capturablePowerCoresTime = currRoundNum;
		}
		if (capturablePowerCores == null) {
			capturablePowerCores = rc.senseCapturablePowerNodes();
		}
		return capturablePowerCores;
	}
}
