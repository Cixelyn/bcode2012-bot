package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;

public class SoldierRobotHT extends BaseRobot {
	private enum BehaviorState {
		/** Want to hibernate, need to find a non-blocking place to do it. */
		LOOKING_TO_HIBERNATE,
		/** Hibernate until someone wakes it up. */
		HIBERNATE,
		/** Has too much flux (from being a battery), needs to give it back to archon. */
		POOL,
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
	boolean senderSwarming;
	int closestSenderDist;
	MapLocation archonTarget;
	BehaviorState behavior;
	MapLocation hibernateTarget;
	
	public SoldierRobotHT(RobotController myRC) throws GameActionException {
		super(myRC);
		
		lockAcquiredRound = -1;
		closestSenderDist = Integer.MAX_VALUE;
		nav.setNavigationMode(NavigationMode.GREEDY);
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL, 
				BroadcastChannel.SOLDIERS,
				BroadcastChannel.EXTENDED_RADAR,
		});
		fbs.setPoolMode();
		behavior = BehaviorState.SWARM;
		senderSwarming = true;
	}

	@Override
	public void run() throws GameActionException {
		
		// Scan everything every turn
		radar.scan(true, true);
		
		RobotInfo closestEnemy = radar.closestEnemy;
		boolean shouldSetNavTarget = true;
		if(closestEnemy != null) {
			// If we scanned an enemy, lock onto it
			behavior = BehaviorState.TARGET_LOCKED;
			target = closestEnemy.location;
			nav.setNavigationMode(NavigationMode.GREEDY);
			lockAcquiredRound = curRound;
		} else if(behavior != BehaviorState.TARGET_LOCKED || 
				curRound > lockAcquiredRound + 12) {
			int distToClosestArchon = curLoc.distanceSquaredTo(dc.getClosestArchon());
			if(behavior==BehaviorState.LOST && distToClosestArchon>25 || 
					distToClosestArchon>64) {
				// If all allied archons are far away, move to closest one
				behavior = BehaviorState.LOST;
				nav.setNavigationMode(NavigationMode.BUG);
				target = dc.getClosestArchon();
				if(previousBugTarget!=null && target.distanceSquaredTo(previousBugTarget)<=25)
					shouldSetNavTarget = false;
				previousBugTarget = target;
			} else {	
				if(behavior == BehaviorState.LOOKING_TO_HIBERNATE && senderSwarming && 
						archonTarget.equals(hibernateTarget) && !curLoc.equals(hibernateTarget)) {
					// Hibernate once we're no longer adjacent to any allies
					if(radar.numAdjacentAllies==0) 
						behavior = BehaviorState.HIBERNATE;
				} else if(closestSenderDist == Integer.MAX_VALUE) { 
					// We did not receive any targeting broadcasts from our archons
					behavior = BehaviorState.SWARM;
					target = dc.getClosestArchon();
				} else if(!senderSwarming && rc.getFlux() > myMaxFlux*2/3) {
					// Would be seeking enemy, but needs to dump flux first
					behavior = BehaviorState.POOL;
					target = dc.getClosestArchon();
				} else {
					// Follow target of closest archon's broadcast
					behavior = senderSwarming ? BehaviorState.SWARM : BehaviorState.SEEK;
					target = archonTarget;
				}
				
				if(behavior == BehaviorState.SWARM && 
						closestSenderDist <= 10 && 
						curLoc.distanceSquaredTo(target) <= 10) { 
					// Close enough to swarm target, look for a place to hibernate
					behavior = BehaviorState.LOOKING_TO_HIBERNATE;
					hibernateTarget = target;
				} 
				nav.setNavigationMode(NavigationMode.GREEDY);
				previousBugTarget = null;
			}
			
			
		}
		if(shouldSetNavTarget)
			nav.setDestination(target);
		
		// Attack an enemy if we can
		if(!rc.isAttackActive() && closestEnemy != null && 
				rc.canAttackSquare(closestEnemy.location)) {
			rc.attackSquare(closestEnemy.location, closestEnemy.robot.getRobotLevel());
		}
		
		// Set the flux balance mode
		if(behavior == BehaviorState.SWARM)
			fbs.setBatteryMode();
		else
			fbs.setPoolMode();
		
		// Set debug string
		rc.setIndicatorString(1, "Target=<"+(target.x-curLoc.x)+","+(target.y-curLoc.y)+">, Behavior="+behavior);
		
		// Reset messaging variables
		closestSenderDist = Integer.MAX_VALUE;
		senderSwarming = true;
		
		// Enter hibernation if desired
		if(behavior == BehaviorState.HIBERNATE) {
			HibernationSystem hsys = new HibernationSystem(this);
			hsys.run();
		}
		
			
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
		case SWARM_TARGET:
			int[] shorts = BroadcastSystem.decodeUShorts(sb);
			MapLocation senderLoc = new MapLocation(shorts[3], shorts[4]);
			int dist = curLoc.distanceSquaredTo(senderLoc);
			boolean wantToSwarm = shorts[0]==0;
			if(senderSwarming&&!wantToSwarm || 
					(senderSwarming==wantToSwarm)&&dist<closestSenderDist) {
				closestSenderDist = dist;
				archonTarget = new MapLocation(shorts[1], shorts[2]);
				senderSwarming = wantToSwarm;
			}
			break;
		default:
			super.processMessage(msgType, sb);
		} 
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		if(rc.getFlux()<1) return null;
		if(behavior == BehaviorState.LOOKING_TO_HIBERNATE) {
			return new MoveInfo(nav.navigateCompletelyRandomly(), false);
		}
		int enemiesInFront = radar.numEnemyDisruptors + radar.numEnemySoldiers 
				+ radar.numEnemyScorchers * 2;
		int tooClose = behavior==BehaviorState.TARGET_LOCKED ? 
				(radar.numAllyRobots >= enemiesInFront ? -1 : 2) : 1;
		int tooFar = behavior==BehaviorState.TARGET_LOCKED ? 
				(radar.numAllyRobots >= enemiesInFront ? 4 : 14) : 11;
		
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
