package ducks;

import battlecode.common.*;

public class HibernationSystem {
	final BaseRobot br;
	
	private boolean wakeOnFlux;

	public static enum HibernationMode {NORMAL, LOW_FLUX}
	public static enum ExitCode {MESSAGED, ATTACKED, REFUELED }
	
	public HibernationSystem(BaseRobot baseRobot) {
		this.br = baseRobot;
		wakeOnFlux = false;
	}

	public void setMode(HibernationMode mode) {
		switch(mode) {
		case NORMAL:
			wakeOnFlux = false;
		case LOW_FLUX:
			wakeOnFlux = true;
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
		int time; 						// field 5
		
		double lastEnergon = rc.getEnergon();
		double lastFlux = rc.getFlux();
		double curEnergon, curFlux;

		// team number check
		int teamkey = br.io.teamkey;

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
			if(wakeOnFlux && (curFlux > lastFlux)) {
				br.resetClock();
				br.updateRoundVariables();
				return ExitCode.REFUELED;
			}
				
			lastEnergon = curEnergon;
			lastFlux = curFlux;
			
			// check for awaken message
			msgs = rc.getAllMessages();
			for (i = msgs.length; --i >= 0;) {

				// validity check
				mints = msgs[i].ints;
				if (mints == null)
					continue;
				if (mints.length != 3)
					continue;
				if (mints[0] != teamkey)
					continue;

				if ((mints[2] <= (time=Clock.getRoundNum())) && (mints[2] > time - 10)) {
					br.resetClock();
					br.updateRoundVariables();
					br.io.sendWakeupCall();
					return ExitCode.MESSAGED; // our exit point
				}

			}

			rc.yield();
		}
	}
}
