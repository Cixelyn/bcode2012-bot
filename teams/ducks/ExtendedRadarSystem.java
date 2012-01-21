package ducks;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class ExtendedRadarSystem {
	private static final int BUFFER_SIZE = 4096;
	
	private final BaseRobot br;
	public final MapLocation[] locationInfo;
	public final int[] energonInfo;
	private final FastIDSet keySet;
	public ExtendedRadarSystem(BaseRobot br) {
		this.br = br;
		locationInfo = new MapLocation[BUFFER_SIZE];
		energonInfo = new int[BUFFER_SIZE];
		keySet = new FastIDSet(5);
	}
	
	/** Ally broadcasted some enemy info to us, integrate it into the system. <br>
	 * Should only be called from processMessage().
	 */
	public void integrateEnemyInfo(int[] info) {
		for(int n=0; n<info.length; n+=4) {
			int id = info[n] % BUFFER_SIZE;
			locationInfo[id] = new MapLocation(info[n+1], info[n+2]);
			energonInfo[id] = info[n+3];
			
			keySet.addID(id);
		}
	}
	
	/** Ally reported that they killed an enemy, remove it from the system. <br>
	 * Should only be called from processMessage().
	 */
	public void integrateEnemyKill(int killID) {
		keySet.removeID(killID % BUFFER_SIZE);
	}
	
	/** Gets info about a kill into your own and nearby robots' extended radar. */
	public void broadcastKill(int killID) {
		br.er.integrateEnemyKill(killID);
		br.io.sendUShort(BroadcastChannel.EXTENDED_RADAR, BroadcastType.ENEMY_KILL, killID);
	}
	
	/** Adds block of enemy info from this turn and removes block that timed out. */
	public void step() {
		keySet.endRound();
	}
	
	/** Returns the location of this closest enemy. */
	public MapLocation getClosestEnemyLocation() {
		int size = keySet.size();
		MapLocation ret = null;
		int bestDist = Integer.MAX_VALUE;
		for(int i=0; i<size; i++) {
			int id = keySet.getID(i);
			int dist = br.curLoc.distanceSquaredTo(locationInfo[id]);
			if(dist<bestDist) {
				ret = locationInfo[id];
				bestDist = dist;
			}
		}
		return ret;
	}
	
	/** Returns a direction to go that will most likely bring you to an enemy. */
	public Direction getDirectionWithMostEnemies() {
		int[] counts = new int[8];
		int size = keySet.size();
		for(int i=0; i<size; i++) {
			int id = keySet.getID(i);
			int dirOrdinal = br.curLoc.directionTo(locationInfo[id]).ordinal();
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
