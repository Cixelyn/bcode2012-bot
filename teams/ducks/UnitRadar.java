package ducks;

import battlecode.common.*;

/**
 * The goal of this class is to cache data from
 * the expensive calls to senseNearbyGameObjects (100 cost)
 * and senseRobotInfo (25 per robot)
 * 
 * The eventual hope is that we can implement timeouts,
 * so that recent data does not need to be rescanned
 * and instead is still available for computation for free
 *
 */
public class UnitRadar {
	
	
	BaseRobot br;
	
	public UnitRadar(BaseRobot br) {
		this.br = br;
	}
	
	public final static int MAX_ROBOTS = 1024;
	public final static int MAX_ENEMY_ROBOTS = 50;
	public final static int MAX_ADJACENT = 17;

	public final RobotInfo[] allyInfos = new RobotInfo[MAX_ROBOTS];
	public final int[] allyTimes = new int[MAX_ROBOTS];
	
	public int numAdjacentAllies;
	public final RobotInfo[] adjacentAllies = new RobotInfo[MAX_ADJACENT];
	
	public final RobotInfo[] enemyInfos = new RobotInfo[MAX_ROBOTS];
	public final int[] enemyTimes = new int[MAX_ROBOTS];
	public final int[] enemyArchons = new int[6];
	public final int[] enemySoldiers = new int[MAX_ENEMY_ROBOTS];
	public final int[] enemyScouts = new int[MAX_ENEMY_ROBOTS];
	public final int[] enemyDisruptors = new int[MAX_ENEMY_ROBOTS];
	public final int[] enemyScorchers = new int[MAX_ENEMY_ROBOTS];
	public int numEnemyArchons;
	public int numEnemySoldiers;
	public int numEnemyScouts;
	public int numEnemyDisruptors;
	public int numEnemyScorchers;
	
	
	
	public MapLocation closestEnemyLoc;
	public double closestEnemyDist;
	
	private void resetEnemyStats() {
		closestEnemyLoc = null;
		closestEnemyDist = 999;
		numEnemyArchons = 0;
		numEnemySoldiers = 0;
		numEnemyScouts = 0;
		numEnemyDisruptors = 0;
		numEnemyScorchers = 0;
	}
	
	private void resetAllyStats() {
		numAdjacentAllies = 0;
//		adjacentAllies = new RobotInfo[MAX_ADJACENT];
	}
	
	
	private void addEnemy(RobotInfo rinfo) {
		int pos = rinfo.robot.getID() % MAX_ROBOTS;
		enemyInfos[pos] = rinfo;
		enemyTimes[pos] = Clock.getRoundNum();
		
		switch (rinfo.type)
		{
		case ARCHON: 		enemyArchons[numEnemyArchons++] = pos; break;
		case DISRUPTER: 	enemyDisruptors[numEnemyDisruptors++] = pos; break;
		case SCORCHER: 		enemyScorchers[numEnemyScorchers++] = pos; break;
		case SCOUT: 		enemyScouts[numEnemyScouts++] = pos; break;
		case SOLDIER: 		enemySoldiers[numEnemySoldiers++] = pos; break;
		}
		
		// Distance Stats
		int dist = rinfo.location.distanceSquaredTo(br.currLoc);
		if(dist < closestEnemyDist) {
			closestEnemyLoc = rinfo.location;
			closestEnemyDist = dist;
		}
	}
	
	private void addAlly(RobotInfo rinfo) {
		int pos = rinfo.robot.getID() % MAX_ROBOTS;
		allyInfos[pos] = rinfo;
		allyTimes[pos] = Clock.getRoundNum();
		
		if(rinfo.location.isAdjacentTo(br.currLoc)) {
			adjacentAllies[numAdjacentAllies++] = rinfo;
		}
	}
	
	
	/**
	 * Call scan to populate radar information. Ally and Enemy information is
	 * guarenteed only to be correct if scanAllies and/or scanEnemies is set to true.
	 * @param scanAllies - enable ally data collection and scanning
	 * @param scanEnemies - enable enemy data collection and scanning
	 */
	public void scan(boolean scanAllies, boolean scanEnemies) {
		Robot[] robots = br.rc.senseNearbyGameObjects(Robot.class);
		
		//reset stat collection
		if(scanAllies) resetEnemyStats();
		if(scanEnemies) resetAllyStats();
		
		for(Robot r : robots) {
			try{
				if (br.myTeam == r.getTeam() ) {
					if(scanAllies) {
						addAlly(br.rc.senseRobotInfo(r));
					}
				} else {
					if(scanEnemies) {
						addEnemy(br.rc.senseRobotInfo(r));
					}
				}
				
			} catch( GameActionException e) {
				br.rc.addMatchObservation(e.toString());
				e.printStackTrace();
			}
		}
	}
}
