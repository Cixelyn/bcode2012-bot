package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldierRobotOLD extends StrategyRobot {
	
	private int rallyPriority;
	private MapLocation objective;
	
	private MapLocation backOffLoc;
	private RobotState prevState;
	
	private boolean initalized;
	private boolean backedoff;
	
	public SoldierRobotOLD(RobotController myRC) {
		super(myRC, RobotState.INITIALIZE);
		initalized = false;
	}


	@Override
	public RobotState processTransitions(RobotState state)
			throws GameActionException {
		
		switch (state)
		{
		case INITIALIZE:
		{
			// go to power save mode
			if (initalized)
				return RobotState.POWER_SAVE;
		} break;
		case RUSH:
		{
			// chase enemy if in range
			if (dc.getClosestEnemy() != null) {
				return RobotState.CHASE;
			}
		} break;
		case CHASE:
		{
			if (dc.getClosestEnemy() == null) {
				return RobotState.RUSH;
			}
		} break;
		case BACK_OFF:
		{
			if (backedoff)
				return prevState;
		} break;
		}
		
		return state;
	}

	@Override
	public void prepareTransition(RobotState newstate, RobotState oldstate)
			throws GameActionException {
		
		switch (newstate) {
		case BACK_OFF:
			backedoff = false;
			prevState = oldstate;
			break;
		case INITIALIZE:
			initalized = false;
			break;
		}
		
	}

	@Override
	public void execute(RobotState state) throws GameActionException {
		// power down if not enough flux, or suicide if we won
		if (currFlux < Constants.MIN_ROBOT_FLUX) {
			if (eai.getNumEnemyArchons() == 0) {
				rc.suicide();
			}
			return;
		}
		switch (state) {
			case INITIALIZE:
				initialize();
				break;
			case POWER_SAVE:
				powerSave();
				break;
			case RUSH:
				rush();
				break;
			case CHASE:
				chase();
				break;
			case BACK_OFF:
				backOff();
				break;
			default:
				break;
		}
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb) throws GameActionException {
		int[] msgInts;
		RobotState curState = getCurrentState();
		//MapLocation[] msgLocs;
		switch(msgType) {
			case 'r':
				if (curState == RobotState.POWER_SAVE) {
					gotoState(RobotState.RUSH);
				}
				msgInts = Radio.decodeShorts(sb);
				int msgRallyPriority = msgInts[0];
				MapLocation msgObjective = new MapLocation(msgInts[1], msgInts[2]);
				if (msgRallyPriority > rallyPriority) {
					objective = msgObjective;
					rallyPriority = msgRallyPriority;
				}
				break;
			case 'd':
				int[] deadEnemyArchonIDs = Radio.decodeShorts(sb);
				for (int id : deadEnemyArchonIDs) {
					eai.reportEnemyArchonKill(id);
				}
			case 'b':
				if (curState != RobotState.BACK_OFF &&
						curState != RobotState.CHASE) {
					backOffLoc = Radio.decodeMapLoc(sb);
					if (currLoc.distanceSquaredTo(backOffLoc) <
							Constants.BACK_OFF_DISTANCE) {
						gotoState(RobotState.BACK_OFF);
					}
				}
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	public void initialize() throws GameActionException {
		// set nav mode
		nav.setNavigationMode(NavigationMode.BUG);
		// set radio addresses
		io.setAddresses(new String[] {"#x", "#s"});
		// set initial objective
		rallyPriority = -1;
		objective = currLoc.add(
				rc.sensePowerCore().getLocation().directionTo(
				dc.getCapturablePowerCores()[0]), GameConstants.MAP_MAX_HEIGHT);
		// done
		initalized = true;
	}
	
	public void powerSave() throws GameActionException {
		// attack closest enemy if possible
		attackClosestEnemy();
		// spin
		if (!rc.isMovementActive()) {
			rc.setDirection(currDir.rotateLeft().rotateLeft().rotateLeft());
		}
	}
	
	public void rush() throws GameActionException {
		//rc.suicide();
		// swarm towards objective
		swarmTowards(objective);
		// send dead enemy archon IDs
		eai.sendDeadEnemyArchonIDs();
	}
	
	public void chase() throws GameActionException {
		// go back to previous state if no enemy in range
		RobotInfo closestEnemy = dc.getClosestEnemy();
		if (closestEnemy == null) {
			return;
		}
		// attack closest enemy
		attackClosestEnemy();
		// charge enemy
		charge(closestEnemy.location);
		// send dead enemy archon IDs
		eai.sendDeadEnemyArchonIDs();
	}
	
	public void backOff() throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		Direction dir = currLoc.directionTo(backOffLoc);
		Direction[] backOffDirs = new Direction[] {
				dir,
				dir.rotateLeft(),
				dir.rotateRight(),
				dir.rotateLeft().rotateLeft(),
				dir.rotateRight().rotateRight(),
				dir.rotateLeft().rotateLeft().rotateLeft(),
				dir.rotateRight().rotateRight().rotateRight()
		};
		// try to go away
		for (Direction d : backOffDirs) {
			if (rc.canMove(d.opposite())) {
				if (currDir != d) {
					rc.setDirection(d);
				} else {
					rc.moveBackward();
				}
				break;
			}
		}
		backedoff = true;
	}
	
	private void attackClosestEnemy() throws GameActionException {
		// wait if attack is active
		if (rc.isAttackActive()) {
			return;
		}
		// see if enemy in range
		RobotInfo closestEnemy = dc.getClosestEnemy();
		if (closestEnemy != null && rc.canAttackSquare(closestEnemy.location)) {
			rc.attackSquare(
					closestEnemy.location, closestEnemy.robot.getRobotLevel());
			if (closestEnemy.type == RobotType.ARCHON &&
					closestEnemy.energon <= myType.attackPower) {
				eai.reportEnemyArchonKill(closestEnemy.robot.getID());
			}
		}
	}
	
	private void swarmTowards(MapLocation target) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// step towards closest archon if too far away
		MapLocation closestArchon = dc.getClosestArchon();
		if (closestArchon != null &&
				currLoc.distanceSquaredTo(closestArchon) > Constants.MAX_SWARM_RADIUS) {
			target = closestArchon;
		}
		// sense tiles
		mc.senseAllTiles();
		// move towards target
		nav.setDestination(target);
		nav.prepare();
		Direction dir = nav.navigateToDestination();
		if (dir != Direction.OMNI && dir != Direction.NONE) {
			// TODO(jven): wiggle code should not be here
			for (int tries = 0; tries < Constants.WIGGLE_TIMEOUT; tries++) {
				if (!rc.canMove(dir)) {
					if (Math.random() < 0.5) {
						dir = dir.rotateLeft();
					} else {
						dir = dir.rotateRight();
					}
				} else {
					break;
				}
			}
			if (!rc.canMove(dir)) {
				return;
			}
			// end wiggle code
			if (currDir != dir) {
				rc.setDirection(dir);
			} else if (rc.canMove(currDir)) {
				rc.moveForward();
			}
		}
	}
	
	private void charge(MapLocation target) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		Direction dir = currLoc.directionTo(target);
		if (dir != Direction.OMNI && dir != Direction.NONE) {
			if (currDir != dir) {
				rc.setDirection(dir);
			} else if (rc.canMove(dir)) {
				rc.moveForward();
			}
		}
	}
}
