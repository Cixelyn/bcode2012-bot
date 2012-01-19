package ducks;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ArchonRobotCL extends BaseRobot {
	
	private enum BehaviorState{SPLIT,DEFEND,RUSH,CAP}
	BehaviorState behavior;
	int myArchonID;
	
	public ArchonRobotCL(RobotController myRC) throws GameActionException {
		super(myRC);
		MapLocation[] alliedArchons = this.dc.getAlliedArchons();
		for(int i=alliedArchons.length; --i>=0; ) {
			if(alliedArchons[i].equals(this.curLoc)) {
				myArchonID = i;
				break;
			}
		}
		
		behavior = BehaviorState.SPLIT;
		
		io.addChannel(BroadcastChannel.ALL);
		io.addChannel(BroadcastChannel.ARCHONS);
	}
	
	
	MapLocation target;
	

	@Override
	public void run() throws GameActionException {
		rc.setIndicatorString(0,behavior.toString());
		
		switch(behavior) {
		case SPLIT:
			if(dc.getClosestArchon().distanceSquaredTo(curLoc) >= 16) {
				behavior = BehaviorState.DEFEND;
			}
			break;
		case DEFEND:
			radar.scan(false, true);
			if(radar.closestEnemy != null) {
				if(radar.closestEnemyDist < 16) {
					target = radar.closestEnemy.location;
					behavior = BehaviorState.RUSH;
					io.sendMapLoc(BroadcastChannel.ALL, BroadcastType.RALLY, radar.closestEnemy.location);
				}
			}
		case RUSH:
			fbs.setBattleMode();
			break;
		case CAP:
			break;
		default:
			break;
		}
		
	}
	
	
	
	
	@Override
	public MoveInfo computeNextMove() throws GameActionException {
		switch(behavior) {
		case SPLIT:
			return new MoveInfo(curLoc.directionTo(myHome).opposite(), false);
		case DEFEND:
			if(rc.getFlux() > 150) {
				boolean[] openDirs = dc.getMovableDirections();
				for(int i=0; i<openDirs.length; i++) {
					if(openDirs[i]) {
						return new MoveInfo(RobotType.SOLDIER,Constants.directions[i]);
					}
				}
			}
			
		default:
			return null;
		}
	}
	
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
		case RALLY:
			target = BroadcastSystem.decodeMapLoc(sb);
			behavior = BehaviorState.RUSH;
			break;
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
	public void useExtraBytecodes() {
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3000)
			nav.prepare(); 
		if(Clock.getRoundNum()%6==myArchonID) {
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>4000)
				ses.broadcastMapFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>2000)
				ses.broadcastPowerNodeFragment();
			if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1000) 
				ses.broadcastMapEdges();
		}
		super.useExtraBytecodes();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>3000) 
			nav.prepare();
		while(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1000) 
			mc.extractUpdatedPackedDataStep();
	}

}
