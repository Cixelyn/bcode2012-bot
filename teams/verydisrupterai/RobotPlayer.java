package verydisrupterai;

import battlecode.common.RobotController;

public class RobotPlayer {
	public static void run(RobotController myRC) {
		BaseRobot br = null;
		
		String owner = "yp";
		
		
		try {
			switch (myRC.getType()) {
			case ARCHON:
				br = new ArchonRobotYP(myRC);
				break;
			case SOLDIER:
				br = new SoldierRobotYP(myRC);
				break;
			case SCOUT:
				br = new ScoutRobot(myRC);
				break;
			case DISRUPTER:
				br = new DisrupterRobotYP(myRC);
				break;
			case SCORCHER:
				br = new ScorcherRobotYP(myRC);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			System.out.println("Robot constructor failed");
			e.printStackTrace();
			br.rc.addMatchObservation(e.toString());
		}
		
		
		// Set people's indicator strings
		if(owner.equals("haitao")) br.dbg.setOwner('h');
		if(owner.equals("cory")) br.dbg.setOwner('c');
		if(owner.equals("yp")) br.dbg.setOwner('y');
		if(owner.equals("justin")) br.dbg.setOwner('j');
		
		
		

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
