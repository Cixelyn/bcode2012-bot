package ducks;

import battlecode.common.*;

public class ArchonRobotCL extends BaseRobot {
	
	private enum BehaviorState{
		SPLIT,  //initial state, only for the split
		DEFEND, //normal state. chilling around the power core, defending
		SEEK,   //locked onto a sensed target. homing in for the kill
		ASSIST, //heard about a target. going for the assist
		CAP     //going to cap towers for fun
	}
	BehaviorState behavior;
	int myArchonID;
	
	public ArchonRobotCL(RobotController myRC) throws GameActionException {
		super(myRC);
		MapLocation[] alliedArchons = this.dc.getAlliedArchons();
		for(int i=alliedArchons.length; --i>=0; ) {
			if(alliedArchons[i].equals(myRC.getLocation())) {
				myArchonID = i;
				break;
			}
		}
		
		System.out.println(myArchonID);
		
		behavior = BehaviorState.SPLIT;
		
		io.addChannel(BroadcastChannel.ALL);
		io.addChannel(BroadcastChannel.ARCHONS);
	}
	
	
	RobotInfo ownTargetInfo;
	int roundsSinceTargetSeen = 0;
	MapLocation receivedTargetLoc;
	
	
	

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
			
			// Seek out an enemy if we sense anyone
			if(radar.closestEnemy == null) {
				break;
			}
			else{   
				if(radar.closestEnemyDist <= 16) {
					ownTargetInfo = radar.closestEnemy;
					behavior = BehaviorState.SEEK;
					io.sendMapLoc(BroadcastChannel.ALL, BroadcastType.RALLY, radar.closestEnemy.location);
				}
			}
		case SEEK:
			fbs.setBattleMode();
			
			if(ownTargetInfo != null) {
				if(rc.canSenseObject((GameObject) ownTargetInfo.robot)) {
					nav.setDestination(ownTargetInfo.location);
					roundsSinceTargetSeen = 0;
				} else {
					roundsSinceTargetSeen ++;
				}
			}
			else if(roundsSinceTargetSeen>5) {
				ownTargetInfo = null;
			}
		case ASSIST:
			
			
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
			if(myArchonID != 0) {
				return new MoveInfo(curLoc.directionTo(myHome).opposite(), false);
			} else {
				return new MoveInfo(curLoc.directionTo(myHome), true);
			}
			
		case DEFEND:
			if(rc.getFlux() > 150) {
				boolean[] openDirs = dc.getMovableDirections();
				for(int i=0; i<openDirs.length; i++) {
					if(openDirs[i]) {
						return new MoveInfo(RobotType.SOLDIER,Constants.directions[i]);
					}
				}
			}
		case SEEK:
			Direction dir = nav.navigateToDestination();
			if(dir==null)
				return null;
			else
				return new MoveInfo(dir,true);
		
			
		default:
			return null;
		}
	}
	
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		switch(msgType) {
		
		case RALLY:
			if(ownTargetInfo == null) {
				
				
			}
			
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
	public void useExtraBytecodes() throws GameActionException {
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
