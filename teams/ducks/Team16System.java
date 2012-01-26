package ducks;

import battlecode.common.MapLocation;
import battlecode.common.Message;

/**
 * A messaging attack on Team 16. We replay messages to him from previous
 * matches in an attempt to screw up his receivers.
 * 
 * His messages (at the time of this writing) consist of 3 ints and a map loc.
 * 
 * @author jven
 */
public class Team16System {
	
	/** Maps round number to 4 ints. The first 2 ints are ints in the message
	 * and the last 2 are the map loc x and y. The round number is also in the
	 * message.
	 */
	private int[][] messageData;
	/** Whether the loadMessageData method has been called. */
	private boolean loaded;
	
	public Team16System() {
		// initialize
		messageData = new int[16384][0];
		loaded = false;
	}
	
	/**
	 * Loads previous messages into messageData. May be a bit expensive in
	 * bytecodes so call during a Robot's initialization and only if the Robot
	 * will use this.
	 */
	public void loadMessageData() {
		if (loaded) {
			return;
		}
		// AUTO GENERATED CODE
		messageData[614] = new int[] {133136254, 315446129, 16383, 31314};
		messageData[636] = new int[] {133136254, 315446086, 16381, 31315};
		messageData[670] = new int[] {133136254, 315436408, 16384, 31316};
		messageData[789] = new int[] {133136254, 315436655, 16385, 31316};
		messageData[811] = new int[] {133136254, 315445740, 16383, 31317};
		messageData[811] = new int[] {133136254, 315445740, 16383, 31317};
		messageData[827] = new int[] {133136254, 315445706, 16383, 31315};
		messageData[865] = new int[] {133136254, 315436682, 16384, 31320};
		messageData[1117] = new int[] {133136254, 315433203, 16385, 31320};
		messageData[1306] = new int[] {133136254, 315433586, 16389, 31315};
		messageData[1359] = new int[] {133136254, 315433695, 16389, 31316};
		messageData[1425] = new int[] {133136254, 315433828, 16389, 31315};
		messageData[1657] = new int[] {133136254, 315434172, 16389, 31323};
		messageData[1677] = new int[] {133136254, 315434323, 16385, 31320};
		messageData[2945] = new int[] {133136254, 315440998, 16401, 31333};
		messageData[3058] = new int[] {133136254, 315441030, 16400, 31330};
		messageData[3083] = new int[] {133136254, 315437129, 16400, 31327};
		messageData[3112] = new int[] {133136254, 315437102, 16398, 31328};
		messageData[3365] = new int[] {133136254, 315437621, 16398, 31329};
		messageData[3385] = new int[] {133136254, 315437569, 16393, 31338};
		messageData[3403] = new int[] {133136254, 315437801, 16398, 31329};
		messageData[3405] = new int[] {133136254, 315437792, 16396, 31334};
		messageData[3407] = new int[] {133136254, 315437793, 16398, 31329};
		messageData[3414] = new int[] {133136254, 315437791, 16393, 31338};
		messageData[3449] = new int[] {133136254, 315437706, 16401, 31353};
		messageData[3452] = new int[] {133136254, 315437709, 16402, 31351};
		messageData[3464] = new int[] {133136254, 315437936, 16405, 31333};
		messageData[3967] = new int[] {133136254, 315438734, 16401, 31345};
		messageData[4597] = new int[] {133136254, 315427724, 16410, 31340};
		messageData[5094] = new int[] {133136254, 315428783, 16413, 31342};
		messageData[5182] = new int[] {133136254, 315424799, 16413, 31342};
		messageData[5218] = new int[] {133136254, 315424941, 16399, 31350};
		messageData[5218] = new int[] {133136254, 315424953, 16413, 31344};
		messageData[5255] = new int[] {133136254, 315425132, 16412, 31342};
		messageData[5480] = new int[] {133136254, 315425458, 16412, 31342};
		messageData[5626] = new int[] {133136254, 315425682, 16412, 31338};
		messageData[5751] = new int[] {133136254, 315425928, 16412, 31338};
		messageData[5886] = new int[] {133136254, 315426203, 16413, 31338};
		messageData[6028] = new int[] {133136254, 315426686, 16410, 31340};
		messageData[6212] = new int[] {133136254, 315431150, 16411, 31341};
		messageData[6259] = new int[] {133136254, 315431046, 16412, 31340};
		messageData[6505] = new int[] {133136254, 315431605, 16415, 31336};
		messageData[6653] = new int[] {133136254, 315431836, 16413, 31339};
		messageData[6827] = new int[] {133136254, 315432241, 16413, 31338};
		messageData[7101] = new int[] {133136254, 315432733, 16413, 31338};
		messageData[7319] = new int[] {133136254, 315429199, 16414, 31343};
		messageData[7440] = new int[] {133136254, 315429500, 16419, 31343};
		messageData[7517] = new int[] {133136254, 315429595, 16414, 31343};
		messageData[7575] = new int[] {133136254, 315429708, 16415, 31341};
		messageData[7776] = new int[] {133136254, 315430054, 16412, 31338};
		messageData[7954] = new int[] {133136254, 315430520, 16418, 31342};
		messageData[8026] = new int[] {133136254, 315430612, 16415, 31343};
		messageData[8245] = new int[] {133136254, 315451444, 16416, 31342};
		messageData[8337] = new int[] {133136254, 315451712, 16413, 31343};
		messageData[8632] = new int[] {133136254, 315452176, 16412, 31340};
		messageData[8973] = new int[] {133136254, 315453051, 16412, 31341};
		messageData[8846] = new int[] {133136254, 315452738, 16417, 31343};
		messageData[9068] = new int[] {133136254, 315453114, 16413, 31343};
		messageData[9148] = new int[] {133136254, 315453210, 16413, 31343};
		messageData[9295] = new int[] {133136254, 315449536, 16419, 31341};
		messageData[9466] = new int[] {133136254, 315449768, 16417, 31341};
		messageData[9791] = new int[] {133136254, 315450407, 16420, 31341};
		messageData[9947] = new int[] {133136254, 315450863, 16420, 31341};
		messageData[10387] = new int[] {133136254, 315455870, 16420, 31340};
		messageData[10575] = new int[] {125828966, 277838490, 16402, 31334};
		messageData[10768] = new int[] {133136254, 315456638, 16420, 31338};
		// END AUTO GENERATED CODE
		loaded = true;
	}
	
	/**
	 * Returns a Team 16 message from a prior game if one is available for the
	 * given round number. Returns null if none is available.
	 * @param roundNum The round number for which an enemy message should be
	 * returned.
	 * @return An enemy message for the given round number.
	 */
	public Message getMessage(int roundNum) {
		// if we haven't loaded message data, return null
		if (!loaded) {
			return null;
		}
		int[] data = messageData[roundNum];
		// if we don't have any message data for this round, return null
		if (data == null) {
			return null;
		} else {
			Message m = new Message();
			m.ints = new int[] {data[0], roundNum, data[1]};
			m.locations = new MapLocation[] {new MapLocation(data[2], data[3])};
			return m;
		}
	}
}
