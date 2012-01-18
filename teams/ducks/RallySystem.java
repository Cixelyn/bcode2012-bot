package ducks;

import battlecode.common.MapLocation;

/**
 * This system is used to communicate objective information between units.
 * @author jven
 *
 */
public class RallySystem {

	private BaseRobot br;
	private int timeUntilBroadcast;
	private MapLocation objective;
	
	public RallySystem(BaseRobot myBR) {
		br = myBR;
		timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
		objective = br.myHome;
	}
	
	public MapLocation getCurrentObjective() {
		return objective;
	}
	
	public void broadcastRally() {
		if (--timeUntilBroadcast <= 0) {
			// share exploration information
			br.ses.broadcastMapEdges();
			br.ses.broadcastMapFragment();
			br.ses.broadcastPowerNodeFragment();
			br.io.sendMapLoc("#xr", br.mc.guessEnemyPowerCoreLocation());
			timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
		}
	}
	
	public void processRally(MapLocation rally) {
		objective = rally;
	}
}
