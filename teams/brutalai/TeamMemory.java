package brutalai;

import battlecode.common.RobotType;

/**
 * Archon memory between rounds. We have 32 longs available. Here is a list of
 * what each long represents:
 * 
 * 0 = round number (0, 1, or 2), touched only by archon 0
 * 1 = enemies seen by archon 0
 * 2 = enemies seen by archon 1
 * 3 = enemies seen by archon 2
 * 4 = enemies seen by archon 3
 * 5 = enemies seen by archon 4
 * 6 = enemies seen by archon 5
 * 7 = UNUSED
 * 8 = UNUSED
 * 9 = UNUSED
 * 10 = UNUSED
 * 11 = UNUSED
 * 12 = UNUSED
 * 13 = UNUSED
 * 14 = UNUSED
 * 15 = UNUSED
 * 16 = UNUSED
 * 17 = UNUSED
 * 18 = UNUSED
 * 19 = UNUSED
 * 20 = UNUSED
 * 21 = UNUSED
 * 22 = UNUSED
 * 23 = UNUSED
 * 24 = UNUSED
 * 25 = UNUSED
 * 26 = UNUSED
 * 27 = UNUSED
 * 28 = UNUSED
 * 29 = UNUSED
 * 30 = UNUSED
 * 31 = UNUSED
 * 
 * @author coryli, jven
 *
 */
public class TeamMemory {

	private final BaseRobot br;
	private final long[] mem;
	private final boolean[][] enemySeenByArchon;
	private int soldiersSeen;
	private int scoutsSeen;
	private int disruptersSeen;
	private int scorchersSeen;
	
	public TeamMemory(BaseRobot br) {
		this.br = br;
		mem = br.rc.getTeamMemory();
		enemySeenByArchon = new boolean[6][65536];
	}

	/** @return the current round number (0th, 1st, 2nd match) */
	public int getRound() {
		return (int)mem[0];
	}
	
	/** Archon zero should advance the round counter */
	public void advanceRound() {
		br.rc.setTeamMemory(0, getRound() + 1);
	}
	
	/**
	 * Call this method when you see an enemy. Next round's units will
	 * have access to the number of enemies seen in the previous round for each
	 * RobotType. Called in Radar.
	 * @param archonID The Archon (0-5) reporting the enemy
	 * @param enemyID The ID of the enemy being reported
	 * @param type The RobotType of the enemy being reported
	 */
	public void rememberEnemy(int archonID, int enemyID, RobotType type) {
		if (archonID < 0 || archonID > 5 || getRound() < 0 || getRound() > 1 ||
				enemySeenByArchon[archonID][enemyID % 65536]) {
			return;
		}
		enemySeenByArchon[archonID][enemyID % 65536] = true;
		switch (type) {
			case SOLDIER:
				soldiersSeen++;
				break;
			case SCOUT:
				scoutsSeen++;
				break;
			case DISRUPTER:
				disruptersSeen++;
				break;
			case SCORCHER:
				scorchersSeen++;
				break;
			case ARCHON:
			case TOWER:
			default:
				break;
		}
		br.rc.setTeamMemory(1 + archonID, ((long)soldiersSeen << 48) +
				((long)scoutsSeen << 32) + ((long)disruptersSeen << 16) +
				(long)scorchersSeen);
	}

	/**
	 * Returns the average number of enemies of the given RobotType in the
	 * previous round, over each Archon. Does not count Archons or Towers.
	 * Returns 0 if called the first round.
	 * @param type The RobotType to report
	 * @return The average number of enemies of the given type seen the
	 * previous round, over each Archon.
	 */
	public int getNumEnemiesLastRound(RobotType type) {
		int totalSoldiersSeen = 0;
		int totalScoutsSeen = 0;
		int totalDisruptersSeen = 0;
		int totalScorchersSeen = 0;
		for (int archonID = 0; archonID < 6; archonID++) {
			long archonReport = mem[1 + archonID];
			totalSoldiersSeen += (int) ((archonReport >> 48) & 0xFFFF);
			totalScoutsSeen += (int) ((archonReport >> 32) & 0xFFFF);
			totalDisruptersSeen += (int) ((archonReport >> 16) & 0xFFFF);
			totalScorchersSeen += (int) (archonReport & 0xFFFF);
		}
		// TODO(jven): right now, calls to rememberEnemy are made in radar, from
		// which we do not have access to archonID, so that only one of the
		// archon report fields is being populated
		switch (type) {
			case SOLDIER:
//				return totalSoldiersSeen / 6;
				return totalSoldiersSeen;
			case SCOUT:
//				return totalScoutsSeen / 6;
				return totalScoutsSeen;
			case DISRUPTER:
//				return totalDisruptersSeen / 6;
				return totalDisruptersSeen;
			case SCORCHER:
//				return totalScorchersSeen / 6;
				return totalScorchersSeen;
			case ARCHON:
			case TOWER:
			default:
				return 0;
		}
	}
}
