package ducksold1;

import battlecode.common.Direction;

/**
 * Various magic numbers and useful constants brought into static space
 */
public final class Constants {

	/** Reverse ordinal mappings */
	public static final Direction[] directions = Direction.values();
	
	/** Robots must have this amount of flux in order to distribute. */
	public static final double MIN_ROBOT_FLUX = 0.5;
	/** Archons try to maintain this ratio of max flux on other units. */
	public static final double MIN_UNIT_BATTLE_FLUX_RATIO = 0.15;
	
	/** The frequency with which units broadcast information. */
	public static final int ARCHON_BROADCAST_FREQUENCY = 4;
	/** Soldier broadcast frequency for dead enemy archon array */
	public static final int SOLDIER_BROADCAST_FREQUENCY = 30;
	/** The number of damaged units that a scout must have in range in order to regen. */
	public static final int MIN_DAMAGED_UNITS_TO_REGEN = 1;
	/** Archons try to stay at this range from enemy units. */
	public static final int ARCHON_SAFETY_RANGE = 32;
	/** Secret keys for use in messaging system, one for each team. */
	public static final int[] RADIO_TEAM_KEYS = new int[] {5555, 1729};
	/** The maximum distance units should be from the closest archon */
	public static final int MAX_SWARM_RADIUS = 17;
	
	/** Minimum distance for archons to be from each other at initial split. */
	public static final int SPLIT_DISTANCE = 16;
	/** Number of soldiers each archon makes after initial split. */
	public static final int SOLDIERS_PER_ARCHON = 5;
	/** Maximum number of rounds to spend splitting. */
	public static final int MAX_SPLIT_TIME = 40;
	/** Number of times to turn side to side before giving up. */
	public static final int WIGGLE_TIMEOUT = 30;
	/** The maximum distance to respond to back off signals. */
	public static final int BACK_OFF_DISTANCE = 10;
	/** The number of archons that should build towers given the number of enemy archons. */
	public static final int[] NUM_ARCHONS_TO_TOWER = new int[] {4, 3, 2, 1, 1, 0, 0};
}