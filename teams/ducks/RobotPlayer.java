package ducks;

import battlecode.common.Clock;
import battlecode.common.RobotController;

public class RobotPlayer {
	public static void run(RobotController myRC) {
		BaseRobot br = null;
		
		int rseed = myRC.getRobot().getID();
		Util.randInit(rseed,rseed*Clock.getRoundNum());
		
		try {
			switch (myRC.getType()) {
			case ARCHON:
				br = new ArchonRobot(myRC);
				break;
			case SOLDIER:
				br = new SoldierRobot(myRC);
				break;
			case SCOUT:
				br = new ScoutRobot(myRC);
				break;
			case DISRUPTER:
				br = new DisrupterRobot(myRC);
				break;
			case SCORCHER:
				break;
			default:
				break;
			}
		} catch (Exception e) {
			System.out.println("Robot constructor failed");
			e.printStackTrace();
			br.rc.addMatchObservation(e.toString());
		}
		
		br.dbg.setOwner('y');
		
		//Main loop should never terminate
		while (true) {
			try {
				br.loop();
			} catch (Exception e) {
				System.out.println("Main loop terminated unexpectedly");
				e.printStackTrace();
				br.rc.addMatchObservation(e.toString());
			}
		}
	}

}
