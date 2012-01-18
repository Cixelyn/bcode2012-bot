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
	public final Debug debug = null; // TODO rewrite debug and deal with this
	public final SharedExplorationSystem ses;
	public final RadarSystem radar;
	public final EnemyArchonKillCache eakc;
	public final ArchonOwnership ao;
	public final RallySystem rally;
	public final MovementStateMachine msm;
	
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
	private int executeStartTime;
	private int executeStartByte;
	
	
	public BaseRobot(RobotController myRC) {
		rc = myRC;
		
		myType = rc.getType();
		myTeam = rc.getTeam();
		myID = rc.getRobot().getID();
		myMaxEnergon = myType.maxEnergon;
		myMaxFlux = myType.maxFlux;
		myHome = rc.sensePowerCore().getLocation();
		birthday = Clock.getRoundNum();
		birthplace = rc.getLocation();
		
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
		
		updateRoundVariables();
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() {
		while(true) {
			
			resetClock();
	
			updateRoundVariables();
			
			// Main Radio Receive Call
			try {
				io.receive();
			} catch(Exception e) {
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
			
			// Main Run Call
			try{
				run();
			} catch (Exception e) {
				e.printStackTrace();
				rc.addMatchObservation(e.toString());
			}
				
			
			// Broadcast Queued Messages
			try {
				io.sendAll();
			} catch (Exception e) {
				e.printStackTrace();
				rc.addMatchObservation(e.toString());
			}
		
			// End of Turn
			stopClock();
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
	}
	
	/**
	 * Message handler for all robot types
	 * @param msgType
	 * @param sb
	 */
	public void processMessage(char msgType, StringBuilder sb) throws GameActionException {}

	/**
	 * @return The age of the robot in rounds
	 */
	public int getAge() { 
		return birthday - curRound; 
	}

	public void resetClock() {
		executeStartTime = Clock.getRoundNum();
		executeStartByte = Clock.getBytecodeNum();
	}
	private void stopClock() {
        if(executeStartTime!=Clock.getRoundNum()) {
            int currRound = Clock.getRoundNum();
            int byteCount = (GameConstants.BYTECODE_LIMIT-executeStartByte) + (currRound-executeStartTime-1) * GameConstants.BYTECODE_LIMIT + Clock.getBytecodeNum();
            System.out.println("Warning: Unit over Bytecode Limit @"+executeStartTime+"-"+currRound +":"+ byteCount);
        }  
	}
	
	/** Should be overridden by any robot that wants to do movements. 
	 * @return a new MoveInfo structure that either represents a spawn, a move, or a turn
	 */
	public MoveInfo computeNextMove() throws GameActionException {
		return null;
	}
	
}
