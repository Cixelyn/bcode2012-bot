package ducksold1;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldierRobotJV extends BaseRobot {
	
	private int rallyPriority;
	private MapLocation objective;
	
	private int timeUntilBroadcast;
	
	private MapLocation backOffLoc;
	private RobotState prevState;

	public SoldierRobotJV(RobotController myRC) {
		super(myRC);
		currState = RobotState.INITIALIZE;
	}

	@Override
	public void run() throws GameActionException {
		if (objective != null) {
			debug.setIndicatorString(1, "Distance to objective: " +
					currLoc.distanceSquaredTo(objective), "jven");
		}
		// power down if not enough flux, or suicide if all archons are dead
		if (currFlux < Constants.MIN_ROBOT_FLUX) {
			if (enemyArchonInfo.getNumEnemyArchons() == 0) {
				rc.suicide();
			}
			return;
		}
		switch (currState) {
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
			case DEFEND:
				backOff();
				break;
			default:
				break;
		}
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb) {
		int[] msgInts;
		//MapLocation[] msgLocs;
		switch(msgType) {
			case 'r':
				if (currState == RobotState.POWER_SAVE) {
					currState = RobotState.RUSH;
				}
				msgInts = Radio.decodeShorts(sb);
				int msgRallyPriority = msgInts[0];
				MapLocation msgObjective = new MapLocation(msgInts[1], msgInts[2]);
				if (msgRallyPriority > rallyPriority) {
					objective = msgObjective;
					mi.setObjective(msgObjective);
					rallyPriority = msgRallyPriority;
				}
				break;
			case 'd':
				int[] deadEnemyArchonIDs = Radio.decodeShorts(sb);
				for (int id : deadEnemyArchonIDs) {
					enemyArchonInfo.reportEnemyArchonKill(id);
				}
			case 'b':
				if (currState != RobotState.BACK_OFF &&
						currState != RobotState.CHASE) {
					backOffLoc = Radio.decodeMapLoc(sb);
					if (currLoc.distanceSquaredTo(backOffLoc) <
							Constants.BACK_OFF_DISTANCE) {
						prevState = currState;
						currState = RobotState.BACK_OFF;
					}
				}
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	public void initialize() throws GameActionException {
		// set nav mode
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		// set radio addresses
		io.setAddresses(new String[] {"#x", "#s"});
		// set initial objective
		rallyPriority = -1;
		objective = currLoc.add(
				rc.sensePowerCore().getLocation().directionTo(
				dc.getCapturablePowerCores()[0]), GameConstants.MAP_MAX_HEIGHT);
		mi.setObjective(objective);
		// set broadcast time
		timeUntilBroadcast = 0;
		// go to power save mode
		currState = RobotState.POWER_SAVE;
		powerSave();
	}
	
	public void powerSave() throws GameActionException {
		// hold position
		mi.setHoldPositionMode();
		mi.attackMove();
	}
	
	public void rush() throws GameActionException {
		// swarm towards objective
		mi.setObjective(objective);
		mi.setSwarmMode(Constants.MAX_SWARM_RADIUS);
		mi.attackMove();
		// chase enemy if in range
		if (dc.getClosestEnemy() != null) {
			currState = RobotState.CHASE;
		}
		// send dead enemy archon IDs
		sendDeadEnemyArchonIDs();
	}
	
	public void chase() throws GameActionException {
		// go back to previous state if no enemy in range
		RobotInfo closestEnemy = dc.getClosestEnemy();
		if (closestEnemy == null) {
			currState = RobotState.RUSH;
			return;
		}
		// attack closest enemy
		mi.setObjective(closestEnemy.location);
		mi.setChargeMode();
		mi.attackMove();
		// send dead enemy archon IDs
		sendDeadEnemyArchonIDs();
	}
	
	public void backOff() throws GameActionException {
	}
	
	private void sendDeadEnemyArchonIDs() throws GameActionException {
		if (--timeUntilBroadcast <= 0) {
			io.sendShorts("#xd", enemyArchonInfo.getDeadEnemyArchonIDs());
			timeUntilBroadcast = Constants.SOLDIER_BROADCAST_FREQUENCY;
		}
	}
}
