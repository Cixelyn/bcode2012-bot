package insaneai;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;

public class MovementStateMachine {
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

	private static final int TURNS_STUCK_UNTIL_ROBOT_STARTS_MOVING_RANDOMLY = 16;
	private MovementState curState;
	private final BaseRobot br;
	private final RobotController rc;
	private final NavigationSystem nav;
	private MoveInfo nextMove;
	private int turnsStuck;
	private Direction dirToSense;
	public MovementStateMachine(BaseRobot br) {
		this.br = br;
		this.rc = br.rc;
		this.nav = br.nav;
		curState = MovementState.COOLDOWN;
	}

	public void reset() {
		curState = MovementState.COOLDOWN;
		nextMove = null;
		turnsStuck = 0;
	}
	public boolean justMoved() {
		return curState == MovementState.JUST_MOVED;
	}
	public void step() throws GameActionException {
		curState = execute();
//		br.dbg.setIndicatorString('h',2, nextMove+", movement_state="+curState);
	}
	private MovementState execute() throws GameActionException {
		switch(curState) {
		case ABOUT_TO_SPAWN:
			boolean spawningAir = nextMove.robotType.isAirborne();
			if(rc.getFlux() >= nextMove.robotType.spawnCost && 
					((spawningAir && (rc.senseObjectAtLocation(br.curLocInFront, RobotLevel.IN_AIR)==null)) || 
					(!spawningAir && rc.canMove(br.curDir)))) {
				rc.spawn(nextMove.robotType);
				return MovementState.IDLE;
			}
			// fall through, no break
		case COOLDOWN:
			if(rc.isMovementActive()) {
				return MovementState.COOLDOWN;
			}
			// fall through, no break
		case IDLE:
			nextMove = br.computeNextMove();
			if(nextMove==null || nextMove.dir==null || 
					nextMove.dir==Direction.NONE || nextMove.dir==Direction.OMNI) {
				return MovementState.IDLE;
			}
			if(nextMove.robotType!=null) {
				if(nextMove.dir==br.curDir) {
					rc.spawn(nextMove.robotType);
					return MovementState.IDLE;
				} else {
					rc.setDirection(nextMove.dir);
					return MovementState.ABOUT_TO_SPAWN;
				}
			}
			if(nextMove.moveForward) {
				Direction dir = nav.wiggleToMovableDirection(nextMove.dir);
				if(dir==null) {
					turnsStuck = 0;
					return MovementState.ABOUT_TO_MOVE;
				} else if(dir==br.curDir) {
					rc.moveForward();
					dirToSense = dir;
					return MovementState.JUST_MOVED;
				} else {
					rc.setDirection(dir);
					turnsStuck = 0;
					return MovementState.ABOUT_TO_MOVE;
				}
			} else if(nextMove.moveBackward) {
				Direction dir = nav.wiggleToMovableDirection(nextMove.dir);
				if(dir==null) {
					turnsStuck = 0;
					return MovementState.ABOUT_TO_MOVE;
				} else if(dir==br.curDir.opposite()) {
					rc.moveBackward();
					dirToSense = dir;
					return MovementState.JUST_MOVED;
				} 
				rc.setDirection(dir.opposite());
				turnsStuck = 0;
				return MovementState.ABOUT_TO_MOVE;
			} 
			rc.setDirection(nextMove.dir);
			return MovementState.IDLE;
		case ABOUT_TO_MOVE:
			if(rc.getFlux()<br.myType.moveCost)
				return MovementState.IDLE;
			if(nextMove.moveForward) {
				if(rc.canMove(br.curDir)) {
					rc.moveForward();
					dirToSense = br.curDir;
					return MovementState.JUST_MOVED;
				}
				Direction dir = nav.wiggleToMovableDirection(nextMove.dir);
				if(dir!=null) {
					rc.setDirection(dir);
				} else {
					turnsStuck++;
					if(turnsStuck>=TURNS_STUCK_UNTIL_ROBOT_STARTS_MOVING_RANDOMLY) {
						Direction randomDir = nav.navigateCompletelyRandomly();
						if(randomDir!=Direction.NONE)
							rc.setDirection(randomDir);
					}
				}
			} else if(nextMove.moveBackward) { 
				if(rc.canMove(br.curDir.opposite())) {
					rc.moveBackward();
					dirToSense = br.curDir.opposite();
					return MovementState.JUST_MOVED;
				}
				Direction dir = nav.wiggleToMovableDirection(nextMove.dir);
				if(dir!=null) {
					rc.setDirection(dir.opposite());
				} else {
					turnsStuck++;
					if(turnsStuck>=TURNS_STUCK_UNTIL_ROBOT_STARTS_MOVING_RANDOMLY) {
						Direction randomDir = nav.navigateCompletelyRandomly();
						if(randomDir!=Direction.NONE)
							rc.setDirection(randomDir.opposite());
					}
				}
			}
			return MovementState.ABOUT_TO_MOVE;
		case JUST_MOVED:
			turnsStuck = 0;
			br.mc.senseAfterMove(dirToSense);
			return MovementState.COOLDOWN;
		default:
			return MovementState.IDLE;
		}
	}
}
