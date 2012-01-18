package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;

public class MovementStateMachine {
	private static final int TURNS_STUCK_UNTIL_ROBOT_STARTS_RANDOMLY = 16;
	MovementState curState;
	final BaseRobot br;
	final RobotController rc;
	final NavigationSystem nav;
	MoveInfo nextMove;
	int turnsStuck;
	Direction dirToSense;
	public MovementStateMachine(BaseRobot br) {
		this.br = br;
		this.rc = br.rc;
		this.nav = br.nav;
		curState = MovementState.COOLDOWN;
	}

	public void step() throws GameActionException {
		curState = execute();
	}
	private MovementState execute() throws GameActionException {
		rc.setIndicatorString(2, curState+"");
		switch(curState) {
		case ABOUT_TO_SPAWN:
			boolean spawningAir = nextMove.robotType.isAirborne();
			if((spawningAir && (rc.senseObjectAtLocation(br.curLocInFront, RobotLevel.IN_AIR)==null)) || 
					(!spawningAir && rc.canMove(br.curDir))) {
				rc.spawn(nextMove.robotType);
				return MovementState.IDLE;
			}
			// fall through, no break
		case COOLDOWN:
			if(rc.isMovementActive()) {
				nav.prepare();
				return MovementState.COOLDOWN;
			}
			// fall through, no break
		case IDLE:
			nextMove = br.computeNextMove();
			if(nextMove==null || nextMove.dir==null || 
					nextMove.dir==Direction.NONE || nextMove.dir==Direction.OMNI) 
				return MovementState.IDLE;
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
				rc.setDirection(dir);
				turnsStuck = 0;
				return MovementState.ABOUT_TO_MOVE;
			} 
			rc.setDirection(nextMove.dir);
			return MovementState.IDLE;
		case ABOUT_TO_MOVE:
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
				if(turnsStuck>=TURNS_STUCK_UNTIL_ROBOT_STARTS_RANDOMLY) 
					rc.setDirection(nav.navigateCompletelyRandomly());
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
