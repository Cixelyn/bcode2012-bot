package ducks;

import battlecode.common.RobotController;
import battlecode.common.Team;

public class RobotPlayer {
	public static void run(RobotController myRC) {
		BaseRobot br = null;

		
		
		String owner = System.getProperty("bc.testing.strategy");
		
		try {
			switch (myRC.getType()) {
			case ARCHON:
				if(owner.equals("haitao")) br = new ArchonRobotHT(myRC);
				else if(owner.equals("justin"))  br = new ArchonRobotJV(myRC);
				else if(owner.equals("yp")) 
				{
//					br = new ArchonRobotYP_SwarmTest(myRC);
//					br = new ArchonRobotYP(myRC);
					if (br==null)
					{
						if (myRC.getTeam()==Team.A)
							br = new ArchonRobotHT(myRC);
						else
							br = new ArchonRobotJV(myRC);
					}
					
				} else br = new ArchonRobot(myRC);
				break;
			case SOLDIER:
				if(owner.equals("haitao")) br = new SoldierRobotHT(myRC);
				else if(owner.equals("justin")) br = new SoldierRobotJV(myRC);
				else if (owner.equals("yp"))
				{
//					br = new SoldierRobotYP(myRC);
					if (br == null)
					{
						if (myRC.getTeam()==Team.A)
							br = new SoldierRobotHT(myRC);
						else
							br = new SoldierRobotJV(myRC);
					}
				} else br = new SoldierRobot(myRC);
				break;
			case SCOUT:
				break;
			case DISRUPTER:
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
