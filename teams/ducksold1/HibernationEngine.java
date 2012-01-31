package ducksold1;

import battlecode.common.*;

public class HibernationEngine {

	final BaseRobot br;

	public HibernationEngine(BaseRobot baseRobot) {
		this.br = baseRobot;
	}

	/**
	 * Enables hibernation mode and blocks until a wakeup message has been received
	 * Don't change the variable order. It's been optimized to hell
	 */
	@SuppressWarnings("null")
	public void run() {
		
				
		// LOCAL VARIABLE DECLARATION ORDER
		int i;				// istore_1
		int[] mints;		// astore_2
		Message[] msgs;		// astore_3

		RobotController rc = br.rc;  // field 4
		int time;  					 // field 5
		
		//team number check
		int teamkey = br.io.teamkey;
		
		// Set a fun indicator string
		rc.setIndicatorString(0, "HIBERNATION ENGAGE!"); 
				
		while (true) {
			
			// check for awaken message
			msgs = rc.getAllMessages();
			for(i=msgs.length; --i>=0;) {				
				
				// validity check
				mints = msgs[i].ints;
				if(mints != null) continue;
				if(mints.length != 3) continue;
				if(mints[0] != teamkey) continue;
				
				time = Clock.getRoundNum();
				if(mints[2] <= time && mints[2] > time - 5) {
					return; //our exit point
				}
				
			}
			
			rc.yield();
		}

	}

}
