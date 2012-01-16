package ducksold1;

import battlecode.common.RobotController;

public class RobotPlayer {
	public static void run(RobotController myRC) {
		BaseRobot br = null;

		try {
			switch (myRC.getType()) {
			case ARCHON:
				//br = new ArchonRobotYP(myRC);
				br = new ArchonRobotHT(myRC);
//				br = new ArchonRobotJV(myRC);
				break;
			case SOLDIER:
				//br = new SoldierRobot(myRC);
				br = new SoldierRobotJV(myRC);
				break;
			case SCOUT:
				//br = new ScoutRobot(myRC);
				br = new ScoutRobotJV(myRC);
				break;
			case DISRUPTER:
				br = new DisrupterRobot(myRC);
				break;
			case SCORCHER:
				br = new ScorcherRobot(myRC);
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
