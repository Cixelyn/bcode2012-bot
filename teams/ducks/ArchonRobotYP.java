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

public class ArchonRobotYP extends StrategyRobotExtended {

	public ArchonRobotYP(RobotController myRC) {
		super(myRC, RobotState.INITIALIZE);
		initialized = false;
	}

	boolean isLeader;
	public int archonIndex;
	boolean initialized;
	Direction expdir;
	MapLocation lastexploc;
	MapLocation curexploc;
	
	
	
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
				expdir = Constants.directions[BroadcastSystem.decodeShort(sb)];
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
							rc.spawn(RobotType.TOWER);
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
		
		MapLocation[] archons = dc.getAlliedArchons();
		
		MapLocation[] formation = createFormation(dir,curLoc);
		
		for (int x=1; x<archons.length; x++)
		{
			if (!archons[x].equals(formation[x]) &&
					(rc.senseTerrainTile(formation[x])==TerrainTile.LAND 
					&& archons[x].distanceSquaredTo(formation[x]) > ArchonConstantsYP.FORMATION_CLOSENESS))
				return false;
		}
		return true;
	}
	
	private MapLocation[] createFormation(Direction dir, MapLocation base) {
		MapLocation[] locs = new MapLocation[dc.getAlliedArchons().length];
		
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
		
		return locs;
	}

	private void swarm() throws GameActionException {
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
		
		MapLocation[] formation = createFormation(expdir, loc);
		micro.setObjective(formation[archonIndex]);
		micro.attackMove();
	}
	
	private void sendSwarmInfo(Direction dir)
	{
		io.sendShort("#aa", dir.ordinal());
	}
}
