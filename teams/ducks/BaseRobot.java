package ducks;

import ducks.Debug.Owner;
import battlecode.common.*;

public abstract class BaseRobot {
	
	final RobotController rc;
	final DataCache dc;
	final MapCache mc;
	final Navigator nav;
	final Micro mi;
	final Radio io;
	final FluxManager fm;
	final Debug debug;
	final SharedExplorationSystem ses;
	final UnitRadar ur;
	final EnemyArchonInfo eai;
	final ArchonOwnership ao;
	
	// Robot Stats
	final RobotType myType;
	final double myMaxEnergon, myMaxFlux;
	final Team myTeam;
	final int myID;
	final MapLocation myHome;

	public double currEnergon, currFlux;
	public MapLocation currLoc, currLocInFront, currLocInBack;
	public Direction currDir;

	public final int birthday;
	public final MapLocation birthplace;
	public int currRound;
	
	// Internal Statistics
	int executeStartTime;
	int executeStartByte;
	
	
	public BaseRobot(RobotController myRC) {
		rc = myRC;
		
		currLoc = rc.getLocation();
		currDir = rc.getDirection();
		myType = rc.getType();
		myTeam = rc.getTeam();
		myID = rc.getRobot().getID();
		myMaxEnergon = myType.maxEnergon;
		myMaxFlux = myType.maxFlux;
		myHome = rc.sensePowerCore().getLocation();
		
		dc = new DataCache(this);
		mc = new MapCache(this);
		nav = new Navigator(this);
		mi = new Micro(this);
		io = new Radio(this);
		fm = new FluxManager(this);
		debug = new Debug(this, Owner.YP);
		ses = new SharedExplorationSystem(this);
		ur = new UnitRadar(this);
		eai = new EnemyArchonInfo(this);
		ao = new ArchonOwnership(this);
		
		birthday = Clock.getRoundNum();
		birthplace = currLoc;
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
			
			debug.setIndicatorString(1,
					"Enemy archons remaining: " + eai.getNumEnemyArchons(),
					Owner.JVEN);
			debug.setIndicatorString(1,
					"current location: " + currLoc,	Owner.YP);
			
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
			
			// Show indicator strings
			debug.showIndicatorStrings();
		
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
	public void processMessage(char msgType, StringBuilder sb) throws GameActionException {}

	/**
	 * @return age of the robot in rounds
	 */
	public int getAge() { return birthday - currRound; }

	private void startClock() {
		executeStartTime = Clock.getRoundNum();
		executeStartByte = Clock.getBytecodeNum();
	}
	
	private void stopClock() {
        if(executeStartTime!=Clock.getRoundNum()) {
            int currRound = Clock.getRoundNum();
            int byteCount = (GameConstants.BYTECODE_LIMIT-executeStartByte) + (currRound-executeStartTime-1) * GameConstants.BYTECODE_LIMIT + Clock.getBytecodeNum();
            debug.println("Warning: Unit over Bytecode Limit @"+executeStartTime+"-"+currRound +":"+ byteCount);
        }  
	}
	
	
}
