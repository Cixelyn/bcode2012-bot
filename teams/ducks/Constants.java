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
	// The frequency with which archons broadcast rally information.
	public static final int ARCHON_BROADCAST_FREQUENCY = 5;
	// The number of damaged units that a scout must have in range in order
	// to regen.
	public static final int MIN_DAMAGED_UNITS_TO_REGEN = 1;
	// Archons try to stay at this range from enemy units.
	public static final int ARCHON_SAFETY_RANGE = 32;
	// Secret keys for use in messaging system, one for each team.
	public static final int[] RADIO_TEAM_KEYS = new int[] {5555, 1729};
}
