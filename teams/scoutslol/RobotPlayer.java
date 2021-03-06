package scoutslol;

import battlecode.common.RobotController;

public class RobotPlayer {
	public static void run(RobotController myRC) {
		BaseRobot br = null;

		try {
			switch (myRC.getType()) {
			case ARCHON:
				br = new ArchonRobot(myRC);
				break;
			case SCOUT:
				br = new ScoutRobot(myRC);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			System.out.println("Initializtion Failed");
			e.printStackTrace();
			br.rc.addMatchObservation(e.toString());
		}

		//main loop should never terminate
		while (true) {
			try {
				br.loop();
			} catch (Exception e) {
				System.out.println("Main Loop Terminated Unexpectedly");
				e.printStackTrace();
				br.rc.addMatchObservation(e.toString());
			}
		}

	}

}
