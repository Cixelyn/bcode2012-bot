package ducks;

import battlecode.common.Direction;

public final class Constants {
	
	public static final Direction[] directions = new Direction[] {
		Direction.NORTH,
		Direction.NORTH_EAST,
		Direction.EAST,
		Direction.SOUTH_EAST,
		Direction.SOUTH,
		Direction.SOUTH_WEST,
		Direction.WEST,
		Direction.NORTH_WEST,
	};
	
	// Archons must have this amount of flux in order to distribute.
	public static final double MIN_ARCHON_FLUX = 0.2;
	// Archons try to maintain this amount of flux on other units.
	public static final double MIN_UNIT_FLUX = 20;
	// Units below this amount of flux will not execute their run method.
	public static final double POWER_DOWN_FLUX = 1;
	// The frequency with which units broadcast information.
	public static final int ARCHON_BROADCAST_FREQUENCY = 4;
	public static final int SOLDIER_BROADCAST_FREQUENCY = 30;
	// The number of damaged units that a scout must have in range in order
	// to regen.
	public static final int MIN_DAMAGED_UNITS_TO_REGEN = 1;
	// Archons try to stay at this range from enemy units.
	public static final int ARCHON_SAFETY_RANGE = 32;
	// Secret keys for use in messaging system, one for each team.
	public static final int[] RADIO_TEAM_KEYS = new int[] {5555, 1729};
	// Initial bearing for all robots
	public static final Direction INITIAL_BEARING = Direction.EAST;
	// The maximum distance units should be from the closest archon
	public static final int MAX_SWARM_RADIUS = 16;
	// The number of rounds after which to start taking towers
	public static final int CIRCLE_MAP_ROUNDS = 2000;
	public static final int TOWER_ROUNDS = 4000;
}