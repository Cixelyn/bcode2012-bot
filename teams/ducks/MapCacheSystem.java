package ducks;

import java.util.HashSet;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

/** This data structure caches the terrain of the world map as sensed by one robot. 
 * It stores a 256x256 boolean array representing the tiles that are walls. 
 * 
 * We set the coordinate (128,128) in the cache to correspond to the location of
 * our team's power core, and linearly shift between cache coordinates and world coordinates
 * using this transformation.
 */
public class MapCacheSystem {
	public final static int MAP_SIZE = 256;
	public final static int POWER_CORE_POSITION = 128;
	public final static int MAP_BLOCK_SIZE = 4;
	public final static int PACKED_MAP_SIZE = 64;
	
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
	public MapCacheSystem(BaseRobot baseRobot) {
		this.baseRobot = baseRobot;
		isWall = new boolean[MAP_SIZE][MAP_SIZE];
		sensed = new boolean[MAP_SIZE][MAP_SIZE];
		packedIsWall = new int[PACKED_MAP_SIZE][PACKED_MAP_SIZE];
		packedSensed = new int[PACKED_MAP_SIZE][PACKED_MAP_SIZE];
		packedDataUpdated = new HashSet<Integer>();
		initPackedDataStructures();
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
	private void initPackedDataStructures() {
		//17,47 are optimized magic numbers from (128-60)/4 and (128+60)/4
		for(int xb=17; xb<47; xb++) for(int yb=17; yb<47; yb++) { 
			packedIsWall[xb][yb] = xb*(1<<22)+yb*(1<<16);
		}
		for(int xb=17; xb<47; xb++)
			System.arraycopy(packedIsWall[xb], 17, packedSensed[xb], 17, 30);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("\nSurrounding map data:\n"); 
		int myX = worldToCacheX(baseRobot.curLoc.x);
		int myY = worldToCacheY(baseRobot.curLoc.y);
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
	
	/** Returns the enemy power core location if robot knows exactly where it is. <br>
	 * Otherwise returns null.
	 * @see MapCacheSystem#guessEnemyPowerCoreLocation()
	 */
	public MapLocation getEnemyPowerCoreLocation() {
		return powerNodeGraph.nodeLocations[powerNodeGraph.enemyPowerCoreID];
	}
	/** 
	 * Returns the enemy power core location if robot knows exactly where it is. <br>
	 * Otherwise, Uses symmetry and known map edges to guess 
	 * the location of the enemy power core. <br>
	 * If no map edges are known, uses power core graph to guess. 
	 * @see MapCacheSystem#getEnemyPowerCoreLocation()
	 */
	public MapLocation guessEnemyPowerCoreLocation() {
		// Return if we know for sure where the enemy core is
		MapLocation knownEnemyCoreLoc = getEnemyPowerCoreLocation();
		if(knownEnemyCoreLoc!=null) return knownEnemyCoreLoc;
		
		// If no map edges known, add up vectors of our base to each known power node location, and return a location far in that direction
		int mapEdgesKnown = 0;
		if(edgeXMin!=0) mapEdgesKnown++;
		if(edgeXMax!=0) mapEdgesKnown++;
		if(edgeYMin!=0) mapEdgesKnown++;
		if(edgeYMax!=0) mapEdgesKnown++;
		if(mapEdgesKnown==0) {
			int sdx = 0;
			int sdy = 0;
			for(int i=2; i<powerNodeGraph.nodeCount; i++) {
				sdx += powerNodeGraph.nodeLocations[i].x - powerCoreWorldX;
				sdy += powerNodeGraph.nodeLocations[i].y - powerCoreWorldY;
			}
			double magnitude = Math.sqrt(sdx*sdx+sdy*sdy);
			sdx = (int)(sdx*90/magnitude);
			sdy = (int)(sdx*90/magnitude);
			return new MapLocation(powerCoreWorldX + sdx, powerCoreWorldY + sdy);
		}
		
		/* Current heuristic: 
		 *   - assume map size is 60 if we don't know it. 
		 *   - assume rotational symmetry
		 */
		int mapSize = 61;
		int xminGuess = edgeXMin;
		int xmaxGuess = edgeXMax;
		int yminGuess = edgeYMin;
		int ymaxGuess = edgeYMax;
		if(xminGuess==0) {
			if(xmaxGuess==0) {
				xminGuess = POWER_CORE_POSITION;
				xmaxGuess = POWER_CORE_POSITION;
			} else {
				xminGuess = xmaxGuess - mapSize;
			}
		} else if(xmaxGuess==0) {
			xmaxGuess = xminGuess + mapSize;
		}
		if(yminGuess==0) {
			if(ymaxGuess==0) {
				yminGuess = POWER_CORE_POSITION;
				ymaxGuess = POWER_CORE_POSITION;
			} else {
				yminGuess = ymaxGuess - mapSize;
			}
		} else if(ymaxGuess==0) {
			ymaxGuess = yminGuess + mapSize;
		}
		int x = xminGuess+xmaxGuess-POWER_CORE_POSITION;
		int y = yminGuess+ymaxGuess-POWER_CORE_POSITION;
		return new MapLocation(cacheToWorldX(x), cacheToWorldY(y));
	}
	/** Returns the location of the best power node to capture, assuming
	 * that the goal is to route to the enemy power core. <br>
	 * Right now, we use the heuristic of going to the power node P with the 
	 * smallest value of dist(cur_pos, P)+dist(P, enemy_power_core_guess)
	 */
	public MapLocation guessBestPowerNodeToCapture() {
		MapLocation enemyPowerCoreGuess = guessEnemyPowerCoreLocation();
		MapLocation[] nodeLocs = baseRobot.dc.getCapturablePowerCores();
		MapLocation bestLoc = null;
		double bestValue = Integer.MAX_VALUE;
		for(MapLocation loc: nodeLocs) {
			double value = Math.sqrt(baseRobot.curLoc.distanceSquaredTo(loc)) + 
					Math.sqrt(loc.distanceSquaredTo(enemyPowerCoreGuess));
			if(value<bestValue) {
				bestValue = value;
				bestLoc = loc;
			}
		}
		return bestLoc;
	}
	
	/** Sense all tiles, all map edges, and all power nodes in the robot's sensing range. */
	public void senseAll() {
		if(baseRobot.myType == RobotType.SOLDIER)
			return;
		senseAllTiles();
		senseAllMapEdges();
		sensePowerNodes();
	}
	/** Sense all tiles, all map edges, and all power nodes in the robot's sensing range. <br>
	 * Assumes that we just moved in a direction, and we only want to sense the new information.
	 */
	public void senseAfterMove(Direction lastMoved) {
		if(baseRobot.myType == RobotType.SOLDIER)
			return;
		if(lastMoved==null || lastMoved==Direction.NONE || lastMoved==Direction.OMNI) {
			senseAll();
			return;
		}
		senseTilesOptimized(lastMoved);
		senseMapEdges(lastMoved);
		sensePowerNodes();
	}
	
	/** Senses the terrain of all tiles in sensing range of the robot. 
	 * Should be called when a unit (probably only archon or scout) is newly spawned.
	 */
	private void senseAllTiles() {
		MapLocation myLoc = baseRobot.curLoc;
		int myX = worldToCacheX(myLoc.x);
		int myY = worldToCacheY(myLoc.y);
		for(int dx=-senseRadius; dx<=senseRadius; dx++) for(int dy=-senseRadius; dy<=senseRadius; dy++) {
			int x = myX+dx;
			int y = myY+dy;
			int xblock = x/MAP_BLOCK_SIZE;
			int yblock = y/MAP_BLOCK_SIZE;
			if(sensed[x][y]) continue;
			MapLocation loc = myLoc.add(dx, dy);
			TerrainTile tt = baseRobot.rc.senseTerrainTile(loc);
			if(tt!=null) {
				boolean b = (tt!=TerrainTile.LAND);
				isWall[x][y] = b;
				if(b) packedIsWall[xblock][yblock] |= (1 << (x%4*4+y%4));
				sensed[x][y] = true;
				packedSensed[xblock][yblock] |= (1 << (x%4*4+y%4));
			}
		}
	}
	/**
	 * A more optimized way of sensing tiles.
	 * Given a direction we just moved in, senses only the tiles that are new.
	 * Assumes that we have sensed everything we could have in the past. 
	 * @param lastMoved the direction we just moved in
	 */
	private void senseTilesOptimized(Direction lastMoved) {
		final int[][] list = optimizedSensingList[lastMoved.ordinal()];
		MapLocation myLoc = baseRobot.curLoc;
		int myX = worldToCacheX(myLoc.x);
		int myY = worldToCacheY(myLoc.y);
		for (int i=0; i<list.length; i++) {
			int dx = list[i][0];
			int dy = list[i][1];
			int x = myX+dx;
			int y = myY+dy;
			int xblock = x/MAP_BLOCK_SIZE;
			int yblock = y/MAP_BLOCK_SIZE;
			if(sensed[x][y]) continue;
			MapLocation loc = myLoc.add(dx, dy);
			TerrainTile tt = baseRobot.rc.senseTerrainTile(loc);
			if(tt!=null) {
				boolean b = (tt!=TerrainTile.LAND);
				isWall[x][y] = b;
				if(b) packedIsWall[xblock][yblock] |= (1 << (x%4*4+y%4));
				sensed[x][y] = true;
				packedSensed[xblock][yblock] |= (1 << (x%4*4+y%4));
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
			if(xblock==32 && yblock==32) System.out.println(" AOPOEAHKEWO "+Integer.toBinaryString(packedIsWall[32][32]));
			packedSensed[xblock][yblock] |= packedSensedInfo;
		}
	}
	/** Extracts some packed data in the updated packed fields into the 
	 * unpacked arrays for terrain data. <br>
	 * This must be run repeatedly to extract everything.
	 * @return true if we are done extracting
	 */
	public boolean extractUpdatedPackedDataStep() {
		if(packedDataUpdated.isEmpty()) return true;
		int block = packedDataUpdated.iterator().next();
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
		packedDataUpdated.remove(block);
		return false;
	}
	
	/** Updates the edges of the map that we can sense. 
	 * Checks all four cardinal directions for edges. 
	 * Should be called in the beginning of the game. */
	private void senseAllMapEdges() {
		MapLocation myLoc = baseRobot.curLoc;
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
	private void senseMapEdges(Direction lastMoved) {
		MapLocation myLoc = baseRobot.curLoc;
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
	private void sensePowerNodes() {
		for(PowerNode node: baseRobot.rc.senseNearbyGameObjects(PowerNode.class)) {
			MapLocation nodeLoc = node.getLocation();
			short id = getPowerNodeID(nodeLoc);
			if(powerNodeGraph.nodeSensed[id])
				continue;
			if(id==0) {
				powerNodeGraph.nodeCount++;
				id = powerNodeGraph.nodeCount;
				powerNodeGraph.nodeLocations[id] = nodeLoc;
				powerNodeID[worldToCacheX(nodeLoc.x)][worldToCacheY(nodeLoc.y)] = id;
			}
			if(node.powerCoreTeam()!=null && node.powerCoreTeam()!=baseRobot.myTeam) {
				powerNodeGraph.enemyPowerCoreID = id;
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
			powerNodeGraph.nodeSensedCount++;
			powerNodeGraph.nodeSensed[id] = true;
		}
	}
	
	/** Update power node graph with data from a message. */
	public void integratePowerNodes(int[] data) {
		int mask = (1<<15)-1;
		if(powerNodeGraph.enemyPowerCoreID==0 && data[0]!=32001) {
			int coreX = data[0] >> 15;
			int coreY = data[0] & mask;
			System.out.println(coreX+" "+coreY);
			short coreID = powerNodeID[worldToCacheX(coreX)][worldToCacheY(coreY)];
			if(coreID==0) {
				powerNodeGraph.nodeCount++;
				coreID = powerNodeGraph.nodeCount;
				powerNodeGraph.nodeLocations[coreID] = new MapLocation(coreX, coreY);
				powerNodeID[worldToCacheX(coreX)][worldToCacheY(coreY)] = coreID;
			} 
			powerNodeGraph.enemyPowerCoreID = coreID;
		}
		int nodeX = data[1] >> 15;
		int nodeY = data[1] & mask;
		MapLocation nodeLoc = new MapLocation(nodeX, nodeY);
		short id = baseRobot.mc.getPowerNodeID(nodeLoc);
		if(powerNodeGraph.nodeSensed[id])
			return;
		if(id==0) {
			powerNodeGraph.nodeCount++;
			id = powerNodeGraph.nodeCount;
			powerNodeGraph.nodeLocations[id] = nodeLoc;
			powerNodeID[worldToCacheX(nodeX)][worldToCacheY(nodeY)] = id;
		}
		for(int i=2; i<data.length; i++) {
			int neighborX = data[i] >> 15;
			int neighborY = data[i] & mask;
			MapLocation neighborLoc = new MapLocation(neighborX, neighborY);
			short neighborID = getPowerNodeID(neighborLoc);
			if(powerNodeGraph.nodeSensed[neighborID]) 
				continue;
			if(neighborID==0) {
				powerNodeGraph.nodeCount++;
				neighborID = powerNodeGraph.nodeCount;
				powerNodeGraph.nodeLocations[neighborID] = neighborLoc;
				powerNodeID[worldToCacheX(neighborX)][worldToCacheY(neighborY)] = neighborID;
			}
			powerNodeGraph.adjacencyList[id][powerNodeGraph.degreeCount[id]++] = neighborID;
			powerNodeGraph.adjacencyList[neighborID][powerNodeGraph.degreeCount[neighborID]++] = id;
		}
		powerNodeGraph.nodeSensedCount++;
		powerNodeGraph.nodeSensed[id] = true;
	}
	
	
	/** Does this robot know about the terrain of the given map location? */
	public boolean isSensed(MapLocation loc) {
		return sensed[worldToCacheX(loc.x)][worldToCacheY(loc.y)];
	}
	/** Is the given map location a wall tile (or an off map tile)? <br>
	 * Will return false if the robot does not know. 
	 */
	public boolean isWall(MapLocation loc) {
		return isWall[worldToCacheX(loc.x)][worldToCacheY(loc.y)];
	}
	/** Is the given map location an off map tile? <br>
	 *  Will return false if the robot does not know. 
	 */
	public boolean isOffMap(MapLocation loc) {
		int x = worldToCacheX(loc.x);
		int y = worldToCacheY(loc.y);
		return edgeXMin!=0 && x<=edgeXMin || edgeXMax!=0 && x>=edgeXMax ||
				edgeYMin!=0 && y<=edgeYMin || edgeYMax!=0 && y>=edgeYMax;
	}
	/** Gets the unique index of the power node at the given location 
	 * for PowerNodeGraph to use in its data structure. <br>
	 * Returns 0 if there is no power node known to be there.
	 */
	private short getPowerNodeID(MapLocation loc) {
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
