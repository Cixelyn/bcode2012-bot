package normalai;

import battlecode.common.Direction;

/**
 * Various magic numbers and useful constants brought into static space
 */
public final class Constants {

	/** Reverse ordinal mappings */
	public static final Direction[] directions = Direction.values();

	
	public static final int ROUNDS_TO_EXPLORE = 120;
	public static final int ROUNDS_TO_GO_HOME = 240;
	public static final int DISTANCE_TO_HOME_ON_GOHOME = 25;
	public static final int ARMY_SIZE_ON_INITIAL_BUILD = 5;
	public static final int BUILD_ARMY_ROUND_THRESHOLD = 1000;
	public static final int SPLIT_DISTANCE = 16;
	public static final int SOLDIER_BROADCAST_FREQUENCY = 30;
	public static final int ARCHON_OWNERSHIP_BUFFER_LENGTH = 5;
	
//	attack move constants
	public static final int NON_TRIVIAL_ENEMY_CONCENTRATION = 2;
	public static final int ARCHON_CHASE_DIRECTION_MULTIPLIER = 9;
	public static final int ARCHON_CHASE_ROUNDS = 50;
	public static final int ARCHON_CHASE_ROUNDS_WHEN_BUILDING = 30;
	public static final int ARCHON_CLOSE_DISTANCE = 16;
	public static final int ARCHON_MOVE_STUCK_ROUNDS = 20;
	public static final int ARCHON_MOVE_REALLY_STUCK_ROUNDS = 40;
	public static final int SWARM_DISTANCE_FROM_ARCHON = 2;
	public static final int SWARM_DISTANCE_FROM_ARCHON2 = 4;
	public static final int MAX_SWARM_ARCHON_DISTANCE_SQUARED = 64;
	public static final int ATTACK_MOVE_KITE_DISTANCE_SQUARED = 17;
	public static final int ARCHON_FLUX_DURING_COMBAT = 40;
	public static final int MAX_ROUNDS_TO_ATTACKBASE = 1500;
	public static final int MAX_ROUNDS_TO_BUILD_ARMY = 800;
	
//	swarm constants
	public static final int SOLDIER_CHASE_ROUNDS = 30;
	public static final int SOLDIER_CHASE_DISTANCE_SQUARED = 72;
	public static final int SOLDIER_CHASE_DISTANCE_MULTIPLIER = 9;
	public static final int SOLDIER_SWARM_IN_FRONT = 4;
	public static final int SOLDIER_SWARM_DISTANCE = 10;

	/*************************************************************
	 * OLD CONSTANTS BELOW
	 *************************************************************
	 */
	
	
	/** Robots must have this amount of flux in order to distribute. */
	public static final double MIN_ROBOT_FLUX = 0.5;
	/** Archons try to maintain this ratio of max flux on other units. */
	public static final double MIN_UNIT_BATTLE_FLUX_RATIO = 0.5;
	
	
	/** The frequency with which units broadcast information. */
	public static final int ARCHON_BROADCAST_FREQUENCY = 4;
	/** Secret keys for use in messaging system, one for each team. */
	public static final int[] RADIO_TEAM_KEYS = new int[] {5555, 1729};
	
}