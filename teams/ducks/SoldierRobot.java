package ducks;

import ducks.Debug.Owner;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class SoldierRobot extends StrategyRobot {
	
	private boolean initialized;
	private final HibernationEngine hbe;
	
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
	
	
	private int roundsSinceSeenEnemy;
	private Direction lastChaseDirection;
	
	private int lastScanRound;
	
	
	public SoldierRobot(RobotController myRC) {
		super(myRC, RobotState.INITIALIZE);
		hbe = new HibernationEngine(this);
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
				if (currRound < Constants.BUILD_ARMY_ROUND_THRESHOLD)
					return RobotState.HOLD_POSITION;
				else
					return RobotState.SWARM;
			} 
		} break;
		case SWARM:
		{
			scanForEnemies();
			if (ur.numEnemyRobots>0)
				return RobotState.CHASE;
		} break;
		case CHASE:
		{
			if (roundsSinceSeenEnemy > Constants.SOLDIER_CHASE_ROUNDS)
				return RobotState.SWARM;
			else if (currLoc.distanceSquaredTo(archonOVERLORD) > Constants.SOLDIER_CHASE_DISTANCE_SQUARED)
				return RobotState.SWARM;
			
		} break;
		case HOLD_POSITION:
		{
			if(ao.getArchonOwnerID()==5) {
				return RobotState.DEFEND_BASE;
			}
		} break;
		case HIBERNATE:
		{
			return RobotState.DEFEND_BASE;
		}
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
			mi.setHoldPositionMode();
			// set flux management mode
			fm.setBatteryMode();
		} break;
		case SWARM:
		{
			// set micro mode
			mi.setSwarmMode(2);
			// set flux management mode
			fm.setBatteryMode();
		} break;
		case CHASE:
		{
			// set micro mode
			mi.setChargeMode();
			// set flux management mode
			fm.setBattleMode();
		} break;
		case DEFEND_BASE:
		{
			io.addAddress("#d");
			mi.setObjective(myHome);
			mi.setNormalMode();
		} break;
		default:
			break;
		}
	}

	@Override
	public void execute(RobotState state) throws GameActionException {
		// TODO(jven): use hibernation engine instead
		// power down if not enough flux
		if (currFlux < Constants.MIN_ROBOT_FLUX) {
			debug.setIndicatorString(0, "" + myType + " - LOW FLUX", Owner.ALL);
			return;
		}
		// TODO(jven): debug, archon ownership stuff
		debug.setIndicatorString(
				2, "Owner ID: " + ao.getArchonOwnerID(), Owner.JVEN);
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
				io.sendWakeupCall();
				io.sendShort("#zz", 0);
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
			char msgType, StringBuilder sb) throws GameActionException {
		
		swarmPriority = 999;
		
		switch(msgType) {
			case 'd':
			{
				eai.reportEnemyArchonKills(Radio.decodeShorts(sb));
			} break;
			case 'o':
			{
				ao.processOwnership(Radio.decodeShorts(sb));
				if (ao.getArchonOwnerID()==5)
					isDefender = true;
				else
					isDefender = false;
				
				ownerTrueID = ao.getArchonOwnerID();
//				ownerRobotID = ao.getArchonRobotID();
				
				findArchon();
				
			} break;
			case 'w':
			{
				if (getCurrentState() == RobotState.HOLD_POSITION) {
					if (isDefender)
						gotoState(RobotState.DEFEND_BASE);
					else
						gotoState(RobotState.SWARM);
				}
			} break;
			case 's':
			{
				if (getCurrentState() == RobotState.HOLD_POSITION) {
					gotoState(RobotState.SWARM);
				}
				int[] msg = Radio.decodeShorts(sb);
				swarmDirection = Constants.directions[msg[1]];
				swarmObjective = new MapLocation(msg[2],msg[3]);
				
			} break;
			case 'z':
			{
				gotoState(RobotState.HIBERNATE);
			} break;
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	public void initialize() throws GameActionException {
		// set navigation mode
		nav.setNavigationMode(NavigationMode.BUG);
		// set radio addresses
		io.setAddresses(new String[] {"#x", "#s"});
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
		mi.attackMove();
		// distribute flux
		fm.manageFlux();
		// send dead enemy archon info
		eai.sendDeadEnemyArchonIDs();
	}
	
	public void defendBase() throws GameActionException {
		mi.attackMove();
		fm.manageFlux();
		eai.sendDeadEnemyArchonIDs();
	}
	
	public void swarm() throws GameActionException {
		
		if (!rc.isMovementActive())
		{
			findArchon();
			
			if (swarmObjective==null)
			{
				mi.setObjective(archonOVERLORD);
				mi.attackMove();
			} else
			{
				MapLocation[] archons = dc.getAlliedArchons();
//				MapLocation loc = archons[0];
				MapLocation loc = archonOVERLORD;
				loc.add(swarmDirection, Constants.SOLDIER_SWARM_IN_FRONT);
				
				mi.setObjective(loc);
				mi.attackMove();
				
//				int dist = currLoc.distanceSquaredTo(loc);
//				if (dist <= Constants.SOLDIER_SWARM_DISTANCE)
//				{
//					if (currDir == swarmDirection)
//					{
//						
//					} else
//					{
//						rc.setDirection(swarmDirection);
//					}
//				} else
//				{
//					mi.setObjective(loc);
//					mi.attackMove();
//				}
			}
		}
	}
	
	public void chase() throws GameActionException {
		scanForEnemies();
		if (ur.numEnemyRobots==0)
		{
			roundsSinceSeenEnemy++;
			if (lastChaseDirection!=null)
			{
				MapLocation chaseLoc = currLoc.add(lastChaseDirection,Constants.SOLDIER_CHASE_DISTANCE_MULTIPLIER);
			} else {
				roundsSinceSeenEnemy += 9999;
			}
		} else
		{
			roundsSinceSeenEnemy = 0;
			
			mi.setChargeMode();
			mi.setObjective(ur.closetEnemy.location);
			mi.attackMove();
			
		}
	}
	
	public void scanForEnemies() throws GameActionException {
		if (currRound>lastScanRound)
		{
			ur.scan(false, true);
			lastScanRound = currRound;
		}
	}
}
