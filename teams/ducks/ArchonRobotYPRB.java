package ducks;

import battlecode.common.GameActionException;
import battlecode.common.Message;
import battlecode.common.RobotController;

public class ArchonRobotYPRB extends BaseRobot {

	public ArchonRobotYPRB(RobotController myRC) throws GameActionException {
		super(myRC);
		// TODO Auto-generated constructor stub
	}
	
	Message m;

	@Override
	public void run() throws GameActionException {
		
		while (true)
		{
			rc.yield();
			Message[] msgs = rc.getAllMessages();
			if (msgs.length>0)
			{
				m = msgs[(Util.randInt()%msgs.length+msgs.length)%msgs.length];
				m.strings[0] = "";
				m.ints[1] = 0;
			}
			
			if (m!=null)
				rc.broadcast(m);
			
		}
	}
	
	@Override
	public void processMessage(BroadcastType msgType, StringBuilder sb)
			throws GameActionException {
		// TODO Auto-generated method stub
		super.processMessage(msgType, sb);
	}

}
