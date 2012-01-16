package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class ArchonRobot extends StrategyRobot {

	
	private RobotInfo attackTarget;
	private MapLocation attackMoveTarget;
	private Direction attackMoveDirection;
	private MapLocation enemyPowerNode;
	
	private int armySizeBuilt;
	private int armySizeTarget;
	private boolean isDefender;
	private boolean initialized;
	
	
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
			if (currRound > Constants.ROUNDS_TO_EXPLORE)
				return RobotState.GOHOME;
		} break;
		case GOHOME:
		{
			MapLocation archon0 = dc.getAlliedArchons()[0];
			if (currLoc.equals(archon0))
			{
				if (currLoc.distanceSquaredTo(myHome) <= Constants.DISTANCE_TO_HOME_ON_GOHOME)
					return RobotState.BUILD_ARMY;
			} else if (currLoc.distanceSquaredTo(archon0) <= Constants.DISTANCE_TO_ARCHON0_ON_GOHOME)
				return RobotState.BUILD_ARMY;
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
			
		} break;
		case GOHOME:
		{
			
		} break;
		case BUILD_ARMY:
		{
			switch (oldstate)
			{
			case GOHOME:
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
		

		isDefender = currLoc.equals(dc.getAlliedArchons()[5]);
		
		
		initialized = true;
	}
	
	public void explore()
	{
		
	}
	
	public void gohome()
	{
		
	}
	
	public void build_army()
	{
		
	}
	
	public void attack_move()
	{
		
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

}
