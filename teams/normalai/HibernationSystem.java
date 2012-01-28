package normalai;

import battlecode.common.*;

public class HibernationSystem {
	final BaseRobot br;
	
	public HibernationSystem(BaseRobot baseRobot) {
		this.br = baseRobot;
	}

	/** Enables hibernation mode and blocks until a wake-up message has been
	 * received Don't change the variable order. It's been optimized to hell
	 */
	public void run() {

		// LOCAL VARIABLE DECLARATION ORDER
		int i; // istore_1
		int[] mints; // astore_2
		Message[] msgs; // astore_3

		RobotController rc = br.rc; // field 4
		int time; // field 5

		// team number check
		int teamkey = br.io.teamkey;

		while (true) {
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

				time = Clock.getRoundNum();
				if (mints[2] <= time && mints[2] > time - 10) {
					br.resetClock();
					br.io.sendShort(BroadcastChannel.SOLDIERS, BroadcastType.MAP_EDGES, 
							5); // dummy message, should never be processed
					br.io.sendWakeupCall();
					return; // our exit point
				}

			}

			rc.yield();
		}
	}
}
