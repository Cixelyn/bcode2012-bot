package ducks;

import battlecode.common.*;

/**
 * The goal of this class is to cache data from the expensive calls to
 * senseNearbyGameObjects (100 cost) and senseRobotInfo (25 per robot)
 * 
 * The eventual hope is that we can implement timeouts, so that recent data does
 * not need to be rescanned and instead is still available for computation for
 * free
 * 
 */
public class RadarSystem {

	BaseRobot br;

	public RadarSystem(BaseRobot br) {
		this.br = br;
		lastscanround = -1;
	}

	public final static int MAX_ROBOTS = 1024;
	public final static int MAX_ENEMY_ROBOTS = 50;
	public final static int MAX_ADJACENT = 17;

	public final RobotInfo[] allyInfos = new RobotInfo[MAX_ROBOTS];
	public final int[] allyTimes = new int[MAX_ROBOTS];

	public int numAdjacentAllies;
	public int numAllyRobots;
	public int numAllyDamaged;
	public final RobotInfo[] adjacentAllies = new RobotInfo[MAX_ADJACENT];

	public final RobotInfo[] enemyInfos = new RobotInfo[MAX_ROBOTS];
	public final int[] enemyTimes = new int[MAX_ROBOTS];
	public final int[] enemyRobots = new int[MAX_ENEMY_ROBOTS];
	public final int[] enemyArchons = new int[6];
	public final int[] enemySoldiers = new int[MAX_ENEMY_ROBOTS];
	public final int[] enemyScouts = new int[MAX_ENEMY_ROBOTS];
	public final int[] enemyDisruptors = new int[MAX_ENEMY_ROBOTS];
	public final int[] enemyScorchers = new int[MAX_ENEMY_ROBOTS];
	public final int[] enemyTowers = new int[MAX_ENEMY_ROBOTS];
	public int numEnemyRobots;
	public int numEnemyArchons;
	public int numEnemySoldiers;
	public int numEnemyScouts;
	public int numEnemyDisruptors;
	public int numEnemyScorchers;
	public int numEnemyTowers;

	public int vecEnemyX;
	public int vecEnemyY;

	public int centerEnemyX;
	public int centerEnemyY;

	public int roundsSinceEnemySighted;

	public int lastscanround;

	public RobotInfo closestEnemy;
	public double closestEnemyDist;

	private void resetEnemyStats() {
		closestEnemy = null;
		closestEnemyDist = 999;
		numEnemyRobots = 0;
		numEnemyArchons = 0;
		numEnemySoldiers = 0;
		numEnemyScouts = 0;
		numEnemyDisruptors = 0;
		numEnemyScorchers = 0;
		numEnemyTowers = 0;

		vecEnemyX = 0;
		vecEnemyY = 0;
		centerEnemyX = 0;
		centerEnemyY = 0;
	}

	private void resetAllyStats() {
		numAdjacentAllies = 0;
		numAllyRobots = 0;
		numAllyDamaged = 0;
	}

	private void addEnemy(RobotInfo rinfo) throws GameActionException {
		if(rinfo.type==RobotType.TOWER && !br.dc.isTowerTargetable(rinfo))
			return;
		
		int pos = rinfo.robot.getID() % MAX_ROBOTS;
		enemyInfos[pos] = rinfo;
		enemyTimes[pos] = Clock.getRoundNum();

		// TODO not caching this right now
		// if this was cached, would double cost of scan loop
		// enemyRobots[numEnemyRobots] = pos;
		numEnemyRobots++;

		switch (rinfo.type) {
		case ARCHON:
			enemyArchons[numEnemyArchons++] = pos;
			break;
		case DISRUPTER:
			enemyDisruptors[numEnemyDisruptors++] = pos;
			break;
		case SCORCHER:
			enemyScorchers[numEnemyScorchers++] = pos;
			break;
		case SCOUT:
			enemyScouts[numEnemyScouts++] = pos;
			break;
		case SOLDIER:
			enemySoldiers[numEnemySoldiers++] = pos;
			break;
		case TOWER:
			enemyTowers[numEnemyTowers++] = pos;
			break;
		}

		// Distance Stats
		MapLocation eloc = rinfo.location;

		centerEnemyX += eloc.x;
		centerEnemyY += eloc.y;

		int dist = eloc.distanceSquaredTo(br.curLoc);
		if (dist < closestEnemyDist) {
			closestEnemy = rinfo;
			closestEnemyDist = dist;
		}
	}

	private void addAlly(RobotInfo rinfo) {
		int pos = rinfo.robot.getID() % MAX_ROBOTS;
		allyInfos[pos] = rinfo;
		allyTimes[pos] = Clock.getRoundNum();

		numAllyRobots++;

		if (rinfo.energon != rinfo.type.maxEnergon) {
			numAllyDamaged++;
		}

		if (rinfo.location.isAdjacentTo(br.curLoc)) {
			adjacentAllies[numAdjacentAllies++] = rinfo;
		}
	}

	/**
	 * Call scan to populate radar information. Ally and Enemy information is
	 * guaranteed only to be correct if scanAllies and/or scanEnemies is set to
	 * true.
	 * 
	 * @param scanAllies
	 *            - enable ally data collection and scanning
	 * @param scanEnemies
	 *            - enable enemy data collection and scanning
	 */
	public void scan(boolean scanAllies, boolean scanEnemies) {

		Robot[] robots = br.rc.senseNearbyGameObjects(Robot.class);

		// reset stat collection
		if (scanEnemies)
			resetEnemyStats();
		if (scanAllies)
			resetAllyStats();

		for (Robot r : robots) {
			try {
				if (br.myTeam == r.getTeam()) {
					if (scanAllies) {
						addAlly(br.rc.senseRobotInfo(r));
					}
				} else {
					if (scanEnemies) {
						addEnemy(br.rc.senseRobotInfo(r));
					}
				}

			} catch (GameActionException e) {
				br.rc.addMatchObservation(e.toString());
				e.printStackTrace();
			}
		}

		if (numEnemyRobots == 0) {
			centerEnemyX = centerEnemyY = -1;
			vecEnemyX = br.curLoc.x;
			vecEnemyY = br.curLoc.y;
		} else {
			centerEnemyX = centerEnemyX / numEnemyRobots;
			centerEnemyY = centerEnemyY / numEnemyRobots;
			vecEnemyX = centerEnemyX - br.curLoc.x;
			vecEnemyY = centerEnemyY - br.curLoc.y;
		}

		// compute some global statistics
		if (numEnemyRobots == 0) {
			roundsSinceEnemySighted++;
		} else {
			roundsSinceEnemySighted = 0;
		}

	}

	public int getArmyDifference() {
		return numAllyRobots - numEnemyRobots;
	}

	public MapLocation getEnemySwarmTarget() {
		double a = Math.sqrt(vecEnemyX * vecEnemyX + vecEnemyY * vecEnemyY) + .001;

		return new MapLocation((int) (vecEnemyX * 7 / a) + br.curLoc.x,
				(int) (vecEnemyY * 7 / a) + br.curLoc.y);

	}

	public MapLocation getEnemySwarmCenter() {
		return new MapLocation(centerEnemyX, centerEnemyY);
	}

}
