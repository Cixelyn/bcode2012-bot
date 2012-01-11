package ducks;

import battlecode.common.*;

public class DataCache {
	
	BaseRobot br;
	MapLocation[] alliedArchons;
	int alliedArchonsTime = -1;

	int moveableDirectionsTime = -1;
	boolean[] moveableDirections = new boolean[8];
	int moveableLandTime = -1;
	boolean[] moveableLand = new boolean[8];
	
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
	
	public DataCache(BaseRobot br) {
		this.br = br;
	}

	
	public MapLocation[] getAlliedArchons() {
		
		if(br.currRound > alliedArchonsTime) {
			alliedArchons = br.rc.senseAlliedArchons();
			alliedArchonsTime = br.currRound;
		}
		
		return alliedArchons;
		
	}
	
	public boolean[] getMovableDirections()
	{
		if (br.currRound > moveableDirectionsTime)
		{
			moveableDirections[0] = br.rc.canMove(Direction.NORTH);
			moveableDirections[1] = br.rc.canMove(Direction.NORTH_EAST);
			moveableDirections[2] = br.rc.canMove(Direction.EAST);
			moveableDirections[3] = br.rc.canMove(Direction.SOUTH_EAST);
			moveableDirections[4] = br.rc.canMove(Direction.SOUTH);
			moveableDirections[5] = br.rc.canMove(Direction.SOUTH_WEST);
			moveableDirections[6] = br.rc.canMove(Direction.WEST);
			moveableDirections[7] = br.rc.canMove(Direction.NORTH_WEST);
			moveableDirectionsTime = br.currRound;
		}
		return moveableDirections;
	}
	
	public boolean[] getMovableLand()
	{
		if (br.currRound > moveableLandTime)
		{
			moveableLand[0] = br.rc.senseTerrainTile(br.currLoc.add(Direction.NORTH))==TerrainTile.LAND;
			moveableLand[1] = br.rc.senseTerrainTile(br.currLoc.add(Direction.NORTH_EAST))==TerrainTile.LAND;
			moveableLand[2] = br.rc.senseTerrainTile(br.currLoc.add(Direction.EAST))==TerrainTile.LAND;
			moveableLand[3] = br.rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH_EAST))==TerrainTile.LAND;
			moveableLand[4] = br.rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH))==TerrainTile.LAND;
			moveableLand[5] = br.rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH_WEST))==TerrainTile.LAND;
			moveableLand[6] = br.rc.senseTerrainTile(br.currLoc.add(Direction.WEST))==TerrainTile.LAND;
			moveableLand[7] = br.rc.senseTerrainTile(br.currLoc.add(Direction.NORTH_WEST))==TerrainTile.LAND;
			moveableLandTime = br.currRound;
		}
		return moveableLand;
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
				GameObject obj = br.rc.senseObjectAtLocation(
						br.rc.getLocation().add(d), level);
				adjacentGameObjectsGround[d.ordinal()] = obj;
				isAdjacentGameObjectGroundCached[d.ordinal()] = true;
				return obj;
			}
		} else if (level == RobotLevel.IN_AIR){
			if (isAdjacentGameObjectAirCached[d.ordinal()]) {
				return adjacentGameObjectsAir[d.ordinal()];
			} else {
				GameObject obj = br.rc.senseObjectAtLocation(
						br.rc.getLocation().add(d), level);
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
			TerrainTile tt = br.rc.senseTerrainTile(br.rc.getLocation().add(d));
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
			nearbyRobots = br.rc.senseNearbyGameObjects(Robot.class);
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
			capturablePowerCores = br.rc.senseCapturablePowerNodes();
		}
		return capturablePowerCores;
	}
}
