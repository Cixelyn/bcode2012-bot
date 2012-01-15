package ducks;

import battlecode.common.MapLocation;
import battlecode.common.PowerNode;

/** This is a sub data structure in the map cache that dynamically stores information 
 * about the power nodes a robot has explored. 
 * Probably only archons and scouts will store this information, and only archons will use it.
 * 
 * Whenever we sense a new power node, we add it and its new neighbors into nodeLocations,
 * and add its connections into an adjacency list.
 */
public class PowerNodeGraph {
	/** The locations of the power nodes we know of. */
	final MapLocation[] nodeLocations;
	/** How many nodes we have heard of the whereabouts of. */
	int nodeCount;
	/** How many nodes we have sensed, either directly or via shared exploration. 
	 * This number can be at most nodeCount. 
	 */
	int nodeSensedCount;
	/** a[i][j] is the index of an edge that node i is connected to. */
	final int[][] adjacencyList;
	/** a[i] is how many edges are currently stored in the adjacency list of node i. 
	 * This is either 0, if we have not managed to sense the power node yet, 
	 * or the accurate number, if we have sensed the power node either directly or via shared exploration
	 */
	final int[] degreeCount;
	
	public PowerNodeGraph() {
		nodeLocations = new MapLocation[50];
		adjacencyList = new int[50][50];
		degreeCount = new int[50];
	}
	
}
