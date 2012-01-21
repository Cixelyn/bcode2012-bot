package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;

public class SoldierRobot extends BaseRobot {
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
		/** Far from target. Use bug to navigate. */
		LOST,
		/** Track enemy's last position and keep following them. */
		TARGET_LOCKED;
	}
	int lockAcquiredRound;
	MapLocation target;
	MapLocation previousBugTarget;
	int closestSenderDist;
	MapLocation archonTarget;
	BehaviorState behavior;
	MapLocation hibernateTarget;
	
	public SoldierRobot(RobotController myRC) throws GameActionException {
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
	}

	@Override
	public void run() throws GameActionException {
		
		// Scan everything every turn
		radar.scan(true, true);
		radar.broadcastEnemyInfo();
		
		MapLocation closestEnemyLocation = er.getClosestEnemyLocation();
		boolean shouldSetNavTarget = true;
		if(closestEnemyLocation != null) {
			// If we know of an enemy, lock onto it
			behavior = BehaviorState.TARGET_LOCKED;
			target = closestEnemyLocation;
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
				if(behavior == BehaviorState.LOOKING_TO_HIBERNATE && 
						archonTarget.equals(hibernateTarget) && !curLoc.equals(hibernateTarget)) {
					// Hibernate once we're no longer adjacent to any allies
					if(radar.numAdjacentAllies==0) 
						behavior = BehaviorState.HIBERNATE;
				} else if(closestSenderDist == Integer.MAX_VALUE) { 
					// We did not receive any targeting broadcasts from our archons
					behavior = BehaviorState.SWARM;
					target = dc.getClosestArchon();
				} else if(rc.getFlux() > myMaxEnergon*2/3) {
					// Needs to dump flux to archon
					behavior = BehaviorState.POOL;
					target = dc.getClosestArchon();
				} else {
					// Follow target of closest archon's broadcast
					behavior = BehaviorState.SWARM ;
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
		
		// Attack an enemy if there is some unit in our attackable squares
		if(!rc.isAttackActive()) {
			MapLocation bestLoc = null;
			RobotLevel bestLevel = null;
			double bestValue = Double.MAX_VALUE;
			for(int n=0; n<radar.numEnemyRobots; n++) {
				RobotInfo ri = radar.enemyInfos[radar.enemyRobots[n]];
				if(!rc.canAttackSquare(ri.location)) 
					return;
				if((bestValue < myType.attackPower && ri.energon < myType.attackPower) ?
						ri.energon > bestValue : ri.energon < bestValue) {
					// Say a soldier does 6 damage. We prefer hitting units with less energon, but we also would rather hit a unit with 5 energon than a unit with 1 energon.
					bestLoc = ri.location;
					bestLevel = ri.type.level;
					bestValue = ri.energon;
				}
			}
			if(bestLoc!=null)
				rc.attackSquare(bestLoc, bestLevel);
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
			int dist = curLoc.distanceSquaredTo(BroadcastSystem.decodeSenderLoc(sb));
			if(dist<closestSenderDist) {
				closestSenderDist = dist;
				archonTarget = new MapLocation(shorts[1], shorts[2]);
			}
			break;
		case ENEMY_INFO:
			er.integrateEnemyInfo(BroadcastSystem.decodeUShorts(sb));
			break;
		case ENEMY_KILL:
			er.integrateEnemyKill(BroadcastSystem.decodeShort(sb));
			break;
		default:
			super.processMessage(msgType, sb);
		} 
	}
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		if(rc.getFlux()<1) return null;
		
		// If we're looking to hibernate, move around randomly
		if(behavior == BehaviorState.LOOKING_TO_HIBERNATE) {
			return new MoveInfo(nav.navigateCompletelyRandomly(), false);
		}
		
		if(behavior == BehaviorState.SWARM ) {
			// If we're on top of our target power node, move around randomly
			if(curLoc.equals(target) && 
					rc.senseObjectAtLocation(curLoc, RobotLevel.POWER_NODE)!=null) {
				return new MoveInfo(nav.navigateCompletelyRandomly(), false);
			}
			// If we're far from the swarm target, follow normal swarm rules
			if(curLoc.distanceSquaredTo(target) >= 11) {
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
				
			// If we're fairly close, and there's lots of allies around, move randomly
			} else if(curLoc.distanceSquaredTo(target) >= 2) {
				if(radar.alliesInFront > 3 && Math.random()<0.05 * radar.alliesInFront) 
					return new MoveInfo(nav.navigateCompletelyRandomly(), false);
			}
		} else {
			
			// Fighting an enemy, kite target
			boolean weHaveBiggerFront = er.getEnergonDifference(16) > 0;
			int tooClose = behavior==BehaviorState.TARGET_LOCKED ? 
					(weHaveBiggerFront ? -1 : 2) : 1;
			int tooFar = behavior==BehaviorState.TARGET_LOCKED ? 
					(weHaveBiggerFront ? 4 : 14) : 11;
			
			if(curLoc.distanceSquaredTo(target) <= tooClose) {
				return new MoveInfo(curLoc.directionTo(target).opposite(), true);
			} else if(curLoc.distanceSquaredTo(target) >= tooFar) {
				Direction dir = nav.navigateToDestination();
				return new MoveInfo(dir, false);
			}
		}
		
		return new MoveInfo(curLoc.directionTo(target));
	}
}
