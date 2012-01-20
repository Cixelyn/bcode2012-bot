package ducks;

import battlecode.common.*;

public class SoldierRobotCL extends BaseRobot {
	
	
	public static int DISTANCE_UNTIL_LOST = 100;
	
	private enum BehaviorState { HIBERNATE, DEFEND, SEEK, ASSIST, LOST}


	final HibernationSystem hbs;
	BehaviorState behavior;
	
	public SoldierRobotCL(RobotController myRC) throws GameActionException {
		super(myRC);
		
		hbs = new HibernationSystem(this);
		io.addChannel(BroadcastChannel.ALL);
		io.addChannel(BroadcastChannel.SOLDIERS);
		fbs.setBattleMode();
		behavior = BehaviorState.DEFEND;
	}
	
	RobotInfo targetEnemy = null;
	MapLocation defendLoc = null;
	
	@Override
	public void run() throws GameActionException {
		rc.setIndicatorString(0,behavior.toString());
		
		
		
		radar.scan(false, true);
		
		if(justRevived) behavior = BehaviorState.DEFEND;
		
	
		if(behavior == BehaviorState.HIBERNATE) {
			hbs.run();
		}
		
		if(behavior == BehaviorState.DEFEND) {
			nav.setNavigationMode(NavigationMode.GREEDY);
			
			if (radar.closestEnemy != null) {
				behavior = BehaviorState.SEEK;
				targetEnemy = radar.closestEnemy;
			} else {
				if (dc.getClosestArchon() != null) {
					if (dc.getClosestArchon().distanceSquaredTo(curLoc) > DISTANCE_UNTIL_LOST) {
						behavior = BehaviorState.LOST;
					}
				}
				
				if (defendLoc == null && curLoc.distanceSquaredTo(myHome) <=8) {
					behavior = BehaviorState.HIBERNATE;
				}
			} 
		}
		
		if(behavior == BehaviorState.SEEK) {
			
			if(radar.roundsSinceEnemySighted > 10) {
				behavior = BehaviorState.DEFEND;
			}
			
			nav.setNavigationMode(NavigationMode.GREEDY);
			if(rc.canSenseObject((GameObject) targetEnemy.robot)) {
				targetEnemy = rc.senseRobotInfo(targetEnemy.robot);
				if(!rc.isAttackActive() && rc.canAttackSquare(targetEnemy.location)) {
					rc.attackSquare(targetEnemy.location, targetEnemy.robot.getRobotLevel());
				}
			} else {
				behavior = BehaviorState.DEFEND;
			}
		}
		
		if(behavior == BehaviorState.LOST) {
			nav.setNavigationMode(NavigationMode.BUG);
		}
	}
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		switch(behavior) {
		case DEFEND:
			if(defendLoc == null) {
				return new MoveInfo(curLoc.directionTo(myHome), false);
			} else {
				return new MoveInfo(curLoc.directionTo(defendLoc), false);
			}
		case SEEK:
			return new MoveInfo(curLoc.directionTo(targetEnemy.location), false);
		case LOST:
			nav.setDestination(dc.getClosestArchon());
			Direction dir = nav.navigateToDestination();
			if(dir == null)
				return null;
			else if(curLoc.add(dir).equals(nav.getDestination())) 
				return new MoveInfo(dir);
			else
				return new MoveInfo(dir, false);
		default:
			return null;
		}
		
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
		case RALLY:
			if(targetEnemy == null) {
				defendLoc = BroadcastSystem.decodeMapLoc(sb);
				nav.setDestination(defendLoc);
				behavior = BehaviorState.DEFEND;
			}
			break;
		case HIBERNATE:
			behavior = BehaviorState.HIBERNATE;
			break;
		}
	}
	


}
