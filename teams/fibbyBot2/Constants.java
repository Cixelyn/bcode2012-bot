package fibbyBot2;

import battlecode.common.Direction;

/**
 * Various magic numbers and useful constants brought into static space
 */
public final class Constants {

	/** Reverse ordinal mappings */
	public static final Direction[] directions = Direction.values();
	
	/** Archons must have this amount of flux in order to distribute. */
	public static final double MIN_ARCHON_FLUX = 0.2;
	/** Archons try to maintain this portion of max flux on other units. */
	public static final double MIN_UNIT_FLUX_RATIO = 0.75;
	/** Units below this amount of flux will not execute their run method. */
	public static final double POWER_DOWN_FLUX = 1;
	
	/** The frequency with which units broadcast information. */
	public static final int ARCHON_BROADCAST_FREQUENCY = 4;
	/** Soldier broadcast frequency for dead enemy archon array */
	public static final int SOLDIER_BROADCAST_FREQUENCY = 30;
	/** The number of damaged units that a scout must have in range in order to regen. */
	public static final int MIN_DAMAGED_UNITS_TO_REGEN = 1;
	/** Archons try to stay at this range from enemy units. */
	public static final int ARCHON_SAFETY_RANGE = 25;
	/** Secret keys for use in messaging system, one for each team. */
	public static final int[] RADIO_TEAM_KEYS = new int[] {5555, 1729};
	
	/** Initial bearing for all robots */
	public static final Direction INITIAL_BEARING = Direction.EAST;
	/** The maximum distance units should be from the closest archon */
	public static final int MAX_SWARM_RADIUS = 16;
	
	/** Minimum distance between archons when splitting. */
	public static final int SPLIT_DISTANCE = 16;
	/** Maximum number of rounds to split. */
	public static final int SPLIT_ROUNDS = 50;
	/** Number of soldiers to make per archon. */
	public static final int SOLDIERS_PER_ARCHON = 5;
	
	/** Maximum distance to respond to back off messages. */
	public static final int BACK_OFF_DISTANCE = 9;
}