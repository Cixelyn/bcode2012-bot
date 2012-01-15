package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class SoldierRobot extends BaseRobot {
	
	private int timeUntilBroadcast;
	private MapLocation target;
	private int targetPriority = -1;

	public SoldierRobot(RobotController myRC) {
		super(myRC);
		nv = new BlindBug(this);
		timeUntilBroadcast = Constants.SOLDIER_BROADCAST_FREQUENCY;
		target = myRC.getLocation().add(Constants.INITIAL_BEARING,
				GameConstants.MAP_MAX_HEIGHT + GameConstants.MAP_MAX_WIDTH);
		currState = RobotState.RUSH;
		io.setAddresses(new String[] {"#x", "#s"});
	}

	@Override
	public void run() throws GameActionException {
		// power down if not enough flux
		if (currFlux < Constants.POWER_DOWN_FLUX) {
			return;
		}
		switch (currState) {
			case RUSH:
				rush();
				break;
			case MICRO:
				micro();
				break;
			default:
				break;
		}
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb) {
		switch(msgType) {
			case 'r':
				int[] msgInts = Radio.decodeShorts(sb);
				int msgTargetPriority = msgInts[0];
				MapLocation msgTarget = new MapLocation(msgInts[1], msgInts[2]);
				if (msgTargetPriority > targetPriority) {
					target = msgTarget;
					targetPriority = msgTargetPriority;
				}
				break;
			case 'd':
				int[] deadEnemyArchonIDs = Radio.decodeShorts(sb);
				for (int id : deadEnemyArchonIDs) {
					enemyArchonInfo.reportEnemyArchonKill(id);
				}
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	private void rush() throws GameActionException {
		// if enemy is in range, micro
		for (Robot r : dc.getNearbyRobots()) {
			if (r.getTeam() != myTeam) {
				RobotInfo rInfo = rc.senseRobotInfo(r);
				if (rInfo.type == RobotType.TOWER && !isTowerTargetable(rInfo)) {
					continue;
				}
				currState = RobotState.MICRO;
				micro();
				return;
			}
		}
		if (!rc.isMovementActive()) {
			// if closest archon is too far, regroup to it, otherwise go to target
			int closestDistance = Integer.MAX_VALUE;
			MapLocation closestArchon = currLoc;
			for (MapLocation archon : dc.getAlliedArchons()) {
				int distance = currLoc.distanceSquaredTo(archon);
				if (distance < closestDistance) {
					closestDistance = distance;
					closestArchon = archon;
				}
			}
			
			
			Direction dir;
			if (closestArchon.distanceSquaredTo(currLoc)<2)
			{
				dir = nv.getOutOfCurrentSquare();
			} else if (closestDistance > Constants.MAX_SWARM_RADIUS) {
				dir = nv.navigateTo(closestArchon);
			} else if (rc.senseObjectAtLocation(currLoc, RobotLevel.POWER_NODE)!=null)
			{
				dir = nv.getOutOfCurrentSquare();
			} else {
				dir = ((BlindBug) nv).navigateToDirection(currLoc.directionTo(target));
			}
			if (dir != null && dir.ordinal()<8) {
				if (currDir != dir) {
					rc.setDirection(dir);
				} else {
					if (rc.canMove(currDir)) {
						rc.moveForward();
					}
				}
			}
		}
		// broadcast message if necessary
		if (--timeUntilBroadcast <= 0) {
			sendSoldierMessage();
			timeUntilBroadcast = Constants.SOLDIER_BROADCAST_FREQUENCY;
		}
	}
	
	private void micro() throws GameActionException {
		int closestDistance = Integer.MAX_VALUE;
		RobotInfo closestEnemy = null;
		// TODO(jven): prioritize archons?
		for (Robot r : dc.getNearbyRobots()) {
			if (r.getTeam() != myTeam) {
				RobotInfo rInfo = rc.senseRobotInfo(r);
				// don't overkill
				if (rInfo.energon <= 0) {
					continue;
				}
				// don't shoot at towers you can't hurt
				if (rInfo.type == RobotType.TOWER && !isTowerTargetable(rInfo)) {
					continue;
				}
				int distance = currLoc.distanceSquaredTo(rInfo.location);
				if (distance < closestDistance) {
					closestDistance = distance;
					closestEnemy = rInfo;
				}
			}
		}
		// change state and return if no enemy in sight
		if (closestEnemy == null) {
			currState = RobotState.RUSH;
			return;
		}
		// shoot if possible
		if (!rc.isAttackActive() && rc.canAttackSquare(closestEnemy.location)) {
			rc.attackSquare(
					closestEnemy.location, closestEnemy.robot.getRobotLevel());
			if (closestEnemy.type == RobotType.ARCHON &&
					closestEnemy.energon <= myType.attackPower) {
				enemyArchonInfo.reportEnemyArchonKill(closestEnemy.robot.getID());
			}
		}
		// charrrrge
		if (rc.isMovementActive()) {
			return;
		}
		Direction dir = currLoc.directionTo(closestEnemy.location);
		if (dir != Direction.OMNI) {
			if (currDir != dir) {
				rc.setDirection(dir);
			} else {
				if (rc.canMove(currDir)){
					rc.moveForward();
				}
			}
		}
	}
	
	private void sendSoldierMessage() throws GameActionException {
		io.sendShorts("#xd", enemyArchonInfo.getDeadEnemyArchonIDs());
	}

	private boolean isTowerTargetable(
			RobotInfo tower) throws GameActionException {
		// don't shoot at enemy towers not connected to one of ours
		PowerNode pn = (PowerNode)rc.senseObjectAtLocation(
				tower.location, RobotLevel.POWER_NODE);
		if (pn == null) {
			return false;
		}
		for (PowerNode myPN : dc.getAlliedPowerNodes()) {
			if (!rc.senseConnected(myPN)) {
				continue;
			}
			for (MapLocation loc : pn.neighbors()) {
				if (myPN.getLocation().equals(loc)) {
					return true;
				}
			}
		}
		return false;
	}
}
