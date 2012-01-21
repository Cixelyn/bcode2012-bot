package ducks;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class ExtendedRadarSystem {
	private static final int BUFFER_SIZE = 4096;
	
	private final BaseRobot br;
	private final MapLocation[] enemyLocationInfo;
	private final int[] enemyEnergonInfo;
	private final FastIDSet enemyKeySet;
	private final MapLocation[] allyLocationInfo;
	private final int[] allyEnergonInfo;
	private final FastIDSet allyKeySet;
	public ExtendedRadarSystem(BaseRobot br) {
		this.br = br;
		enemyLocationInfo = new MapLocation[BUFFER_SIZE];
		enemyEnergonInfo = new int[BUFFER_SIZE];
		enemyKeySet = new FastIDSet(5);
		allyLocationInfo = new MapLocation[BUFFER_SIZE];
		allyEnergonInfo = new int[BUFFER_SIZE];
		allyKeySet = new FastIDSet(5);
	}
	
	/** Ally broadcasted some enemy info to us, integrate it into the system. <br>
	 * Should only be called from processMessage().
	 */
	public void integrateEnemyInfo(int[] info) {
		int senderID = info[0] % BUFFER_SIZE;
		allyLocationInfo[senderID] = new MapLocation(info[1], info[2]);
		allyEnergonInfo[senderID] = info[3];
		allyKeySet.addID(senderID);
		
		for(int n=4; n<info.length; n+=4) {
			int id = info[n] % BUFFER_SIZE;
			enemyLocationInfo[id] = new MapLocation(info[n+1], info[n+2]);
			enemyEnergonInfo[id] = info[n+3];
			
			enemyKeySet.addID(id);
		}
	}
	
	/** Ally reported that they killed an enemy, remove it from the system. <br>
	 * Should only be called from processMessage().
	 */
	public void integrateEnemyKill(int killID) {
		enemyKeySet.removeID(killID % BUFFER_SIZE);
	}
	
	/** Gets info about a kill into your own and nearby robots' extended radar. */
	public void broadcastKill(int killID) {
		br.er.integrateEnemyKill(killID);
		br.io.sendUShort(BroadcastChannel.EXTENDED_RADAR, BroadcastType.ENEMY_KILL, killID);
	}
	
	/** Adds block of enemy info from this turn and removes block that timed out. */
	public void step() {
		enemyKeySet.endRound();
	}
	
	/** Returns how much more energon worth of robots we have in the given radius. 
	 * Returns a positive number iff we have more energon than the other team.
	 */
	public int getEnergonDifference(int radiusSquared) {
		int diff = 0;
		int size = enemyKeySet.size();
		for(int i=0; i<size; i++) {
			int id = enemyKeySet.getID(i);
			if(br.curLoc.distanceSquaredTo(enemyLocationInfo[id]) <= radiusSquared) 
				diff -= enemyEnergonInfo[id];
		}
		size = allyKeySet.size();
		for(int i=0; i<size; i++) {
			int id = allyKeySet.getID(i);
			if(br.curLoc.distanceSquaredTo(allyLocationInfo[id]) <= radiusSquared) 
				diff += allyEnergonInfo[id];
		}
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
	
	/** Returns the average of the directions of the enemies. 
	 * May be useful for approximating a retreat direction. 
	 */
	public Direction getAverageEnemyDirection() {
		throw new RuntimeException("ExtendedRadarSystem.getAverageEnemyDirection() not yet implemented!");
	}
}
