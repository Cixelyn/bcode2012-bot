package ypstrategyframework;

import battlecode.common.*;

public class DataCache {
	
	private BaseRobot br;
	private RobotController rc;
	
	private MapLocation[] alliedArchons;
	private int alliedArchonsTime = -1;
	
	private MapLocation closestArchon;
	private int closestArchonTime = -1;

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
	
	private RobotInfo closestEnemy;
	private int closestEnemyTime = -1;
	
	private MapLocation closestCapturablePowerCore;
	private int closestCapturablePowerCoreTime = -1;
	

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
	
	public MapLocation getClosestArchon() {
		if (br.currRound > closestArchonTime) {
			closestArchon = null;
			int closestDistance = Integer.MAX_VALUE;
			for (MapLocation archon : getAlliedArchons()) {
				int distance = br.currLoc.distanceSquaredTo(archon);
				if (distance > 0 && distance < closestDistance) {
					closestArchon = archon;
					closestDistance = distance;
				}
			}
		}
		return closestArchon;
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
			TerrainTile tt;
			tt = rc.senseTerrainTile(br.currLoc.add(Direction.NORTH));
			moveableLand[0] = tt==TerrainTile.LAND||tt==null;
			tt = rc.senseTerrainTile(br.currLoc.add(Direction.NORTH_EAST));
			moveableLand[1] = tt==TerrainTile.LAND||tt==null;
			tt = rc.senseTerrainTile(br.currLoc.add(Direction.EAST));
			moveableLand[2] = tt==TerrainTile.LAND||tt==null;
			tt = rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH_EAST));
			moveableLand[3] = tt==TerrainTile.LAND||tt==null;
			tt = rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH));
			moveableLand[4] = tt==TerrainTile.LAND||tt==null;
			tt = rc.senseTerrainTile(br.currLoc.add(Direction.SOUTH_WEST));
			moveableLand[5] = tt==TerrainTile.LAND||tt==null;
			tt = rc.senseTerrainTile(br.currLoc.add(Direction.WEST));
			moveableLand[6] = tt==TerrainTile.LAND||tt==null;
			tt = rc.senseTerrainTile(br.currLoc.add(Direction.NORTH_WEST));
			moveableLand[7] = tt==TerrainTile.LAND||tt==null;
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
	
	public RobotInfo getClosestEnemy() throws GameActionException {
		if (br.currRound > closestEnemyTime) {
			int closestDistance = Integer.MAX_VALUE;
			closestEnemy = null;
			// TODO(jven): prioritize archons?
			for (Robot r : getNearbyRobots()) {
				if (r.getTeam() != br.myTeam) {
					RobotInfo rInfo = rc.senseRobotInfo(r);
					// don't overkill
					if (rInfo.energon <= 0) {
						continue;
					}
					// don't shoot at towers you can't hurt
					if (rInfo.type == RobotType.TOWER && !isTowerTargetable(rInfo)) {
						continue;
					}
					int distance = br.currLoc.distanceSquaredTo(rInfo.location);
					if (distance < closestDistance) {
						closestDistance = distance;
						closestEnemy = rInfo;
					}
				}
			}
			closestEnemyTime = br.currRound;
		}
		return closestEnemy;
	}
	
	public MapLocation getClosestCapturablePowerCore()
			throws GameActionException {
		if (br.currRound > closestCapturablePowerCoreTime) {
			int closestDistance = Integer.MAX_VALUE;
			closestCapturablePowerCore = null;
			for (MapLocation capturablePowerCore : getCapturablePowerCores()) {
				int distance = br.currLoc.distanceSquaredTo(capturablePowerCore);
				if (distance < closestDistance) {
					closestDistance = distance;
					closestCapturablePowerCore = capturablePowerCore;
				}
			}
			closestCapturablePowerCoreTime = br.currRound;
		}
		return closestCapturablePowerCore;
	}
	
	
	private boolean isTowerTargetable(
			RobotInfo tower) throws GameActionException {
		// don't shoot at enemy towers not connected to one of ours
		PowerNode pn = (PowerNode)rc.senseObjectAtLocation(
				tower.location, RobotLevel.POWER_NODE);
		if (pn == null) {
			return false;
		}
		for (PowerNode myPN : getAlliedPowerNodes()) {
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
