package ducks;

import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class SoldierRobot extends BaseRobot {

	public SoldierRobot(RobotController myRC) {
		super(myRC);
		nv = new Beeline(this, myType.attackRadiusMaxSquared);
		currState = RobotState.DIZZY;
		io.setAddresses(new String[]{"#s"});
	}

	@Override
	public void run() throws GameActionException {
		switch (currState) {
			case DIZZY:
				dizzy();
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
		
		System.out.println(sb.toString());
		System.out.println(msgType);
		
		switch(msgType) {
		case '0':
			rc.setIndicatorString(2, Radio.decodeMapLoc(sb).toString());
			break;
		default:
			super.processMessage(msgType, sb);
		}
		
	}
	
	private void dizzy() throws GameActionException {
		// if enemy in range, micro... spin otherwise
		boolean enemySighted = false;
		for (Robot r : dc.getNearbyRobots()) {
			// TODO(jven): consider towers not adjacent to one of ours
			if (r.getTeam() != myTeam) {
				enemySighted = true;
				break;
			}
		}
		if (enemySighted) {
			currState = RobotState.MICRO;
		} else if (!rc.isMovementActive()) {
			rc.setDirection(currDir.rotateLeft().rotateLeft().rotateLeft());
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
			currState = RobotState.DIZZY;
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
