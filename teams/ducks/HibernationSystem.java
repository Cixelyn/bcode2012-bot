package ducks;

import battlecode.common.*;

public class HibernationSystem {
	final BaseRobot br;
	
	public static enum ExitCode {MESSAGED, ATTACKED }
	
	public HibernationSystem(BaseRobot baseRobot) {
		this.br = baseRobot;
	}

	/** Enables hibernation mode and blocks until a wake-up message has been
	 * received Don't change the variable order. It's been optimized to hell
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
			
			if((curEnergon = rc.getEnergon()) < lastEnergon) return ExitCode.ATTACKED;
			if((curFlux = rc.getFlux()) < lastFlux - 1.0) return ExitCode.ATTACKED;
				
			lastEnergon = curEnergon;
			lastFlux = curFlux;
			
			// check for awaken message
			msgs = rc.getAllMessages();
			time = Clock.getRoundNum();
			for (i = msgs.length; --i >= 0;) {

				// validity check
				mints = msgs[i].ints;
				if (mints == null)
					continue;
				if (mints.length != 3)
					continue;
				if (mints[0] != teamkey)
					continue;

				if (mints[2] <= time && mints[2] > time - 10) {
					br.resetClock();
					br.io.sendWakeupCall();
					return ExitCode.MESSAGED; // our exit point
				}

			}

			rc.yield();
		}
	}
}
