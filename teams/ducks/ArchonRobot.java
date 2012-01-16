package ducks;

import ducks.Debug.Owner;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class ArchonRobot extends StrategyRobot {

	private Direction explorationDirection;
	
	
	
	private int armySizeBuilt;
	private int armySizeTarget;
	private boolean isDefender;
	private boolean initialized;
	
	/** The true (starting) index of the archon */
	private int trueArchonIndex;
	
	// attack move variables
	private boolean isLeader;
	private int archonIndex;
	private RobotInfo attackTarget;
	private MapLocation attackMoveTarget;
	private Direction attackMoveDirection;
	private int roundsLastSeenEnemy;
	private MapLocation enemyPowerNode;
	private int roundsToChase;
	private int roundSinceMove;
	private RobotState prevState;
	private boolean gathering;
	private boolean moving;
	private int lastRoundCheckedTargets;
	

	// defender variables
	private int numDefenders;
	
	
	public ArchonRobot(RobotController myRC) {
		super(myRC, RobotState.INITIALIZE);
	
		// calculate true archon ID
		MapLocation[] alliedArchons = dc.getAlliedArchons();
		for(int i=0; i<alliedArchons.length; i++ ) {
			if(myRC.getLocation().equals(alliedArchons[i])) {
				trueArchonIndex = i;
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
			if (currRound > Constants.ROUNDS_TO_EXPLORE ||
					dc.getClosestEnemy() != null)
				return RobotState.GOHOME;
		} break;
		case GOHOME:
		{
			if (currLoc.distanceSquaredTo(myHome) <=
					Constants.DISTANCE_TO_HOME_ON_GOHOME)
				return RobotState.SPLIT;
		} break;
		case SPLIT:
		{
			if (currLoc.distanceSquaredTo(dc.getClosestArchon()) >=
					Constants.SPLIT_DISTANCE || currFlux >= myType.maxFlux) {
				return RobotState.BUILD_ARMY;
			}
		} break;
		case BUILD_ARMY:
		{
			if ((dc.getClosestEnemy() != null &&
					dc.getClosestEnemy().type != RobotType.SCOUT) ||
					armySizeBuilt >= armySizeTarget) {
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
			if (enemyPowerNode!=null && attackMoveTarget==null)
			{
				// TODO(jven): shouldn't we be defending our base in case of counter?
				if (dc.getAlliedArchons().length >=
						eai.getNumEnemyArchons() + 2)
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
			mi.setObjective(
					currLoc.add(explorationDirection, GameConstants.MAP_MAX_HEIGHT));
			mi.setNormalMode();
		} break;
		case GOHOME:
		{
			// set micro objective and mode
			mi.setObjective(myHome);
			mi.setNormalMode();
		} break;
		case SPLIT:
		{
			// set micro objective and mode
			mi.setKiteMode(Constants.SPLIT_DISTANCE);
			// set flux management mode
			fm.setBatteryMode();
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
		} break;
		case ATTACK_ENEMY_BASE:
		{
			prevState = oldstate;
			
			roundsToChase = Constants.CHASE_ROUNDS;
			mi.setNormalMode();
			// set flux management mode
			fm.setBattleMode();
		} break;
		case DEFEND_BASE:
		{
			numDefenders = 0;
			
		} break;
		case POWER_CAP:
		{
			// set flux management mode
			fm.setBatteryMode();
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
		debug.setIndicatorString(
				2, "Unack ownerships: " + ao.getNumUnacknowledgedOwnerships(),
				Owner.JVEN);
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
	public void processMessage(char msgType, StringBuilder sb)
			throws GameActionException {
		switch(msgType) {
		
//		swarm messages
		case 's':
		{
			if (!isDefender)
				gotoState(RobotState.ATTACK_ENEMY_BASE);
			int[] msg = Radio.decodeShorts(sb);
			if (msg[0]<archonIndex)
			{
				attackMoveDirection = Constants.directions[msg[1]];
				attackMoveTarget = new MapLocation(msg[2], msg[3]);
				lastRoundCheckedTargets = currRound;
			}
		} break;
		case 'x':
		{
//			TODO ??????
		}break;
		
//			dead archon messages
		case 'd':
			eai.reportEnemyArchonKills(Radio.decodeShorts(sb));
			break;
		case 'o':
			ao.processAcknowledgement(Radio.decodeShorts(sb));
			break;
		default:
			super.processMessage(msgType, sb);
		}
	}
	
	public void initialize()
	{
		// set navigation mode mode
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		// set radio addresses
		io.setAddresses(new String[] {"#x", "#a"});
		// see if i'm the defender
		isDefender = currLoc.equals(dc.getAlliedArchons()[5]);
		if (!isDefender) checkAttackMoveStatus();
		// set my initial exploration direction
		explorationDirection = myHome.directionTo(currLoc);
		// sense all
		mc.senseAll();
		// done
		initialized = true;
	}
	
	public void explore() throws GameActionException
	{
		// navigate towards exploration target
		mi.attackMove();
		// change direction halfway through
		if (currRound == Constants.ROUNDS_TO_EXPLORE / 2) {
			explorationDirection = explorationDirection.rotateLeft().rotateLeft();
			mi.setObjective(currLoc.add(
					explorationDirection, GameConstants.MAP_MAX_HEIGHT));
		}
	}
	
	public void gohome() throws GameActionException
	{
		// navigate towards home
		mi.attackMove();
	}
	
	public void split() throws GameActionException
	{
		// kite from closest archon
		mi.setObjective(dc.getClosestArchon());
		mi.attackMove();
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
		fm.manageFlux();
		// send ownership information
		ao.sendOwnerships(trueArchonIndex);
	}
	
	public void checkAttackMoveStatus()
	{
		if (!isLeader)
		{
			MapLocation[] locs = dc.getAlliedArchons();
			if (locs[0].equals(currLoc))
			{
				isLeader = true;
				archonIndex = 0;
				mi.setKiteMode(Constants.ATTACK_MOVE_KITE_DISTANCE_SQUARED);
			} else if (locs[1].equals(currLoc))
			{
				archonIndex = 1;
			} else if (locs[2].equals(currLoc))
			{
				archonIndex = 2;
			} else if (locs[3].equals(currLoc))
			{
				archonIndex = 3;
			} else if (locs[4].equals(currLoc))
			{
				archonIndex = 4;
			}
		}
	}
	
	public void attack_enemy_base() throws GameActionException
	{
		if (attackTarget == null || !rc.canSenseObject(attackTarget.robot))
		{
			checkAttackMoveTargets();
			if (enemyPowerNode == null)
			{
				enemyPowerNode = mc.getEnemyPowerCoreLocation();
				if (enemyPowerNode == null)
				{
					attackMoveTarget = mc.guessEnemyPowerCoreLocation();
					if (attackMoveTarget == null)
					{
						attackMoveDirection = Direction.SOUTH;
						attackMoveTarget = currLoc.add(attackMoveDirection,60);
					} else
					{
						attackMoveDirection = currLoc.directionTo(attackMoveTarget);
					}
				}
			}
		}
		attack_move(attackMoveTarget, attackMoveDirection, attackTarget);
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
		attack_move(attackTarget, currLoc.directionTo(attackTarget), null);
	}
	
	public void attack_move(Direction attackDir) throws GameActionException
	{
		attack_move(currLoc.add(attackDir,Constants.CHASE_DIRECTION_MULTIPLIER), attackDir, null);
	}
	
	public void attack_move(MapLocation attackTarget, Direction attackDir, RobotInfo attackInfo) throws GameActionException
	{
		checkAttackMoveStatus();
		
//		checkAttackMoveTargets();
		
		if (isLeader)
		{
//			leader code
			// TODO retreat code
			debug.setIndicatorString(2, "leader "+attackDir+" "+attackTarget, Owner.YP);
			if (!rc.isMovementActive())
			{
				roundSinceMove++;
				//TODO build soldiers
				
//				resend swarm information
				sendSwarmInfo(attackDir, attackTarget);
				
//				calculate if we should move
				int archons_ready = 0;
				int myval = 0;
				switch (attackDir)
				{
				case NORTH:			myval = -currLoc.y;					break;
				case NORTH_EAST:	myval = -currLoc.y+currLoc.x;		break;
				case EAST:			myval = currLoc.x;					break;
				case SOUTH_EAST:	myval = currLoc.y+currLoc.x;		break;
				case SOUTH:			myval = currLoc.y;					break;
				case SOUTH_WEST:	myval = currLoc.y+currLoc.x;		break;
				case WEST:			myval = -currLoc.x;					break;
				case NORTH_WEST:	myval = -currLoc.y-currLoc.x;		break;
				default: myval = 0; break;
				}
				myval--;
				
				MapLocation[] archons = dc.getAlliedArchons();
				for (int x=1; x<archons.length; x++)
				{
					// TODO inefficient, swap out later - more code
					MapLocation loc = archons[x];
					int value;
					switch (attackDir)
					{
					case NORTH:			value = -loc.y;				break;
					case NORTH_EAST:	value = -loc.y+loc.x;		break;
					case EAST:			value = loc.x;				break;
					case SOUTH_EAST:	value = loc.y+loc.x;		break;
					case SOUTH:			value = loc.y;				break;
					case SOUTH_WEST:	value = loc.y+loc.x;		break;
					case WEST:			value = -loc.x;				break;
					case NORTH_WEST:	value = -loc.y-loc.x;		break;
					default: value = 0; break;
					}
					
					int dsq = loc.distanceSquaredTo(currLoc);
					
					if (value >= myval && dsq < Constants.MAX_SWARM_ARCHON_DISTANCE_SQUARED) 
						archons_ready++;
					else if (dsq < Constants.ARCHON_CLOSE_DISTANCE) 
						archons_ready++;
				}
				
				int archon_threshold = archons.length*5/8;
				
				if (archons_ready >= archon_threshold)
				{
					mi.setObjective(attackTarget);
					mi.attackMove();
					
					roundSinceMove = 0;
				} else
				{
					if (roundSinceMove > Constants.ARCHON_MOVE_STUCK_ROUNDS)
					{
						io.sendShort("#xx", attackDir.ordinal());
					}
//					else if (roundSinceMove > Constants.ARCHON_MOVE_STUCK_ROUNDS)
//					{
////						resend swarm information
//						io.sendShorts("#xs", 
//								new int[]{	attackMoveDirection.ordinal(), 
//											attackMoveTarget.x, attackMoveTarget.y});
//					}
				}
			}
		} else
		{
			// follow code
			MapLocation[] archons = dc.getAlliedArchons();
			MapLocation leader = archons[0];
			if (!rc.isMovementActive())
			{
				MapLocation target = null;
				
				if (attackDir==null || 
						currLoc.distanceSquaredTo(leader) > Constants.MAX_SWARM_ARCHON_DISTANCE_SQUARED)
				{
					target = leader;
				} else
				{
					switch (archonIndex)
					{
					case 1:
						target = leader.add(attackDir.rotateLeft().rotateLeft(),Constants.SWARM_DISTANCE_FROM_ARCHON);
						break;
					case 2:
						target = leader.add(attackDir.rotateRight().rotateRight(),Constants.SWARM_DISTANCE_FROM_ARCHON);
						break;
					case 3:
						target = leader.add(attackDir.rotateLeft().rotateLeft(),Constants.SWARM_DISTANCE_FROM_ARCHON2);
						break;
					case 4:
						target = leader.add(attackDir.rotateRight().rotateRight(),Constants.SWARM_DISTANCE_FROM_ARCHON2);
						break;
					}
					
					target = target.add(attackDir);
				}
				
				debug.setIndicatorString(2, archonIndex+" "+target, Owner.YP);
				
				mi.setObjective(target);
				mi.attackMove();
			}
		}
		// distribute flux
		fm.manageFlux();
	}
	
	public void sendSwarmInfo(Direction swarmDirection, MapLocation target)
	{
		if (swarmDirection==null || target==null)
			System.out.println("null");
		io.sendShorts("#as", 
				new int[]{	archonIndex, swarmDirection.ordinal(), 
							target.x, target.y});
	}
	
	public void sendRallyAtLoc(MapLocation target)
	{
		io.sendShorts("#ax", 
				new int[]{	archonIndex, target.x, target.y});
	}
	
	public boolean checkAttackMoveTargets()
	{
		if (lastRoundCheckedTargets<currRound)
		{
			lastRoundCheckedTargets = currRound;
			ur.scan(false, true);
			if (ur.numEnemyArchons>0)
			{
//				for now, just use the first one TODO
				attackTarget = ur.enemyInfos[ur.enemyArchons[0]];
				attackMoveTarget = attackTarget.location;
				attackMoveDirection = currLoc.directionTo(attackMoveTarget);
				roundsLastSeenEnemy = 0;
				return true;
			}
			
			if (ur.numEnemyTowers>0)
			{
				for (int x=0; x<ur.numEnemyTowers; x++)
				{
					// TODO check if we can attack this tower
					boolean canAttackTower = false;
					if (canAttackTower)
					{
						attackTarget = ur.enemyInfos[ur.enemyTowers[x]];
						attackMoveTarget = attackTarget.location;
						attackMoveDirection = currLoc.directionTo(attackMoveTarget);
						roundsLastSeenEnemy = 0;
						return true;
					}
				}
			}
			
			if (ur.numEnemyRobots-ur.numEnemyTowers>Constants.NON_TRIVIAL_ENEMY_CONCENTRATION)
			{
//				for now, just use the closest TODO
				attackTarget = ur.closetEnemy;
				attackMoveTarget = attackTarget.location;
				attackMoveDirection = currLoc.directionTo(attackMoveTarget);
				roundsLastSeenEnemy = 0;
				return true;
			}
			
			if (roundsLastSeenEnemy < roundsToChase)
			{
				attackMoveTarget = currLoc.add(attackMoveDirection, Constants.CHASE_DIRECTION_MULTIPLIER);
				roundsLastSeenEnemy++;
				return true;
			}
			
			attackTarget = null;
			attackMoveTarget = null;
			attackMoveDirection = null;
			
			return false;
		}
		
//		doesn't matter, will only be called a second time in
//		attack_move()
		return false;
	}
	
	public void power_cap()
	{
		// distribute flux
		fm.manageFlux();
	}
	
	public void defend_base()
	{
		// distribute flux
		fm.manageFlux();
	}
	
	private boolean spawnUnitInDir(
			RobotType type, Direction dir) throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {	
			return false;
		}
		// turn in direction to spawn
		if (currDir != dir) {
			rc.setDirection(dir);
			return false;
		}
		// wait if not enough flux
		if (currFlux < type.spawnCost) {
			return false;
		}
		// wait if unit is in the way
		if (dc.getAdjacentGameObject(dir, type.level) != null) {
			return false;
		}
		// spawn unit
		rc.spawn(type);
		currFlux -= type.spawnCost;
		return true;
	}
	
	private void wakeUpAllUnits() {
		io.sendShort("#xw", 0);
	}
	
	private void wakeUpMyUnits() {
		io.sendShort("#" + trueArchonIndex + "w", 0);
	}

}
