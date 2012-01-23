package ducks;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class ExtendedRadarSystem {
	private static final int BUFFER_SIZE = 4096;
	private static final int MEMORY_TIMEOUT = 15;
	
	private final BaseRobot br;
	private final MapLocation[] enemyLocationInfo;
	private final int[] enemyEnergonInfo;
	private final FastIDSet enemyKeySet;
	private final MapLocation[] allyLocationInfo;
	private final int[] allyEnergonInfo;
	private final FastIDSet allyKeySet;
	private final int[] flags;
	private int flagCount;
	public ExtendedRadarSystem(BaseRobot br) {
		this.br = br;
		enemyLocationInfo = new MapLocation[BUFFER_SIZE];
		enemyEnergonInfo = new int[BUFFER_SIZE];
		enemyKeySet = new FastIDSet(MEMORY_TIMEOUT);
		allyLocationInfo = new MapLocation[BUFFER_SIZE];
		allyEnergonInfo = new int[BUFFER_SIZE];
		allyKeySet = new FastIDSet(MEMORY_TIMEOUT);
		flags = new int[BUFFER_SIZE];
		flagCount = 0;
	}
	
	/** Ally broadcasted some enemy info to us, integrate it into the system. <br>
	 * Should only be called from processMessage().
	 */
	public void integrateEnemyInfo(int[] info) {
		boolean firstIDIsAnAlly = info[3]>10000;
		
		if(firstIDIsAnAlly) {
			int senderID = info[0];
			allyLocationInfo[senderID] = new MapLocation(info[1], info[2]);
			allyEnergonInfo[senderID] = info[3]-10001;
			allyKeySet.addID(senderID);
		}
		for(int n=firstIDIsAnAlly?4:0; n<info.length; n+=4) {
			int id = info[n];
			enemyLocationInfo[id] = new MapLocation(info[n+1], info[n+2]);
			enemyEnergonInfo[id] = info[n+3];
			enemyKeySet.addID(id);
		}
	}
	
	/** Ally reported that they killed an enemy, remove it from the system. <br>
	 * Should only be called from processMessage().
	 */
	public void integrateEnemyKill(int killID) {
		enemyKeySet.removeID(killID);
	}
	
	/** Gets info about a kill into your own and nearby robots' extended radar. */
	public void broadcastKill(int killID) {
		br.er.integrateEnemyKill(killID);
		br.io.sendUShort(BroadcastChannel.EXTENDED_RADAR, BroadcastType.ENEMY_KILL, killID);
	}
	
	/** Adds block of enemy info from this turn and removes block that timed out. */
	public void step() {
		allyKeySet.endRound();
		enemyKeySet.endRound();
	}
	
	/** Returns how much more energon worth of robots we have in the given radius. 
	 * Returns a positive number iff we have more energon than the other team.
	 */
	public int getEnergonDifference(MapLocation center, int radiusSquared) {
		flagCount++;
		int diff = 0;
		
		String indicatorString = "";
		
		// Subtract enemy energon
		int size = enemyKeySet.size();
		for(int i=0; i<size; i++) {
			int id = enemyKeySet.getID(i);
			if(center.distanceSquaredTo(enemyLocationInfo[id]) <= radiusSquared) 
				{diff -= enemyEnergonInfo[id]; indicatorString+=" e-"+id+"-"+diff;}
		}
		
		// Add ally energon
		size = allyKeySet.size();
		for(int i=0; i<size; i++) {
			int id = allyKeySet.getID(i);
			if(center.distanceSquaredTo(allyLocationInfo[id]) <= radiusSquared) {
				flags[id] = flagCount;
				diff += allyEnergonInfo[id];
				indicatorString+=" a-"+id+"-"+diff;
			}
		}
		
		// Add ally energon from robots in the local radar but not in the ER
		for(int i=0; i<br.radar.numAllyRobots; i++) {
			int id = br.radar.allyRobots[i];
			if(flags[id]==flagCount) continue;
			RobotInfo ri = br.radar.allyInfos[id];
			if(center.distanceSquaredTo(ri.location) <= radiusSquared) {
				diff += ri.energon;
				indicatorString+=" b-"+id+"-"+diff;
			}
		}
		
		// Add myself if necessary
		if(flags[br.myID]!=flagCount && center.distanceSquaredTo(br.curLoc) <= radiusSquared) {
			diff += br.curEnergon;
		}
		
		br.dbg.setIndicatorString('h', 0, toString()+" ----- energon difference: "+diff+" "+indicatorString);
		return diff;
	}
	
	/** Returns the location of this closest enemy. */
	public MapLocation getClosestEnemyLocation() {
		int size = enemyKeySet.size();
		MapLocation ret = null;
		int bestDist = Integer.MAX_VALUE;
		for(int i=0; i<size; i++) {
			int id = enemyKeySet.getID(i);
			int dist = br.curLoc.distanceSquaredTo(enemyLocationInfo[id]);
			if(dist<bestDist) {
				ret = enemyLocationInfo[id];
				bestDist = dist;
			}
		}
		return ret;
	}
	
	/** Returns a direction to go that will most likely bring you to an enemy. */
	public Direction getDirectionWithMostEnemies() {
		int[] counts = getEnemiesInEachDirection();
		int bestdirOrdinal = -1;
		int bestValue = -1;
		for(int i=0; i<8; i++) {
			if(counts[i]>bestValue) {
				bestdirOrdinal = i;
				bestValue = counts[i];
			}
		}
		return Constants.directions[bestdirOrdinal];
	}
	
	/**
	 * Counts the number of enemies in each direction in our extended radar
	 */
	public int[] getEnemiesInEachDirection() {
		int[] counts = new int[8];
		int size = enemyKeySet.size();
		for(int i=0; i<size; i++) {
			int id = enemyKeySet.getID(i);
			int dirOrdinal = br.curLoc.directionTo(enemyLocationInfo[id]).ordinal();
			if(dirOrdinal<8) {
				counts[(dirOrdinal+1)%8]++;
				counts[dirOrdinal]+=2;
				counts[(dirOrdinal+7)%8]++;
			}
		}
		return counts;
	}
	
	/** Returns the average of the directions of the enemies. 
	 * May be useful for approximating a retreat direction. 
	 */
	public Direction getAverageEnemyDirection() {
		throw new RuntimeException("ExtendedRadarSystem.getAverageEnemyDirection() not yet implemented!");
	}
	
	@Override
	public String toString() {
		String ret = "";
		int size = enemyKeySet.size();
		for(int i=0; i<size; i++) {
			int id = enemyKeySet.getID(i);
			ret+="id="+id+",pos=<"+(enemyLocationInfo[id].x-br.curLoc.x)+","+(enemyLocationInfo[id].y-br.curLoc.y)+">,hp="+enemyEnergonInfo[id]+"     ";
		}
		size = allyKeySet.size();
		for(int i=0; i<size; i++) {
			int id = allyKeySet.getID(i);
			ret+="(ally)id="+id+",pos=<"+(allyLocationInfo[id].x-br.curLoc.x)+","+(allyLocationInfo[id].y-br.curLoc.y)+">,hp="+allyEnergonInfo[id]+"     ";
		}
		return ret;
	}
}
