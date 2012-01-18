package ducks;

import battlecode.common.Direction;
import battlecode.common.RobotType;

/** A data structure containing a command to be processed by the movement state machine. */
public class MoveInfo {
	public RobotType robotType;
	public Direction dir;
	public boolean moveForward;
	public boolean moveBackward;
	/** Turn in a direction. Do not move. */
	public MoveInfo(Direction dirToTurn) {
		this.dir = dirToTurn;
	}
	/** Move in a direction. 
	 * @param moonwalk true if we want to move backwards, false if we want to move forwards */
	public MoveInfo(Direction dirToMove, boolean moonwalk) {
		this.dir = dirToMove;
		if(moonwalk) moveBackward = true;
		else moveForward = true;
	}
	/** Spawn a robot in a given direction. */
	public MoveInfo(RobotType robotType, Direction dirToSpawn) {
		this.robotType = robotType;
		this.dir = dirToSpawn;
	}
}
