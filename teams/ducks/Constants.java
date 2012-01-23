package ducks;

import battlecode.common.Direction;

/**
 * Various magic numbers and useful constants brought into static space
 */
public final class Constants {

	/** Reverse ordinal mappings */
	public static final Direction[] directions = Direction.values();

	public static final int SOLDIER_BROADCAST_FREQUENCY = 30;
	public static final int ARCHON_OWNERSHIP_BUFFER_LENGTH = 5;
	

	/*************************************************************
	 * OLD CONSTANTS BELOW
	 *************************************************************
	 */
	
	/** The frequency with which units broadcast information. */
	public static final int ARCHON_BROADCAST_FREQUENCY = 4;
	
}