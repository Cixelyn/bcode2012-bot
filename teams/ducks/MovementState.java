package ducks;

/** States to be used in the movement state machine. */
public enum MovementState {
	/** Movement cooldown is not active. Spawn stuff if desirable. 
	 * Compute where to move next, and turn appropriately.*/
	IDLE, 
	/** Was just idle, and decided to spawn something, but needed to turn first. */
	ABOUT_TO_SPAWN,
	/** Was just idle, and computed where to move, and turned to move. */
	ABOUT_TO_MOVE, 
	/** Just moved forward or backward. Need to call mc.senseAfterMove(justMovedDir) here. */
	JUST_MOVED, 
	/** Waiting to be idle again. Prepare for the next navigation computation. */
	COOLDOWN;
}
