package veryharderai;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class ExtendedRadarSystem {
	private static final int BUFFER_SIZE = 4096;
	private static final int MEMORY_TIMEOUT = 15;
	private static final RobotType[] robotTypes = RobotType.values();
	
	private final BaseRobot br;
	public final MapLocation[] enemyLocationInfo;
	public final int[] enemyEnergonInfo;
	public final RobotType[] enemyTypeInfo;
	private final FastIDSet enemyKeySet;
	public final MapLocation[] allyLocationInfo;
	public final int[] allyEnergonInfo;
	public final RobotType[] allyTypeInfo;
	private final FastIDSet allyKeySet;
	private final int[] flags;
	private int flagCount;
	public ExtendedRadarSystem(BaseRobot br) {
		this.br = br;
		enemyLocationInfo = new MapLocation[BUFFER_SIZE];
		enemyEnergonInfo = new int[BUFFER_SIZE];
		enemyTypeInfo = new RobotType[BUFFER_SIZE];
		enemyKeySet = new FastIDSet(MEMORY_TIMEOUT);
		allyLocationInfo = new MapLocation[BUFFER_SIZE];
		allyEnergonInfo = new int[BUFFER_SIZE];
		allyTypeInfo = new RobotType[BUFFER_SIZE];
		allyKeySet = new FastIDSet(MEMORY_TIMEOUT);
		flags = new int[BUFFER_SIZE];
		flagCount = 0;
	}
	
	/** Ally broadcasted some enemy info to us, integrate it into the system. <br>
	 * Should only be called from processMessage().
	 */
	public void integrateEnemyInfo(int[] info) {
		boolean firstIDIsAnAlly = info[3]>9000; // lol
		
		if(firstIDIsAnAlly) {
			int senderID = info[0];
			allyLocationInfo[senderID] = new MapLocation(info[1], info[2]);
			allyEnergonInfo[senderID] = info[3]-10001;
			allyTypeInfo[senderID] = robotTypes[info[4]];
			allyKeySet.addID(senderID);
		}
		for(int n=firstIDIsAnAlly?5:0; n<info.length; n+=5) {
			int id = info[n];
			enemyLocationInfo[id] = new MapLocation(info[n+1], info[n+2]);
			enemyEnergonInfo[id] = info[n+3];
			enemyTypeInfo[id] = robotTypes[info[n+4]];
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
		
		int i; // hardcoded to bring i into istore_3
		
		flagCount++;
		int diff = 0;
		
		// Subtract enemy energon
		for(i=enemyKeySet.size(); --i>=0;) {
			int id = enemyKeySet.getID(i);
			if(center.distanceSquaredTo(enemyLocationInfo[id]) <= radiusSquared) 
				diff -= enemyEnergonInfo[id];
		}
		
		// Subtract enemy energon from robots in the local radar but not in the ER
		for(i=br.radar.numEnemyRobots; --i>=0;) {
			int id = br.radar.enemyRobots[i];
			if(flags[id]==flagCount) continue;
			RobotInfo ri = br.radar.enemyInfos[id];
			
			if(ri.type == RobotType.SOLDIER || ri.type == RobotType.DISRUPTER || ri.type == RobotType.SCORCHER) {
				if(center.distanceSquaredTo(ri.location) <= radiusSquared) {
					diff -= ri.energon;
				}
			}
		}
		
		// Add ally energon
		for(i=allyKeySet.size(); --i>=0;) {
			int id = allyKeySet.getID(i);
			if(center.distanceSquaredTo(allyLocationInfo[id]) <= radiusSquared) {
				flags[id] = flagCount;
				diff += allyEnergonInfo[id];
			}
		}
		
		// Add ally energon from robots in the local radar but not in the ER
		for(i=br.radar.numAllyRobots; --i>=0;) {
			int id = br.radar.allyRobots[i];
			if(flags[id]==flagCount) continue;
			RobotInfo ri = br.radar.allyInfos[id];
			
			if(ri.type == RobotType.SOLDIER || ri.type == RobotType.DISRUPTER || ri.type == RobotType.SCORCHER) {
				if(center.distanceSquaredTo(ri.location) <= radiusSquared) {
					diff += ri.energon;
				}
			}
		}
		
		// Add myself if necessary
		if(flags[br.myID]!=flagCount && center.distanceSquaredTo(br.curLoc) <= radiusSquared) {
			diff += br.curEnergon;
		}
		
//		br.dbg.setIndicatorString('h', 0, toString()+" ----- energon difference: "+diff);
		return diff;
	}
	
	/** Returns the id of this closest enemy, or -1 if there are no enemies. */
	public int getClosestEnemyID() {
		int ret = -1;
		int bestDist = Integer.MAX_VALUE;
		for(int i=enemyKeySet.size(); --i>=0;) {
			int id = enemyKeySet.getID(i);
			int dist = br.curLoc.distanceSquaredTo(enemyLocationInfo[id]);
			if(dist<bestDist) {
				ret = id;
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
	 * Counts the number of enemies in each direction in our extended radar,
	 * Also adds them to the two neighboring directions
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
	
	/**
	 * Counts the number of enemies in each direction in our extended radar
	 */
	public int[] getEnemiesInEachDirectionOnly() {
		int[] counts = new int[8];
		int size = enemyKeySet.size();
		for(int i=0; i<size; i++) {
			int id = enemyKeySet.getID(i);
			int dirOrdinal = br.curLoc.directionTo(enemyLocationInfo[id]).ordinal();
			if(dirOrdinal<8) {
				counts[dirOrdinal]++;
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
			ret+=" id="+id+", pos=<"+(enemyLocationInfo[id].x-br.curLoc.x)+","+(enemyLocationInfo[id].y-br.curLoc.y)+">, hp="+enemyEnergonInfo[id]+"     ";
		}
		size = allyKeySet.size();
		for(int i=0; i<size; i++) {
			int id = allyKeySet.getID(i);
			ret+=" (ally)id="+id+", pos=<"+(allyLocationInfo[id].x-br.curLoc.x)+","+(allyLocationInfo[id].y-br.curLoc.y)+">, hp="+allyEnergonInfo[id]+"     ";
		}
		return ret;
	}
}
