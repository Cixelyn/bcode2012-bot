package team047;

import battlecode.common.Clock;
import battlecode.common.MapLocation;

public class SharedExplorationSystem {
	final BaseRobot baseRobot;
	public SharedExplorationSystem(BaseRobot baseRobot) {
		this.baseRobot = baseRobot;
	}
	
	/** Broadcasts robot's knowledge of one column of the map.
	 * The column is decided by modding the turn number by robot's guess of the number
	 * of columns on the map.
	 */
	public void broadcastMapFragment() {
		int startRow;
		int numRowBlocks;
		if(baseRobot.mc.edgeXMin!=0) {
			startRow = (baseRobot.mc.edgeXMin+1)/MapCacheSystem.MAP_BLOCK_SIZE;
			if(baseRobot.mc.edgeXMax!=0) {
				numRowBlocks = baseRobot.mc.edgeXMax/MapCacheSystem.MAP_BLOCK_SIZE-(baseRobot.mc.edgeXMin+1)/MapCacheSystem.MAP_BLOCK_SIZE+1;
			} else {
				numRowBlocks = 16;
			}
		} else if(baseRobot.mc.edgeXMax!=0) {
			numRowBlocks = 16;
			startRow = baseRobot.mc.edgeXMax/MapCacheSystem.MAP_BLOCK_SIZE-numRowBlocks+1;
		} else {
			startRow = 0;
			numRowBlocks = 64;
		}
		int startCol;
		int numColBlocks;
		if(baseRobot.mc.edgeYMin!=0) {
			startCol = (baseRobot.mc.edgeYMin+1)/MapCacheSystem.MAP_BLOCK_SIZE;
			if(baseRobot.mc.edgeYMax!=0) {
				numColBlocks = baseRobot.mc.edgeYMax/MapCacheSystem.MAP_BLOCK_SIZE-(baseRobot.mc.edgeYMin+1)/MapCacheSystem.MAP_BLOCK_SIZE+1;
			} else {
				numColBlocks = 16;
			}
		} else if(baseRobot.mc.edgeYMax!=0) {
			numColBlocks = 16;
			startCol = baseRobot.mc.edgeYMax/MapCacheSystem.MAP_BLOCK_SIZE-numColBlocks+1;
		} else {
			startCol = 0;
			numColBlocks = 64;
		}
		int xb = startCol + (Clock.getRoundNum()/6 % numColBlocks);
		
		int[] buffer = new int[256];
		int c=0;
		for(int yb=startRow; yb<startRow+numRowBlocks; yb++) {
			int data = baseRobot.mc.packedSensed[xb][yb];
			if(data % 65536 == 0) continue;
			buffer[c++] = baseRobot.mc.packedIsWall[xb][yb];
			buffer[c++] = data;
		}
		if(c>0) {
			int[] ints = new int[c];
			System.arraycopy(buffer, 0, ints, 0, c);
			baseRobot.io.sendUInts(BroadcastChannel.EXPLORERS, BroadcastType.MAP_FRAGMENTS, ints);
		}
	}
	/** Broadcasts robot's knowledge of the four map edges. */
	public void broadcastMapEdges() {
		int[] edges = new int[] {
				baseRobot.mc.edgeXMin, 
				baseRobot.mc.edgeXMax,
				baseRobot.mc.edgeYMin,
				baseRobot.mc.edgeYMax
		};
		baseRobot.io.sendUShorts(BroadcastChannel.ALL, BroadcastType.MAP_EDGES, edges);
	}
	/** Broadcasts data about one node in the power node graph and its neighbors. */
	public void broadcastPowerNodeFragment() {
		PowerNodeGraph png = baseRobot.mc.powerNodeGraph;
		int id = (Clock.getRoundNum()/6 % (png.nodeCount-1)) + 2;
		if(!png.nodeSensed[id]) return;
		int degree = png.degreeCount[id];
		int[] ints = new int[degree+2];
		if(png.enemyPowerCoreID!=0) {
			MapLocation loc = png.nodeLocations[png.enemyPowerCoreID];
			ints[0] = (loc.x << 15) + loc.y;
		} else {
			ints[0] = 32001;
		}
		ints[1] = (png.nodeLocations[id].x << 15) + png.nodeLocations[id].y;
		for(int i=0; i<degree; i++) {
			int neighborID = png.adjacencyList[id][i];
			ints[i+2] = (png.nodeLocations[neighborID].x << 15) + png.nodeLocations[neighborID].y;
		}
		baseRobot.io.sendUInts(BroadcastChannel.EXPLORERS, BroadcastType.POWERNODE_FRAGMENTS, ints);
	}
	
	/** Receive data equivalent to one broadcast of a map fragment. */
	public void receiveMapFragment(int[] data) {
		for(int i=0; i<data.length; i+=2) {
			baseRobot.mc.integrateTerrainInfo(data[i], data[i+1]);
		}
	}
	/** Receive data equivalent to one broadcast of the four map edges. */
	public void receiveMapEdges(int[] data) {
		if(baseRobot.mc.edgeXMin==0) baseRobot.mc.edgeXMin = data[0];
		if(baseRobot.mc.edgeXMax==0) baseRobot.mc.edgeXMax = data[1];
		if(baseRobot.mc.edgeYMin==0) baseRobot.mc.edgeYMin = data[2];
		if(baseRobot.mc.edgeYMax==0) baseRobot.mc.edgeYMax = data[3];
	}
	/** Receive data equivalent to one broadcast of a power node fragment. */
	public void receivePowerNodeFragment(int[] data) {
		baseRobot.mc.integratePowerNodes(data);
	}
}
