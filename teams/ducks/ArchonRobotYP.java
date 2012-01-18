package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

class ArchonConstantsYP {
	public static final int FORMATION_CLOSENESS = 5;
}

enum FormationType {
	FIXED_PYRAMID,
	FIXED_ARROW,
	FIXED_F,
}

public class ArchonRobotYP extends StrategyRobotExtended {

	public ArchonRobotYP(RobotController myRC) {
		super(myRC, RobotState.INITIALIZE);
		initialized = false;
		formation = FormationType.FIXED_PYRAMID;
	}

	boolean isLeader;
	public int archonIndex;
	boolean initialized;
	Direction expdir;
	MapLocation lastexploc;
	MapLocation curexploc;
	
	FormationType formation;
	static final FormationType[] formationvals = FormationType.values();
	
	@Override
	public RobotState processTransitions(RobotState state)
			throws GameActionException {
		
		if (isLeader)
		{
			switch (state)
			{
			case INITIALIZE:
			{
				if (initialized)
					return RobotState.EXPLORE;
			} break;
			case EXPLORE:
			{
				
			} break;
			case SUICIDE:
			{
//				this unit just became a leader
				nav.setNavigationMode(NavigationMode.BUG);
				micro.setNormalMode();
				return RobotState.EXPLORE;
			}
			}
		} else
		{
			switch (state)
			{
			case INITIALIZE:
			{
				if (initialized)
					return RobotState.SWARM;
			}
			}
		}
		
		return state;
	}

	@Override
	public void prepareTransition(RobotState newstate, RobotState oldstate)
			throws GameActionException {
		
		switch (newstate)
		{
		
		
		}
		
		
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb)
			throws GameActionException {
		switch (msgType) {
		case 'a' :
		{
			if (!isLeader) {
				int[] msg = BroadcastSystem.decodeShorts(sb);
				expdir = Constants.directions[msg[0]];
				formation = formationvals[msg[1]];
			}
		} break;
		}
	}
	
	@Override
	public void initializeForRound() throws GameActionException {
		if (!isLeader)
		{
			MapLocation[] archons = dc.getAlliedArchons();
			
			if (curLoc.equals(archons[0]))
			{
				archonIndex = 0;
				isLeader = true;
				gotoState(RobotState.SUICIDE);
			} else if (curLoc.equals(archons[1]))
			{
				archonIndex = 1;
				isLeader = false;
			} else if (curLoc.equals(archons[2]))
			{
				archonIndex = 2;
				isLeader = false;
			} else if (curLoc.equals(archons[3]))
			{
				archonIndex = 3;
				isLeader = false;
			} else if (curLoc.equals(archons[4]))
			{
				archonIndex = 4;
				isLeader = false;
			} else
			{
				archonIndex = 5;
				isLeader = false;
			}
		} else
		{
			if (directionToSenseIn!=null)
			{
				mc.senseAfterMove(directionToSenseIn);
				directionToSenseIn = null;
			}
		}
	}

	@Override
	public void execute(RobotState state) throws GameActionException {
		
		switch (state)
		{
		case INITIALIZE:
			initialize();
			break;
		case EXPLORE:
			explore();
			break;
		case SUICIDE:
//			????
			break;
		case SWARM:
			swarm();
			break;
		
		}
	}

	private void initialize() {
		io.addAddress("#a");
		
		if (isLeader)
		{
			nav.setNavigationMode(NavigationMode.BUG);
			micro.setNormalMode();
		} else
		{
			nav.setNavigationMode(NavigationMode.BUG);
			micro.setNormalMode();
		}
		mc.senseAll();
		
		initialized = true;
	}
	
	private void explore() throws GameActionException {
		
//		MapLocation exploreloc = myHome.add(Direction.WEST,40);
		MapLocation exploreloc = mc.guessBestPowerNodeToCapture();
		
		if (curLoc.isAdjacentTo(exploreloc))
		{
			Direction dir = curLoc.directionTo(exploreloc);
			if (!rc.isMovementActive())
			{
				if (curDir!=dir)
					rc.setDirection(dir);
				else
					if (rc.getFlux()>=RobotType.TOWER.spawnCost)
						if (rc.senseObjectAtLocation(exploreloc, RobotLevel.ON_GROUND) == null)
						{
							rc.spawn(RobotType.TOWER);
							formation = formationvals[(formation.ordinal()+1)%formationvals.length];
						}
						else
							sendSwarmInfo(dir);
			}
			return;
		}
		
		if (exploreloc.equals(lastexploc))
		{
			exploreloc = curexploc;
			switch (myHome.directionTo(exploreloc))
			{
			case WEST:
				if (mc.edgeXMin!=0)
				{
					curexploc = generateExploreLoc();
					exploreloc = curexploc;
				}
				break;
			case NORTH:
				if (mc.edgeYMin!=0)
				{
					curexploc = generateExploreLoc();
					exploreloc = curexploc;
				}
				break;
			case EAST:
				if (mc.edgeXMax!=0)
				{
					curexploc = generateExploreLoc();
					exploreloc = curexploc;
				}
				break;
			case SOUTH:
				if (mc.edgeYMax!=0)
				{
					curexploc = generateExploreLoc();
					exploreloc = curexploc;
				}
				break;
			}
		} else if (curLoc.equals(exploreloc))
		{
			lastexploc = exploreloc;
			curexploc = generateExploreLoc();
			exploreloc = curexploc;
		} else if (curLoc.isAdjacentTo(exploreloc) && !rc.canMove(curLoc.directionTo(exploreloc)))
		{
			lastexploc = exploreloc;
			curexploc = generateExploreLoc();
			exploreloc = curexploc;
		}
		
		rc.setIndicatorString(1, "curloc "+curLoc);
		rc.setIndicatorString(2, "exploring to "+exploreloc);
		
		expdir = curLoc.directionTo(exploreloc);
		
		if (!rc.isMovementActive())
		{
			nav.setDestination(exploreloc);
			Direction dir = nav.wiggleToMovableDirection(nav.navigateToDestination());
			if (dir==null) return;
			if (curDir != dir)
				rc.setDirection(dir);
			else
			{
				if (checkFormation(dir) && rc.canMove(dir))
				{
					rc.moveForward();
					directionToSenseIn = dir;
				}
				else
				{
					sendSwarmInfo(dir);
				}
			}
		}
	}
	
	MapLocation generateExploreLoc()
	{
		if (mc.edgeXMin==0)
		{
			return myHome.add(Direction.WEST,60);
		} else if (mc.edgeYMax==0)
		{
			return myHome.add(Direction.SOUTH,60);
		} else if (mc.edgeXMax==0)
		{
			return myHome.add(Direction.EAST,60);
		} else if (mc.edgeYMin==0)
		{
			return myHome.add(Direction.NORTH,60);
		}
		return myHome;
	}

	private boolean checkFormation(Direction dir) {
		switch (formation)
		{
		case FIXED_PYRAMID:
		case FIXED_ARROW:
		case FIXED_F:
			return checkFixedFormation(dir, formation);
		}
		
		return true;
	}
	
	private boolean checkFixedFormation(Direction dir, FormationType formation) {
		MapLocation[] archons = dc.getAlliedArchons();
		
		MapLocation[] formationloc = createFixedFormation(dir,curLoc,formation);
		
		for (int x=1; x<archons.length; x++)
		{
			if (!archons[x].equals(formationloc[x]) &&
					(rc.senseTerrainTile(formationloc[x])==TerrainTile.LAND 
					&& archons[x].distanceSquaredTo(formationloc[x]) > ArchonConstantsYP.FORMATION_CLOSENESS))
				return false;
		}
		return true;
	}
	
	private MapLocation[] createFixedFormation(Direction dir, MapLocation base, FormationType formation) {
		
		MapLocation[] locs = new MapLocation[dc.getAlliedArchons().length];
		
		switch (formation)
		{
		case FIXED_PYRAMID:
		{
			Direction right = dir.rotateRight().rotateRight();
			Direction left = dir.rotateLeft().rotateLeft();
			
			for (int x=1; x<locs.length; x++)
			{
				switch (x)
				{
				case 1:
					locs[1] = base.add(dir).add(right);
					break;
				case 2:
					locs[2] = base.add(dir).add(left);
					break;
				case 3:
					locs[3] = base.add(right).add(right);
					break;
				case 4:
					locs[4] = base.add(left).add(left);
					break;
				case 5:
					locs[5] = base.add(dir,3);
					break;
				}
			}
		} break;
		case FIXED_ARROW:
		{
			Direction right = dir.rotateRight().rotateRight();
			Direction left = dir.rotateLeft().rotateLeft();
			
			for (int x=1; x<locs.length; x++)
			{
				switch (x)
				{
				case 1:
					locs[1] = base.add(dir).add(right);
					break;
				case 2:
					locs[2] = base.add(dir).add(left);
					break;
				case 3:
					locs[3] = base.add(right,2);
					break;
				case 4:
					locs[4] = base.add(left,2);
					break;
				case 5:
					locs[5] = base.add(dir.opposite(),2);
					break;
				}
			}
		} break;
		case FIXED_F:
		{
			for (int x=1; x<locs.length; x++)
			{
				switch (x)
				{
				case 1:
					locs[1] = base.add(0,-4);
					break;
				case 2:
					locs[2] = base.add(2,-4);
					break;
				case 3:
					locs[3] = base.add(0,-2);
					break;
				case 4:
					locs[4] = base.add(2,0);
					break;
				case 5:
					locs[5] = base.add(0,2);
					break;
				}
			}
		} break;
		}
		
		return locs;
	}

	private void swarm() throws GameActionException {
		switch (formation)
		{
		case FIXED_ARROW:
		case FIXED_PYRAMID:
		case FIXED_F:
			fixedSwarm(formation);
			break;
		default:
			double d = 5/0;
			if (d==42) d = 2;
			
		}
		
	}
	
	private void fixedSwarm(FormationType formation) throws GameActionException {
		MapLocation loc = dc.getAlliedArchons()[0];
		if (expdir==null) 
		{
			if (rc.canSenseSquare(loc))
			{
				RobotInfo ri = rc.senseRobotInfo((Robot) rc.senseObjectAtLocation(loc, RobotLevel.ON_GROUND));
				expdir = ri.direction;
			} else
			{
				micro.setObjective(loc);
				micro.attackMove();
				return;
			}
		}
		
		MapLocation[] formationlocs = createFixedFormation(expdir, loc, formation);
		micro.setObjective(formationlocs[archonIndex]);
		micro.attackMove();
	}
	
	private void sendSwarmInfo(Direction dir)
	{
		io.sendShorts("#aa", new int[] {dir.ordinal(), formation.ordinal()});
	}
	
//	@Override
//	public MoveInfo computeNextMove() throws GameActionException {
//		if (isLeader)
//		{
//			return simpleExplore();
//		} else
//		{
//			return simpleSwarm();
//		}
//	}
	
	public MoveInfo simpleExplore() throws GameActionException
	{

//		MapLocation exploreloc = myHome.add(Direction.WEST,40);
		MapLocation exploreloc = mc.guessBestPowerNodeToCapture();
		
		if (curLoc.isAdjacentTo(exploreloc))
		{
			Direction dir = curLoc.directionTo(exploreloc);
			if (curDir!=dir)
				return new MoveInfo(RobotType.TOWER,dir);
			else
				if (rc.getFlux()>=RobotType.TOWER.spawnCost)
					if (rc.senseObjectAtLocation(exploreloc, RobotLevel.ON_GROUND) == null)
						return new MoveInfo(RobotType.TOWER,dir);
					else
						sendSwarmInfo(dir);
			return null;
		}
		
		if (exploreloc.equals(lastexploc))
		{
			exploreloc = curexploc;
			switch (myHome.directionTo(exploreloc))
			{
			case WEST:
				if (mc.edgeXMin!=0)
				{
					curexploc = generateExploreLoc();
					exploreloc = curexploc;
				}
				break;
			case NORTH:
				if (mc.edgeYMin!=0)
				{
					curexploc = generateExploreLoc();
					exploreloc = curexploc;
				}
				break;
			case EAST:
				if (mc.edgeXMax!=0)
				{
					curexploc = generateExploreLoc();
					exploreloc = curexploc;
				}
				break;
			case SOUTH:
				if (mc.edgeYMax!=0)
				{
					curexploc = generateExploreLoc();
					exploreloc = curexploc;
				}
				break;
			}
		} else if (curLoc.equals(exploreloc))
		{
			lastexploc = exploreloc;
			curexploc = generateExploreLoc();
			exploreloc = curexploc;
		} else if (curLoc.isAdjacentTo(exploreloc) && !rc.canMove(curLoc.directionTo(exploreloc)))
		{
			lastexploc = exploreloc;
			curexploc = generateExploreLoc();
			exploreloc = curexploc;
		}
		
		rc.setIndicatorString(1, "curloc "+curLoc);
		rc.setIndicatorString(2, "exploring to "+exploreloc);
		
		nav.setDestination(exploreloc);
		Direction dir = nav.wiggleToMovableDirection(nav.navigateToDestination());
		if (dir==null) return null;
		if (curDir != dir)
			rc.setDirection(dir);
		else
		{
			if (checkFormation(dir) && rc.canMove(dir))
			{
				rc.moveForward();
				directionToSenseIn = dir;
			}
			else
			{
				sendSwarmInfo(dir);
			}
		}
		return null;
	}
	
	
	public MoveInfo simpleSwarm() throws GameActionException
	{
		MapLocation loc = dc.getAlliedArchons()[0];
		if (expdir==null) 
		{
			if (rc.canSenseSquare(loc))
			{
				RobotInfo ri = rc.senseRobotInfo((Robot) rc.senseObjectAtLocation(loc, RobotLevel.ON_GROUND));
				expdir = ri.direction;
			} else
			{
				nav.setDestination(loc);
				return new MoveInfo(nav.navigateToDestination());
			}
		}
		
		MapLocation[] formationlocs = createFixedFormation(expdir, loc, formation);
		nav.setDestination(formationlocs[archonIndex]);
		return new MoveInfo(nav.navigateToDestination());
	}
}
