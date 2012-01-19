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
	public boolean justRevived;
	
	// TODO(jven): temporary?      
	// hmao: yea get rid of this shit, dont use it anymore
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
			
			// Note: We use try-catch judiciously to isolate system failures
			// For instance, if message receiving gets dicked, we still want
			// to be able to execute normal moves. Similarly, if the extra
			// bytecode engine fails, we want to still make it to yield
			
			// Begin New Turn
			resetClock();
			updateRoundVariables();
			

			// Message Receive Loop
			try {
				if(justRevived)
					io.flushAllMessages();
				else
					io.receive();
			} catch (Exception e) {
				e.printStackTrace(); rc.addMatchObservation(e.toString()); }
			
			
			// Main Run Call
			try {
				run();
			} catch (Exception e) {
				e.printStackTrace(); rc.addMatchObservation(e.toString());
			}

			// Call Movement State Machine
			try {
				msm.step();
			} catch (Exception e) {
				e.printStackTrace(); rc.addMatchObservation(e.toString());
			}

			// Check if we've already run out of bytecodes
			if(checkClock()) {
				rc.yield();
				continue;
			}
				
			// Use excess bytecodes
			try {
				if(Clock.getRoundNum()==executeStartTime && Clock.getBytecodesLeft()>1000)
					useExtraBytecodes();
			} catch (Exception e) {
				e.printStackTrace(); rc.addMatchObservation(e.toString());
			}
		
			// End of Turn
			if(checkClock())
				System.out.println("Very bad! useExcessBytecodes() ran over the bytecode limit. " +
						"You must fix this so it only uses the available bytecodes and no more.");
			rc.yield();
		}
	}
	
	private void updateRoundVariables() {
		curRound = Clock.getRoundNum();
		curEnergon = rc.getEnergon();
		curLoc = rc.getLocation();
		curDir = rc.getDirection();
		curLocInFront = curLoc.add(curDir);
		curLocInBack = curLoc.add(curDir.opposite());

		justRevived = (lastResetTime < executeStartTime - 10);
	}
	
	/**
	 * Message handler for all robot types
	 * @param msgType
	 * @param sb
	 */
	public void processMessage(BroadcastType msgType, StringBuilder sb) throws GameActionException {}

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
        System.out.println("Warning: Unit over Bytecode Limit @"+executeStartTime+"-"+currRound +":"+ byteCount);
        return true;
	}
	
	/** Should be overridden by any robot that wants to do movements. 
	 * @return a new MoveInfo structure that either represents a spawn, a move, or a turn
	 */
	public MoveInfo computeNextMove() throws GameActionException {
		return null;
	}
	
	/** If there are bytecodes left to use this turn, will call this function
	 * a single time. Function should very hard not to run over bytecodes.
	 */
	public void useExtraBytecodes() {
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>2000 &&
				rc.getFlux() > 0.05) 
			io.sendAll();
		
		if(Clock.getRoundNum()==curRound && Clock.getBytecodesLeft()>4000)
			fbs.manageFlux();
	}
}
