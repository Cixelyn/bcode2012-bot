package ducks;

import battlecode.common.*;

public class RobotPlayer {
	public static void run(RobotController myRC) {
		while (true) {
			try {
				switch (myRC.getType()) {
				case ARCHON:
					break;
				case SOLDIER:
					break;
				case SCOUT:
					break;
				default:
					System.out.println("Dicks");
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			try {
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
		}

	}

}
