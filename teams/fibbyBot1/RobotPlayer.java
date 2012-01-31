package fibbyBot1;

import fibbyBot1.behaviors.*;
import battlecode.common.*;

public class RobotPlayer {

	public static void run(RobotController myRC) {
		try {
			Unit me = null;
			if (myRC.getType() == RobotType.ARCHON) {
				me = new Archon(myRC);
			} else if (myRC.getType() == RobotType.SOLDIER) {
				me = new Soldier(myRC);
			} else if (myRC.getType() == RobotType.SCOUT) {
				me = new Scout(myRC);
			}
			while (true) {
				if (myRC.getFlux() >= 5) {
					me.run();
				}
				myRC.yield();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}