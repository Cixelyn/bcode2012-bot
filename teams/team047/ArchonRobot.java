package team047;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ArchonRobot extends StrategyRobot {

	private Direction explorationDirection;
	
	
	
	private int armySizeBuilt;
	private int armySizeTarget;
	private boolean isDefender;
	private boolean initialized;
	
	/** The true (starting) index of the archon */
	private int trueArchonIndex;
	private String myRadioChannel;
	
	// attack move variables
	private boolean isLeader;
	private int archonIndex;
	private RobotInfo attackTarget;
	private MapLocation attackMoveTarget;
	private Direction attackMoveDirection;
	private int roundsLastSeenEnemy;
	
	private int roundsToChase;
	private int roundSinceMove;
	private RobotState prevState;
	private boolean gathering;
	private boolean moving;
	private int lastRoundCheckedTargets;
	private MapLocation leaderLoc;
	
	private int enemyDiff;
	
	private MapLocation enemyPowerNode;
	private boolean doneWithAttackBase;
	

	// defender variables
	private int numDefenders;
	
	// power cap variables
	private MapLocation nextNodeToCapture;
	
	
	public ArchonRobot(RobotController myRC) throws GameActionException {
		super(myRC, RobotState.INITIALIZE);
	
		// calculate true archon ID
		MapLocation[] alliedArchons = dc.getAlliedArchons();
		for(int i=0; i<alliedArchons.length; i++ ) {
			if(myRC.getLocation().equals(alliedArchons[i])) {
				trueArchonIndex = i;
				myRadioChannel = "#"+i;
			}
		}
		
	}

	@Override
	public RobotState processTransitions(RobotState state)
			throws GameActionException {
		switch (state)
		{
		case INITIALIZE:
		{
			if (initialized)
				return RobotState.EXPLORE;
		} break;
		case EXPLORE:
		{
			if (curRound > Constants.ROUNDS_TO_EXPLORE ||
					dc.getClosestEnemy() != null)
				return RobotState.GOHOME;
		} break;
		case GOHOME:
		{
			if (curLoc.distanceSquaredTo(myHome) <=
					Constants.DISTANCE_TO_HOME_ON_GOHOME || curRound >=
					Constants.ROUNDS_TO_GO_HOME)
				return RobotState.SPLIT;
		} break;
		case SPLIT:
		{
			if (curLoc.distanceSquaredTo(dc.getClosestArchon()) >=
					Constants.SPLIT_DISTANCE || rc.getFlux() >= myType.maxFlux) {
				return RobotState.BUILD_ARMY;
			}
		} break;
		case BUILD_ARMY:
		{
			if (curRound > Constants.MAX_ROUNDS_TO_ATTACKBASE)
			{
				return RobotState.POWER_CAP;
			}
			if ((dc.getClosestEnemy() != null &&
					dc.getClosestEnemy().type != RobotType.SCOUT) ||
					curRound >= Constants.MAX_ROUNDS_TO_BUILD_ARMY) {
				// TODO(jven): pick one!
				//wakeUpMyUnits();
				wakeUpAllUnits();
				if (isDefender) {
					return RobotState.DEFEND_BASE;
				} else {
					return RobotState.ATTACK_ENEMY_BASE;
				}
			}
		} break;
		case ATTACK_ENEMY_BASE:
		{
			if (curRound > Constants.MAX_ROUNDS_TO_ATTACKBASE)
			{
				return RobotState.POWER_CAP;
			}
			if (doneWithAttackBase && attackMoveTarget==null)
			{
				// TODO(jven): shouldn't we be defending our base in case of counter?
				if (dc.getAlliedArchons().length >=
						eakc.getNumEnemyArchonsAlive() + 2)
				{
					return RobotState.POWER_CAP;
				} else if (enemyPowerNode!=null)
				{
					return RobotState.POWER_CAP;
				}
			}
		} break;
		case DEFEND_BASE:
		{
			
		} break;
		case POWER_CAP:
		{
			
		} break;
		}
		return state;
	}

	@Override
	public void prepareTransition(RobotState newstate, RobotState oldstate)
			throws GameActionException {
		switch (newstate)
		{
		case INITIALIZE:
		{
			initialized = false;
		} break;
		case EXPLORE:
		{
			// set micro objective and mode
			micro.setObjective(
					curLoc.add(explorationDirection, GameConstants.MAP_MAX_HEIGHT));
			micro.setNormalMode();
		} break;
		case GOHOME:
		{
			// set micro objective and mode
			micro.setObjective(myHome);
			micro.setNormalMode();
		} break;
		case SPLIT:
		{
			// set micro objective and mode
			micro.setKiteMode(Constants.SPLIT_DISTANCE);
			// set flux management mode
			fbs.setBatteryMode();
		} break;
		case BUILD_ARMY:
		{
			switch (oldstate)
			{
			case SPLIT:
			{
				armySizeBuilt = 0;
				armySizeTarget = Constants.ARMY_SIZE_ON_INITIAL_BUILD;
			} break;
			}
			fbs.setBatteryMode();
			
		} break;
		case ATTACK_ENEMY_BASE:
		{
			prevState = oldstate;
			doneWithAttackBase = true;
			roundsToChase = Constants.ARCHON_CHASE_ROUNDS;
			micro.setNormalMode();
			// set flux management mode
			fbs.setBattleMode();
		} break;
		case DEFEND_BASE:
		{
			numDefenders = 0;
			micro.setObjective(myHome);
			micro.setMoonwalkMode();
			fbs.setBattleMode();
			
		} break;
		case POWER_CAP:
		{
			nav.setNavigationMode(NavigationMode.TANGENT_BUG);
			
			// set flux management mode
			fbs.setBatteryMode();
		} break;
		}
		
		
		switch (oldstate)
		{
		case ATTACK_ENEMY_BASE:
		{
			attackMoveDirection = null;
		} break;
		}
	}

	@Override
	public void execute(RobotState state) throws GameActionException {
		
		if(directionToSenseIn!=null) {
			mc.senseAfterMove(directionToSenseIn);
			directionToSenseIn = null;
		}
		
		switch (state) {
		case INITIALIZE:
			initialize();
			break;
		case EXPLORE:
			explore();
			break;
		case GOHOME:
			gohome();
			break;
		case SPLIT:
			split();
			break;
		case BUILD_ARMY:
			build_army();
			break;
		case ATTACK_ENEMY_BASE:
			attack_enemy_base();
			break;
		case DEFEND_BASE:
			defend_base();
			break;
		case POWER_CAP:
			power_cap();
			break;
		default:
			break;
		}
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb)
			throws GameActionException {
		int priority = 99;
		
		switch(msgType) {
//		
//		case 's':
//		{
//			RobotState curstate = getCurrentState();
//			
//			if (curstate == RobotState.BUILD_ARMY)
//			{
//				if (isDefender)
//				{
//					gotoState(RobotState.DEFEND_BASE);
//				} else
//				{
//					gotoState(RobotState.ATTACK_ENEMY_BASE);
//				}
//			}
//			
//		} break;
		
//		swarm messages
//		case 's':
//		{
//			if (!isDefender)
//			{
//				if (!isLeader)
//				{
//					gotoState(RobotState.ATTACK_ENEMY_BASE);
//					
//					int[] msg = Radio.decodeShorts(sb);
//					if (msg[0]<archonIndex)
//					{
//						attackMoveDirection = Constants.directions[msg[1]];
//						attackMoveTarget = new MapLocation(msg[2], msg[3]);
//						lastRoundCheckedTargets = currRound;
//					}
//				}
//			}
//		} break;
//		case 'x':
//		{
////			TODO ??????
//		} break;
//		case 'a':
//		{
//			if (!isDefender)
//			{
//				int[] msg = Radio.decodeShorts(sb);
//				if (attackTarget==null || roundsLastSeenEnemy>0)
//				{
//					attackMoveTarget = new MapLocation(msg[1], msg[2]);
//					roundsLastSeenEnemy = 0;
////					gotoState(RobotState.ATTACK_ENEMY_BASE);
//				}
//			}
//		} break;
		
//			dead archon messages
		case ENEMY_ARCHON_KILL:
			eakc.reportEnemyArchonKills(BroadcastSystem.decodeUShorts(sb));
			break;
		case OWNERSHIP_CLAIM:
			ao.processAcknowledgement(BroadcastSystem.decodeUShorts(sb));
			break;
		default:
			super.processMessage(msgType, sb);
			break;
		}
	}
	
	public void initialize()
	{
		// set navigation mode mode
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		// set radio addresses
		io.setChannels(new BroadcastChannel[]{
				BroadcastChannel.ALL,
				BroadcastChannel.ARCHONS
		});
		// see if i'm the defender
		isDefender = curLoc.equals(dc.getAlliedArchons()[5]);
		if (!isDefender) checkAttackMoveStatus();
		// set my initial exploration direction
		explorationDirection = myHome.directionTo(curLoc);
		// sense all
		mc.senseAll();
		// done
		initialized = true;
	}
	
	public void explore() throws GameActionException
	{
		// navigate towards exploration target
		micro.attackMove();
		// change direction halfway through
		if (curRound == Constants.ROUNDS_TO_EXPLORE / 2) {
			explorationDirection = explorationDirection.rotateLeft().rotateLeft();
			micro.setObjective(curLoc.add(
					explorationDirection, GameConstants.MAP_MAX_HEIGHT));
		}
	}
	
	public void gohome() throws GameActionException
	{
		// navigate towards home
		micro.attackMove();
	}
	
	public void split() throws GameActionException
	{
		// kite from closest archon
		micro.setObjective(dc.getClosestArchon());
		micro.attackMove();
	}
	
	public void build_army() throws GameActionException
	{
		// build army
		for (Direction d : Constants.directions) {
			if (d == Direction.OMNI || d == Direction.NONE) {
				continue;
			}
			if (dc.getMovableDirections()[d.ordinal()]) {
				if (spawnUnitInDir(RobotType.SOLDIER, d)) {
					// claim ownership of the unit
					ao.claimOwnership();
					armySizeBuilt++;
				}
			}
		}
		// distribute flux
		fbs.manageFlux();
		// send ownership information
		ao.broadcastOwnerships(trueArchonIndex);
	}
	
	public void checkAttackMoveStatus()
	{
		if (!isLeader)
		{
			MapLocation[] locs = dc.getAlliedArchons();
			leaderLoc = locs[0];
			if (leaderLoc.equals(curLoc))
			{
				isLeader = true;
				archonIndex = 0;
				micro.setKiteMode(Constants.ATTACK_MOVE_KITE_DISTANCE_SQUARED);
			} else if (locs[1].equals(curLoc))
			{
				archonIndex = 1;
			} else if (locs[2].equals(curLoc))
			{
				archonIndex = 2;
			} else if (locs[3].equals(curLoc))
			{
				archonIndex = 3;
			} else if (locs[4].equals(curLoc))
			{
				archonIndex = 4;
			}
		}
	}
	
	public void attack_enemy_base() throws GameActionException
	{
		enemyPowerNode = mc.getEnemyPowerCoreLocation();
		MapLocation guess;// = mc.guessEnemyPowerCoreLocation();
		if (enemyPowerNode != null)
		{
			doneWithAttackBase = true;
			guess = myHome;
		} else
		{
			guess = mc.guessEnemyPowerCoreLocation();
		}
		
		attack_move(guess);
		
//		checkAttackMoveStatus();
//		if (isLeader)
//		{
//			if (attackTarget == null || !rc.canSenseObject(attackTarget.robot))
//			{
//				checkAttackMoveTargets();
//				if (attackTarget == null)
//				{
//					enemyPowerNode = mc.getEnemyPowerCoreLocation();
//					if (enemyPowerNode == null)
//					{
//						attackMoveTarget = mc.guessEnemyPowerCoreLocation();
//						if (currLoc.distanceSquaredTo(attackMoveTarget) < 36)
//						{
//							doneWithAttackBase = true;
//							return;
//						}
//						if (attackMoveTarget == null)
//						{
//							attackMoveDirection = Direction.SOUTH;
//							attackMoveTarget = currLoc.add(attackMoveDirection,60);
//						} else
//						{
//							attackMoveDirection = currLoc.directionTo(attackMoveTarget);
//						}
//					} else
//					{
//						doneWithAttackBase = true;
//						return;
//					}
//				}
//			}
//		} else
//		{
//			checkAttackMoveTargets();
//			if (attackTarget!=null)
//			{
//				switch (attackTarget.type)
//				{
//				case ARCHON:
//					sendEnemyAtLoc(attackTarget.location);
//					attack_move(attackMoveTarget, attackMoveDirection, attackTarget);
//					return;
//				}
//			}
//			
//			enemyPowerNode = mc.getEnemyPowerCoreLocation();
//			if (enemyPowerNode == null)
//			{
//				attackMoveTarget = mc.guessEnemyPowerCoreLocation();
//				if (currLoc.distanceSquaredTo(attackMoveTarget) < 36)
//				{
//					doneWithAttackBase = true;
//					return;
//				}
//			}
//			
//			attackMoveTarget = leaderLoc;
//			attackMoveDirection = currLoc.directionTo(attackMoveTarget);
//		}
//		
//		attack_move(attackMoveTarget, attackMoveDirection, attackTarget);
	}
	
//	radio 
//	a - attack target/announce target
//	s - swarm - dir, target loc
//	z - swarm retreat, dir
//	x - gather
//	
//	priorities (high to low)
//	as,az,ax - only sent by leader archon
//	aa - sent by other archosn to announce enemys //TODO
//	ss,sz,sx - sent by archons to their soldiers
//			- possibly to be replaced by *s*z*x, where * = robot id
	public void attack_move(MapLocation attackTarget) throws GameActionException
	{
		attack_move(attackTarget, curLoc.directionTo(attackTarget), null);
	}
	
	public void attack_move(Direction attackDir) throws GameActionException
	{
		attack_move(curLoc.add(attackDir,Constants.ARCHON_CHASE_DIRECTION_MULTIPLIER), attackDir, null);
	}
	
	public void attack_move(MapLocation moveTarget, Direction attackDir, RobotInfo attackInfo) throws GameActionException
	{
		checkAttackMoveStatus();
		
		checkAttackMoveTargets();
		
		if (!rc.isMovementActive())
		{
			if (rc.getFlux() > Constants.ARCHON_FLUX_DURING_COMBAT+RobotType.SOLDIER.spawnCost)
			{
				boolean[] movable = dc.getMovableDirections();
				// build army
				for (Direction d : Constants.directions) {
					if (d == Direction.OMNI || d == Direction.NONE) {
						continue;
					}
					if (movable[d.ordinal()]) {
						if (spawnUnitInDir(RobotType.SOLDIER, d)) {
							// claim ownership of the unit
							ao.claimOwnership();
							armySizeBuilt++;
						}
					}
				}
			}
		}
		ao.broadcastOwnerships(trueArchonIndex);
		
		
		if (enemyDiff >= 0)
		{
			if (attackMoveTarget == null)
			{
				sendSwarmInfo(moveTarget,enemyDiff);
				
				micro.setNormalMode();
				micro.setObjective(moveTarget);
				micro.attackMove();
			} else
			{
				sendSwarmInfo(attackMoveTarget,enemyDiff);
				
				MapLocation swarmcenter = radar.getEnemySwarmCenter();
				
				micro.setKiteMode(25);
				micro.setObjective(swarmcenter);
				micro.attackMove();
			}
		} else
		{
			MapLocation swarmcenter = radar.getEnemySwarmCenter();
			micro.setKiteMode(100);
			micro.setObjective(swarmcenter);
			micro.attackMove();
		}
		
		

		
		
		
//		if (isLeader)
//		{
////			leader code
//			// TODO retreat code
//			debug.setIndicatorString(2, "leader "+attackDir+" "+attackTarget, Owner.YP);
//			if (!rc.isMovementActive())
//			{
//				roundSinceMove++;
//				//TODO build soldiers
//				
////				resend swarm information
//				sendSwarmInfo(attackDir, attackTarget);
//				
////				calculate if we should move
//				int archons_ready = 0;
//				int myval = 0;
//				switch (attackDir)
//				{
//				case NORTH:			myval = -currLoc.y;					break;
//				case NORTH_EAST:	myval = -currLoc.y+currLoc.x;		break;
//				case EAST:			myval = currLoc.x;					break;
//				case SOUTH_EAST:	myval = currLoc.y+currLoc.x;		break;
//				case SOUTH:			myval = currLoc.y;					break;
//				case SOUTH_WEST:	myval = currLoc.y+currLoc.x;		break;
//				case WEST:			myval = -currLoc.x;					break;
//				case NORTH_WEST:	myval = -currLoc.y-currLoc.x;		break;
//				default: myval = 0; break;
//				}
//				myval--;
//				
//				MapLocation[] archons = dc.getAlliedArchons();
//				for (int x=1; x<archons.length; x++)
//				{
//					// TODO inefficient, swap out later - more code
//					MapLocation loc = archons[x];
//					int value;
//					switch (attackDir)
//					{
//					case NORTH:			value = -loc.y;				break;
//					case NORTH_EAST:	value = -loc.y+loc.x;		break;
//					case EAST:			value = loc.x;				break;
//					case SOUTH_EAST:	value = loc.y+loc.x;		break;
//					case SOUTH:			value = loc.y;				break;
//					case SOUTH_WEST:	value = loc.y+loc.x;		break;
//					case WEST:			value = -loc.x;				break;
//					case NORTH_WEST:	value = -loc.y-loc.x;		break;
//					default: value = 0; break;
//					}
//					
//					int dsq = loc.distanceSquaredTo(currLoc);
//					
//					if (value >= myval && dsq < Constants.MAX_SWARM_ARCHON_DISTANCE_SQUARED) 
//						archons_ready++;
//					else if (dsq < Constants.ARCHON_CLOSE_DISTANCE) 
//						archons_ready++;
//				}
//				
//				int archon_threshold = archons.length*3/8;
//				
//				if (archons_ready >= archon_threshold)
//				{
//					mi.setObjective(attackTarget);
//					mi.attackMove();
//					
//					roundSinceMove = 0;
//				} else
//				{
//					if (roundSinceMove > Constants.ARCHON_MOVE_STUCK_ROUNDS)
//					{
//						sendRallyAtLoc(currLoc,attackMoveDirection);
//					}
////					else if (roundSinceMove > Constants.ARCHON_MOVE_STUCK_ROUNDS)
////					{
//////						resend swarm information
////						io.sendShorts("#xs", 
////								new int[]{	attackMoveDirection.ordinal(), 
////											attackMoveTarget.x, attackMoveTarget.y});
////					}
//				}
//			}
//		} else
//		{
//			// follow code
//			MapLocation[] archons = dc.getAlliedArchons();
//			MapLocation leader = archons[0];
//			
//
//			
//			if (!rc.isMovementActive())
//			{
//				MapLocation target = null;
//				
//				if (attackDir==null || 
//						currLoc.distanceSquaredTo(attackTarget) > Constants.MAX_SWARM_ARCHON_DISTANCE_SQUARED)
//				{
//					target = attackTarget;
//				} else
//				{
//					switch (archonIndex)
//					{
//					case 1:
//						target = attackTarget.add(attackDir.rotateLeft().rotateLeft(),Constants.SWARM_DISTANCE_FROM_ARCHON);
//						break;
//					case 2:
//						target = attackTarget.add(attackDir.rotateRight().rotateRight(),Constants.SWARM_DISTANCE_FROM_ARCHON);
//						break;
//					case 3:
//						target = attackTarget.add(attackDir.rotateLeft().rotateLeft(),Constants.SWARM_DISTANCE_FROM_ARCHON2);
//						break;
//					case 4:
//						target = attackTarget.add(attackDir.rotateRight().rotateRight(),Constants.SWARM_DISTANCE_FROM_ARCHON2);
//						break;
//					}
//					
//					target = target.add(attackDir);
//				}
//				
////				resend swarm information
//				sendSwarmInfo(attackDir, target.add(attackDir,3));
//				
//				debug.setIndicatorString(2, archonIndex+" "+target, Owner.YP);
//				
//				mi.setObjective(target);
//				mi.attackMove();
//			}
//		}
		// distribute flux
		fbs.manageFlux();
	}
	
	public void sendSwarmInfo(MapLocation target, int diff)
	{
		int[] msg = new int[] {archonIndex, target.x, target.y, diff+100, curLoc.x, curLoc.y};
		
//		io.sendShorts("#xs", msg);
//		io.sendShorts(myRadioChannel+"s", msg);
	}
	
	public void sendRallyAtLoc(MapLocation target, Direction swarmDirection)
	{
//		io.sendShorts("#ax", 
//				new int[]{	archonIndex, target.x, target.y, swarmDirection.ordinal()});
	}
	
	public void sendEnemyAtLoc(MapLocation target)
	{
//		io.sendShorts("#aa", 
//				new int[]{	archonIndex, target.x, target.y});
	}
	
	public boolean checkAttackMoveTargets()
	{
		if (lastRoundCheckedTargets<curRound)
		{
			radar.scan(true, true);
			enemyDiff = radar.getArmyDifference();
			
			
			if (radar.numEnemyRobots-radar.numEnemyTowers > 0)
			{
				attackMoveTarget = radar.getEnemySwarmTarget();
				attackMoveDirection = curLoc.directionTo(attackMoveTarget);
				return true;
				
//			} else if (attackMoveDirection!=null && roundsLastSeenEnemy < roundsToChase)
//			{
//				attackMoveTarget = currLoc.add(attackMoveDirection, Constants.ARCHON_CHASE_DIRECTION_MULTIPLIER);
//				roundsLastSeenEnemy++;
//				return true;
			} else
			{
				attackTarget = null;
				attackMoveTarget = null;
				attackMoveDirection = null;
				
				return false;
			}
		}
//			
//			lastRoundCheckedTargets = currRound;
//			ur.scan(false, true);
//			if (ur.numEnemyArchons>0)
//			{
////				for now, just use the first one TODO
//				attackTarget = ur.enemyInfos[ur.enemyArchons[0]];
//				attackMoveTarget = attackTarget.location;
//				attackMoveDirection = currLoc.directionTo(attackMoveTarget);
//				roundsLastSeenEnemy = 0;
//				return true;
//			}
//			
//			if (ur.numEnemyTowers>0)
//			{
//				for (int x=0; x<ur.numEnemyTowers; x++)
//				{
//					// TODO check if we can attack this tower
//					boolean canAttackTower = false;
//					if (canAttackTower)
//					{
//						attackTarget = ur.enemyInfos[ur.enemyTowers[x]];
//						attackMoveTarget = attackTarget.location;
//						attackMoveDirection = currLoc.directionTo(attackMoveTarget);
//						roundsLastSeenEnemy = 0;
//						return true;
//					}
//				}
//			}
//			
//			if (ur.numEnemyRobots-ur.numEnemyTowers>Constants.NON_TRIVIAL_ENEMY_CONCENTRATION)
//			{
////				for now, just use the closest TODO
//				attackTarget = ur.closetEnemy;
//				attackMoveTarget = attackTarget.location;
//				attackMoveDirection = currLoc.directionTo(attackMoveTarget);
//				roundsLastSeenEnemy = 0;
//				return true;
//			}
//			
//			if (attackMoveDirection!=null && roundsLastSeenEnemy < roundsToChase)
//			{
//				attackMoveTarget = currLoc.add(attackMoveDirection, Constants.ARCHON_CHASE_DIRECTION_MULTIPLIER);
//				roundsLastSeenEnemy++;
//				return true;
//			}
//			
//			attackTarget = null;
//			attackMoveTarget = null;
//			attackMoveDirection = null;
//			
//			return false;
//		}
		
//		doesn't matter, will only be called a second time in
//		attack_move()
		return false;
	}
	
	public void power_cap() throws GameActionException
	{
		checkAttackMoveTargets();
		if (attackMoveTarget!=null && enemyDiff >= 0)
			attack_move(attackMoveTarget);
				
		if(nextNodeToCapture != null) {
			boolean stillCapturable = false;
			for(MapLocation loc: dc.getCapturablePowerCores()) {
				if(loc.equals(nextNodeToCapture)) {
					stillCapturable = true;
					break;
				}
			}
			if(!stillCapturable) nextNodeToCapture = null;
		}
		if(nextNodeToCapture == null || Math.random()<0.01) {
			nextNodeToCapture = mc.guessBestPowerNodeToCapture();
			micro.setObjective(nextNodeToCapture);
			
		}
		if(curLocInFront.equals(nextNodeToCapture)) {
			if(rc.getFlux() > 200 && rc.canMove(curDir)) {
				rc.spawn(RobotType.TOWER);
			}
		} else {
			attack_move(nextNodeToCapture);
			
//			mi.setNormalMode();
//			mi.attackMove();
			// distribute flux
			fbs.manageFlux();
		}
		
	}


	public void defend_base() throws GameActionException
	{
		radar.scan(false, true);
		if (radar.numEnemySoldiers > 0 || radar.numEnemyArchons > 0) {
			io.sendWakeupCall();
//			io.sendShort("#zz", 0); //FIXME: dummy call for now to trigger wakeup
			nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		} else {
			if(curRound % 5 == 0 && radar.roundsSinceEnemySighted > 10) { // send back to sleep
//				io.sendShort("#dz", 0);
			}
			nav.setNavigationMode(NavigationMode.RANDOM);
		}
	
		
		if(rc.getFlux() > 150) 
		{
			boolean spawned = false;
			if(numDefenders % 4 == 1) {
				spawned = spawnUnitInDir(RobotType.SCOUT,curDir);
			} else {
				spawned = spawnUnitInDir(RobotType.SOLDIER,curDir);
			}
			
			if(spawned) {
				numDefenders++;
				ao.claimOwnership();
			}
		}
		else {
			micro.setMoonwalkMode();
			micro.attackMove();
			System.out.println(micro.getObjective());
		}	
			
		
		ao.broadcastOwnerships(trueArchonIndex);
		fbs.manageFlux();
	}
	
	private boolean spawnUnitInDir(
			RobotType type, Direction dir) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {	
			return false;
		}
		// turn in direction to spawn
		if (curDir != dir) {
			rc.setDirection(dir);
			return false;
		}
		// return if not enough flux
		if (rc.getFlux() < type.spawnCost) {
			return false;
		}
		// return if terrain tile is bad
		if ((type.level == RobotLevel.ON_GROUND &&
				!rc.canMove(dir)) ||
				(type.level == RobotLevel.IN_AIR &&
				rc.senseTerrainTile(curLoc.add(dir)) == TerrainTile.OFF_MAP)) {
			return false;
		}
		// return if unit is in the way
		if (dc.getAdjacentGameObject(dir, type.level) != null) {
			return false;
		}
		// spawn unit
		rc.spawn(type);
		return true;
	}
	
	private void wakeUpAllUnits() {
//		io.sendShort("#xw", 0);
	}
	
	private void wakeUpMyUnits() {
//		io.sendShort("#" + trueArchonIndex + "w", 0);
	}

}
