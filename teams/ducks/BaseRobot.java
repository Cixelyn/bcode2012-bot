package ducks;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public abstract class BaseRobot {
	
	public final RobotController rc;
	public final DataCache dc;
	public final MapCacheSystem mc;
	public final NavigationSystem nav;
	public final Micro micro;
	public final BroadcastSystem io;
	public final FluxBalanceSystem fbs;
	public final SharedExplorationSystem ses;
	public final RadarSystem radar;
	public final EnemyArchonKillCache eakc;
	public final ArchonOwnership ao;
	public final RallySystem rally;
	public final MovementStateMachine msm;
	public final ScoutWireSystem sws;
	
	// Robot Statistics - Permanent
	public final RobotType myType;
	public final double myMaxEnergon, myMaxFlux;
	public final Team myTeam;
	public final int myID;
	public final MapLocation myHome;
	public final int birthday;
	public final MapLocation birthplace;
	
	// Robot Statistics - updated per turn
	public double curEnergon;
	public MapLocation curLoc, curLocInFront, curLocInBack;
	public Direction curDir;
	public int curRound;
	
	// TODO(jven): temporary?
	// Robot State - left over from previous turns
	public Direction directionToSenseIn;
	
	// Internal Statistics
	private int lastResetTime = 50;
	private int executeStartTime = 50;
	private int executeStartByte;
	
	
	public BaseRobot(RobotController myRC) throws GameActionException {
		rc = myRC;
		
		myType = rc.getType();
		myTeam = rc.getTeam();
		myID = rc.getRobot().getID();
		myMaxEnergon = myType.maxEnergon;
		myMaxFlux = myType.maxFlux;
		myHome = rc.sensePowerCore().getLocation();
		birthday = Clock.getRoundNum();
		birthplace = rc.getLocation();
		updateRoundVariables();
		
		if(myType==RobotType.ARCHON) {
			Direction dir = curLoc.directionTo(myHome).opposite();
			if(rc.canMove(dir)) {
				if(curDir==dir) rc.moveForward();
				else if(curDir==dir.opposite()) rc.moveBackward();
				else {
					rc.setDirection(dir);
					rc.yield();
					rc.moveForward();
				}
			}
		}
		
		dc = new DataCache(this);
		mc = new MapCacheSystem(this);
		nav = new NavigationSystem(this);
		micro = new Micro(this);
		io = new BroadcastSystem(this);
		fbs = new FluxBalanceSystem(this);
		ses = new SharedExplorationSystem(this);
		radar = new RadarSystem(this);
		eakc = new EnemyArchonKillCache(this);
		ao = new ArchonOwnership(this);
		rally = new RallySystem(this);
		msm = new MovementStateMachine(this);
		sws = new ScoutWireSystem(this);
		
		mc.senseAll();
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() {
		while(true) {
			
			resetClock();
	
			updateRoundVariables();
			
			// Main Radio Receive Call
			try {
				if(lastResetTime < executeStartTime - 10)
					io.flushAllMessages();
				else
					io.receive();
			} catch(Exception e) {
				e.printStackTrace();
				rc.addMatchObservation(e.toString());
			}
			
			// Main Run Call
			try{
				run();
			} catch (Exception e) {
				e.printStackTrace();
				rc.addMatchObservation(e.toString());
			}
			
			// Call Movement State Machine
			try {
				msm.step();
			} catch (Exception e) {
				e.printStackTrace();
				rc.addMatchObservation(e.toString());
			}
			
			// Check if we've already run out of bytecodes
			if(stopClock()) {
				System.out.println("We went over bytecodes before calling useExtraBytecodes().");
	            rc.yield();
				continue;
			}
			
			// Use excess bytecodes
			try {
				useExtraBytecodes();
			} catch (Exception e) {
				e.printStackTrace();
				rc.addMatchObservation(e.toString());
			}
		
			// End of Turn
			if(!stopClock()) {
				rc.yield();
			}
		}
	}
	
	private void updateRoundVariables() {
		curRound = Clock.getRoundNum();
		curEnergon = rc.getEnergon();
		curLoc = rc.getLocation();
		curDir = rc.getDirection();
		curLocInFront = curLoc.add(curDir);
		curLocInBack = curLoc.add(curDir.opposite());
	}
	
	/**
	 * Message handler for all robot types
	 * @param msgType
	 * @param sb
	 */
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {}

	/**
	 * @return The age of the robot in rounds
	 */
	public int getAge() { 
		return birthday - curRound; 
	}

	public void resetClock() {
		lastResetTime = executeStartTime;
		executeStartTime = Clock.getRoundNum();
		executeStartByte = Clock.getBytecodeNum();
	}
	/** Returns whether we went over bytecodes. */
	private boolean stopClock() {
        if(executeStartTime!=Clock.getRoundNum()) {
            int currRound = Clock.getRoundNum();
            int byteCount = (GameConstants.BYTECODE_LIMIT-executeStartByte) + (currRound-executeStartTime-1) * GameConstants.BYTECODE_LIMIT + Clock.getBytecodeNum();
            System.out.println("Warning: Unit over Bytecode Limit @"+executeStartTime+"-"+currRound +":"+ byteCount);
            return true;
        }  
        return false;
	}
	
	/** Should be overridden by any robot that wants to do movements. 
	 * @return a new MoveInfo structure that either represents a spawn, a move, or a turn
	 */
	public MoveInfo computeNextMove() throws GameActionException {
		return null;
	}
	
	/** If there are bytecodes left to use this turn, will call this function
	 * until it returns false.
	 * @param bytecodesLeft number of bytecodes left to use this turn
	 * @return whether anything was done in this call
	 */
	public void useExtraBytecodes() {
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>1200) 
			io.sendAll();
		
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>4200)
			fbs.manageFlux();
	}
}
