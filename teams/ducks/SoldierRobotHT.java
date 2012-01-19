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
		/** Far from target. Use bug to navigate. */
		LOST,
		/** Track enemy's last position and keep following them. */
		TARGET_LOCKED;
	}
	int lockAcquiredRound;
	MapLocation target;
	MapLocation previousBugTarget;
	boolean swarming;
	int closestSenderDist;
	MapLocation archonTarget;
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
		swarming = true;
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
			int distToClosestArchon = curLoc.distanceSquaredTo(dc.getClosestArchon());
			if(behavior==BehaviorState.LOST && distToClosestArchon>36 || 
					distToClosestArchon>64) {
				// Really far from all allied archons, move to closest one
				behavior = BehaviorState.LOST;
				nav.setNavigationMode(NavigationMode.BUG);
				target = dc.getClosestArchon();
				if(previousBugTarget==null || 
						target.distanceSquaredTo(previousBugTarget)>25)
					nav.setDestination(target);
				previousBugTarget = target;
			} else {	
				if(closestSenderDist == Integer.MAX_VALUE) { 
					// We did not receive any targeting broadcasts from our archons
					behavior = BehaviorState.SWARM;
					target = dc.getClosestArchon();
				} else {
					// Follow target of closest archon's broadcast
					behavior = swarming ? BehaviorState.SWARM : BehaviorState.SEEK;
					target = archonTarget;
				}
				nav.setDestination(target);
				previousBugTarget = null;
			}
		}
		
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
			boolean wantToSwarm = shorts[0]==0;
			if(swarming&&!wantToSwarm || 
					(swarming==wantToSwarm)&&dist<closestSenderDist) {
				closestSenderDist = dist;
				archonTarget = new MapLocation(shorts[1], shorts[2]);
				swarming = wantToSwarm;
			}
			break;
		default:
			super.processMessage(msgType, sb);
		} 
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		if(rc.getFlux()<1) return null;
		int enemiesInFront = radar.numEnemyDisruptors + radar.numEnemySoldiers 
				+ radar.numEnemyScorchers * 2;
		int tooClose = behavior==BehaviorState.TARGET_LOCKED ? 
				(radar.numAllyRobots > enemiesInFront ? -1 : 2) : 1;
		int tooFar = behavior==BehaviorState.TARGET_LOCKED ? 
				(radar.numAllyRobots > enemiesInFront ? 4 : 14) : 14;
		
		if(curLoc.equals(target) && 
				rc.senseObjectAtLocation(curLoc, RobotLevel.POWER_NODE)!=null) {
			return new MoveInfo(nav.navigateCompletelyRandomly(), false);
		}
		if(curLoc.distanceSquaredTo(target) <= tooClose) {
			return new MoveInfo(curLoc.directionTo(target).opposite(), true);
		} else if(curLoc.distanceSquaredTo(target) >= tooFar) {
			Direction dir = nav.navigateToDestination();
			if(behavior == BehaviorState.SWARM) {
				if(radar.alliesInFront==0 && Math.random()<0.75) 
					return null;
				if(radar.alliesInFront > 3 && Math.random()<0.05 * radar.alliesInFront) 
					dir = nav.navigateCompletelyRandomly();
				if(radar.alliesOnLeft > radar.alliesOnRight && Math.random()<0.3) 
					dir = dir.rotateRight();
				else if(radar.alliesOnLeft < radar.alliesOnRight && Math.random()<0.3) 
					dir = dir.rotateLeft();
			}
			return new MoveInfo(dir, false);
		} else {
			if(behavior == BehaviorState.SWARM) {
				if(radar.alliesInFront > 3 && Math.random()<0.05 * radar.alliesInFront) 
					return new MoveInfo(nav.navigateCompletelyRandomly(), false);
			}
		}
		
		
		
		return new MoveInfo(curLoc.directionTo(target));
	}
}
