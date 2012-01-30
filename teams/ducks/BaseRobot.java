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

	// Core Subsystems
	public final RobotController rc;
	public final DataCache dc;
	public final MapCacheSystem mc;
	public final NavigationSystem nav;
	public final BroadcastSystem io;
	public final FluxBalanceSystem fbs;
	public final SharedExplorationSystem ses;
	public final TeamMemory tmem;
	public final RadarSystem radar;
	public final ExtendedRadarSystem er;
	public final MovementStateMachine msm;
	public final DebugSystem dbg;
	public final MatchObservationSystem mos;
	public final HibernationSystem hsys;
	public final MessageAttackSystem mas;
	
	// Robot Statistics - permanent variables
	public final RobotType myType;
	public final double myMaxEnergon, myMaxFlux;
	public final Team myTeam;
	public final int myID;
	public final MapLocation myHome;
	public final int birthday;
	public final MapLocation birthplace;
	public int myArchonID = -1;
	
	// Robot Statistics - updated per turn
	public double curEnergon;
	public MapLocation curLoc, curLocInFront, curLocInBack;
	public Direction curDir;
	public int curRound;
	
	// Robot Flags - toggle important behavior changes
	public boolean justRevived;
	public boolean gameEndNow = false;
	public boolean gameEndDetected = false;
	public int gameEndTime = Integer.MAX_VALUE;
	
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
		
		
		// archon specific initializers
		if(myType==RobotType.ARCHON) {
			
			// hardcoded split
			Direction dir = curLoc.directionTo(myHome).opposite();
			if(rc.canMove(dir)) {
				if(curDir==dir) rc.moveForward();
				else if(curDir==dir.opposite()) rc.moveBackward();
				else {
					rc.setDirection(dir);
					rc.yield();
					rc.moveForward();
				}
				rc.yield();
				updateRoundVariables();
			}
			
			// compute archon ID
			MapLocation[] alliedArchons = myRC.senseAlliedArchons();
			for(int i=alliedArchons.length; --i>=0; )
				if(alliedArchons[i].equals(curLoc))
					myArchonID = i;
		}
			
		
		// DO NOT CHANGE THE ORDER OF THESE DECLARATIONS
		// SOME CONTRUCTORS NEED OTHERS TO ALREADY BE DECLARED
		// the boolean is whether to encrypt (222 bytecodes)
		dbg = new DebugSystem(this);
		mos = new MatchObservationSystem(this);
		dc = new DataCache(this);
		mc = new MapCacheSystem(this);
		nav = new NavigationSystem(this);
		io = new BroadcastSystem(this);
		tmem = new TeamMemory(this);
		radar = new RadarSystem(this);
		er = new ExtendedRadarSystem(this);
		fbs = new FluxBalanceSystem(this);
		ses = new SharedExplorationSystem(this);
		msm = new MovementStateMachine(this);
		hsys = new HibernationSystem(this);
		mas = new MessageAttackSystem(this);
		
		mc.senseAll();
		
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() {
		while(true) {
			
			// Begin New Turn
			resetClock();
			updateRoundVariables();
			
			// Flush our send queue (if messages remaining because we went over bytecodes)
			io.flushSendQueue();
			
			// Message Receive Loop - in its own try-catch to protect against messaging attacks
			try {
				if(justRevived)
					io.flushIncomingQueue();
				else
					io.receive();
			} catch (Exception e) {
				e.printStackTrace(); rc.addMatchObservation(e.toString()); }
			
			
			try {
				
				// Main Run Call
				dbg.setIndicatorString('e', 0, gameEndDetected+" "+gameEndTime);
				run();
				
				// Call Movement State Machine
				msm.step();
				
				// Update Extended Radar
				er.step();
				
				// Check if we've already run out of bytecodes
				if(checkClock()) {
					rc.yield();
					continue;
				}
				
				// Use excess bytecodes
				if(Clock.getRoundNum()==executeStartTime && Clock.getBytecodesLeft()>1000)
					useExtraBytecodes();
				
			} catch (Exception e) {
				e.printStackTrace(); rc.addMatchObservation(e.toString());
			}
		
			// End of Turn
			if(checkClock())
				dbg.println('e', "Very bad! useExcessBytecodes() ran over the bytecode limit. " +
						"You must fix this so it only uses the available bytecodes and no more.");
			
			rc.yield();
		}
	}
	
	/** Resets the current round variables of the robot. */
	public void updateRoundVariables() {
		curRound = Clock.getRoundNum();
		curEnergon = rc.getEnergon();
		curLoc = rc.getLocation();
		curDir = rc.getDirection();
		curLocInFront = curLoc.add(curDir);
		curLocInBack = curLoc.add(curDir.opposite());

		justRevived = (lastResetTime < executeStartTime - 3);
		gameEndNow = curRound > gameEndTime;
	}
	
	/**
	 * Message handler for all robot types
	 * @param msgType
	 * @param sb
	 */
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {
		if(msgType == BroadcastType.DETECTED_GAME_END) {
			int round;
			if((round=BroadcastSystem.decodeShort(sb)) < gameEndTime) {
				gameEndTime = round;
			}
			this.gameEndDetected = true;
		}
	}

	/** @return The age of the robot in rounds */
	public int getAge() { 
		return birthday - curRound; 
	}
	
	/** Resets the internal bytecode counter. */
	public void resetClock() {
		lastResetTime = executeStartTime;
		executeStartTime = Clock.getRoundNum();
		executeStartByte = Clock.getBytecodeNum();
	}
	
	/** Prints a warning if we ran over bytecodes. 
	 * @return whether we run out of bytecodes this round.
	 */
	private boolean checkClock() {
        if(executeStartTime==Clock.getRoundNum())
        	return false;
        int currRound = Clock.getRoundNum();
        int byteCount = (GameConstants.BYTECODE_LIMIT-executeStartByte) + (currRound-executeStartTime-1) * GameConstants.BYTECODE_LIMIT + Clock.getBytecodeNum();
        dbg.println('e', "Warning: Over Bytecode @"+executeStartTime+"-"+currRound +":"+ byteCount);
        return true;
	}
	
	/** Should be overridden by any robot that wants to do movements. 
	 * @return a new MoveInfo structure that either represents a spawn, a move, or a turn
	 */
	public MoveInfo computeNextMove() throws GameActionException {
		return null;
	}
	
	/** If there are bytecodes left to use this turn, will call this function
	 * a single time. Function should try very hard not to run over bytecodes.
	 * Overriding functions should make sure to call super.
	 * @throws GameActionException 
	 */
	public void useExtraBytecodes() throws GameActionException {
		
		// Game Ending Detection Stuff
		if(gameEndDetected && Clock.getRoundNum() == curRound && Clock.getBytecodesLeft() > 300) {
			if(Clock.getRoundNum()%11 == myID%11)  //announce to allies
				io.sendUShort(BroadcastChannel.ALL, BroadcastType.DETECTED_GAME_END, gameEndTime);
		}
	
		// Message IO
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>2000 &&
				rc.getFlux() > 0.1) 
			io.sendAll();
	
		// Flux Management
		if(Clock.getRoundNum()==curRound && (Clock.getBytecodesLeft()>4000 ||
				(radar.hasScannedAllies() && Clock.getBytecodesLeft()>1500))) {
			if(!rc.isMovementActive() || msm.justMoved())
				fbs.manageFlux();
		}
	}
	
	public String locationToVectorString(MapLocation loc) {
		if(loc==null) return "<null>";
		return "<"+(loc.x-curLoc.x)+","+(loc.y-curLoc.y)+">";
	}
}
