package ducks;

import java.util.HashSet;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.TerrainTile;

/** This data structure caches the terrain of the world map as sensed by one robot. 
 * It stores a 256x256 boolean array representing the tiles that are walls. 
 * 
 * We set the coordinate (128,128) in the cache to correspond to the location of
 * our team's power core, and linearly shift between cache coordinates and world coordinates
 * using this transformation.
 */
public class MapCache {
	final static int MAP_SIZE = 256;
	final static int POWER_CORE_POSITION = 128;
	final static int MAP_BLOCK_SIZE = 4;
	final static int PACKED_MAP_SIZE = 64;
	
	final BaseRobot baseRobot;
	/** True if the tile is a wall, false if the tile is ground or out of bounds. */
	final boolean[][] isWall;
	final int[][] packedIsWall;
	/** True if we have sensed the tile or been told by another robot about the tile. */
	final boolean[][] sensed;
	final int[][] packedSensed;
	final HashSet<Integer> packedDataUpdated;
	/** Stores the IDs of power nodes the robot has discovered. */
	final short[][] powerNodeID;
	final PowerNodeGraph powerNodeGraph;
	final int powerCoreWorldX, powerCoreWorldY;
	/** The edges of the map, in cache coordinates. <br>
	 * These values are all exclusive, so anything with one of these coordinates is out of bounds.
	 */
	public int edgeXMin, edgeXMax, edgeYMin, edgeYMax;
	/** Just a magic number to optimize the senseAllTiles() function. */
	int senseRadius;
	/** optimized sensing list of position vectors for each unit */
	private final int[][][] optimizedSensingList;
	public MapCache(BaseRobot baseRobot) {
		this.baseRobot = baseRobot;
		isWall = new boolean[MAP_SIZE][MAP_SIZE];
		sensed = new boolean[MAP_SIZE][MAP_SIZE];
		packedIsWall = new int[PACKED_MAP_SIZE][PACKED_MAP_SIZE];
		packedSensed = new int[PACKED_MAP_SIZE][PACKED_MAP_SIZE];
		packedDataUpdated = new HashSet<Integer>();
		//initPackedDataStructures();
		powerNodeID = new short[MAP_SIZE][MAP_SIZE];
		MapLocation loc = baseRobot.rc.sensePowerCore().getLocation();
		powerCoreWorldX = loc.x;
		powerCoreWorldY = loc.y;
		powerNodeGraph = new PowerNodeGraph();
		edgeXMin = 0;
		edgeXMax = 0;
		edgeYMin = 0;
		edgeYMax = 0;
		senseRadius = (int)Math.sqrt(baseRobot.myType.sensorRadiusSquared);
		switch(baseRobot.myType) {
			case ARCHON: optimizedSensingList = sensorRangeARCHON; break;
			case SCOUT: optimizedSensingList = sensorRangeSCOUT; break;
			case DISRUPTER: optimizedSensingList = sensorRangeDISRUPTER; break;
			case SCORCHER: optimizedSensingList = sensorRangeSCORCHER; break;
			case SOLDIER: optimizedSensingList = sensorRangeSOLDIER; break;
			default:
				optimizedSensingList = new int[0][0][0];
		}
	}
	public void initPackedDataStructures() {
		//17,47 are optimized magic numbers from (128-60)/4 and (128+60)/4
		for(int xb=17; xb<47; xb++) for(int yb=17; yb<47; yb++) { 
			packedIsWall[xb][yb] = xb*(1<<22)+yb*(1<<16);
		}
		for(int xb=17; xb<47; xb++)
		System.arraycopy(packedIsWall[xb], 17, packedSensed[xb], 17, 30);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("\nSurrounding map data:"); 
		int myX = worldToCacheX(baseRobot.currLoc.x);
		int myY = worldToCacheY(baseRobot.currLoc.y);
		for(int y=myY-10; y<myY+10; y++) { 
			for(int x=myX-10; x<myX+10; x++) 
				sb.append((y==myY&&x==myX)?'x':(!sensed[x][y])?'o':(isWall[x][y])?'#':'.'); 
			sb.append("\n"); 
		} 
		sb.append("Edge data: \nx=["+edgeXMin+","+edgeXMax+"] y=["+edgeYMin+","+edgeYMax+"] \n");
		sb.append("Power node graph:");
		sb.append(powerNodeGraph.toString());
		return sb.toString();
	}
	/** Sense all tiles, all map edges, and all power nodes in the robot's sensing range. */
	public void senseAll() {
		senseAllTiles();
		senseAllMapEdges();
		sensePowerNodes();
		
	}
	
	/** Sense all tiles, all map edges, and all power nodes in the robot's sensing range. <br>
	 * Assumes that we just moved in a direction, and we only want to sense the new information.
	 */
	public void senseAfterMove(Direction lastMoved) {
		if(lastMoved==null || lastMoved==Direction.NONE || lastMoved==Direction.OMNI) {
			senseAll();
			return;
		}
		senseTilesOptimized(lastMoved);
		senseMapEdges(lastMoved);
		sensePowerNodes();
//		if(Clock.getRoundNum()%100==5 && Clock.getRoundNum()<=1050)
//			System.out.println(this);
	}
	
	/** Senses the terrain of all tiles in sensing range of the robot. 
	 * Should be called when a unit (probably only archon or scout) is newly spawned.
	 */
	public void senseAllTiles() {
		MapLocation myLoc = baseRobot.currLoc;
		int myX = worldToCacheX(myLoc.x);
		int myY = worldToCacheY(myLoc.y);
		for(int dx=-senseRadius; dx<=senseRadius; dx++) for(int dy=-senseRadius; dy<=senseRadius; dy++) {
			int x = myX+dx;
			int y = myY+dy;
			int xblock = myX/MAP_BLOCK_SIZE;
			int yblock = myY/MAP_BLOCK_SIZE;
			if(sensed[x][y]) continue;
			MapLocation loc = myLoc.add(dx, dy);
			TerrainTile tt = baseRobot.rc.senseTerrainTile(loc);
			if(tt!=null) {
				boolean b = (tt!=TerrainTile.LAND);
				isWall[x][y] = b;
				if(b) packedIsWall[xblock][yblock] |= (1 << x%4*4+y&4);
				sensed[x][y] = true;
				packedSensed[xblock][yblock] |= (1 << x%4*4+y&4);
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
		final int[][] list = optimizedSensingList[lastMoved.ordinal()];
		MapLocation myLoc = baseRobot.currLoc;
		int myX = worldToCacheX(myLoc.x);
		int myY = worldToCacheY(myLoc.y);
		for (int i=0; i<list.length; i++) {
			int dx = list[i][0];
			int dy = list[i][1];
			int x = myX+dx;
			int y = myY+dy;
			int xblock = myX/MAP_BLOCK_SIZE;
			int yblock = myY/MAP_BLOCK_SIZE;
			if(sensed[x][y]) continue;
			MapLocation loc = myLoc.add(dx, dy);
			TerrainTile tt = baseRobot.rc.senseTerrainTile(loc);
			if(tt!=null) {
				boolean b = (tt!=TerrainTile.LAND);
				isWall[x][y] = b;
				if(b) packedIsWall[xblock][yblock] |= (1 << x%4*4+y&4);
				sensed[x][y] = true;
				packedSensed[xblock][yblock] |= (1 << x%4*4+y&4);
			}
			
		}
	}
	
	/** Combines packed terrain data with existing packed terrain data. */
	public void integrateTerrainInfo(int packedIsWallInfo, int packedSensedInfo) {
		int block = (packedIsWallInfo >> 16);
		int xblock = block / 64;
		int yblock = block % 64;
		if(packedSensed[xblock][yblock]!=packedSensedInfo) {
			packedDataUpdated.add(block);
			packedIsWall[xblock][yblock] |= packedIsWallInfo;
			packedSensed[xblock][yblock] |= packedSensedInfo;
		}
	}
	/** Extracts all the packed data in the updated packed fields into the 
	 * unpacked arrays for terrain data.
	 */
	public void extractUpdatedPackedData() {
		for(int block: packedDataUpdated) {
			int xblock = block / 64;
			int yblock = block % 64;
			int isWallData = packedIsWall[xblock][yblock];
			int sensedData = packedSensed[xblock][yblock];
			for(int bit=0; bit<16; bit++) {
				int x = xblock*MAP_BLOCK_SIZE+bit/4;
				int y = yblock*MAP_BLOCK_SIZE+bit%4;
				isWall[x][y] = ((isWallData & (1<<bit)) != 0);
				sensed[x][y] = ((sensedData & (1<<bit)) != 0);
			}
		}
	}
	
	/** Updates the edges of the map that we can sense. 
	 * Checks all four cardinal directions for edges. 
	 * Should be called in the beginning of the game. */
	public void senseAllMapEdges() {
		MapLocation myLoc = baseRobot.currLoc;
		if(edgeXMin==0 && 
				baseRobot.rc.senseTerrainTile(myLoc.add(Direction.WEST, senseRadius))==TerrainTile.OFF_MAP) {
			int d = senseRadius;
			while(baseRobot.rc.senseTerrainTile(myLoc.add(Direction.WEST, d-1))==TerrainTile.OFF_MAP) {
				d--;
			}
			edgeXMin = worldToCacheX(myLoc.x) - d;
		}
		if(edgeXMax==0 && 
				baseRobot.rc.senseTerrainTile(myLoc.add(Direction.EAST, senseRadius))==TerrainTile.OFF_MAP) {
			int d = senseRadius;
			while(baseRobot.rc.senseTerrainTile(myLoc.add(Direction.EAST, d-1))==TerrainTile.OFF_MAP) {
				d--;
			}
			edgeXMax = worldToCacheX(myLoc.x) + d;
		}
		if(edgeYMin==0 && 
				baseRobot.rc.senseTerrainTile(myLoc.add(Direction.NORTH, senseRadius))==TerrainTile.OFF_MAP) {
			int d = senseRadius;
			while(baseRobot.rc.senseTerrainTile(myLoc.add(Direction.NORTH, d-1))==TerrainTile.OFF_MAP) {
				d--;
			}
			edgeYMin = worldToCacheY(myLoc.y) - d;
		}
		if(edgeYMax==0 && 
				baseRobot.rc.senseTerrainTile(myLoc.add(Direction.SOUTH, senseRadius))==TerrainTile.OFF_MAP) {
			int d = senseRadius;
			while(baseRobot.rc.senseTerrainTile(myLoc.add(Direction.SOUTH, d-1))==TerrainTile.OFF_MAP) {
				d--;
			}
			edgeYMax = worldToCacheY(myLoc.y) + d;
		}
	}
	/** Updates the edges of the map that we can sense. 
	 * Only checks for edges in directions that we are moving towards.
	 *  
	 * For example, if we just moved NORTH, we only need to check to the north of us for a new wall,
	 * since a wall could not have appeared in any other direction. */
	public void senseMapEdges(Direction lastMoved) {
		MapLocation myLoc = baseRobot.currLoc;
		if(edgeXMin==0 && lastMoved.dx==-1 && 
				baseRobot.rc.senseTerrainTile(myLoc.add(Direction.WEST, senseRadius))==TerrainTile.OFF_MAP) {
			int d = senseRadius;
			while(baseRobot.rc.senseTerrainTile(myLoc.add(Direction.WEST, d-1))==TerrainTile.OFF_MAP) {
				d--;
			}
			edgeXMin = worldToCacheX(myLoc.x) - d;
		}
		if(edgeXMax==0 && lastMoved.dx==1 && 
				baseRobot.rc.senseTerrainTile(myLoc.add(Direction.EAST, senseRadius))==TerrainTile.OFF_MAP) {
			int d = senseRadius;
			while(baseRobot.rc.senseTerrainTile(myLoc.add(Direction.EAST, d-1))==TerrainTile.OFF_MAP) {
				d--;
			}
			edgeXMax = worldToCacheX(myLoc.x) + d;
		}
		if(edgeYMin==0 && lastMoved.dy==-1 && 
				baseRobot.rc.senseTerrainTile(myLoc.add(Direction.NORTH, senseRadius))==TerrainTile.OFF_MAP) {
			int d = senseRadius;
			while(baseRobot.rc.senseTerrainTile(myLoc.add(Direction.NORTH, d-1))==TerrainTile.OFF_MAP) {
				d--;
			}
			edgeYMin = worldToCacheY(myLoc.y) - d;
		}
		if(edgeYMax==0 && lastMoved.dy==1 && 
				baseRobot.rc.senseTerrainTile(myLoc.add(Direction.SOUTH, senseRadius))==TerrainTile.OFF_MAP) {
			int d = senseRadius;
			while(baseRobot.rc.senseTerrainTile(myLoc.add(Direction.SOUTH, d-1))==TerrainTile.OFF_MAP) {
				d--;
			}
			edgeYMax = worldToCacheY(myLoc.y) + d;
		}
	}
	
	/** Updates the power node graph with all the power nodes in sensing range and their connections. */
	public void sensePowerNodes() {
		for(PowerNode node: baseRobot.rc.senseNearbyGameObjects(PowerNode.class)) {
			MapLocation nodeLoc = node.getLocation();
			short id = getPowerNodeID(nodeLoc);
			if(powerNodeGraph.nodeSensed[id])
				continue;
			if(id==0) {
				powerNodeGraph.nodeCount++;
				id = powerNodeGraph.nodeCount;
				powerNodeGraph.nodeLocations[id] = node.getLocation();
				powerNodeID[worldToCacheX(nodeLoc.x)][worldToCacheY(nodeLoc.y)] = id;
			}
			for(MapLocation neighborLoc: node.neighbors()) {
				short neighborID = getPowerNodeID(neighborLoc);
				if(powerNodeGraph.nodeSensed[neighborID]) 
					continue;
				if(neighborID==0) {
					powerNodeGraph.nodeCount++;
					neighborID = powerNodeGraph.nodeCount;
					powerNodeGraph.nodeLocations[neighborID] = neighborLoc;
					powerNodeID[worldToCacheX(neighborLoc.x)][worldToCacheY(neighborLoc.y)] = neighborID;
				}
				powerNodeGraph.adjacencyList[id][powerNodeGraph.degreeCount[id]++] = neighborID;
				powerNodeGraph.adjacencyList[neighborID][powerNodeGraph.degreeCount[neighborID]++] = id;
			}
			powerNodeGraph.nodeSensed[id] = true;
		}
	}
	
	
	/** Does this robot know about the terrain of the given map location? */
	public boolean isSensed(MapLocation loc) {
		return sensed[worldToCacheX(loc.x)][worldToCacheY(loc.y)];
	}
	/** Is the given map location a wall tile? Will return false if the robot does not know. */
	public boolean isWall(MapLocation loc) {
		return isWall[worldToCacheX(loc.x)][worldToCacheY(loc.y)];
	}
	/** Gets the unique index of the power node at the given location 
	 * for PowerNodeGraph to use in its data structure.
	 */
	public short getPowerNodeID(MapLocation loc) {
		return powerNodeID[worldToCacheX(loc.x)][worldToCacheY(loc.y)];
	}
	
	/** Converts from world x coordinates to cache x coordinates. */
	public int worldToCacheX(int worldX) {
		return worldX-powerCoreWorldX+POWER_CORE_POSITION;
	}
	/** Converts from world x coordinates to cache y coordinates. */
	public int cacheToWorldX(int cacheX) {
		return cacheX+powerCoreWorldX-POWER_CORE_POSITION;
	}
	/** Converts from cache x coordinates to world x coordinates. */
	public int worldToCacheY(int worldY) {
		return worldY-powerCoreWorldY+POWER_CORE_POSITION;
	}
	/** Converts from cache x coordinates to world y coordinates. */
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
