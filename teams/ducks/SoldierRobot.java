package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class SoldierRobot extends StrategyRobot {
	
	private boolean initialized;
	private final HibernationSystem hbe;
	
	private int ownerTrueID;
//	private int ownerRobotID;
	
	private boolean isDefender;
	private MapLocation swarmObjective;
	private Direction swarmDirection;
	private int swarmPriority;
	private MapLocation archonOVERLORD;
	
	private RobotInfo target;
	private MapLocation targetLoc;
	private RobotInfo target2;
	private MapLocation target2Loc;
	
	private MapLocation fromObjective;
	private int enemydiff;
	
	private int roundsSinceSeenEnemy;
	private Direction lastChaseDirection;
	
	private int lastScanRound;
	
	
	public SoldierRobot(RobotController myRC) throws GameActionException {
		super(myRC, RobotState.INITIALIZE);
		hbe = new HibernationSystem(this);
		initialized = false;
		lastScanRound = -1;
		ownerTrueID = -1;
//		ownerRobotID = -1;
	}


	@Override
	public RobotState processTransitions(RobotState state)
			throws GameActionException {
		
		switch (state) 
		{
		case INITIALIZE:
		{
			if (initialized) {
				if (curRound < Constants.BUILD_ARMY_ROUND_THRESHOLD)
					return RobotState.HOLD_POSITION;
				else
					return RobotState.SWARM;
			} 
		} break;
		case SWARM:
		{
			scanForEnemies();
			if (radar.numEnemyRobots>0 && enemydiff>=0)
				return RobotState.CHASE;
		} break;
		case CHASE:
		{
			if (roundsSinceSeenEnemy > Constants.SOLDIER_CHASE_ROUNDS)
				return RobotState.SWARM;
			else if (curLoc.distanceSquaredTo(archonOVERLORD) > Constants.SOLDIER_CHASE_DISTANCE_SQUARED)
				return RobotState.SWARM;
			
		} break;
		case HOLD_POSITION:
		{
			if(ao.getArchonOwnerID()==5) {
				return RobotState.DEFEND_BASE;
			}
		} break;
		case HIBERNATE:
		{}
		default:
			break;
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
		case HOLD_POSITION:
		{
			// set micro mode
			micro.setHoldPositionMode();
			// set flux management mode
			fbs.setBatteryMode();
		} break;
		case SWARM:
		{
			// set micro mode
			//micro.setSwarmMode(2, 36);
			// set flux management mode
			fbs.setBatteryMode();
		} break;
		case CHASE:
		{
			// set micro mode
			micro.setChargeMode();
			// set flux management mode
			fbs.setBattleMode();
		} break;
		case DEFEND_BASE:
		{
			micro.setObjective(myHome);
			micro.setNormalMode();
		} break;
		default:
			break;
		}
	}

	@Override
	public void execute(RobotState state) throws GameActionException {
		// TODO(jven): use hibernation engine instead
		// power down if not enough flux
		if (rc.getFlux() < Constants.MIN_ROBOT_FLUX) {
			return;
		}
		// TODO(jven): debug, archon ownership stuff
		switch (state) {
			case INITIALIZE:
				initialize();
				break;
			case HOLD_POSITION:
				holdPosition();
				break;
			case HIBERNATE:
				hbe.run(); //this call will halt until wakeup
				gotoState(RobotState.DEFEND_BASE);
				
//				io.sendWakeupCall();
//				io.sendShort("#zz", 0);
				break;
			case DEFEND_BASE:
				defendBase();
				break;
			case SUICIDE:
				rc.suicide();
				break;
			case SWARM:
				swarm();
				break;
			case CHASE:
				chase();
				break;
			default:
				break;
		}
	}
	
	@Override
	public void processMessage(
			BroadcastType msgType, StringBuilder sb) throws GameActionException {
		
		swarmPriority = 999;
		int closest = 999;
		MapLocation temp;
		
	}
	
	public void initialize() throws GameActionException {
		// set navigation mode
		nav.setNavigationMode(NavigationMode.BUG);
		// set radio addresses
		io.setChannels(new BroadcastChannel[] {BroadcastChannel.ALL, BroadcastChannel.SOLDIERS});
		// sense all
		mc.senseAll();
		
		// just get closest for now
		findArchon();
		
		// done
		initialized = true;
	}
	
	private void findArchon() throws GameActionException {
		if (ownerTrueID >= 0)
		{
			MapLocation[] archons = dc.getAlliedArchons();
			archonOVERLORD = archons[(ownerTrueID)%archons.length];
		} else
		{
			archonOVERLORD = dc.getClosestArchon();
		}
	}
	
	public void holdPosition() throws GameActionException {
		// hold position
		micro.attackMove();
		// distribute flux
		fbs.manageFlux();
		// send dead enemy archon info
		eakc.broadcastDeadEnemyArchonIDs();
	}
	
	public void swarm() throws GameActionException {
		
		
		
		if (enemydiff>=0)
		{
			if (swarmObjective == null)
			{
				findArchon();
				//micro.setSwarmMode(2, 36);
				micro.setObjective(archonOVERLORD);
				micro.attackMove();
			} else
			{
				micro.setChargeMode();
				micro.setObjective(swarmObjective);
				micro.attackMove();
			}
			
		} else
		{
			if (fromObjective == null)
			{
				findArchon();
				//micro.setSwarmMode(2, 36);
				micro.setObjective(archonOVERLORD);
				micro.attackMove();
			} else
			{
				//micro.setSwarmMode(2, 36);
				micro.setObjective(fromObjective);
				micro.attackMove();
			}
		}
		
		fbs.manageFlux();
		
		return;
		
//		if (!rc.isMovementActive())
//		{
//			findArchon();
//			
//			if (swarmObjective==null)
//			{
//				mi.setObjective(archonOVERLORD);
//				mi.attackMove();
//			} else
//			{
//				MapLocation[] archons = dc.getAlliedArchons();
////				MapLocation loc = archons[0];
//				MapLocation loc = archonOVERLORD;
//				loc.add(swarmDirection, Constants.SOLDIER_SWARM_IN_FRONT);
//				
//				mi.setObjective(loc);
//				mi.attackMove();
//				
////				int dist = currLoc.distanceSquaredTo(loc);
////				if (dist <= Constants.SOLDIER_SWARM_DISTANCE)
////				{
////					if (currDir == swarmDirection)
////					{
////						
////					} else
////					{
////						rc.setDirection(swarmDirection);
////					}
////				} else
////				{
////					mi.setObjective(loc);
////					mi.attackMove();
////				}
//			}
//		}
	}
	
	
	
	
	public void chase() throws GameActionException {
		scanForEnemies();
		if (radar.numEnemyRobots-radar.numEnemyTowers==0)
		{
			roundsSinceSeenEnemy++;
			if (lastChaseDirection!=null)
			{
				MapLocation chaseLoc = curLoc.add(lastChaseDirection,Constants.SOLDIER_CHASE_DISTANCE_MULTIPLIER);
				micro.setChargeMode();
				micro.setObjective(chaseLoc);
				micro.attackMove();
			} else {
				roundsSinceSeenEnemy += 9999;
			}
		} else
		{
			roundsSinceSeenEnemy = 0;
			
			micro.setKiteMode(3);
			micro.setObjective(radar.closestEnemy.location);
			micro.attackMove();
			
		}
		fbs.manageFlux();
	}
	
	public void scanForEnemies() throws GameActionException {
		if (curRound>lastScanRound)
		{
			radar.scan(false, true);
			lastScanRound = curRound;
		}
	}
	
	public void defendBase() throws GameActionException {
		micro.attackMove();
		fbs.manageFlux();
		eakc.broadcastDeadEnemyArchonIDs();
	}
}
