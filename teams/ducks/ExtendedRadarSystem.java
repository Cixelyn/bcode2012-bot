package ducks;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class ExtendedRadarSystem {
	private static final int BUFFER_SIZE = 4096;
	
	private final BaseRobot br;
	public final MapLocation[] locationInfo;
	public final int[] energonInfo;
	public ExtendedRadarSystem(BaseRobot br) {
		this.br = br;
		locationInfo = new MapLocation[BUFFER_SIZE];
		energonInfo = new int[BUFFER_SIZE];
	}
	
	/** Ally broadcasted some enemy info to us, integrate it into the system. <br>
	 * Should only be called from processMessage().
	 */
	public void integrateEnemyInfo(int[] info) {
		for(int n=0; n<info.length; n+=4) {
			int id = info[n] % BUFFER_SIZE;
			locationInfo[id] = new MapLocation(info[n+1], info[n+2]);
			energonInfo[id] = info[n+3];
			
			// TODO add ID to ID_SET
		}
	}
	
	/** Ally reported that they killed an enemy, remove it from the system. <br>
	 * Should only be called from processMessage().
	 */
	public void integrateEnemyKill(int killID) {
		int id = killID % BUFFER_SIZE;
			
		// TODO if ID is in ID_SET, remove ID from ID_SET
	}
	
	/** Gets info about a kill into your own and nearby robots' extended radar. */
	public void broadcastKill(int killID) {
		br.er.integrateEnemyKill(killID);
		br.io.sendUShort(BroadcastChannel.EXTENDED_RADAR, BroadcastType.ENEMY_KILL, killID);
	}
	
	/** Adds block of enemy info from this turn and removes block that timed out. */
	public void step() {
		// TODO do shit in the ID_SET
	}
	
	/** Returns the location of this closest enemy. */
	public MapLocation getClosestEnemy() {
		
		return null;
	}
	
	/** Returns a direction to go that will most likely bring you to an enemy. */
	public Direction getDirectionWithMostEnemies() {
		
		return null;
	}
	
	/** Returns the average of the directions of the enemies. 
	 * May be useful for approximating a retreat direction. 
	 */
	public Direction getAverageEnemyDirection() {
		
		return null;
	}
}
