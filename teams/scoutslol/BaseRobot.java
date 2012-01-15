package scoutslol;

import battlecode.common.*;

public abstract class BaseRobot {
	
	final RobotController rc;
	final DataCache dc;
	final MapCache mc;
	final Navigator nav;
	final Radio io;
	Navigation nv;
	
	// Robot Stats
	final RobotType myType;
	final double myMaxEnergon, myMaxFlux;
	final Team myTeam;
	final int myID;

	public double currEnergon, currFlux;
	public MapLocation currLoc, currLocInFront, currLocInBack;
	public Direction currDir;

	public final int spawnRound;
	public int currRound;
	
	public RobotState currState;
	
	// Internal Statistics
	int executeStartTime;
	int executeStartByte;
	
	
	public BaseRobot(RobotController myRC) {
		rc = myRC;
		
		myType = rc.getType();
		myTeam = rc.getTeam();
		myID = rc.getRobot().getID();
		myMaxEnergon = myType.maxEnergon;
		myMaxFlux = myType.maxFlux;
		
		dc = new DataCache(this);
		mc = new MapCache(this);
		nav = new Navigator(this);
		io = new Radio(this);
		
		spawnRound = Clock.getRoundNum();
	}
	
	public abstract void run() throws GameActionException;
	
	public void loop() {
		while(true) {
			
			startClock();
	
			// Useful Statistics
			currEnergon = rc.getEnergon();
			currFlux = rc.getFlux();
			currLoc = rc.getLocation();
			currDir = rc.getDirection();
			
			currLocInFront = currLoc.add(currDir);
			currLocInBack = currLoc.add(currDir.opposite());
			
			currRound = Clock.getRoundNum();
			
			// show state of robot
			rc.setIndicatorString(0, "" + myType + " - " + currState);
			
			// Main Radio Receive Call
			try {
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
	
	/**
	 * message handler for all robot types
	 * @param msgType
	 * @param sb
	 */
	public void processMessage(char msgType, StringBuilder sb) {}

	/**
	 * @return age of the robot in rounds
	 */
	public int getAge() { return spawnRound - currRound; }

	private void startClock() {
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
	
	
}
