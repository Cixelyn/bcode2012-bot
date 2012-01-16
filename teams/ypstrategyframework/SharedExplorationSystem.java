package ypstrategyframework;

public class SharedExplorationSystem {
	final BaseRobot baseRobot;
	public SharedExplorationSystem(BaseRobot baseRobot) {
		this.baseRobot = baseRobot;
	}
	
	public void broadcastMapFragment() {
		
	}
	public void broadcastMapEdges() {
		int[] edges = new int[] {
				baseRobot.mc.edgeXMin, 
				baseRobot.mc.edgeXMax,
				baseRobot.mc.edgeYMin,
				baseRobot.mc.edgeYMax
		};
		baseRobot.io.sendShorts("wtf goes here", edges);
	}
	public void broadcastPowerNodeGraph() {
		
	}
	
	public void receiveMapUpdates(int[] data) {
		//first 32 elements of data are 16 4x4 map blocks
		
		//then next 4 are edge data
		
		//next 1 is the number of powernodes about to be sent, N
		//then next N are a series of cache coordinates (256*x+y) for power nodes
		//then rest are edges between two powernodes, 
		//   packed as 256*i+j for an edge between nodes i and j in the above list
		
		//for 60x60 map with a C_50 powernode graph, this will be 32+4+1+50+50=137 ints
	}
}
