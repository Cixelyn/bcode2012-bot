package fibbyBot2;

import battlecode.common.*;

public class DataCache {
	
	private BaseRobot br;
	private RobotController rc;
	
	private MapLocation[] alliedArchons;
	private int alliedArchonsTime = -1;

	private int moveableDirectionsTime = -1;
	private boolean[] moveableDirections = new boolean[8];
	private int moveableLandTime = -1;
	private boolean[] moveableLand = new boolean[8];
	
	private boolean[] isAdjacentGameObjectGroundCached;
	private boolean[] isAdjacentGameObjectAirCached;
	private GameObject[] adjacentGameObjectsGround =
			new GameObject[Direction.values().length];
	private GameObject[] adjacentGameObjectsAir =
			new GameObject[Direction.values().length];
	private int adjacentGameObjectsTime = -1;
	
	private boolean[] isAdjacentTerrainTileCached;
	private TerrainTile[] adjacentTerrainTiles =
			new TerrainTile[Direction.values().length];
	private int adjacentTerrainTilesTime = -1;
	
	private Robot[] nearbyRobots;
	private int nearbyRobotsTime = -1;
	
	private MapLocation[] capturablePowerCores;
	private int capturablePowerCoresTime = -1;
	
	private PowerNode[] alliedPowerNodes;
	private int alliedPowerNodesTime = -1;
	
	public DataCache(BaseRobot br) {
		this.br = br;
		this.rc = br.rc;
	}
	
	public MapLocation[] getAlliedArchons() {
		if(br.currRound > alliedArchonsTime) {
			alliedArchons = rc.senseAlliedArchons();
			alliedArchonsTime = br.currRound;
		}
		return alliedArchons;
	}
	
	public boolean[] getMovableDirections()
	{
		if (br.currRound > moveableDirectionsTime)
		{
			moveableDirections[0] = rc.canMove(Direction.NORTH);
			moveableDirections[1] = rc.canMove(Direction.NORTH_EAST);
			moveableDirections[2] = rc.canMove(Direction.EAST);
			moveableDirections[3] = rc.canMove(Direction.SOUTH_EAST);
			moveableDirections[4] = rc.canMove(Direction.SOUTH);
			moveableDirections[5] = rc.canMove(Direction.SOUTH_WEST);
			moveableDirections[6] = rc.canMove(Direction.WEST);
			moveableDirections[7] = rc.canMove(Direction.NORTH_WEST);
			moveableDirectionsTime = br.currRound;
		}
		return moveableDirections;
	}
	
	public boolean[] getMovableLand()
	{
		if (br.currRound > moveableLandTime)
		{
			moveableLand[0] = rc.senseTerrainTile(br.currLoc.add(Direction.NORTH))==TerrainTile.LAND;
			moveableLand[1] = rc.senseTerrainTile(br.currLoc.add(Direction.NORTH_EAST))==TerrainTile.LAND;
			moveableLand[2] = rc.senseTerrainTile(br.currLoc.add(Direction.EAST))==TerrainTile.LAND;
			moveableLand[3] = rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH_EAST))==TerrainTile.LAND;
			moveableLand[4] = rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH))==TerrainTile.LAND;
			moveableLand[5] = rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH_WEST))==TerrainTile.LAND;
			moveableLand[6] = rc.senseTerrainTile(br.currLoc.add(Direction.WEST))==TerrainTile.LAND;
			moveableLand[7] = rc.senseTerrainTile(br.currLoc.add(Direction.NORTH_WEST))==TerrainTile.LAND;
			moveableLandTime = br.currRound;
		}
		return moveableLand;
	}
	
	
	public GameObject getAdjacentGameObject(
			Direction d, RobotLevel level) throws GameActionException {
		if (br.currRound > adjacentGameObjectsTime) {
			isAdjacentGameObjectGroundCached =
					new boolean[Direction.values().length];
			isAdjacentGameObjectAirCached =
					new boolean[Direction.values().length];
			adjacentGameObjectsTime = br.currRound;
		}
		if (level == RobotLevel.ON_GROUND) {
			if (isAdjacentGameObjectGroundCached[d.ordinal()]) {
				return adjacentGameObjectsGround[d.ordinal()];
			} else {
				GameObject obj = rc.senseObjectAtLocation(
						rc.getLocation().add(d), level);
				adjacentGameObjectsGround[d.ordinal()] = obj;
				isAdjacentGameObjectGroundCached[d.ordinal()] = true;
				return obj;
			}
		} else if (level == RobotLevel.IN_AIR){
			if (isAdjacentGameObjectAirCached[d.ordinal()]) {
				return adjacentGameObjectsAir[d.ordinal()];
			} else {
				GameObject obj = rc.senseObjectAtLocation(
						rc.getLocation().add(d), level);
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
		if (br.currRound > adjacentTerrainTilesTime) {
			isAdjacentTerrainTileCached =
					new boolean[Direction.values().length];
			adjacentTerrainTilesTime = br.currRound;
		}
		if (isAdjacentTerrainTileCached[d.ordinal()]) {
			return adjacentTerrainTiles[d.ordinal()];
		} else {
			TerrainTile tt = rc.senseTerrainTile(rc.getLocation().add(d));
			this.adjacentTerrainTiles[d.ordinal()] = tt;
			isAdjacentTerrainTileCached[d.ordinal()] = true;
			return tt;
		}
	}
	
	public Robot[] getNearbyRobots() {
		if (br.currRound > nearbyRobotsTime) {
			nearbyRobots = null;
			nearbyRobotsTime = br.currRound;
		}
		if (nearbyRobots == null) {
			nearbyRobots = rc.senseNearbyGameObjects(Robot.class);
		}
		return nearbyRobots;
	}
	
	public MapLocation[] getCapturablePowerCores() {
		if (br.currRound > capturablePowerCoresTime) {
			capturablePowerCores = null;
			capturablePowerCoresTime = br.currRound;
		}
		if (capturablePowerCores == null) {
			capturablePowerCores = rc.senseCapturablePowerNodes();
		}
		return capturablePowerCores;
	}
	
	public PowerNode[] getAlliedPowerNodes() {
		if (br.currRound > alliedPowerNodesTime) {
			alliedPowerNodes = null;
			alliedPowerNodesTime = br.currRound;
		}
		if (alliedPowerNodes == null) {
			alliedPowerNodes = rc.senseAlliedPowerNodes();
		}
		return alliedPowerNodes;
	}
}
