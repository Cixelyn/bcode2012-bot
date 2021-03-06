package brutalai;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

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

	public final static int MAX_ROBOTS = 4096;
	public final static int MAX_ENEMY_ROBOTS = 50;
	public final static int MAX_ADJACENT = 17;

	public final RobotInfo[] allyInfos = new RobotInfo[MAX_ROBOTS];
	public final int[] allyTimes = new int[MAX_ROBOTS];
	public final int[] allyRobots = new int[MAX_ROBOTS];

	public int numAdjacentAllies;
	public int numAllyRobots;
	public int numAllyToRegenerate;
	public final RobotInfo[] adjacentAllies = new RobotInfo[MAX_ADJACENT];
	public int alliesOnLeft;
	public int alliesOnRight;
	public int alliesInFront;

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
	public boolean needToScanEnemies;
	public boolean needToScanAllies;
	
	public Robot[] robots;
	
//	yp's variables for archon retreat code
	public int[] closestInDir;
	final static int[] blank_closestInDir = new int[] {99,99,99,99,99,99,99,99,99,99};
	public int[] allies_in_dir;
	
	public RobotInfo closestEnemy;
	public int closestEnemyDist;
	public RobotInfo closestEnemyWithFlux;
	public int closestEnemyWithFluxDist;
	
	public RobotInfo closestLowFluxAlly;
	public int closestLowFluxAllyDist;
	
	public RobotInfo closestAllyScout;
	public int closestAllyScoutDist;
	
	public RobotInfo lowestFluxAllied;
	public double lowestFlux;
	public RobotInfo lowestEnergonAllied;
	public double lowestEnergonRatio;
	
	final boolean cachepositions;
	final boolean isArchon;

	public RadarSystem(BaseRobot br) {
		this.
		br = br;
		lastscanround = -1;
		needToScanEnemies = true;
		needToScanAllies = true;
		robots = null;
		closestInDir = new int[10];
		switch (br.myType)
		{
		case SOLDIER:
		case SCORCHER:
		case DISRUPTER:
		case SCOUT:
			cachepositions = true;
			isArchon = false;
			break;
		case ARCHON:
			cachepositions = false;
			isArchon = true;
			break;
		default:
			cachepositions = false;
			isArchon = false;
		}
	}
	
	private void resetEnemyStats() {
		closestEnemy = null;
		closestEnemyDist = Integer.MAX_VALUE;
		closestEnemyWithFlux = null;
		closestEnemyWithFluxDist = Integer.MAX_VALUE;
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
		
		System.arraycopy(blank_closestInDir, 0, closestInDir, 0, 10);
	}

	private void resetAllyStats() {
		closestAllyScout = null;
		closestAllyScoutDist = Integer.MAX_VALUE;
		numAdjacentAllies = 0;
		numAllyRobots = 0;
		numAllyToRegenerate = 0;
		alliesOnLeft = 0;
		alliesOnRight = 0;
		alliesInFront = 0;
		closestLowFluxAlly = null;
		closestLowFluxAllyDist = Integer.MAX_VALUE;
		allies_in_dir = new int[8];
		lowestFluxAllied = null;
		lowestFlux = 9000;
		lowestEnergonAllied = null;
		lowestEnergonRatio = 1.0;
	}

	private void addEnemy(RobotInfo rinfo) throws GameActionException {
		if(rinfo.type==RobotType.TOWER && !br.dc.isTowerTargetable(rinfo))
			return;
		
		int pos = rinfo.robot.getID();
		enemyInfos[pos] = rinfo;
		enemyTimes[pos] = Clock.getRoundNum();

		enemyRobots[numEnemyRobots++] = pos;

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
		MapLocation enemyLoc = rinfo.location;

		centerEnemyX += enemyLoc.x;
		centerEnemyY += enemyLoc.y;

		int dist = enemyLoc.distanceSquaredTo(br.curLoc);
		if (dist < closestEnemyDist) {
			closestEnemy = rinfo;
			closestEnemyDist = dist;
		}
	}
	
	private void addEnemyForScout(RobotInfo rinfo) throws GameActionException {
		if(rinfo.type==RobotType.TOWER && !br.dc.isTowerTargetable(rinfo))
			return;
		
		int pos = rinfo.robot.getID();
		enemyInfos[pos] = rinfo;
		enemyTimes[pos] = Clock.getRoundNum();

		enemyRobots[numEnemyRobots++] = pos;
		
		// Distance Stats
		MapLocation eloc = rinfo.location;

		centerEnemyX += eloc.x;
		centerEnemyY += eloc.y;
		
		int dist = eloc.distanceSquaredTo(br.curLoc);
		
		if (dist < closestEnemyDist) {
			closestEnemy = rinfo;
			closestEnemyDist = dist;
		}

		switch (rinfo.type) {
		case ARCHON:
			enemyArchons[numEnemyArchons++] = pos;
			break;
		case DISRUPTER:
			enemyDisruptors[numEnemyDisruptors++] = pos;
			if (rinfo.flux >= 0.15 && dist <= closestEnemyWithFluxDist ) {
				closestEnemyWithFlux = rinfo;
				closestEnemyWithFluxDist = dist;
			}
			
			break;
		case SCORCHER:
			enemyScorchers[numEnemyScorchers++] = pos;
			break;
		case SCOUT:
			enemyScouts[numEnemyScouts++] = pos;
			break;
		case SOLDIER:
			enemySoldiers[numEnemySoldiers++] = pos;
			if (rinfo.flux >= 0.15 && dist < closestEnemyWithFluxDist ) {
				closestEnemyWithFlux = rinfo;
				closestEnemyWithFluxDist = dist;
			}
			break;
		case TOWER:
			enemyTowers[numEnemyTowers++] = pos;
			break;
		}
		
		Direction dir = br.curLoc.directionTo(eloc);
		switch (rinfo.type) {
		case ARCHON:
		case SOLDIER:
		case SCORCHER:
		case DISRUPTER:
			if (closestInDir[dir.ordinal()] > dist)
				closestInDir[dir.ordinal()] = dist;
			break;
		}
	}
	
	private void addEnemyForArchon(RobotInfo rinfo) throws GameActionException {
		if(rinfo.type==RobotType.TOWER && !br.dc.isTowerTargetable(rinfo))
			return;
		
		int pos = rinfo.robot.getID();
		enemyInfos[pos] = rinfo;
		enemyTimes[pos] = Clock.getRoundNum();

		enemyRobots[numEnemyRobots++] = pos;

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
		
		// TODO(jven): archonID is not exposed here so right now all archons are
		// competing for the same slot
		br.tmem.rememberEnemy(0, rinfo.robot.getID(), rinfo.type);
		
		// Distance Stats
		MapLocation eloc = rinfo.location;
		Direction dir = br.curLoc.directionTo(eloc);

		centerEnemyX += eloc.x;
		centerEnemyY += eloc.y;

		int dist = eloc.distanceSquaredTo(br.curLoc);
		if (dist < closestEnemyDist) {
			closestEnemy = rinfo;
			closestEnemyDist = dist;
		}
		
		switch (rinfo.type) {
		case ARCHON:
		case SOLDIER:
		case SCORCHER:
		case DISRUPTER:
		{
			if (closestInDir[dir.ordinal()] > dist)
				closestInDir[dir.ordinal()] = dist;
		} break;
		}
	}

	private void addAlly(RobotInfo rinfo) {
		if (rinfo.type == RobotType.TOWER) return;
		
		int pos = rinfo.robot.getID();
		allyRobots[numAllyRobots++] = pos;
		allyInfos[pos] = rinfo;
		allyTimes[pos] = Clock.getRoundNum();


//		int dist = br.curLoc.distanceSquaredTo(rinfo.location);
		
//		if ((rinfo.energon < rinfo.type.maxEnergon - 0.2) && !rinfo.regen
//				&& dist <= 5) {
//			numAllyToRegenerate++;
//		}

		if (rinfo.location.distanceSquaredTo(br.curLoc) <= 2) {
			adjacentAllies[numAdjacentAllies++] = rinfo;
		}
		
		int ddir = (br.curLoc.directionTo(rinfo.location).ordinal()-
				br.curDir.ordinal()+8) % 8;
		if(ddir >= 5)
			alliesOnLeft++;
		else if(ddir >= 1 && ddir <= 3)
			alliesOnRight++;
		if(ddir <= 1 || ddir == 7)
			alliesInFront++;
		
//		if(rinfo.type==RobotType.SCOUT && dist<closestAllyScoutDist) {
//			closestAllyScoutDist = dist;
//			closestAllyScout = rinfo;
//		}
		
//		// TODO(jven): this stuff should be linked with fbs
//		if (rinfo.type != RobotType.ARCHON && rinfo.type != RobotType.SCOUT &&
//				rinfo.flux < rinfo.energon / 3 &&
//				dist < closestLowFluxAllyDist) {
//			closestLowFluxAlly = rinfo;
//			closestLowFluxAllyDist = dist;
//		}
	}
	
	private void addAllyForScout(RobotInfo rinfo) {
		if (rinfo.type == RobotType.TOWER) return;
		
		int pos = rinfo.robot.getID();
		allyRobots[numAllyRobots++] = pos;
		allyInfos[pos] = rinfo;
		allyTimes[pos] = Clock.getRoundNum();

		int dist = br.curLoc.distanceSquaredTo(rinfo.location);
		
		if ((rinfo.energon < rinfo.type.maxEnergon - 0.2) && !rinfo.regen
				&& dist <= RobotType.SCOUT.attackRadiusMaxSquared) {
			numAllyToRegenerate++;
		}
				
		if (rinfo.location.isAdjacentTo(br.curLoc)) {
			adjacentAllies[numAdjacentAllies++] = rinfo;
		}
		
		double ratio = 2.0;
		double flux = 9999;
		
		switch (rinfo.type) {
		case ARCHON:
			ratio = rinfo.energon/150.0;
			break;
		case DISRUPTER:
			ratio = rinfo.energon/70.0;
			flux = rinfo.flux;
			break;
		case SCORCHER:
			ratio = rinfo.energon/70.0;
			flux = rinfo.flux;
			break;
		case SCOUT:
			flux = rinfo.flux;
			
			if(dist<closestAllyScoutDist) {
				closestAllyScoutDist = dist;
				closestAllyScout = rinfo;
			}
			break;
		case SOLDIER:
			ratio = rinfo.energon/40.0;
			flux = rinfo.flux;
			break;
		case TOWER:
			break;
		}
		
		if (!rinfo.regen && ratio < lowestEnergonRatio)
		{
			lowestEnergonRatio = ratio;
			lowestEnergonAllied = rinfo;
		}
		
		if (flux < lowestFlux)
		{
			lowestFlux = rinfo.flux;
			lowestFluxAllied = rinfo;
		}
	}
	
	private void addAllyForArchon(RobotInfo rinfo) {
		if (rinfo.type == RobotType.TOWER) return;
		
		int pos = rinfo.robot.getID();
		allyInfos[pos] = rinfo;
		allyTimes[pos] = Clock.getRoundNum();
		

		numAllyRobots++;

		if (rinfo.energon != rinfo.type.maxEnergon) {
			numAllyToRegenerate++;
		}

		if (rinfo.location.distanceSquaredTo(br.curLoc) <= 2) {
			adjacentAllies[numAdjacentAllies++] = rinfo;
		}
		
		
		switch (rinfo.type) {
		case ARCHON:
		case SOLDIER:
		case SCORCHER:
		case DISRUPTER:
		{
			allies_in_dir[br.curLoc.directionTo(rinfo.location).ordinal()]++;
		} break;
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
		
		if (lastscanround < br.curRound)
		{
			needToScanAllies = true;
			needToScanEnemies = true;
			lastscanround = br.curRound;
			robots = br.rc.senseNearbyGameObjects(Robot.class);
		}
		
		if (scanAllies)
		{
			if (needToScanAllies)
				needToScanAllies = false;
			else
				scanAllies = false;
		}
		
		if (scanEnemies)
		{
			if (needToScanEnemies)
				needToScanEnemies = false;
			else
				scanEnemies = false;
		}
		
		if (scanAllies || scanEnemies)
		{
			Robot[] robots = this.robots;

			// reset stat collection
			if (scanEnemies)
				resetEnemyStats();
			if (scanAllies)
				resetAllyStats();
		
			// Bring some vars into local space for the raep loop
			RobotController rc = br.rc;
			Team myTeam = rc.getTeam();
			switch (br.myType)
			{
			case ARCHON:
				for (int idx = robots.length; --idx >= 0;) {
					Robot r = robots[idx];
					try {
						if (myTeam == r.getTeam()) {
							if (scanAllies) {
								addAllyForArchon(rc.senseRobotInfo(r));
							}
						} else {
							if (scanEnemies) {
								addEnemyForArchon(rc.senseRobotInfo(r));
							}
						}
					} catch (GameActionException e) {
						br.rc.addMatchObservation(e.toString());
						e.printStackTrace();
					}
				}
				break;
			case SCOUT:
				for (int idx = robots.length; --idx >= 0;) {
					Robot r = robots[idx];
					try {
						if (myTeam == r.getTeam()) {
							if (scanAllies) {
								addAllyForScout(rc.senseRobotInfo(r));
							}
						} else {
							if (scanEnemies) {
								addEnemyForScout(rc.senseRobotInfo(r));
							}
						}
					} catch (GameActionException e) {
						br.rc.addMatchObservation(e.toString());
						e.printStackTrace();
					}
				}
				break;
			default:
				for (int idx = robots.length; --idx >= 0;) {
					Robot r = robots[idx];
					try {
					
						if (myTeam == r.getTeam()) {
							if (scanAllies) {
								addAlly(rc.senseRobotInfo(r));
							}
						} else {
							if (scanEnemies) {
								addEnemy(rc.senseRobotInfo(r));
							}
						}
					} catch (GameActionException e) {
						br.rc.addMatchObservation(e.toString());
						e.printStackTrace();
					}
				}
				break;
					
			}
			
			if (scanEnemies)
			{
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
		}
	}
	
	/**
	 * Check if we have scanned enemies this round
	 */
	public boolean hasScannedEnemies()
	{
		return (lastscanround == br.curRound && !needToScanEnemies);
	}
	
	/**
	 * Check if we have scanned allies this round
	 */
	public boolean hasScannedAllies()
	{
		return (lastscanround == br.curRound && !needToScanAllies);
	}
	
//	/**
//	 * Get the robot info for given enemy robot id.
//	 */
//	public RobotInfo getEnemyInfo(int robotid)
//	{
//		return enemyInfos[robotid];
//	}
//	
//	/**
//	 * Get the robot info for given allied robot id.
//	 */
//	public RobotInfo getAlliedInfo(int robotid)
//	{
//		return allyInfos[robotid];
//	}

	/**
	 * Get the difference in strength between the two swarms
	 */
	public int getArmyDifference() {
		return numAllyRobots - numEnemyRobots;
	}

	/**
	 * Gets the calculated swarm target in order to chase an enemy swarm
	 */
	public MapLocation getEnemySwarmTarget() {
		double a = Math.sqrt(vecEnemyX * vecEnemyX + vecEnemyY * vecEnemyY) + .001;

		return new MapLocation((int) (vecEnemyX * 7 / a) + br.curLoc.x,
				(int) (vecEnemyY * 7 / a) + br.curLoc.y);

	}
	
	public int getAlliesInDirection(Direction dir)
	{
		if(dir==null || dir==Direction.NONE || dir==Direction.OMNI)
			return 0;
		return allies_in_dir[dir.ordinal()]+allies_in_dir[(dir.ordinal()+1)%8]+
				allies_in_dir[(dir.ordinal()+7)%8];
	}

	/**
	 * Gets the calculated enemy swarm center
	 */
	public MapLocation getEnemySwarmCenter() {
		return new MapLocation(centerEnemyX, centerEnemyY);
	}
	
	/** Gets the enemy info from the radar into your own and nearby robots' extended radar. */
	public void broadcastEnemyInfo(boolean sendOwnInfo) {
		if(numEnemyRobots==numEnemyScouts) return;
		
		int[] shorts = new int[(numEnemyRobots-numEnemyScouts)*5+(sendOwnInfo?5:0)];
		if(sendOwnInfo) {
			shorts[0] = br.myID;
			shorts[1] = br.curLoc.x;
			shorts[2] = br.curLoc.y;
			shorts[3] = 10000+(int)Math.ceil(Util.getOwnStrengthEstimate(br.rc));
			shorts[4] = br.myType.ordinal();
		}
		for(int i=0, c=sendOwnInfo?5:0; i<numEnemyRobots; i++, c+=5) {
			RobotInfo ri = enemyInfos[enemyRobots[i]];
			if(ri.type==RobotType.SCOUT) {
				c-=5;
				continue;
			}
			shorts[c] = ri.robot.getID();
			shorts[c+1] = ri.location.x;
			shorts[c+2] = ri.location.y;
			shorts[c+3] = (int)Math.ceil(Util.getStrengthEstimate(ri));
			shorts[c+4] = ri.type.ordinal();
		}
		if(br.myType==RobotType.SOLDIER || br.myType==RobotType.DISRUPTER || br.myType==RobotType.SCORCHER)
			br.er.integrateEnemyInfo(shorts);
		br.io.sendUShorts(BroadcastChannel.EXTENDED_RADAR, BroadcastType.ENEMY_INFO, shorts);
	}
	


}
