package ducks;

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
	
	// attack move variables
	private boolean isLeader;
	private int archonIndex;
	private RobotInfo attackTarget;
	private MapLocation attackMoveTarget;
	private Direction attackMoveDirection;
	private MapLocation enemyPowerNode;
	
	public ArchonRobot(RobotController myRC) {
		super(myRC, RobotState.INITIALIZE);
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
			if (dc.getClosestEnemy()!=null || armySizeBuilt>=armySizeTarget)
			{
				//TODO wakeup code here? maybe?
				if (isDefender)
					return RobotState.DEFEND_BASE;
				else return RobotState.ATTACK_MOVE;
			}
		} break;
		case ATTACK_MOVE:
		{
			if (attackMoveDirection!=null)
			{
				if (dc.getAlliedArchons().length >= enemyArchonInfo.getNumEnemyArchons()+2)
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
			if (checkAttackMoveTargets())
			{
				return RobotState.ATTACK_MOVE;
			}
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
		case ATTACK_MOVE:
		{
			
		} break;
		case DEFEND_BASE:
		{
			
		} break;
		case POWER_CAP:
		{
			
		} break;
		}
	}

	@Override
	public void execute(RobotState state) throws GameActionException {
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
		case ATTACK_MOVE:
			attack_move();
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
			case 'd':
				int[] deadEnemyArchonIDs = Radio.decodeShorts(sb);
				for (int id : deadEnemyArchonIDs) {
					enemyArchonInfo.reportEnemyArchonKill(id);
				}
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	public void initialize()
	{
		// set nav mode
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		// set radio addresses
		io.setAddresses(new String[] {"#x", "#a"});
		// see if i'm the defender
		isDefender = currLoc.equals(dc.getAlliedArchons()[5]);
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
					armySizeBuilt++;
				}
			}
		}
		// distribute flux
		fm.manageFlux();
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
	
	
//	radio 
//	a - attack loc
//	s - swarm - dir, target loc
//	z - swarm retreat, dir
//	x - ??? profit
	public void attack_move()
	{
		if (currLoc.equals(dc.getAlliedArchons()[0]))
		{
//			leader code
			ur.scan(false, true);
			
			if (ur.numEnemyArchons>0)
			{
				
			}
			
			
		} else
		{
//			follow code
			
			
			
		}
	}
	
	public boolean checkAttackMoveTargets()
	{
		return false;
	}
	
	public void power_cap()
	{
		
	}
	
	public void defend_base()
	{
		
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

}
