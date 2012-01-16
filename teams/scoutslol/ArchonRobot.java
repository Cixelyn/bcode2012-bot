package scoutslol;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class ArchonRobot extends BaseRobot {

	private int splitTime;
	private Direction splitDirection;
	
	private int rallyPriority;
	private Direction rushDirection;
	private MapLocation explorationTarget;
	private MapLocation objective;
	
	public ArchonRobot(RobotController myRC) {
		super(myRC);
		currState = RobotState.INITIALIZE;
	}

	@Override
	public void run() throws GameActionException {
		switch (currState) {
			case INITIALIZE:
				initialize();
				break;
			case SPLIT:
				split();
				break;
			case SPAWN_SCOUTS:
				spawnScouts();
				break;
		}
	}
	
	@Override
	public void processMessage(char msgType, StringBuilder sb) {
		int[] msgInts;
		switch(msgType) {
			case 'r':
				msgInts = Radio.decodeInts(sb);
				int msgRallyPriority = msgInts[0];
				Direction msgRushDirection = Direction.values()[msgInts[1]];
				if (msgRallyPriority > rallyPriority) {
					rushDirection = msgRushDirection;
					explorationTarget = currLoc.add(
							rushDirection, GameConstants.MAP_MAX_HEIGHT);
					objective = new MapLocation(msgInts[2], msgInts[3]);
					rallyPriority = msgRallyPriority;
				}
				break;
			default:
				super.processMessage(msgType, sb);
		}
	}
	
	public void initialize() throws GameActionException {
		// set nav mode
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		// set addresses
		io.setAddresses(new String[] {"#x"});
		// initially unsure
		rallyPriority = 0;
		// set split direction
		splitDirection = rc.sensePowerCore().getLocation().directionTo(currLoc);
		// get rush direction
		boolean up = rc.senseTerrainTile(currLoc.add(Direction.NORTH, 6)) ==
				TerrainTile.OFF_MAP;
		boolean right = rc.senseTerrainTile(currLoc.add(Direction.EAST, 6)) ==
				TerrainTile.OFF_MAP;
		boolean down = rc.senseTerrainTile(currLoc.add(Direction.SOUTH, 6)) ==
				TerrainTile.OFF_MAP;
		boolean left = rc.senseTerrainTile(currLoc.add(Direction.WEST, 6)) ==
				TerrainTile.OFF_MAP;
		if (up && right) {
			rushDirection = Direction.SOUTH_WEST;
			rallyPriority = 2;
		} else if (right && down) {
			rushDirection = Direction.NORTH_WEST;
			rallyPriority = 2;
		} else if (down && left) {
			rushDirection = Direction.NORTH_EAST;
			rallyPriority = 2;
		} else if (left && up) {
			rushDirection = Direction.SOUTH_EAST;
			rallyPriority = 2;
		} else if (up) {
			rushDirection = Direction.SOUTH;
			rallyPriority = 1;
		} else if (right) {
			rushDirection = Direction.WEST;
			rallyPriority = 1;
		} else if (down) {
			rushDirection = Direction.NORTH;
			rallyPriority = 1;
		} else if (left) {
			rushDirection = Direction.EAST;
			rallyPriority = 1;
		} else {
			rushDirection = Constants.INITIAL_BEARING;
		}
		explorationTarget = currLoc.add(
				rushDirection, GameConstants.MAP_MAX_HEIGHT);
		objective = explorationTarget;
		// communicate rush direction
		sendRushDirection();
		// split
		currState = RobotState.SPLIT;
		split();
	}
	
	public void split() throws GameActionException {
		// wait if movement is active
		if (rc.isMovementActive()) {
			return;
		}
		// if not facing away from power core, set direction and wait
		if (currDir != splitDirection) {
			rc.setDirection(splitDirection);
			return;
		}
		// if we can't move away from the power core, start spawning soldiers
		if (!rc.canMove(splitDirection)) {
			rc.setDirection(rushDirection);
			currState = RobotState.SPAWN_SCOUTS;
			return;
		}
		boolean keepGoing = false;
		// if we're too close to another archon, keep going
		if (currLoc.distanceSquaredTo(dc.getClosestArchon()) <
				Constants.SPLIT_DISTANCE) {
			keepGoing = true;
		}
		// if there's not enough room to build units, keep going
		int numOpenSquares = 0;
		for (boolean canMove : dc.getMovableDirections()) {
			if (canMove) {
				numOpenSquares++;
			}
		}
		if (numOpenSquares < Constants.SOLDIERS_PER_ARCHON) {
			keepGoing = true;
		}
		// if we time out, stop splitting
		if (splitTime++ > Constants.MAX_SPLIT_TIME) {
			keepGoing = false;
		}
		// keep splitting if necessary
		if (keepGoing) {
			rc.moveForward();
		} else {
			rc.setDirection(rushDirection);
			currState = RobotState.SPAWN_SCOUTS;
		}
	}

	public void spawnScouts() throws GameActionException {
		// build scouts in front
		spawnUnitInDir(RobotType.SCOUT, rushDirection);
		// send rush direction
		sendRushDirection();
		// distribute flux
		distributeFlux();
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
	
	private void distributeFlux() throws GameActionException {
		// check all directions around you, ground and air
		for (Direction d : Direction.values()) {
			// ignore none direction
			if (d == Direction.NONE) {
				continue;
			}
			for (RobotLevel level : RobotLevel.values()) {
				// if we don't have flux to give, abort
				if (this.currFlux < Constants.MIN_ARCHON_FLUX) {
					break;
				}
				// ignore power node level
				if (level == RobotLevel.POWER_NODE) {
					continue;
				}
				// can't give flux to yourself, silly!
				if (d == Direction.OMNI && level == RobotLevel.ON_GROUND) {
					continue;
				}
				GameObject obj = dc.getAdjacentGameObject(d, level);
				if (obj instanceof Robot && obj.getTeam() == myTeam) {
					// TODO(jven): data cache this?
					RobotInfo rInfo = rc.senseRobotInfo((Robot)obj);
					// don't give flux to towers
					if (rInfo.type == RobotType.TOWER || rInfo.type == RobotType.ARCHON) {
						continue;
					}
					if (rInfo.flux <
							Constants.MIN_UNIT_FLUX_RATIO * rInfo.type.maxFlux) {
						double fluxToTransfer = Math.min(
								Constants.MIN_UNIT_FLUX_RATIO * rInfo.type.maxFlux - rInfo.flux,
								currFlux - Constants.MIN_ARCHON_FLUX);
						if (fluxToTransfer > 0) {
							// if we throw an exception, our info is stale, so abort
							try {
								rc.transferFlux(
										rInfo.location, rInfo.robot.getRobotLevel(), fluxToTransfer);
								currFlux -= fluxToTransfer;
							} catch (GameActionException e) {
								return;
							}
						}
					}
				}
			}
		}
	}
	
	private void sendRushDirection() throws GameActionException {
		io.sendInts("#xr", new int[] {rallyPriority, rushDirection.ordinal(),
				objective.x, objective.y});
	}
}
