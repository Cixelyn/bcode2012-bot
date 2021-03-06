package ducksold1;

import battlecode.common.Clock;

public class SharedExplorationSystem {
	final BaseRobot baseRobot;
	public SharedExplorationSystem(BaseRobot baseRobot) {
		this.baseRobot = baseRobot;
	}
	
	public void broadcastMapFragment() {
		int startRow;
		int numRowBlocks;
		if(baseRobot.mc.edgeXMin!=0) {
			startRow = (baseRobot.mc.edgeXMin+1)/MapCache.MAP_BLOCK_SIZE;
			if(baseRobot.mc.edgeXMax!=0) {
				numRowBlocks = baseRobot.mc.edgeXMax/MapCache.MAP_BLOCK_SIZE-(baseRobot.mc.edgeXMin+1)/MapCache.MAP_BLOCK_SIZE+1;
			} else {
				numRowBlocks = 16;
			}
		} else if(baseRobot.mc.edgeXMax!=0) {
			numRowBlocks = 16;
			startRow = baseRobot.mc.edgeXMax/MapCache.MAP_BLOCK_SIZE-numRowBlocks+1;
		} else {
			startRow = 0;
			numRowBlocks = 64;
		}
		int startCol;
		int numColBlocks;
		if(baseRobot.mc.edgeYMin!=0) {
			startCol = (baseRobot.mc.edgeYMin+1)/MapCache.MAP_BLOCK_SIZE;
			if(baseRobot.mc.edgeYMax!=0) {
				numColBlocks = baseRobot.mc.edgeYMax/MapCache.MAP_BLOCK_SIZE-(baseRobot.mc.edgeYMin+1)/MapCache.MAP_BLOCK_SIZE+1;
			} else {
				numColBlocks = 16;
			}
		} else if(baseRobot.mc.edgeYMax!=0) {
			numColBlocks = 16;
			startCol = baseRobot.mc.edgeYMax/MapCache.MAP_BLOCK_SIZE-numColBlocks+1;
		} else {
			startCol = 0;
			numColBlocks = 64;
		}
		int xb = startCol + (Clock.getRoundNum() % numColBlocks);
		
		int[] buffer = new int[32];
		int c=0;
		for(int yb=startRow; yb<startRow+numRowBlocks; yb++) {
			int data = baseRobot.mc.packedSensed[xb][yb];
			if(data % 65536 == 0) continue;
			buffer[c++] = baseRobot.mc.packedIsWall[xb][yb];
			buffer[c++] = data;
		}
		int[] ints = new int[c];
		System.arraycopy(buffer, 0, ints, 0, c);
		baseRobot.io.sendInts("#em", ints);
	}
	public void broadcastMapEdges() {
		int[] edges = new int[] {
				baseRobot.mc.edgeXMin, 
				baseRobot.mc.edgeXMax,
				baseRobot.mc.edgeYMin,
				baseRobot.mc.edgeYMax
		};
		baseRobot.io.sendShorts("#ee", edges);
	}
	public void broadcastPowerNodeGraph() {
		PowerNodeGraph png = baseRobot.mc.powerNodeGraph;
		int id = Clock.getRoundNum() % (png.nodeCount) + 1;
		if(!png.nodeSensed[id]) return;
		int degree = png.degreeCount[id];
		int[] ints = new int[degree+1];
		ints[0] = (png.nodeLocations[id].x << 15) + png.nodeLocations[id].y;
		for(int i=0; i<degree; i++) {
			int neighborID = png.adjacencyList[id][i];
			ints[i+1] = (png.nodeLocations[neighborID].x << 15) + png.nodeLocations[neighborID].y;
		}
		baseRobot.io.sendInts("#ep", ints);
	}
	
	public void receiveMapFragment(int[] data) {
		for(int i=0; i<data.length; i+=2) {
			baseRobot.mc.integrateTerrainInfo(data[i], data[i+1]);
		}
	}
	public void receiveMapEdges(int[] data) {
		if(baseRobot.mc.edgeXMin==0) baseRobot.mc.edgeXMin = data[0];
		if(baseRobot.mc.edgeXMax==0) baseRobot.mc.edgeXMax = data[0];
		if(baseRobot.mc.edgeYMin==0) baseRobot.mc.edgeXMin = data[0];
		if(baseRobot.mc.edgeYMax==0) baseRobot.mc.edgeYMax = data[0];
	}
	public void receivePowerNodeGraph(int[] data) {
		baseRobot.mc.integratePowerNodes(data);
	}
}
