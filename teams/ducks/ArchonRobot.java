package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

public class ArchonRobot extends BaseRobot{
	private enum StrategyState {
		/** Initial split. */
		SPLIT,
		/** Seek and destroy towards a target. */
		RUSH, 
		/** Hold a position. */
		DEFEND,
		/** Take power nodes. */
		CAP,
		
		
	}
	private enum BehaviorState {
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
	int roundStartWakeupMode;
	MapLocation target;
	Direction targetDir;
	StrategyState strategy;
	BehaviorState behavior;
	int closestSenderDist;
	boolean senderSwarming;
	MapLocation closestArchonTarget;
	MapLocation previousWakeupTarget;
	
	public ArchonRobot(RobotController myRC) throws GameActionException {
		super(myRC);
		
		roundLockTarget = -Integer.MAX_VALUE;
		// compute archon ID
		MapLocation[] alliedArchons = dc.getAlliedArchons();
		for(int i=alliedArchons.length; --i>=0; ) {
			if(alliedArchons[i].equals(curLoc)) {
				myArchonID = i;
				break;
			}
		}
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.ARCHONS,
				BroadcastChannel.EXPLORERS
		});
		fbs.setPoolMode();
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		strategy = StrategyState.SPLIT;
		behavior = BehaviorState.BATTLE;
		senderSwarming = true;
		closestSenderDist = Integer.MAX_VALUE;
	}
	
	@Override
	public void run() throws GameActionException {
		// Currently the strategy transition is based on hard-coded turn numbers
		if(Clock.getRoundNum()>1700 && myArchonID!=0) {
			strategy = StrategyState.CAP;
		} else if(Clock.getRoundNum()>1000 || mc.powerNodeGraph.enemyPowerCoreID != 0) {
			strategy = StrategyState.DEFEND;
		} else if(Clock.getRoundNum()>20) {
			strategy = StrategyState.RUSH;
		}
		
		// Scan everything every turn
		radar.scan(true, true);
		
//		if (behavior == BehaviorState.RETREAT)
//		{
//			if (radar.alliesInFront - radar.numEnemyRobots+radar.numEnemyArchons == 0)
//				roundLockTarget = 0;
//		}
		
		// If there is an enemy in sensor range, set target as enemy swarm target
		if(radar.closestEnemy != null) {
//			target = radar.getEnemySwarmTarget();
			roundLockTarget = curRound;
			if (radar.getArmyDifference() < -2 ||
					(radar.alliesInFront==0 && radar.numEnemyRobots-radar.numEnemyArchons>0))
			{
				behavior = BehaviorState.RETREAT;
				computeRetreatTarget();
			} else if(curDir == curLoc.directionTo(radar.getEnemySwarmCenter()) &&
					radar.alliesInFront > radar.numEnemyRobots - radar.numEnemyArchons)
			{
				behavior = BehaviorState.CHASE;
				computeChaseTarget();
			} else
			{
				behavior = BehaviorState.BATTLE;
				computeBattleTarget();
			}
		}
		
		// If we haven't seen anyone for 30 turns, go to swarm mode and reset target
		else if(curRound > roundLockTarget + 30 || targetDir==null) {
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
		
		// otherwise, we should update the target based on the prevoius target direction
		// if we are chasing or retreating
		else
		{
			switch (behavior)
			{
			case CHASE: updateChaseTarget(); break;
			case RETREAT: updateRetreatTarget(); break;
			}
		}
		
		// If we change to a new target, wake up hibernating allies
		if(previousWakeupTarget == null ||
				target.distanceSquaredTo(previousWakeupTarget) > 25 ||
				behavior != BehaviorState.SWARM) {
			roundStartWakeupMode = curRound;
			previousWakeupTarget = target;
		}
		if(curRound < roundStartWakeupMode + 10) {
			io.sendWakeupCall();
		}
			
		// Set the target for the navigator 
		nav.setDestination(target);
		
		// Set the flux balance mode
		if(behavior == BehaviorState.SWARM)
			fbs.setBatteryMode();
		else
			fbs.setPoolMode();
		
		if (behavior == BehaviorState.RETREAT)
		{
			// Broadcast my target info to the soldier swarm
			int[] shorts = new int[3];
			shorts[0] = (behavior == BehaviorState.SWARM) ? 0 : 1;
			shorts[1] = curLoc.x;
			shorts[2] = curLoc.y;
			io.sendUShorts(BroadcastChannel.ALL, BroadcastType.SWARM_TARGET, shorts);
		} else
		{
			// Broadcast my target info to the soldier swarm
			int[] shorts = new int[3];
			shorts[0] = (behavior == BehaviorState.SWARM) ? 0 : 1;
			shorts[1] = target.x;
			shorts[2] = target.y;
			io.sendUShorts(BroadcastChannel.ALL, BroadcastType.SWARM_TARGET, shorts);
			rc.setIndicatorString(1, "Target= <"+(target.x-curLoc.x)+","+(target.y-curLoc.y)+">, Strategy="+strategy+", Behavior="+behavior);
		}
		
		
		
		
	
		closestSenderDist = Integer.MAX_VALUE;
	}
	
	private void computeChaseTarget()
	{
		target = radar.getEnemySwarmTarget();
		targetDir = curLoc.directionTo(target);
	}
	
	private void updateChaseTarget()
	{
		if (curLoc.distanceSquaredTo(target) < 10)
			target = curLoc.add(targetDir,5);
	}
	
	private void computeRetreatTarget()
	{
		int[] closest_in_dir = radar.closestInDir;
		
		String dir = ""	+(closest_in_dir[0]==99?"o":"x")
						+(closest_in_dir[1]==99?"o":"x")
						+(closest_in_dir[2]==99?"o":"x")
						+(closest_in_dir[3]==99?"o":"x")
						+(closest_in_dir[4]==99?"o":"x")
						+(closest_in_dir[5]==99?"o":"x")
						+(closest_in_dir[6]==99?"o":"x")
						+(closest_in_dir[7]==99?"o":"x");
		dir = dir+dir;
		int index;
		rc.setIndicatorString(1, "Target= <"+(target.x-curLoc.x)+","+(target.y-curLoc.y)+">, Strategy="+strategy+", Behavior="+behavior+" "+dir);
		index = dir.indexOf("ooooooo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+3)%8];
			target = curLoc.add(targetDir,5);
			return;
		}
		
		index = dir.indexOf("oooooo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+3)%8];
			target = curLoc.add(targetDir,5);
			return;
		}
		
		index = dir.indexOf("ooooo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+2)%8];
			target = curLoc.add(targetDir,5);
			return;
		}
		
		index = dir.indexOf("oooo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+2)%8];
			target = curLoc.add(targetDir,5);
			return;
		}
		
		index = dir.indexOf("ooo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+1)%8];
			target = curLoc.add(targetDir,5);
			return;
		}
		
		index = dir.indexOf("oo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+1)%8];
			target = curLoc.add(targetDir,5);
			return;
		}
		
		index = dir.indexOf("o");
		if (index>-1)
		{
			targetDir = Constants.directions[(index)%8];
			target = curLoc.add(targetDir,5);
			return;
		}
		
		System.out.println("GONNTA GET GEE'D");
		target = radar.getEnemySwarmTarget();
		targetDir = target.directionTo(curLoc);
		target = curLoc.add(targetDir,5);
		
	}
	
	private void updateRetreatTarget()
	{
		if (curLoc.distanceSquaredTo(target) < 10)
			target = curLoc.add(targetDir,5);
	}
	
	private void computeBattleTarget()
	{
		target = radar.getEnemySwarmTarget();
		targetDir = curLoc.directionTo(target);
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
		case SWARM_TARGET:
			int[] shorts = BroadcastSystem.decodeUShorts(sb);
			MapLocation senderLoc = BroadcastSystem.decodeSenderLoc(sb);
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
		int fluxToMakeSoldierAt;
		
		if (behavior==BehaviorState.RETREAT)
		{
			fluxToMakeSoldierAt = 130;
			
			if(rc.getFlux() > fluxToMakeSoldierAt)
				if(rc.canMove(curDir)) 
					return new MoveInfo(RobotType.SOLDIER, curDir);
			
			Direction dir = nav.navigateToDestination();
			if(dir==null) 
				return null;
			else 
				return new MoveInfo(dir, true);
		}
		
		
		switch (behavior)
		{
		case SWARM: fluxToMakeSoldierAt = 280; break;
		case RETREAT: fluxToMakeSoldierAt = 130; break;
		default:
			switch (strategy)
			{
			case CAP: fluxToMakeSoldierAt = 225; break;
			default: fluxToMakeSoldierAt = 150; break;
			}
			break;
		}
		
		if(strategy == StrategyState.SPLIT) {
			return new MoveInfo(curLoc.directionTo(myHome).opposite(), false);
		}
		
		if(rc.getFlux() > fluxToMakeSoldierAt)
			if(rc.canMove(curDir)) 
				return new MoveInfo(RobotType.SOLDIER, curDir);
			
		
		if(radar.closestEnemyDist <= 20 && behavior != BehaviorState.CHASE) {
			return new MoveInfo(curLoc.directionTo(radar.getEnemySwarmCenter()).opposite(), true);
		}
		
		if(dc.getClosestArchon()!=null) {
			int distToNearestArchon = curLoc.distanceSquaredTo(dc.getClosestArchon());
			if(distToNearestArchon <= 36 &&
					!(strategy==StrategyState.CAP && curLoc.distanceSquaredTo(target)<=36 && rc.senseObjectAtLocation(dc.getClosestArchon(), RobotLevel.ON_GROUND).getID() > myID) && 
					Math.random() < 0.75-Math.sqrt(distToNearestArchon)/10) {
				return new MoveInfo(curLoc.directionTo(dc.getClosestArchon()).opposite(), false);
			}
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
	public void useExtraBytecodes() throws GameActionException {
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>5000)
			nav.prepare(); 
		if(Clock.getRoundNum()%6==myArchonID) {
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>5000)
				ses.broadcastMapFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3000)
				ses.broadcastPowerNodeFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>2000) 
				ses.broadcastMapEdges();
		}
		super.useExtraBytecodes();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3000) 
			nav.prepare();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1000) 
			mc.extractUpdatedPackedDataStep();
	}
}
