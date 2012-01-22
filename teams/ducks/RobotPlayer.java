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
				if(owner.equals("haitao")) br = new ArchonRobot(myRC);
				else if(owner.equals("justin"))  br = new ArchonRobotJV(myRC);
				else if(owner.equals("cory")) br = new ArchonRobotCL(myRC);
				else if(owner.equals("yp")) 
				{
//					br = new ArchonRobotYP_SwarmTest(myRC);
//					br = new ArchonRobotYP(myRC);
//					br = new ArchonRobotHT(myRC);
//					br = new FunBot(myRC);
					if (br==null)
					{
						if (myRC.getTeam()==Team.A)
//							br = new ArchonRobot(myRC);
							br = new ArchonRobotYP(myRC);
//							br = new ArchonRobotHT(myRC);
//							br = new ArchonRobotJV(myRC);
//							br = new ArchonRobotCL(myRC);
						
						else
							br = new ArchonRobot(myRC);
//							br = new ArchonRobotYP(myRC);
//							br = new ArchonRobotHT(myRC);
//							br = new ArchonRobotJV(myRC);
					}
					
				} else br = new ArchonRobot(myRC);
				break;
			case SOLDIER:
				if(owner.equals("haitao")) br = new SoldierRobot(myRC);
				else if(owner.equals("justin")) br = new AttackerRobotJV(myRC);
				else if(owner.equals("cory")) br = new SoldierRobotCL(myRC);
				else if (owner.equals("yp"))
				{
//					br = new SoldierRobotYP(myRC);
//					br = new SoldierRobotHT(myRC);
//					br = new FunBot(myRC);
					if (br == null)
					{
						if (myRC.getTeam()==Team.A)
//							br = new SoldierRobot(myRC);
							br = new SoldierRobotYP(myRC);
//							br = new SoldierRobotHT(myRC);
//							br = new SoldierRobotCL(myRC);
						else
							br = new SoldierRobot(myRC);
//							br = new SoldierRobotYP(myRC);
//							br = new SoldierRobotHT(myRC);
					}
				} else br = new SoldierRobot(myRC);
				break;
			case SCOUT:
				if (owner.equals("justin")) br = new ScoutRobotJV(myRC);
				else if (owner.equals("cory")) br = new ScoutRobotCL(myRC);
				else if (owner.equals("yp"))
				{
//					br = new ScoutRobotYP(myRC);
//					br = new ScoutRobotYP(myRC);
//					br = new FunBot(myRC);
					if (br == null)
					{
						if (myRC.getTeam()==Team.A)
							br = new ScoutRobotYP(myRC);
//							br = new ScoutRobotCL(myRC);
						else
							br = new ScoutRobotYP(myRC);
//							br = new ScoutRobotJV(myRC);
					}
				}
				break;
			case DISRUPTER:
				break;
			case SCORCHER:
				if (owner.equals("justin")) br = new AttackerRobotJV(myRC);
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
