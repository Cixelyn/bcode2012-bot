package verydisrupterai;

import battlecode.common.GameActionException;
import battlecode.common.Message;
import battlecode.common.RobotController;

public class HibernationSystem {
	final BaseRobot br;
	
	private boolean lowFluxMode;

	public static enum HibernationMode {NORMAL, LOW_FLUX}
	public static enum ExitCode {MESSAGED, ATTACKED, REFUELED }

	public HibernationSystem(BaseRobot baseRobot) {
		this.br = baseRobot;
		lowFluxMode = false;
	}

	public void setMode(HibernationMode mode) {
		switch(mode) {
		case NORMAL:
			lowFluxMode = false;
		case LOW_FLUX:
			lowFluxMode = true;
		}
	}
	

	/** Enables hibernation mode and blocks until a wake-up message has been
	 * received Don't change the variable order. It's been optimized to hell
	 * @return reason for wakeup
	 */
	public ExitCode run() {

		// LOCAL VARIABLE DECLARATION ORDER
		int i; 							// istore_1
		int[] mints; 					// astore_2
		Message[] msgs;	 				// astore_3

		RobotController rc = br.rc; 	// field 4
		int time=br.myID;				// field 5
		boolean localLowFluxMode = this.lowFluxMode;
	
		double lastEnergon = rc.getEnergon();
		double lastFlux = rc.getFlux();
		double curEnergon, curFlux;

		// team number check
		int teamkey = br.io.teamkey;
		
		
		// generate our help message
		Message helpMsg = new Message();
		
		String data = 
				BroadcastChannel.SCOUTS.chanHeader.concat(
				BroadcastType.LOW_FLUX_HELP.header_s).concat(
				br.io.generateMetadata());
		helpMsg.ints = new int[]{
				br.io.teamkey,
				br.io.hashMessage(new StringBuilder(data)),
		};
		helpMsg.strings = new String[]{data};
		

		while (true) {
			
			// emergency wakeup conditions
			if((curEnergon = rc.getEnergon()) < lastEnergon) {
				br.resetClock();
				br.updateRoundVariables();
				br.io.sendWakeupCall();
				return ExitCode.ATTACKED;
			}
			if((curFlux = rc.getFlux()) < lastFlux - 1.0) {
				br.resetClock();
				br.updateRoundVariables();
				br.io.sendWakeupCall();
				return ExitCode.ATTACKED;
			}      
			
			// low flux mode checks
			// we check the boolean only on special events so that
			// our bytecode usage is lower in low flux mode
			if(curFlux > lastFlux && localLowFluxMode) {
				br.resetClock();
				br.updateRoundVariables();
				return ExitCode.REFUELED;
			}
			
			if(time++ % 60 == 0 && localLowFluxMode) {
				try{
					rc.broadcast(helpMsg);
				} catch(GameActionException e) {
					br.dbg.println('c', "I ran out of flux and killed myself.");
//					e.printStackTrace();
//					rc.addMatchObservation(e.toString());
				}
			}
			
			lastEnergon = curEnergon;
			lastFlux = curFlux;
			
			// check for awaken message
			msgs = rc.getAllMessages();
			for (i = msgs.length; --i >= 0;) {

				// validity check
				if ((mints = msgs[i].ints) == null)
					continue;
				if (mints.length != 3)
					continue;
				if (mints[0] != teamkey)
					continue;
				
				br.resetClock();
				br.updateRoundVariables();
				br.io.sendWakeupCall();
				return ExitCode.MESSAGED; // our exit point
			}

			rc.yield();
		}
	}
}
