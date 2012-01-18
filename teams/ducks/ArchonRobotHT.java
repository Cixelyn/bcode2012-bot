package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ArchonRobotHT extends BaseRobot{
	int myArchonID;
	int keepTargetTurns;
	MapLocation target;
	public ArchonRobotHT(RobotController myRC) {
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
	}
	
	@Override
	public void run() throws GameActionException {
//		if(myArchonID==0 && Clock.getRoundNum()%500==5) {
//			System.out.println(mc);
//			System.out.println(mc.guessEnemyPowerCoreLocation());
//			System.out.println(mc.guessBestPowerNodeToCapture());
//		}
		
		fbs.manageFlux();
		
		if(Clock.getBytecodeNum()<5000 && Clock.getRoundNum()%6==myArchonID) {
			ses.broadcastPowerNodeFragment();
			ses.broadcastMapFragment();
			ses.broadcastMapEdges();
			mc.extractUpdatedPackedDataStep();
		}
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
		boolean startCapping = Clock.getRoundNum()>1000;
		radar.scan(false, true);
		if(radar.closestEnemy != null) {
			target = radar.closestEnemy.location;
			keepTargetTurns = 30;
		} else {
			keepTargetTurns--;
		}
		
		if(keepTargetTurns<0) 
			target = startCapping ? (myArchonID==0 ? myHome : 
				mc.guessBestPowerNodeToCapture()) : 
				mc.guessEnemyPowerCoreLocation();
		rc.setIndicatorString(0, "Target: <"+(target.x-curLoc.x)+","+(target.y-curLoc.y)+">");
		nav.setDestination(target);
		
		
		
		if(Clock.getRoundNum() < 60) {
			return new MoveInfo(curLoc.directionTo(myHome).opposite(), false);
		}
		if(radar.closestEnemyDist <= 20) {
			return new MoveInfo(curLoc.directionTo(radar.getEnemySwarmCenter()).opposite(), true);
		}
		if(rc.canMove(curDir) && curLocInFront.equals(nav.getDestination()) && 
				mc.isPowerNode(curLocInFront)) {
			if(startCapping && rc.getFlux() > 200) {
				return new MoveInfo(RobotType.TOWER, curDir);
			}
		} else if(rc.getFlux()> (startCapping ? 210 : 200)) {
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
}
