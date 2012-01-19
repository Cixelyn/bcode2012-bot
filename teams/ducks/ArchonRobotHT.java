package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ArchonRobotHT extends BaseRobot{
	private enum StrategyState {
		/** Initial split. */
		SPLIT,
		/** Seek and destroy towards a target. */
		RUSH, 
		/** Hold a position. */
		DEFEND,
		/** Take power nodes. */
		CAP;
	}
	private enum BehaviorState {
		/** No enemies around, already at target. */
		WAITING,
		/** No enemies to deal with. */
		SWARM,
		/** Run away from enemy forces. */
		RETREAT, 
		/** Fight the enemy forces. Micro, maybe kite. */
		BATTLE, 
		/** Track enemy's last position and keep following them. */
		CHASE;
	}
	int myArchonID;
	int roundLockTarget;
	MapLocation target;
	StrategyState strategy;
	BehaviorState behavior;
	int closestSenderDist;
	boolean senderSwarming;
	MapLocation closestArchonTarget;
	
	public ArchonRobotHT(RobotController myRC) throws GameActionException {
		super(myRC);
		
		roundLockTarget = -Integer.MAX_VALUE;
		// compute archon ID
		MapLocation[] alliedArchons = this.dc.getAlliedArchons();
		for(int i=alliedArchons.length; --i>=0; ) {
			if(alliedArchons[i].equals(this.curLoc)) {
				myArchonID = i;
				break;
			}
		}
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.ARCHONS,
				BroadcastChannel.EXPLORERS
		});
		fbs.setBattleMode();
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		strategy = StrategyState.SPLIT;
		behavior = BehaviorState.BATTLE;
		senderSwarming = true;
		closestSenderDist = Integer.MAX_VALUE;
	}
	
	@Override
	public void run() throws GameActionException {
		// Currently the strategy transition is based on hard-coded turn numbers
		if(Clock.getRoundNum()>1700) {
			strategy = StrategyState.CAP;
		} else if(Clock.getRoundNum()>1000 || mc.powerNodeGraph.enemyPowerCoreID != 0) {
			strategy = StrategyState.DEFEND;
		} else if(Clock.getRoundNum()>20) {
			strategy = StrategyState.RUSH;
		}
		
		// Scan everything every turn
		radar.scan(true, true);

		// If there is an enemy in sensor range, set target as enemy swarm target
		if(radar.closestEnemy != null) {
			target = radar.getEnemySwarmTarget();
			roundLockTarget = curRound;
			if(curDir == curLoc.directionTo(radar.getEnemySwarmCenter()) &&
					radar.alliesInFront > radar.numEnemyRobots - radar.numEnemyArchons)
				behavior = BehaviorState.CHASE;
			else
				behavior = BehaviorState.BATTLE;
		}
		
		// If we haven't seen anyone for 30 turns, go to swarm mode and reset target
		if(curRound > roundLockTarget + 30) {
			behavior = BehaviorState.SWARM;
			if(closestSenderDist != Integer.MAX_VALUE && !senderSwarming) {
				target = closestArchonTarget;
			} else if(strategy == StrategyState.DEFEND) {
				target = myHome;
			} else if(strategy == StrategyState.RUSH) {
				target = mc.guessEnemyPowerCoreLocation();
			} else {
				target = mc.guessBestPowerNodeToCapture();
			}
		}
		nav.setDestination(target);
		
		// Broadcast my target info to the soldier swarm
		int[] shorts = new int[5];
		shorts[0] = (behavior == BehaviorState.SWARM) ? 0 : 1;
		shorts[1] = target.x;
		shorts[2] = target.y;
		shorts[3] = curLoc.x;
		shorts[4] = curLoc.y;
		io.sendUShorts(BroadcastChannel.ALL, BroadcastType.SWARM_TARGET, shorts);
		
		rc.setIndicatorString(0, "Target: <"+(target.x-curLoc.x)+","+(target.y-curLoc.y)+">");
		rc.setIndicatorString(1, "strategy_state="+strategy+", behavior_state="+behavior);
	
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
			if(senderSwarming&&!wantToSwarm || 
					(senderSwarming==wantToSwarm)&&dist<closestSenderDist) {
				closestSenderDist = dist;
				closestArchonTarget = new MapLocation(shorts[1], shorts[2]);
				senderSwarming = wantToSwarm;
			}
			break;
		case MAP_EDGES:
			ses.receiveMapEdges(BroadcastSystem.decodeUShorts(sb));
			break;
		case MAP_FRAGMENTS:
			ses.receiveMapFragment(BroadcastSystem.decodeInts(sb));
			break;
		case POWERNODE_FRAGMENTS:
			ses.receivePowerNodeFragment(BroadcastSystem.decodeInts(sb));
			break;
		default:
			super.processMessage(msgType, sb);
		} 
	}
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		int fluxToMakeSoldierAt = (strategy==StrategyState.CAP) ? 
				((behavior==BehaviorState.SWARM) ? 301 : 210) : 150;
		
		if(strategy == StrategyState.SPLIT) {
			return new MoveInfo(curLoc.directionTo(myHome).opposite(), false);
		}
		
		if(radar.closestEnemyDist <= 20 && behavior != BehaviorState.CHASE) {
			return new MoveInfo(curLoc.directionTo(radar.getEnemySwarmCenter()).opposite(), true);
		}
		
		if(dc.getClosestArchon()!=null) {
			int distToNearestArchon = curLoc.distanceSquaredTo(dc.getClosestArchon());
			if(distToNearestArchon <= 25 && Math.random() < 0.6-Math.sqrt(distToNearestArchon)/10) 
				return new MoveInfo(curLoc.directionTo(dc.getClosestArchon()).opposite(), false);
		}
		
		if(behavior == BehaviorState.SWARM && radar.alliesInFront==0 && Math.random()<0.9)
			return null;
		
		if(strategy == StrategyState.CAP && 
				rc.canMove(curDir) && 
				curLocInFront.equals(target) && 
				mc.isPowerNode(curLocInFront)) {
			if(rc.getFlux() > 200) 
				return new MoveInfo(RobotType.TOWER, curDir);
			
		} else if(strategy == StrategyState.CAP && 
				curLoc.equals(target) && mc.isPowerNode(curLoc)) {
			return new MoveInfo(nav.navigateCompletelyRandomly(), true);
			
		} else if(rc.getFlux() > fluxToMakeSoldierAt) {
			if(rc.canMove(curDir)) 
				return new MoveInfo(RobotType.SOLDIER, curDir);
			else 
				return new MoveInfo(curDir.rotateLeft());
			
		} else {
			Direction dir = nav.navigateToDestination();
			if(dir==null) 
				return null;
			else if(curLoc.add(dir).equals(nav.getDestination()))
				return new MoveInfo(dir);
			else 
				return new MoveInfo(dir, false);
			
		}
		return null;
	}
	
	@Override
	public void useExtraBytecodes() {
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3000)
			nav.prepare(); 
		if(Clock.getRoundNum()%6==myArchonID) {
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>4000)
				ses.broadcastMapFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>2000)
				ses.broadcastPowerNodeFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1000) 
				ses.broadcastMapEdges();
		}
		super.useExtraBytecodes();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3000) 
			nav.prepare();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1000) 
			mc.extractUpdatedPackedDataStep();
	}
}
