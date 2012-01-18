package ducks;

import battlecode.common.MapLocation;

/**
 * This system is used to communicate objective information between units.
 * @author jven
 */
public class RallySystem {

	private BaseRobot br;
	private int timeUntilBroadcast;
	
	public RallySystem(BaseRobot myBR) {
		br = myBR;
		timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
	}
	
	public MapLocation getCurrentObjective() {
		return br.mc.guessEnemyPowerCoreLocation();
	}
	
	public void broadcastRally() {
		if (--timeUntilBroadcast <= 0) {
			br.io.sendShort("#xr", 0);
			timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
		}
	}
}
