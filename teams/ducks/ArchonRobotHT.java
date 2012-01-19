package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ArchonRobotHT extends BaseRobot{
	enum StrategyState {
		/** Initial split. */
		SPLIT,
		/** Seek and destroy towards a target. */
		RUSH, 
		/** Hold a position. */
		DEFEND,
		/** Take power nodes. */
		CAP;
	}
	enum BehaviorState {
		/** No enemies to deal with. */
		SWARM,
		/** Run away from enemy forces. */
		RETREAT, 
		/** Fight the enemy forces. Micro. */
		BATTLE, 
		/** Track enemy's last position and keep following them. */
		CHASE;
	}
	int myArchonID;
	int keepTargetTurns;
	MapLocation target;
	StrategyState strategy;
	BehaviorState behavior;
	public ArchonRobotHT(RobotController myRC) throws GameActionException {
		super(myRC);
		
		keepTargetTurns = -1;
		// compute archon ID
		MapLocation[] alliedArchons = this.dc.getAlliedArchons();
		for(int i=alliedArchons.length; --i>=0; ) {
			if(alliedArchons[i].equals(this.curLoc)) {
				myArchonID = i;
				break;
			}
		}
		io.setChannels(new BroadcastChannel[] {
				BroadcastChannel.ALL,
				BroadcastChannel.ARCHONS,
				BroadcastChannel.EXPLORERS
		});
		fbs.setBattleMode();
		nav.setNavigationMode(NavigationMode.TANGENT_BUG);
		strategy = StrategyState.SPLIT;
		behavior = BehaviorState.BATTLE;
	}
	
	@Override
	public void run() throws GameActionException {
		if(Clock.getRoundNum()>1200) {
			strategy = StrategyState.CAP;
		} else if(Clock.getRoundNum()>1000) {
			strategy = StrategyState.DEFEND;
		} else if(Clock.getRoundNum()>50) {
			strategy = StrategyState.RUSH;
		}
		radar.scan(true, true);
		if(radar.closestEnemy != null) {
			target = radar.closestEnemy.location;
			keepTargetTurns = 30;
		} else {
			keepTargetTurns--;
		}
		
		if(keepTargetTurns<0) {
			if(strategy == StrategyState.DEFEND) {
				target = myHome;
			} else if(strategy == StrategyState.RUSH) {
				target = mc.guessEnemyPowerCoreLocation();
			} else {
				target = mc.guessBestPowerNodeToCapture();
			}
		}
		rc.setIndicatorString(0, "Target: <"+(target.x-curLoc.x)+","+(target.y-curLoc.y)+">");
		nav.setDestination(target);
		
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
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
	public MoveInfo computeNextMove() throws GameActionException {
		
		int fluxToMakeSoldierAt = (strategy==StrategyState.CAP) ? 210 : 150;
		
		if(strategy == StrategyState.SPLIT) {
			return new MoveInfo(curLoc.directionTo(myHome).opposite(), false);
		}
		if(radar.closestEnemyDist <= 20) {
			return new MoveInfo(curLoc.directionTo(radar.getEnemySwarmCenter()).opposite(), true);
		}
		if(strategy == StrategyState.CAP && 
				rc.canMove(curDir) && 
				curLocInFront.equals(nav.getDestination()) && 
				mc.isPowerNode(curLocInFront)) {
			if(rc.getFlux() > 200) {
				return new MoveInfo(RobotType.TOWER, curDir);
			}
		} else if(rc.getFlux() > fluxToMakeSoldierAt) {
			if(rc.canMove(curDir)) {
				return new MoveInfo(RobotType.SOLDIER, curDir);
			} else {
				return new MoveInfo(curDir.rotateLeft());
			}
		} else {
			Direction dir = nav.navigateToDestination();
			if(dir==null) 
				return null;
			if(curLoc.add(dir).equals(nav.getDestination()))
				return new MoveInfo(dir);
			return new MoveInfo(dir, false);
		}
		return null;
	}
	
	@Override
	public void useExtraBytecodes() {
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>2500)
			nav.prepare(); 
		if(Clock.getRoundNum()%6==myArchonID) {
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3700)
				ses.broadcastMapFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1700)
				ses.broadcastPowerNodeFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>450) 
				ses.broadcastMapEdges();
		}
		super.useExtraBytecodes();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>2500) 
			nav.prepare();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1050) 
			mc.extractUpdatedPackedDataStep();
	}
}
