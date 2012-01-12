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
	/** optimized sensing list for each unit */
	private final int[][][] optimizedSensingList;
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
		switch(baseRobot.myType)
		{
		case ARCHON: optimizedSensingList = sensorRangeARCHON; break;
		case SCOUT: optimizedSensingList = sensorRangeSCOUT; break;
		case DISRUPTER: optimizedSensingList = sensorRangeDISRUPTER; break;
		case SCORCHER: optimizedSensingList = sensorRangeSCORCHER; break;
		case SOLDIER: optimizedSensingList = sensorRangeSOLDIER; break;
		default:
			optimizedSensingList = new int[0][0][0];
		}
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
		if (lastMoved.ordinal()>7) return;
		final int[][] list = optimizedSensingList[lastMoved.ordinal()];
		MapLocation myLoc = baseRobot.currLoc;
		int x = worldToCacheX(myLoc.x);
		int y = worldToCacheY(myLoc.y);
		for (int i=0; i<list.length; i++)
		{
			int dx = list[i][0];
			int dy = list[i][1];
			if(sensed[x+dx][y+dy]) continue;
			MapLocation loc = myLoc.add(dx, dy);
			TerrainTile tt = baseRobot.rc.senseTerrainTile(loc);
			if(tt!=null) {
				isWall[x+dx][y+dy] = tt!=TerrainTile.LAND;
				sensed[x+dx][y+dy] = true;
			}
			
		}
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
	
	
//	Magic arrays
	private static final int[][][] sensorRangeARCHON = new int[][][] { //ARCHON
		{ //NORTH
			{-6,0},{-5,-3},{-4,-4},{-3,-5},{-2,-5},{-1,-5},{0,-6},{1,-5},{2,-5},{3,-5},{4,-4},{5,-3},{6,0},
		},
		{ //NORTH_EAST
			{-3,-5},{-2,-5},{0,-6},{0,-5},{1,-5},{2,-5},{3,-5},{3,-4},{4,-4},{4,-3},{5,-3},{5,-2},{5,-1},{5,0},{5,2},{5,3},{6,0},
		},
		{ //EAST
			{0,-6},{0,6},{3,-5},{3,5},{4,-4},{4,4},{5,-3},{5,-2},{5,-1},{5,1},{5,2},{5,3},{6,0},
		},
		{ //SOUTH_EAST
			{-3,5},{-2,5},{0,5},{0,6},{1,5},{2,5},{3,4},{3,5},{4,3},{4,4},{5,-3},{5,-2},{5,0},{5,1},{5,2},{5,3},{6,0},
		},
		{ //SOUTH
			{-6,0},{-5,3},{-4,4},{-3,5},{-2,5},{-1,5},{0,6},{1,5},{2,5},{3,5},{4,4},{5,3},{6,0},
		},
		{ //SOUTH_WEST
			{-6,0},{-5,-3},{-5,-2},{-5,0},{-5,1},{-5,2},{-5,3},{-4,3},{-4,4},{-3,4},{-3,5},{-2,5},{-1,5},{0,5},{0,6},{2,5},{3,5},
		},
		{ //WEST
			{-6,0},{-5,-3},{-5,-2},{-5,-1},{-5,1},{-5,2},{-5,3},{-4,-4},{-4,4},{-3,-5},{-3,5},{0,-6},{0,6},
		},
		{ //NORTH_WEST
			{-6,0},{-5,-3},{-5,-2},{-5,-1},{-5,0},{-5,2},{-5,3},{-4,-4},{-4,-3},{-3,-5},{-3,-4},{-2,-5},{-1,-5},{0,-6},{0,-5},{2,-5},{3,-5},
		},
	};
	private static final int[][][] sensorRangeSOLDIER = new int[][][] { //SOLDIER
		{ //NORTH
			{-3,-1},{-2,-2},{-1,-3},{0,-3},{1,-3},{2,-2},{3,-1},
		},
		{ //NORTH_EAST
			{-1,-3},{0,-3},{1,-3},{1,-2},{2,-2},{2,-1},{3,-1},{3,0},{3,1},
		},
		{ //EAST
			{1,-3},{1,3},{2,-2},{2,2},{3,-1},{3,0},{3,1},
		},
		{ //SOUTH_EAST
			{-1,3},{0,3},{1,2},{1,3},{2,1},{2,2},{3,-1},{3,0},{3,1},
		},
		{ //SOUTH
			{-3,1},{-2,2},{-1,3},{0,3},{1,3},{2,2},{3,1},
		},
		{ //SOUTH_WEST
			{-3,-1},{-3,0},{-3,1},{-2,1},{-2,2},{-1,2},{-1,3},{0,3},{1,3},
		},
		{ //WEST
			{-3,-1},{-3,0},{-3,1},{-2,-2},{-2,2},{-1,-3},{-1,3},
		},
		{ //NORTH_WEST
			{-3,-1},{-3,0},{-3,1},{-2,-2},{-2,-1},{-1,-3},{-1,-2},{0,-3},{1,-3},
		},
	};
	private static final int[][][] sensorRangeSCOUT = new int[][][] { //SCOUT
		{ //NORTH
			{-5,0},{-4,-3},{-3,-4},{-2,-4},{-1,-4},{0,-5},{1,-4},{2,-4},{3,-4},{4,-3},{5,0},
		},
		{ //NORTH_EAST
			{-3,-4},{-2,-4},{0,-5},{0,-4},{1,-4},{2,-4},{3,-4},{3,-3},{4,-3},{4,-2},{4,-1},{4,0},{4,2},{4,3},{5,0},
		},
		{ //EAST
			{0,-5},{0,5},{3,-4},{3,4},{4,-3},{4,-2},{4,-1},{4,1},{4,2},{4,3},{5,0},
		},
		{ //SOUTH_EAST
			{-3,4},{-2,4},{0,4},{0,5},{1,4},{2,4},{3,3},{3,4},{4,-3},{4,-2},{4,0},{4,1},{4,2},{4,3},{5,0},
		},
		{ //SOUTH
			{-5,0},{-4,3},{-3,4},{-2,4},{-1,4},{0,5},{1,4},{2,4},{3,4},{4,3},{5,0},
		},
		{ //SOUTH_WEST
			{-5,0},{-4,-3},{-4,-2},{-4,0},{-4,1},{-4,2},{-4,3},{-3,3},{-3,4},{-2,4},{-1,4},{0,4},{0,5},{2,4},{3,4},
		},
		{ //WEST
			{-5,0},{-4,-3},{-4,-2},{-4,-1},{-4,1},{-4,2},{-4,3},{-3,-4},{-3,4},{0,-5},{0,5},
		},
		{ //NORTH_WEST
			{-5,0},{-4,-3},{-4,-2},{-4,-1},{-4,0},{-4,2},{-4,3},{-3,-4},{-3,-3},{-2,-4},{-1,-4},{0,-5},{0,-4},{2,-4},{3,-4},
		},
	};
	private static final int[][][] sensorRangeDISRUPTER = new int[][][] { //DISRUPTER
		{ //NORTH
			{-4,0},{-3,-2},{-2,-3},{-1,-3},{0,-4},{1,-3},{2,-3},{3,-2},{4,0},
		},
		{ //NORTH_EAST
			{-2,-3},{0,-4},{0,-3},{1,-3},{2,-3},{2,-2},{3,-2},{3,-1},{3,0},{3,2},{4,0},
		},
		{ //EAST
			{0,-4},{0,4},{2,-3},{2,3},{3,-2},{3,-1},{3,1},{3,2},{4,0},
		},
		{ //SOUTH_EAST
			{-2,3},{0,3},{0,4},{1,3},{2,2},{2,3},{3,-2},{3,0},{3,1},{3,2},{4,0},
		},
		{ //SOUTH
			{-4,0},{-3,2},{-2,3},{-1,3},{0,4},{1,3},{2,3},{3,2},{4,0},
		},
		{ //SOUTH_WEST
			{-4,0},{-3,-2},{-3,0},{-3,1},{-3,2},{-2,2},{-2,3},{-1,3},{0,3},{0,4},{2,3},
		},
		{ //WEST
			{-4,0},{-3,-2},{-3,-1},{-3,1},{-3,2},{-2,-3},{-2,3},{0,-4},{0,4},
		},
		{ //NORTH_WEST
			{-4,0},{-3,-2},{-3,-1},{-3,0},{-3,2},{-2,-3},{-2,-2},{-1,-3},{0,-4},{0,-3},{2,-3},
		},
	};
	private static final int[][][] sensorRangeSCORCHER = new int[][][] { //SCORCHER
		{ //NORTH
			{-2,-2},{-1,-3},{0,-3},{1,-3},{2,-2},
		},
		{ //NORTH_EAST
			{-1,-3},{0,-3},{1,-3},{1,-2},{2,-2},{2,-1},{3,-1},{3,0},{3,1},
		},
		{ //EAST
			{2,-2},{2,2},{3,-1},{3,0},{3,1},
		},
		{ //SOUTH_EAST
			{-1,3},{0,3},{1,2},{1,3},{2,1},{2,2},{3,-1},{3,0},{3,1},
		},
		{ //SOUTH
			{-2,2},{-1,3},{0,3},{1,3},{2,2},
		},
		{ //SOUTH_WEST
			{-3,-1},{-3,0},{-3,1},{-2,1},{-2,2},{-1,2},{-1,3},{0,3},{1,3},
		},
		{ //WEST
			{-3,-1},{-3,0},{-3,1},{-2,-2},{-2,2},
		},
		{ //NORTH_WEST
			{-3,-1},{-3,0},{-3,1},{-2,-2},{-2,-1},{-1,-3},{-1,-2},{0,-3},{1,-3},
		},
	};
}
