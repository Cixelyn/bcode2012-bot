package normalai;

import battlecode.common.*;

public class SoldierRobotCL extends BaseRobot {
	
	
	public static int DISTANCE_UNTIL_LOST = 100;
	
	private enum BehaviorState { HIBERNATE, RUSH, SEEK, LOST}


	final HibernationSystem hbs;
	BehaviorState behavior;
	
	public SoldierRobotCL(RobotController myRC) throws GameActionException {
		super(myRC);
		
		hbs = new HibernationSystem(this);
		io.addChannel(BroadcastChannel.ALL);
		io.addChannel(BroadcastChannel.SOLDIERS);
		fbs.setBattleMode();
		behavior = BehaviorState.RUSH;
		
		
	}
	
	MapLocation rushLoc = curLoc;
	RobotInfo targetEnemy = null;
	
	@Override
	public void run() throws GameActionException {
		rc.setIndicatorString(0,behavior.toString());
		
		if(justRevived) behavior = BehaviorState.RUSH;
		
		
		switch(behavior) {
		case HIBERNATE:
			hbs.run();
			break;
		case RUSH:
			nav.setNavigationMode(NavigationMode.GREEDY);
			radar.scan(false, true);

			if (dc.getClosestArchon() != null) {
				if (dc.getClosestArchon().distanceSquaredTo(curLoc) > DISTANCE_UNTIL_LOST) {
					behavior = BehaviorState.LOST;
				}
			}
			
			if(radar.closestEnemy == null) break;
			behavior = BehaviorState.SEEK;
			targetEnemy = radar.closestEnemy;
			//intentional fallthrough
		case SEEK:
			if(radar.roundsSinceEnemySighted > 10) {
				behavior = BehaviorState.LOST;
			}
			nav.setNavigationMode(NavigationMode.GREEDY);
			if(rc.canSenseObject((GameObject) targetEnemy.robot)) {
				targetEnemy = rc.senseRobotInfo(targetEnemy.robot);
				if(!rc.isAttackActive() && rc.canAttackSquare(targetEnemy.location)) {
					rc.attackSquare(targetEnemy.location, targetEnemy.robot.getRobotLevel());
				}
			} else {
				behavior = BehaviorState.RUSH;
			}
			break;
		case LOST:
			nav.setNavigationMode(NavigationMode.BUG);
			break;
		}
	}
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		switch(behavior) {
		case RUSH:
			return new MoveInfo(curLoc.directionTo(rushLoc), false);
		case SEEK:
			return new MoveInfo(curLoc.directionTo(targetEnemy.location), false);
		case LOST:
		default:
			nav.setDestination(dc.getClosestArchon());
			Direction dir = nav.navigateToDestination();
			if(dir == null)
				return null;
			else if(curLoc.add(dir).equals(nav.getDestination())) 
				return new MoveInfo(dir);
			else
				return new MoveInfo(dir, false);
		}
		
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
		case RALLY:
			rushLoc = BroadcastSystem.decodeMapLoc(sb);
			nav.setDestination(rushLoc);
			behavior = BehaviorState.RUSH;
			break;
		case HIBERNATE:
			behavior = BehaviorState.HIBERNATE;
			break;
		}
	}
	


}
