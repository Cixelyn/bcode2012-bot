package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
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
		CHASE,
	}
	int myArchonID;
	
	/** round we are releasing our lock */
	int stayTargetLockedUntilRound;
	int roundStartWakeupMode;
	MapLocation target;
	Direction targetDir;
	StrategyState strategy;
	BehaviorState behavior;
	MapLocation previousWakeupTarget;
	MapLocation enemySpottedTarget;
	int enemySpottedRound;
	
	static final int RETREAT_RADIUS = 5;
	static final int CHASE_COMPUTE_RADIUS = 7;
	static final int TURNS_TO_LOCK_ONTO_AN_ENEMY = 30;
	MapLocation lastPowerNodeGuess;
	
	public ArchonRobot(RobotController myRC) throws GameActionException {
		super(myRC);
		
		stayTargetLockedUntilRound = -Integer.MAX_VALUE;
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
		enemySpottedRound = -55555;
		enemySpottedTarget = null;
		lastPowerNodeGuess = null;
	}
	
	@Override
	public void run() throws GameActionException {
		
		// Currently the strategy transition is based on hard-coded turn numbers
		if(Clock.getRoundNum()>2800) {
			strategy = StrategyState.CAP;
		} else if(Clock.getRoundNum()>1700 && myArchonID!=0) {
			strategy = StrategyState.CAP;
		} else if(Clock.getRoundNum()>1000 || 
				(mc.powerNodeGraph.enemyPowerCoreID != 0 && enemySpottedTarget == null)) {
			strategy = StrategyState.DEFEND;
			enemySpottedTarget = null;
		} else if(Clock.getRoundNum()>20) {
			strategy = StrategyState.RUSH;
		}
		
		dbg.setIndicatorString('h', 0, enemySpottedTarget+"");
		
		// If insufficiently prepared, prepare
		if(nav.getTurnsPrepared() < TangentBug.DEFAULT_MIN_PREP_TURNS)
			nav.prepare();
		
		// Scan everything every turn
		radar.scan(true, true);
		
		// Broadcast enemy info every 3 turns
		if(curRound%3 == myArchonID%3)
			radar.broadcastEnemyInfo(false);
		
		if (behavior == BehaviorState.RETREAT && radar.getArmyDifference() > 2)
			stayTargetLockedUntilRound = 0;
		
		// If there is an enemy in sensor range, set target as enemy swarm target
		if(radar.closestEnemy != null) {
			enemySpottedRound = curRound;
			enemySpottedTarget = radar.closestEnemy.location;
			stayTargetLockedUntilRound = curRound + TURNS_TO_LOCK_ONTO_AN_ENEMY;
			if (radar.getArmyDifference() < -2 || (radar.alliesInFront==0 && 
					radar.numEnemyRobots-radar.numEnemyArchons>0)) {
				stayTargetLockedUntilRound = curRound+30;
				behavior = BehaviorState.RETREAT;
				String ret = computeRetreatTarget();
				dbg.setIndicatorString('e',1, "Target= "+locationToVectorString(target)+", Strategy="+strategy+", Behavior="+behavior+" "+ret);
				
			} else if(curDir == curLoc.directionTo(radar.getEnemySwarmCenter()) &&
					radar.alliesInFront > radar.numEnemyRobots - radar.numEnemyArchons) {
				behavior = BehaviorState.CHASE;
				computeChaseTarget();
			} else {
				behavior = BehaviorState.BATTLE;
				computeBattleTarget();
			}
		
		// we should update the target based on the previous target direction if we are chasing or retreating
		} else if(curRound <= stayTargetLockedUntilRound && targetDir!=null) {
			switch (behavior)
			{
			case CHASE: updateChaseTarget(); break;
			case RETREAT: updateRetreatTarget(); break;
			}
			
		// If someone else told us of an enemy spotting, go to that location
		} else if(strategy != StrategyState.DEFEND && enemySpottedTarget != null) {
			behavior = BehaviorState.SWARM;
			target = enemySpottedTarget;
			if(curLoc.distanceSquaredTo(enemySpottedTarget) <= 16) {
				enemySpottedTarget = null;
			}
			
		// If we haven't seen anyone for a while, go back to swarm mode and reset target
		} else {
			behavior = BehaviorState.SWARM;
			if(strategy == StrategyState.DEFEND) {
				target = myHome;
			} else if(strategy == StrategyState.RUSH) {
				computeExploreTarget();
			} else {
				target = mc.guessBestPowerNodeToCapture();
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
		if(behavior == BehaviorState.SWARM && enemySpottedTarget == null)
			fbs.setBatteryMode();
		else
			fbs.setPoolMode();
		
		// Broadcast stuff
		if (behavior == BehaviorState.CHASE) {
			MapLocation tar = radar.getEnemySwarmTarget();
			// Broadcast my target info to the soldier swarm
			int[] shorts = new int[3];
			shorts[0] = 1;
			shorts[1] = tar.x;
			shorts[2] = tar.y;
			io.sendUShorts(BroadcastChannel.ALL, BroadcastType.SWARM_TARGET, shorts);
		} else {
			// Broadcast my target info to the soldier swarm
			int[] shorts = new int[3];
			shorts[0] = (behavior == BehaviorState.RETREAT) ? 0 : 1;
			shorts[1] = target.x;
			shorts[2] = target.y;
			io.sendUShorts(BroadcastChannel.ALL, BroadcastType.SWARM_TARGET, shorts);
		}
		
		// Broadcast a possibly out of date enemy sighting every 20 turns
		if(enemySpottedTarget != null && curRound%20 == myArchonID*3) {
			int[] shorts = new int[3];
			shorts[0] = enemySpottedRound;
			shorts[1] = enemySpottedTarget.x;
			shorts[2] = enemySpottedTarget.y;
			io.sendUShorts(BroadcastChannel.ALL, BroadcastType.ENEMY_SPOTTED, shorts);
		}
		
		// Set debug string
		if (behavior != BehaviorState.RETREAT)
			dbg.setIndicatorString('h',1, "Target= "+locationToVectorString(target)+", Strategy="+strategy+", Behavior="+behavior);
		
	}
	
	private void computeChaseTarget()
	{
		lastPowerNodeGuess = null;
		target = radar.getEnemySwarmTarget();
		targetDir = curLoc.directionTo(target);
		
//		TODO not doing anything special right now about edges or corners
////		now, deal with when we are close to map boundaries
//		if (mc.edgeXMax!=0 && mc.cacheToWorldX(mc.edgeXMax) < curLoc.x+CHASE_COMPUTE_RADIUS)
//		{
//			if (mc.edgeYMax!=0 && mc.cacheToWorldY(mc.edgeYMax) < curLoc.y+CHASE_COMPUTE_RADIUS)
//			{
////				we are near the SOUTH_EAST corner
//				
//			} else if (mc.edgeYMin!=0 && mc.cacheToWorldY(mc.edgeYMin) > curLoc.y-CHASE_COMPUTE_RADIUS)
//			{
////				we are near the NORTH_EAST corner
//				
//			}
//		} else if (mc.edgeXMin!=0 && mc.cacheToWorldX(mc.edgeXMin) > curLoc.x-CHASE_COMPUTE_RADIUS)
//		{
//			if (mc.edgeYMax!=0 && mc.cacheToWorldY(mc.edgeYMax) < curLoc.y+CHASE_COMPUTE_RADIUS)
//			{
////				we are near the SOUTH_WEST corner
//				
//			} else if (mc.edgeYMin!=0 && mc.cacheToWorldY(mc.edgeYMin) > curLoc.y-CHASE_COMPUTE_RADIUS)
//			{
////				we are near the NORTH_WEST corner
//				
//			}
//		}
	}
	
	private void updateChaseTarget()
	{
		if (curLoc.distanceSquaredTo(target) < 10)
			target = curLoc.add(targetDir,5);
	}
	
	private String computeRetreatTarget()
	{
		lastPowerNodeGuess = null;
//		7 0 1
//		6   2
//		5 4 3
		int[] closest_in_dir = er.getEnemiesInEachDirection();
		int[] wall_in_dir = new int[8];
		
//		now, deal with when we are close to map boundaries
		if (mc.edgeXMax!=0 && mc.cacheToWorldX(mc.edgeXMax) < curLoc.x+RETREAT_RADIUS)
		{
			if (mc.edgeYMax!=0 && mc.cacheToWorldY(mc.edgeYMax) < curLoc.y+RETREAT_RADIUS)
			{
//				we are near the SOUTH_EAST corner
				wall_in_dir[1] = wall_in_dir[2] = wall_in_dir[3] = wall_in_dir[4] = wall_in_dir[5] = 1;
			} else if (mc.edgeYMin!=0 && mc.cacheToWorldY(mc.edgeYMin) > curLoc.y-RETREAT_RADIUS)
			{
//				we are near the NORTH_EAST corner
				wall_in_dir[1] = wall_in_dir[2] = wall_in_dir[3] = wall_in_dir[0] = wall_in_dir[7] = 1;
			} else
			{
//				we are near the EAST edge
				wall_in_dir[1] = wall_in_dir[2] = wall_in_dir[3] = 1;
			}
		} else if (mc.edgeXMin!=0 && mc.cacheToWorldX(mc.edgeXMin) > curLoc.x-RETREAT_RADIUS)
		{
			if (mc.edgeYMax!=0 && mc.cacheToWorldY(mc.edgeYMax) < curLoc.y+RETREAT_RADIUS)
			{
//				we are near the SOUTH_WEST corner
				wall_in_dir[7] = wall_in_dir[6] = wall_in_dir[5] = wall_in_dir[4] = wall_in_dir[3] = 1;
			} else if (mc.edgeYMin!=0 && mc.cacheToWorldY(mc.edgeYMin) > curLoc.y-RETREAT_RADIUS)
			{
//				we are near the NORTH_WEST corner
				wall_in_dir[7] = wall_in_dir[6] = wall_in_dir[5] = wall_in_dir[0] = wall_in_dir[1] = 1;
			} else
			{
//				we are near the WEST edge
				wall_in_dir[7] = wall_in_dir[6] = wall_in_dir[5] = 1;
			}
		} else
		{
			if (mc.edgeYMax!=0 && mc.cacheToWorldY(mc.edgeYMax) < curLoc.y+RETREAT_RADIUS)
			{
//				we are near the SOUTH edge
				wall_in_dir[5] = wall_in_dir[4] = wall_in_dir[3] = 1;
			} else if (mc.edgeYMin!=0 && mc.cacheToWorldY(mc.edgeYMin) > curLoc.y-RETREAT_RADIUS)
			{
//				we are near the NORTH edge
				wall_in_dir[7] = wall_in_dir[0] = wall_in_dir[1] = 1;
			} else
			{
//				we are not near any wall or corner
			}
		}
		
		String dir = ""	+(closest_in_dir[0]==0?(wall_in_dir[0]==0?"o":"x"):"x")
						+(closest_in_dir[1]==0?(wall_in_dir[1]==0?"o":"x"):"x")
						+(closest_in_dir[2]==0?(wall_in_dir[2]==0?"o":"x"):"x")
						+(closest_in_dir[3]==0?(wall_in_dir[3]==0?"o":"x"):"x")
						+(closest_in_dir[4]==0?(wall_in_dir[4]==0?"o":"x"):"x")
						+(closest_in_dir[5]==0?(wall_in_dir[5]==0?"o":"x"):"x")
						+(closest_in_dir[6]==0?(wall_in_dir[6]==0?"o":"x"):"x")
						+(closest_in_dir[7]==0?(wall_in_dir[7]==0?"o":"x"):"x");
		dir = dir+dir;
		int index;
		
		index = dir.indexOf("ooooooo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+3)%8];
			target = curLoc.add(targetDir,5);
			return dir;
		}
		
		index = dir.indexOf("oooooo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+3)%8];
			target = curLoc.add(targetDir,5);
			return dir;
		}
		
		index = dir.indexOf("ooooo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+2)%8];
			target = curLoc.add(targetDir,5);
			return dir;
		}
		
		index = dir.indexOf("oooo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+2)%8];
			target = curLoc.add(targetDir,5);
			return dir;
		}
		
		index = dir.indexOf("ooo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+1)%8];
			target = curLoc.add(targetDir,5);
			return dir;
		}
		
		index = dir.indexOf("oo");
		if (index>-1)
		{
			targetDir = Constants.directions[(index+1)%8];
			target = curLoc.add(targetDir,5);
			return dir;
		}
		
		index = dir.indexOf("o");
		if (index>-1)
		{
			targetDir = Constants.directions[(index)%8];
			target = curLoc.add(targetDir,5);
			return dir;
		}
		
		dbg.println('y',"GONNTA GET GEE'D");
//		int lowest = closest_in_dir[0];
//		int lowesti = 0;
//		for (int x=1; x<8; x++)
//			if (closest_in_dir[x]<lowest)
//			{
//				lowesti = x;
//				lowest = closest_in_dir[x];
//			}
		target = radar.getEnemySwarmTarget();
//		targetDir = target.directionTo(curLoc);
//		targetDir = Constants.directions[lowesti];
		target = curLoc.add(targetDir,5);
		return null;
	}
	
	private void updateRetreatTarget()
	{
		if (curLoc.distanceSquaredTo(target) < 10)
			target = curLoc.add(targetDir,5);
		dbg.setIndicatorString('y',1, "Target= <"+(target.x-curLoc.x)+","+(target.y-curLoc.y)+">, Strategy="+strategy+", Behavior="+behavior+" no recalc");
	}
	
	private void computeBattleTarget()
	{
		target = radar.getEnemySwarmTarget();
		targetDir = curLoc.directionTo(target);
	}
	
	private void computeExploreTarget()
	{
		MapLocation t = mc.guessEnemyPowerCoreLocation();
		if (t.equals(lastPowerNodeGuess))
		{
			if (curLoc.distanceSquaredTo(target) < 20 && mc.getEnemyPowerCoreLocation()==null)
			{
				switch (myHome.directionTo(curLoc))
				{
				case NORTH:
				{
					if (mc.edgeYMin==0)
						target = curLoc.add(Direction.NORTH, 10);
					else if (mc.edgeXMax==0)
						target = curLoc.add(Direction.EAST, 10);
					else if (mc.edgeXMin==0)
						target = curLoc.add(Direction.WEST, 10);
					else if (mc.edgeYMax==0)
						target = curLoc.add(Direction.SOUTH, 10);
				} break;
				case NORTH_EAST:
				{
					if (mc.edgeYMin==0)
						target = curLoc.add(Direction.NORTH, 10);
					else if (mc.edgeXMax==0)
						target = curLoc.add(Direction.EAST, 10);
					else if (mc.edgeXMin==0)
						target = curLoc.add(Direction.WEST, 10);
					else if (mc.edgeYMax==0)
						target = curLoc.add(Direction.SOUTH, 10);
				} break;
				case EAST:
				{
					if (mc.edgeXMax==0)
						target = curLoc.add(Direction.EAST, 10);
					else if (mc.edgeYMin==0)
						target = curLoc.add(Direction.NORTH, 10);
					else if (mc.edgeYMax==0)
						target = curLoc.add(Direction.SOUTH, 10);
					else if (mc.edgeXMin==0)
						target = curLoc.add(Direction.WEST, 10);
				} break;
				case SOUTH_EAST:
				{
					if (mc.edgeXMax==0)
						target = curLoc.add(Direction.EAST, 10);
					else if (mc.edgeYMax==0)
						target = curLoc.add(Direction.SOUTH, 10);
					else if (mc.edgeYMin==0)
						target = curLoc.add(Direction.NORTH, 10);
					else if (mc.edgeXMin==0)
						target = curLoc.add(Direction.WEST, 10);
				} break;
				case SOUTH:
				{
					if (mc.edgeYMax==0)
						target = curLoc.add(Direction.SOUTH, 10);
					else if (mc.edgeXMin==0)
						target = curLoc.add(Direction.WEST, 10);
					else if (mc.edgeXMax==0)
						target = curLoc.add(Direction.EAST, 10);
					else if (mc.edgeYMin==0)
						target = curLoc.add(Direction.NORTH, 10);
				} break;
				case SOUTH_WEST:
				{
					if (mc.edgeYMax==0)
						target = curLoc.add(Direction.SOUTH, 10);
					else if (mc.edgeXMin==0)
						target = curLoc.add(Direction.WEST, 10);
					else if (mc.edgeXMax==0)
						target = curLoc.add(Direction.EAST, 10);
					else if (mc.edgeYMin==0)
						target = curLoc.add(Direction.NORTH, 10);
				} break;
				case WEST:
				{
					if (mc.edgeXMin==0)
						target = curLoc.add(Direction.WEST, 10);
					else if (mc.edgeYMax==0)
						target = curLoc.add(Direction.SOUTH, 10);
					else if (mc.edgeYMin==0)
						target = curLoc.add(Direction.NORTH, 10);
					else if (mc.edgeXMax==0)
						target = curLoc.add(Direction.EAST, 10);
				} break;
				case NORTH_WEST:
				{
					if (mc.edgeXMin==0)
						target = curLoc.add(Direction.WEST, 10);
					else if (mc.edgeYMin==0)
						target = curLoc.add(Direction.NORTH, 10);
					else if (mc.edgeYMax==0)
						target = curLoc.add(Direction.SOUTH, 10);
					else if (mc.edgeXMax==0)
						target = curLoc.add(Direction.EAST, 10);
				} break;
				}
			}
		} else
		{
			lastPowerNodeGuess = target = t;
		}
		
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
		case ENEMY_SPOTTED:
			int[] shorts = BroadcastSystem.decodeUShorts(sb);
			enemySpottedRound = shorts[0];
			enemySpottedTarget = new MapLocation(shorts[1], shorts[2]);
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
		
		
		
		if (behavior==BehaviorState.RETREAT) {
			if(rc.getFlux() > 130) {
				for(int d=curDir.ordinal(); d<curDir.ordinal()+8; d++)
					if (rc.canMove(Constants.directions[d%8]))
						return new MoveInfo(RobotType.SOLDIER, Constants.directions[d%8]);
			} else {
				return new MoveInfo(nav.navigateToDestination(), true);
			}
		}
		
		if(strategy == StrategyState.SPLIT) {
			return new MoveInfo(curLoc.directionTo(myHome).opposite(), false);
		}
		
		int fluxToMakeSoldierAt;
		switch (behavior) {
		case SWARM: fluxToMakeSoldierAt = 280; break;
		case RETREAT: fluxToMakeSoldierAt = 130; break;
		default:
			fluxToMakeSoldierAt = (strategy==StrategyState.CAP) ? 225 : 150; 
			break;
		}
		
		if(rc.getFlux() > fluxToMakeSoldierAt) {
			if(Math.random() < 0.000001 && Clock.getRoundNum() > 500 && 
					rc.senseObjectAtLocation(curLocInFront, RobotLevel.IN_AIR)==null) {
				return new MoveInfo(RobotType.SCOUT, curDir);
			} else if(rc.canMove(curDir)) {
				return new MoveInfo(RobotType.SOLDIER, curDir);
			}
		}
		
		if(radar.closestEnemyDist <= 20 && behavior != BehaviorState.CHASE) {
			return new MoveInfo(curLoc.directionTo(radar.getEnemySwarmCenter()).opposite(), true);
		}
		
		// If I'm swarming and closest of all archons to my target, slow down
		if(behavior == BehaviorState.SWARM && radar.alliesInFront==0) {
			boolean isClosestToTarget = true;
			int myDist = curLoc.distanceSquaredTo(target);
			for(MapLocation loc: rc.senseAlliedArchons()) {
				if(loc.distanceSquaredTo(target) < myDist) {
					isClosestToTarget = false;
					break;
				}
			}
			if(isClosestToTarget) {
				if (Clock.getRoundNum()>=100 && Clock.getRoundNum()<=110 && rc.getFlux()>90) {
					return new MoveInfo(RobotType.SCOUT, curDir);
				} else if(Math.random()<0.8) {
					return null;
				}
			}
		}
		
		// If I'm too close to an allied archon, move away from him with some probability
		if(dc.getClosestArchon()!=null) {
			int distToNearestArchon = curLoc.distanceSquaredTo(dc.getClosestArchon());
			if(distToNearestArchon <= 36 &&
					!(strategy==StrategyState.CAP && curLoc.distanceSquaredTo(target)<=36 && 
					rc.senseObjectAtLocation(dc.getClosestArchon(), RobotLevel.ON_GROUND).getID() > myID) && 
					Math.random() < 0.85-Math.sqrt(distToNearestArchon)/10) {
				return new MoveInfo(curLoc.directionTo(dc.getClosestArchon()).opposite(), false);
			}
		}
		
		// If we can build a tower at our target node, do so
		if(strategy == StrategyState.CAP && 
				rc.canMove(curDir) && 
				curLocInFront.equals(target) && 
				mc.isPowerNode(curLocInFront)) {
			if(rc.getFlux() > 200) 
				return new MoveInfo(RobotType.TOWER, curDir);
		
		// If we are on top of our target node, move backwards randomly
		} else if(strategy == StrategyState.CAP && 
				curLoc.equals(target) && mc.isPowerNode(curLoc)) {
			return new MoveInfo(nav.navigateCompletelyRandomly(), true);
		
		// None of the above conditions met, move to the destination
		} else {
			Direction dir = nav.navigateToDestination();
			if(dir==null) 
				return null;
			else if(curLoc.add(dir).equals(nav.getDestination()))
				return new MoveInfo(dir);
			else if(Clock.getRoundNum() < 500)
				return new MoveInfo(dir, true);
			else
				return new MoveInfo(dir, false);
			
		}
		return null;
	}
	
	@Override
	public void useExtraBytecodes() throws GameActionException {
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
