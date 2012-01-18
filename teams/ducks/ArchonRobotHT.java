package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ArchonRobotHT extends BaseRobot{
	int myArchonID;
	public ArchonRobotHT(RobotController myRC) {
		super(myRC);
		
		// compute archon ID
		MapLocation[] alliedArchons = this.dc.getAlliedArchons();
		for(int i=alliedArchons.length; --i>=0; ) {
			if(alliedArchons[i].equals(this.curLoc)) {
				myArchonID = i;
				break;
			}
		}
		io.setAddresses(new String[] {"#e", "#x", "#a"});
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
	public void processMessage(MessageType msgType, StringBuilder sb) throws GameActionException {
		int[] data = null;
		switch(msgType) {
		case MAP_EDGES:
			data = BroadcastSystem.decodeUShorts(sb);
			ses.receiveMapEdges(data);
			break;
		case MAP_FRAGMENTS:
			data = BroadcastSystem.decodeInts(sb);
			ses.receiveMapFragment(data);
			break;
		case POWERNODE_FRAGMENTS:
			data = BroadcastSystem.decodeInts(sb);
			ses.receivePowerNodeFragment(data);
			break;
		default:
			super.processMessage(msgType, sb);
				
		} 
	}
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		MapLocation target = mc.guessBestPowerNodeToCapture();
		rc.setIndicatorString(0, "Target: <"+(target.x-curLoc.x)+","+(target.y-curLoc.y)+">");
		nav.setDestination(target);
		
		if(rc.canMove(curDir) && curLocInFront.equals(nav.getDestination())) {
			if(rc.getFlux()>200) {
				return new MoveInfo(RobotType.TOWER, curDir);
			}
		} else if(rc.getFlux()>280) {
			if(rc.canMove(curDir)) {
				return new MoveInfo(RobotType.SOLDIER, curDir);
			} else {
				return new MoveInfo(curDir.rotateLeft());
			}
		} else {
			Direction dir = nav.navigateToDestination();
			if(dir==null) return null;
			if(curLoc.add(dir).equals(nav.getDestination()))
				return new MoveInfo(dir);
			return new MoveInfo(dir, false);
		}
		return null;
	}
}
