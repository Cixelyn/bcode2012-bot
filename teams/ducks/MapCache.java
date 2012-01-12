package ducks;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

/** This data structure caches the terrain of the world map as sensed by one robot. 
 * It stores a 256x256 boolean array representing the tiles that are walls. 
 * 
 * We set the coordinate (128,128) in the cache to correspond to the location of
 * our team's power core, and linearly shift between cache coordinates and world coordinates
 * using this transformation.
 * 
 * @author Haitao
 */
public class MapCache {
	/** True if the tile is a wall, false if the tile is ground or out of bounds. */
	final boolean[][] isWall;
	/** True if we have sensed the tile or been told by another robot about the tile. */
	final boolean[][] sensed;
	final static int MAP_SIZE = 256;
	final static int POWER_CORE_POSITION = 128;
	final BaseRobot baseRobot;
	final int powerCoreWorldX, powerCoreWorldY;
	/** The boundaries of tiles we have confirmed are ground tiles. */
	int minXGround, maxXGround, minYGround, maxYGround;
	int senseDist;
	public MapCache(BaseRobot baseRobot) {
		isWall = new boolean[MAP_SIZE][MAP_SIZE];
		sensed = new boolean[MAP_SIZE][MAP_SIZE];
		this.baseRobot = baseRobot;
		MapLocation loc = baseRobot.rc.sensePowerCore().getLocation();
		powerCoreWorldX = loc.x;
		powerCoreWorldY = loc.y;
		minXGround = powerCoreWorldX - 1;
		maxXGround = powerCoreWorldX + 1;
		minYGround = powerCoreWorldY - 1;
		maxYGround = powerCoreWorldY + 1;
		senseDist = baseRobot.myType==RobotType.ARCHON ? 5 :
			baseRobot.myType==RobotType.SCOUT ? 4 : 3;
	}
	
	/** Senses most tiles around current location in range. 
	 * Not quite all tiles, i.e. for archons does not sense tiles <6,0> away. 
	 */
	public void senseAllTiles() {
		MapLocation myLoc = baseRobot.currLoc;
		int myX = worldToCacheX(myLoc.x);
		int myY = worldToCacheY(myLoc.y);
		System.out.println(myX+" "+myY);
		for(int dx=-senseDist; dx<=senseDist; dx++) for(int dy=-senseDist; dy<=senseDist; dy++) {
			if(sensed[myX+dx][myY+dy]) continue;
			MapLocation loc = myLoc.add(dx, dy);
			TerrainTile tt = baseRobot.rc.senseTerrainTile(loc);
			if(tt!=null) {
				isWall[myX+dx][myY+dy] = tt!=TerrainTile.LAND;
				sensed[myX+dx][myY+dy] = true;
			}
		}
	}
	/**
	 * A more optimized way of sensing tiles.
	 * Given a direction we just moved in, senses only the tiles that are new.
	 * Assumes that we have sensed everything we could have in the past. 
	 * @param lastMoved the direction we just moved in
	 */
	public void senseTilesOptimized(Direction lastMoved) {
		//TODO implement
	}
	
	public int worldToCacheX(int worldX) {
		return worldX-powerCoreWorldX+POWER_CORE_POSITION;
	}
	public int cacheToWorldX(int cacheX) {
		return cacheX+powerCoreWorldX-POWER_CORE_POSITION;
	}
	public int worldToCacheY(int worldY) {
		return worldY-powerCoreWorldY+POWER_CORE_POSITION;
	}
	public int cacheToWorldY(int cacheY) {
		return cacheY+powerCoreWorldY-POWER_CORE_POSITION;
	}
	
}
