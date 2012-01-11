package ducks;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class SoldierRobot extends BaseRobot {
	
	private BlindBug bugNav;
	private Beeline beeNav;
	private MapLocation target;
	private int targetPriority = -1;

	public SoldierRobot(RobotController myRC) {
		super(myRC);
		bugNav = new BlindBug(this);
		beeNav = new Beeline(this, myType.attackRadiusMaxSquared);
		nv = bugNav;
		currState = RobotState.RUSH;
		io.setAddresses(new String[] {"#x"});
	}

	@Override
	public void run() throws GameActionException {
		// show target
		rc.setIndicatorString(
				2, "Target: " + target + ", priority " + targetPriority);
		// TODO(jven): use processMessage
		for (Message m : rc.getAllMessages()) {
			if (m.strings != null && m.strings.length == 1 &&
					m.strings[0] == "sup" && m.ints != null && m.ints.length == 2 &&
					m.ints[1] > targetPriority) {
				target = m.locations[0];
				targetPriority = m.ints[1];
			}
		}
		// power down if not enough flux
		if (currFlux < Constants.POWER_DOWN_FLUX) {
			return;
		}
		switch (currState) {
			case RUSH:
				nv = bugNav;
				rush();
				break;
			case MICRO:
				nv = beeNav;
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
				// TODO(jven): get these from message
				MapLocation msgTarget = null;
				int msgTargetPriority = -1;
				if (msgTargetPriority > targetPriority) {
					target = msgTarget;
					targetPriority = msgTargetPriority;
				}
				break;
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	private void rush() throws GameActionException {
		// see if enemy is in range
		boolean enemySighted = false;
		for (Robot r : dc.getNearbyRobots()) {
			// TODO(jven): consider towers not adjacent to one of ours
			if (r.getTeam() != myTeam) {
				enemySighted = true;
				break;
			}
		}
		// if enemy in range, micro... rush otherwise
		if (enemySighted) {
			currState = RobotState.MICRO;
		} else if (target != null) {
			// TODO(jven): ensure this is non-null elsewhere?
			nv.navigateTo(target);
		}
	}
	
	private void micro() throws GameActionException {
		int closestDistance = Integer.MAX_VALUE;
		RobotInfo closestEnemy = null;
		for (Robot r : dc.getNearbyRobots()) {
			if (r.getTeam() != myTeam) {
				// TODO(jven): don't shoot at towers not adjacent to one of ours
				RobotInfo rInfo = rc.senseRobotInfo(r);
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
		}
		// dance if possible
		nv.navigateTo(closestEnemy.location);
	}

}
