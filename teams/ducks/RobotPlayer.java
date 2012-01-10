package ducks;

import battlecode.common.RobotController;

public class RobotPlayer {
	public static void run(RobotController myRC) {
		while (true) {
			BaseRobot r = null;
			
			try {
				switch (myRC.getType()) {
				case ARCHON:
					r = new ArchonRobot(myRC);
					break;
				case SOLDIER:
					r = new SoldierRobot(myRC);
					break;
				case SCOUT:
					r = new ScoutRobot(myRC);
					break;
				default:
					System.out.println("Dicks");
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try {
				r.loop();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}

	}

}
