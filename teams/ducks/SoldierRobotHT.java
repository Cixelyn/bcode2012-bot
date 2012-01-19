package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;

public class SoldierRobotHT extends BaseRobot {
	private enum BehaviorState {
		/** No enemies to deal with. */
		SWARM,
		/** Run away from enemy forces. */
		RETREAT, 
		/** Fight the enemy forces. Micro. */
		SEEK, 
		/** Track enemy's last position and keep following them. */
		TARGET_LOCKED;
	}
	int lockAcquiredRound;
	int closestSenderDist;
	MapLocation target;
	MapLocation previousTarget;
	int potential;
	MapLocation swarmTarget;
	BehaviorState behavior;
	
	public SoldierRobotHT(RobotController myRC) throws GameActionException {
		super(myRC);
		
		lockAcquiredRound = -1;
		closestSenderDist = Integer.MAX_VALUE;
		nav.setNavigationMode(NavigationMode.GREEDY);
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL, 
				BroadcastChannel.SOLDIERS
		});
		fbs.setBattleMode();
		behavior = BehaviorState.SWARM;
	}

	@Override
	public void run() throws GameActionException {
		radar.scan(true, true);
		RobotInfo closestEnemy = radar.closestEnemy;
		if(closestEnemy != null) {
			// Scanned an enemy, lock onto it
			behavior = BehaviorState.TARGET_LOCKED;
			target = closestEnemy.location;
			nav.setNavigationMode(NavigationMode.GREEDY);
			lockAcquiredRound = curRound;
		} else if(behavior != BehaviorState.TARGET_LOCKED || 
				curRound > lockAcquiredRound + 12) {
			if(closestSenderDist == Integer.MAX_VALUE) { 
				// We did not receive any targeting broadcasts from our archons
				behavior = BehaviorState.SWARM;
				target = dc.getClosestArchon();
				
			} else {
				// Follow target of closest archon's broadcast
				behavior = BehaviorState.SEEK;
				target = swarmTarget;
			}
		}
		
		if(curLoc.distanceSquaredTo(target)>49) {
			nav.setNavigationMode(NavigationMode.BUG);
			if(target.distanceSquaredTo(previousTarget)>25)
				nav.setDestination(target);
		} else {
			nav.setNavigationMode(NavigationMode.GREEDY);
			nav.setDestination(target);
		}
		previousTarget = target;
		
		if(!rc.isAttackActive() && closestEnemy != null && 
				rc.canAttackSquare(closestEnemy.location)) {
			rc.attackSquare(closestEnemy.location, closestEnemy.robot.getRobotLevel());
		}
		
		rc.setIndicatorString(0, "Target: <"+(target.x-curLoc.x)+","+(target.y-curLoc.y)+">");
		rc.setIndicatorString(1, "behavior_state="+behavior);
		
		
		closestSenderDist = Integer.MAX_VALUE;
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
		case SWARM_TARGET:
			int[] shorts = BroadcastSystem.decodeUShorts(sb);
			MapLocation senderLoc = new MapLocation(shorts[3], shorts[4]);
			int dist = curLoc.distanceSquaredTo(senderLoc);
			if(dist<closestSenderDist) {
				closestSenderDist = dist;
				swarmTarget = new MapLocation(shorts[1], shorts[2]);
				potential = shorts[0];
			}
			break;
		default:
			super.processMessage(msgType, sb);
		} 
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		if(rc.getFlux()<1) return null;
		
		if(curLoc.equals(target) && 
				rc.senseObjectAtLocation(curLoc, RobotLevel.POWER_NODE)!=null) {
			return new MoveInfo(nav.navigateCompletelyRandomly(), true);
		}
		if(curLoc.distanceSquaredTo(target) <= 2) {
			return new MoveInfo(curLoc.directionTo(target).opposite(), false);
		} else if(curLoc.distanceSquaredTo(target) > 5) {
			Direction dir = nav.navigateToDestination();
			if(dir==null) return null;
			for(int i=0; i<8; i++) {
				MapLocation loc = curLoc.add(dir);
				if(rc.canMove(dir) && !(rc.canSenseSquare(loc) && 
						rc.senseObjectAtLocation(loc, RobotLevel.POWER_NODE)!=null))
					break;
				dir = dir.rotateRight();
			}
			return new MoveInfo(dir, false);
		}
		
		
		
		return new MoveInfo(curLoc.directionTo(target));
	}
}
