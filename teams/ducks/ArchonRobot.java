package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.PowerNode;
import battlecode.common.RobotController;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ArchonRobot extends BaseRobot{
	private enum StrategyState {
		/** Initially try to explore some terrain. */
		INITIAL_EXPLORE,
		/** Split up and defend base. Transitions into defend. */
		RETURN_HOME,
		/** Defend the power core. */
		DEFEND,
		/** Power core is connected to enemy. Defend it at all costs! */
		SAVE_POWER_CORE,
		/** Seek and destroy towards a target. */
		RUSH, 
		/** Take power nodes of distance <= 15 adjacent to power core. */
		ADJACENT_CAP,
		/** Take power nodes efficiently. */
		EFFICIENT_CAP,
		/** Game is about to end. Cap as many towers as possible! */
		ENDGAME_CAP,
	}
	private enum BehaviorState {
		/** No enemies to deal with. */
		SWARM,
		/** Run away from enemy forces. */
		RETREAT, 
		/** Fight the enemy forces. Micro, maybe kite. */
		BATTLE, 
	}
	
	/** round we are releasing our lock */
	int stayTargetLockedUntilRound;
	int roundStartWakeupMode;
	MapLocation target;
	boolean movingTarget;
	Direction targetDir;
	StrategyState strategy;
	BehaviorState behavior;
	MapLocation previousWakeupTarget;
	MapLocation enemySpottedTarget;
	int enemySpottedRound;
	boolean detectedGameEnd;
	MapLocation[] neighborsOfPowerCore;
	
	Direction lastFlee;
	
	static final int RETREAT_RADIUS = 6;
	static final int RETREAT_DISTANCE = 8;
	static final int CHASE_COMPUTE_RADIUS = 7;
	static final int TURNS_TO_LOCK_ONTO_AN_ENEMY = 30;
	static final int TURNS_TO_RETREAT = 30;
	MapLocation lastPowerNodeGuess;
	
	public ArchonRobot(RobotController myRC) throws GameActionException {
		super(myRC);
		
		// bind radio channels
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.ARCHONS,
				BroadcastChannel.EXPLORERS
		});
		
		// read/write team memory
		if(myArchonID==0) tmem.advanceRound();
		tmem.initReadEnemyCount();
		
		// set subsystem modes
		fbs.setPoolMode();
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		
		// init starting behaviors
		strategy = StrategyState.ADJACENT_CAP;
		behavior = BehaviorState.SWARM;
		
		// init state variables
		stayTargetLockedUntilRound = -Integer.MAX_VALUE;
		enemySpottedRound = -55555;
		enemySpottedTarget = null;
		lastPowerNodeGuess = null;
		lastFlee = null;
		detectedGameEnd = false;
		neighborsOfPowerCore = rc.sensePowerCore().neighbors();
	}
	
	boolean gotOutput = false;
	@Override
	public void run() throws GameActionException {
		
		// Currently the strategy transition is based on hard-coded turn numbers
//		if(curRound>4000) {
//			strategy = StrategyState.CAP;
//		} else if(curRound>2400 && myArchonID!=0) {
//			strategy = StrategyState.CAP;
//		} else if(curRound>1800 || 
//				(mc.powerNodeGraph.enemyPowerCoreID != 0 && enemySpottedTarget == null)) {
//			strategy = StrategyState.DEFEND;
//		} else if(curRound>30) {
//			strategy = StrategyState.RUSH;
//		}
		
//		 The new strategy transition
		if(detectedGameEnd)
			strategy = StrategyState.ENDGAME_CAP;
		switch(strategy) {
		case INITIAL_EXPLORE:
			if(curRound > 150) 
				strategy = StrategyState.RETURN_HOME;
			break;
		case RETURN_HOME:
			if(curLoc.distanceSquaredTo(myHome) <= 64)
				strategy = StrategyState.DEFEND;
			break;
		case DEFEND:
			if(curRound > 800) 
				strategy = StrategyState.ADJACENT_CAP;
			break;
		case SAVE_POWER_CORE:
			break;
		case ADJACENT_CAP:
			if(getNextPowerNodeAdjacentToCore()==null)
				strategy = StrategyState.EFFICIENT_CAP;
			break;
		case EFFICIENT_CAP:
			break;
		case ENDGAME_CAP:
			break;
		default:
			break;
		}

		
		// If insufficiently prepared, prepare
		if(nav.getTurnsPrepared() < TangentBug.DEFAULT_MIN_PREP_TURNS)
			nav.prepare();
		
		// Scan 
		boolean needToScanAllies = !rc.isMovementActive() || msm.justMoved();
		boolean needToScanEnemies = !rc.isMovementActive() || msm.justMoved() || curRound%6==myArchonID;
		radar.scan(needToScanAllies, needToScanEnemies);
		
		// Broadcast enemy info every 6 turns
		if(curRound%6 == myArchonID)
			radar.broadcastEnemyInfo(false);
		
		// Update retreat behavior
		if (behavior != BehaviorState.RETREAT) lastFlee = null;
		if (behavior == BehaviorState.RETREAT && radar.getArmyDifference() > 3)
			stayTargetLockedUntilRound = -55555;
		
		// If there is a non-scout enemy in sensor range, set target as enemy swarm target
		if(radar.closestEnemy != null && radar.numEnemyScouts < radar.numEnemyRobots) {
			enemySpottedRound = curRound;
			enemySpottedTarget = radar.closestEnemy.location;
			stayTargetLockedUntilRound = curRound + TURNS_TO_LOCK_ONTO_AN_ENEMY;
			Direction enemySwarmDir = curLoc.directionTo(radar.getEnemySwarmTarget());
			if (radar.getArmyDifference() < -2 || 
					radar.getAlliesInDirection(enemySwarmDir) < 
					radar.numEnemyRobots-radar.numEnemyArchons-radar.numEnemyTowers) {
				stayTargetLockedUntilRound = curRound+TURNS_TO_RETREAT;
				behavior = BehaviorState.RETREAT;
				String ret = computeRetreatTarget();
				dbg.setIndicatorString('e', 1, "Target= "+locationToVectorString(target)+", Strategy="+strategy+", Behavior="+behavior+" "+ret);
				
			} else {
				behavior = BehaviorState.BATTLE;
				computeBattleTarget();
			}
		
		// We should update the target based on the previous target direction if we are chasing or retreating
		} else if(curRound <= stayTargetLockedUntilRound && targetDir!=null) {
			if(behavior == BehaviorState.RETREAT)
				updateRetreatTarget();
			
		// If someone else told us of a recent enemy spotting, go to that location
		} else if(strategy != StrategyState.DEFEND && curRound < enemySpottedRound + Constants.ENEMY_SPOTTED_SIGNAL_TIMEOUT) {
			behavior = BehaviorState.SWARM;
			target = enemySpottedTarget;
			movingTarget = true;
			if(curLoc.distanceSquaredTo(enemySpottedTarget) <= 16) {
				enemySpottedTarget = null;
				enemySpottedRound = -55555;
			}
			
		// If we haven't seen anyone for a while, go back to swarm mode and reset target
		} else {
			behavior = BehaviorState.SWARM;
			movingTarget = false;
			switch(strategy) {
			case DEFEND:
			case RETURN_HOME:
				target = myHome;
				break;
			case RUSH:
				computeExploreTarget();
				break;
			case ADJACENT_CAP:
				target = getNextPowerNodeAdjacentToCore();
				break;
			case EFFICIENT_CAP:
				target = mc.guessBestPowerNodeToCapture();
				break;
			default:
				target = myHome;
				break;
			}
		}
		
		// If we change to a new target, wake up hibernating allies
		if(previousWakeupTarget == null ||
				target.distanceSquaredTo(previousWakeupTarget) > 25 ||
				behavior != BehaviorState.SWARM) {
			roundStartWakeupMode = curRound;
			previousWakeupTarget = target;
		}
		if(curRound%6==myArchonID && curRound < roundStartWakeupMode + 50) {
			io.sendWakeupCall();
		}
			
		// Set the target for the navigator 
		nav.setDestination(target);
		
		// Set the flux balance mode
		if(behavior == BehaviorState.SWARM && curRound > enemySpottedRound + Constants.ENEMY_SPOTTED_SIGNAL_TIMEOUT)
			fbs.setBatteryMode();
		else
			fbs.setPoolMode();
		
		// Broadcast my target info to the soldier swarm every 6 turns
		if(curRound%6 == myArchonID && strategy!=StrategyState.INITIAL_EXPLORE) {
			int[] shorts = new int[3];
			shorts[0] = movingTarget ? 1 : 0;
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
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		// If it's between turn 100 and 150, try to build a scout (archons 0 and 1)
		if (curRound>=100 && curRound<=150 && rc.getFlux()>90) {
			if(myArchonID==0) {
				if(curRound%10==0) {
					Direction dir = curDir;
					while(rc.senseTerrainTile(curLoc.add(dir))==TerrainTile.OFF_MAP || 
							rc.senseObjectAtLocation(curLoc.add(dir), RobotLevel.IN_AIR)!=null) 
						dir = dir.rotateLeft();
					return new MoveInfo(RobotType.SCOUT, dir);
				} else return null;
			} else if(myArchonID==1) {
				if(curRound%10==5){
					Direction dir = curDir;
					while(rc.senseTerrainTile(curLoc.add(dir))==TerrainTile.OFF_MAP || 
							rc.senseObjectAtLocation(curLoc.add(dir), RobotLevel.IN_AIR)!=null) 
						dir = dir.rotateLeft();
					return new MoveInfo(RobotType.SCOUT, dir);
				} else return null;
			}
		}
				
		// Retreat behavior
		if (behavior==BehaviorState.RETREAT) {
			nav.setDestination(target);
			if(rc.getFlux() > 130) {
				for(int d=curDir.ordinal(); d<curDir.ordinal()+8; d++)
					if (rc.canMove(Constants.directions[d%8]))
						return new MoveInfo(RobotType.SOLDIER, Constants.directions[d%8]);
			}
			return new MoveInfo(nav.navigateToDestination(), true);
		}
		
		// If we have sufficient flux, make a soldier
		int fluxToMakeSoldierAt;
		switch (behavior) {
		case SWARM: fluxToMakeSoldierAt = 280; break;
		case RETREAT: fluxToMakeSoldierAt = 130; break;
		default:
			fluxToMakeSoldierAt = (strategy==StrategyState.EFFICIENT_CAP) ? 225 : 150; 
			break;
		}
		if(rc.getFlux() > fluxToMakeSoldierAt) {
			if(Util.randDouble() < 0.1 && Clock.getRoundNum() > 1000 && 
					rc.senseObjectAtLocation(curLocInFront, RobotLevel.IN_AIR)==null) {
				return new MoveInfo(RobotType.SCOUT, curDir);
			} else {
				Direction dir = nav.wiggleToMovableDirection(curDir);
				if(dir!=null)
					return new MoveInfo(RobotType.SOLDIER, dir);
			}
		}
		
		// If there's an enemy within 20 dist, and we've been weakened, run away
		if(radar.closestEnemyDist <= 20 && curEnergon < 100) {
			return new MoveInfo(curLoc.directionTo(radar.getEnemySwarmCenter()).opposite(), true);
		}
		
		switch(strategy) {
		case INITIAL_EXPLORE:
			if(curRound > 80 && Util.randDouble()<0.3) 
				return new MoveInfo(nav.navigateCompletelyRandomly(), false);

			return new MoveInfo(getDirAwayFromAlliedArchons(400), false);
		
		case SAVE_POWER_CORE:
		case RETURN_HOME:
			return new MoveInfo(nav.navigateToDestination(), false);
			
		case DEFEND:
			if(curLoc.distanceSquaredTo(myHome) <= 100) {
				if(Util.randDouble()<0.02) 
					return new MoveInfo(nav.navigateCompletelyRandomly(), false);
				else
					return new MoveInfo(getDirAwayFromAlliedArchons(32), false);
			} else {
				return new MoveInfo(nav.navigateToDestination(), false);
			}
		
		case ADJACENT_CAP:
		case ENDGAME_CAP:
		case EFFICIENT_CAP:
			// If we can build a tower at our target node, do so
			if(rc.canMove(curDir) && curLocInFront.equals(target) && 
					mc.isPowerNode(curLocInFront)) {
				if(rc.getFlux() > 200) 
					return new MoveInfo(RobotType.TOWER, curDir);
				else return null;
			}
			
			// If we are on top of our target node, move backwards randomly
			if(curLoc.equals(target) && mc.isPowerNode(curLoc)) {
				return new MoveInfo(nav.navigateCompletelyRandomly(), true);
			} 
			
			// If I'm the closest archon to my target...
			MapLocation closestToTarget = null;
			int minDist = Integer.MAX_VALUE;
			for(MapLocation loc: dc.getAlliedArchons()) {
				int dist = loc.distanceSquaredTo(target);
				if(dist < minDist) {
					closestToTarget = loc;
					minDist = dist;
				}
			}
			if(curLoc.equals(closestToTarget)) {
				// If there are no allies in front, slow down (maintain compact swarm)
				if(behavior == BehaviorState.SWARM && radar.alliesInFront==0 && 
						Util.randDouble()<0.8)
					return null;
				
			// If I'm NOT the closest archon to my target...
			} else {
				// If I'm too close to an allied archon, disperse probabilistically
				if(dc.getClosestArchon()!=null) {
					int distToNearestArchon = curLoc.distanceSquaredTo(dc.getClosestArchon());
					if(distToNearestArchon <= 25 && 
							Util.randDouble() < 1.05-Math.sqrt(distToNearestArchon)/10) {
						return new MoveInfo(getDirAwayFromAlliedArchons(32), false);
					}
				}
			}
			
			
			
			// By default, move to the destination
			Direction dir = nav.navigateToDestination();
			if(dir==null) 
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
		case ENEMY_SPOTTED:
			int[] shorts = BroadcastSystem.decodeUShorts(sb);
			if(shorts[0] > enemySpottedRound) {
				enemySpottedRound = shorts[0];
				enemySpottedTarget = new MapLocation(shorts[1], shorts[2]);
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
		if(Clock.getRoundNum()==curRound && Clock.getRoundNum() > tmem.timeCountWritten + 10) {
			tmem.writeEnemyCount();
		}
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3000) 
			nav.prepare();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1000) 
			mc.extractUpdatedPackedDataStep();
	}
	
	private String computeRetreatTarget()
	{
		lastPowerNodeGuess = null;
//		7 0 1
//		6   2
//		5 4 3
//		int[] closest_in_dir = er.getEnemiesInEachDirectionOnly();
		int[] closest_in_dir = radar.closestInDir;
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
		
//		dbg.setIndicatorString('y', 2, ""	+wall_in_dir[0]+wall_in_dir[1]+wall_in_dir[2]+wall_in_dir[3]
//											+wall_in_dir[4]+wall_in_dir[5]+wall_in_dir[6]+wall_in_dir[7]
//											+" "+mc.edgeXMax+" "+mc.edgeXMin+" "+mc.edgeYMax+" "+mc.edgeYMin+" "+mc.cacheToWorldX(mc.edgeXMax));
		
		if (lastFlee != null) wall_in_dir[lastFlee.opposite().ordinal()] = 1;
		
		String dir =  "".concat(closest_in_dir[0]==99?(wall_in_dir[0]==0?"o":"x"):"x")
						.concat(closest_in_dir[1]==99?(wall_in_dir[1]==0?"o":"x"):"x")
						.concat(closest_in_dir[2]==99?(wall_in_dir[2]==0?"o":"x"):"x")
						.concat(closest_in_dir[3]==99?(wall_in_dir[3]==0?"o":"x"):"x")
						.concat(closest_in_dir[4]==99?(wall_in_dir[4]==0?"o":"x"):"x")
						.concat(closest_in_dir[5]==99?(wall_in_dir[5]==0?"o":"x"):"x")
						.concat(closest_in_dir[6]==99?(wall_in_dir[6]==0?"o":"x"):"x")
						.concat(closest_in_dir[7]==99?(wall_in_dir[7]==0?"o":"x"):"x");
		dir = dir.concat(dir);
		int index;
		
		targetDir = curLoc.directionTo(target);
		
		Direction newdir;
		index = dir.indexOf("ooooooo");
		if (index>-1)
		{
			newdir = Constants.directions[(index+3)%8];
			if (!isAdjacent(newdir,targetDir) || curLoc.distanceSquaredTo(target) < 10)
			{
				lastFlee = targetDir = newdir;
				target = curLoc.add(targetDir, RETREAT_DISTANCE);
			}
			return dir;
		}
		
		index = dir.indexOf("oooooo");
		if (index>-1)
		{
			newdir = Constants.directions[(index+3)%8];
			if (!isAdjacent(newdir,targetDir) || curLoc.distanceSquaredTo(target) < 10)
			{
				lastFlee = targetDir = newdir;
				target = curLoc.add(targetDir, RETREAT_DISTANCE);
			}
			return dir;
		}
		
		index = dir.indexOf("ooooo");
		if (index>-1)
		{
			newdir = Constants.directions[(index+2)%8];
			if (!isAdjacent(newdir,targetDir) || curLoc.distanceSquaredTo(target) < 10)
			{
				lastFlee = targetDir = newdir;
				target = curLoc.add(targetDir, RETREAT_DISTANCE);
			}
			return dir;
		}
		
		index = dir.indexOf("oooo");
		if (index>-1)
		{
			newdir = Constants.directions[(index+2)%8];
			if (!isAdjacent(newdir,targetDir) || curLoc.distanceSquaredTo(target) < 10)
			{
				lastFlee = targetDir = newdir;
				target = curLoc.add(targetDir, RETREAT_DISTANCE);
			}
			return dir;
		}
		
		index = dir.indexOf("ooo");
		if (index>-1)
		{
			newdir = Constants.directions[(index+1)%8];
			if (!isAdjacent(newdir,targetDir) || curLoc.distanceSquaredTo(target) < 10)
			{
				lastFlee = targetDir = newdir;
				target = curLoc.add(targetDir, RETREAT_DISTANCE);
			}
			return dir;
		}
		
		index = dir.indexOf("oo");
		if (index>-1)
		{
			newdir = Constants.directions[(index+1)%8];
			if (!isAdjacent(newdir,targetDir) || curLoc.distanceSquaredTo(target) < 10)
			{
				lastFlee = targetDir = newdir;
				target = curLoc.add(targetDir, RETREAT_DISTANCE);
			}
			return dir;
		}
		
		index = dir.indexOf("o");
		if (index>-1)
		{
			newdir = Constants.directions[(index)%8];
			if (!isAdjacent(newdir,targetDir) || curLoc.distanceSquaredTo(target) < 10)
			{
				lastFlee = targetDir = newdir;
				target = curLoc.add(targetDir, RETREAT_DISTANCE);
			}
			return dir;
		}
		
		dbg.println('y',"GONNTA GET GEE'D");
		int lowest = closest_in_dir[0];
		int lowesti = 0;
		for (int x=1; x<8; x++)
			if (closest_in_dir[x]<lowest)
			{
				lowesti = x;
				lowest = closest_in_dir[x];
			}
//		target = radar.getEnemySwarmTarget();
//		newdir = target.directionTo(curLoc);
		newdir = Constants.directions[lowesti];
		if (!isAdjacent(newdir,targetDir) || curLoc.distanceSquaredTo(target) < 10)
		{
			lastFlee = targetDir = newdir;
			target = curLoc.add(targetDir, RETREAT_DISTANCE);
			return null;
		}
		return null;
	}
	
	private boolean isAdjacent(Direction d1, Direction d2)
	{
		return (d1.ordinal()-d2.ordinal()+9)%8<2;
	}
	
	private void updateRetreatTarget()
	{
		if (curLoc.distanceSquaredTo(target) < 10)
		{
			target = curLoc.add(targetDir, RETREAT_DISTANCE);
			while (mc.isWall(target)) target = target.add(Constants.directions[(int)(Util.randDouble()*8)]);
		}
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
	
	/** Returns the direction that will help most in keeping the archons
	 * at least 16 dist apart.
	 * @param minKeepApartDistSquared the distance we try to keep our archons apart by. <br>
	 * Above this distance, we don't care how separated the archons are.
	 */
	private Direction getDirAwayFromAlliedArchons(int minKeepApartDistSquared) {
		boolean[] movable = dc.getMovableDirections();
		Direction bestDir = null;
		int bestDist = 0;
		for(int i=8; i>=0; i--) {
			if(i!=8 && !movable[i]) continue;
			Direction dir = i==8 ? null : Constants.directions[i];
			MapLocation newLoc = i==8 ? curLoc : curLoc.add(dir);
			int dist = Integer.MAX_VALUE;
			for(MapLocation loc: dc.getAlliedArchons()) {
				if(loc.equals(curLoc)) continue;
				dist = Math.min(dist, loc.distanceSquaredTo(newLoc));
			}
			if(bestDist <= minKeepApartDistSquared && dist > bestDist) {
				bestDist = dist;
				bestDir = dir;
			}
		}
		return bestDir;
	}
	
	/** Returns a location of a power node adjacent to our power core that we
	 * do not control. If we control them all, returns null.
	 */
	private MapLocation getNextPowerNodeAdjacentToCore() {
		PowerNode[] nodes = dc.getAlliedPowerNodes();
		for(int i=0; i<neighborsOfPowerCore.length; i++) {
			MapLocation loc = neighborsOfPowerCore[i];
			if(myHome.distanceSquaredTo(loc) > 400 || mc.isDeadEndPowerNode(loc))
				continue;
			boolean flag = false;
			for(int j=0; j<nodes.length; j++) {
				if(loc.equals(nodes[j].getLocation())) {
					flag = true;
					break;
				}
			}
			if(!flag) return loc;
		}
		return null;
	}
}
