package ducks;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class RobotPlayer {
	public static void run(RobotController myRC) {
		BaseRobot br = null;
		int rseed = myRC.getRobot().getID();
		Util.randInit(rseed,rseed*Clock.getRoundNum());
		
		String owner = System.getProperty("bc.testing.strategy");
		
		
		try {
			switch (myRC.getType()) {
			case ARCHON:
				if(owner.equals("haitao")) br = new ArchonRobot(myRC);
				else if(owner.equals("justin"))  br = new ArchonRobotJVBackdoor(myRC);
				else if(owner.equals("cory")) br = new ArchonRobotCL(myRC);
				else if(owner.equals("yp")) 
				{
//					br = new ArchonRobotYP_SwarmTest(myRC);
					br = new ArchonRobotYP(myRC);
//					br = new ArchonRobotHT(myRC);
//					br = new FunBot(myRC);
//					br = new ArchonRobot(myRC);
					if (br==null)
					{
						if (myRC.getTeam()==Team.A)
//							br = new ArchonRobot(myRC);
							br = new ArchonRobotYP(myRC);
//							br = new ArchonRobotYPOld(myRC);
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
				else if(owner.equals("justin")) br = new SoldierRobotJVBackdoor(myRC);
				else if(owner.equals("cory")) br = new SoldierRobotCL(myRC);
				else if (owner.equals("yp"))
				{
					br = new SoldierRobotYP(myRC);
//					br = new SoldierRobotHT(myRC);
//					br = new FunBot(myRC);
//					br = new SoldierRobot(myRC);
					if (br == null)
					{
						if (myRC.getTeam()==Team.A)
							br = new SoldierRobot(myRC);
//							br = new SoldierRobotYP(myRC);
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
				if (owner.equals("haitao")) br = new ScoutRobot(myRC);
				else if (owner.equals("justin")) br = null;
				else if (owner.equals("yp"))
				{
//					br = new ScoutRobotYP(myRC);
//					br = new ScoutRobotYP(myRC);
//					br = new FunBot(myRC);
					br = new ScoutRobot(myRC);
					if (br == null)
					{
						if (myRC.getTeam()==Team.A)
							br = new ScoutRobot(myRC);
//							br = new ScoutRobotYP(myRC);
//							br = new ScoutRobotCL(myRC);
						else
							br = new ScoutRobot(myRC);
//							br = new ScoutRobotYP(myRC);
//							br = new ScoutRobotJV(myRC);
					}
				}
				break;
			case DISRUPTER:
				if (owner.equals("yp"))
				{
					
					br = new DisrupterRobotYP(myRC);
					if (br == null)
					{
						if (myRC.getTeam()==Team.A)
							br = new DisrupterRobotYP(myRC);
						
						else
							br = new DisrupterRobotYP(myRC);
						
					}
				}
				else if (owner.equals("justin")) br = new DisrupterRobotJV(myRC);
				break;
			case SCORCHER:
				if (owner.equals("justin")) br = null;
				else if (owner.equals("yp"))
				{
//					br = new ScorcherRobotYP(myRC);
//					br = new ScorcherRobotYP(myRC);
//					br = new FunBot(myRC);
					br = new ScorcherRobotYP(myRC);
					if (br == null)
					{
						if (myRC.getTeam()==Team.A)
							br = new ScorcherRobotYP(myRC);
//							br = new ScorcherRobotYP(myRC);
//							br = new ScorcherRobotYP(myRC);
						else
							br = new ScorcherRobotYP(myRC);
//							br = new ScorcherRobotYP(myRC);
//							br = new ScorcherRobotYP(myRC);
					}
				}
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
