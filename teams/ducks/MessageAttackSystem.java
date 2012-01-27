package ducks;

import battlecode.common.MapLocation;
import battlecode.common.Message;

/**
 * Messaging attack stuff. We replay messages to other teams from our previous
 * matches vs them in an attempt to screw up their units.
 * 
 * His messages (at the time of this writing) consist of 3 ints and a map loc.
 * 
 * @author jven
 */
public class MessageAttackSystem {
	
	private final BaseRobot br;
	
	/** The enemy team number. */
	private int enemyTeam;
	
	/** Maps round number to ints used in generating an enemy message. */
	private int[][] messageData;
	
	/** Whether the loadMessageData method has been called. */
	private boolean loaded;
	
	public MessageAttackSystem(BaseRobot myBR) {
		br = myBR;
		enemyTeam = -1;
		messageData = new int[16384][0];
		loaded = false;
	}
	
	/**
	 * Guess which team we are playing given one of their messages. Guess is
	 * based on messages sent in past games.
	 * @param m An enemy message.
	 * @return True if we set a guess on the enemy team, False otherwise.
	 */
	public boolean detectTeam(Message m) {
		
		// return if we already made a guess
		if (enemyTeam != -1) {
			return false;
		}
		
		// return if message is null
		if (m == null) {
			return false;
		}
		
		// 016: Team 16: seems to always send 3 ints and a map location... 3rd
		// i think 1st int is message type, 2nd int is round num, 3rd is a hash,
		// and map loc is a target
		if (m.ints != null && m.ints.length == 3 &&
				m.ints[1] == br.curRound &&
				m.strings == null || m.strings.length == 0 &&
				m.locations != null && m.locations.length == 1) {
			enemyTeam = 16;
			return true;
		}
		
		// 053: Chaos Legion: sometimes sends 1 encrypted, sometimes sends
		// 3 ints, 3rd is round number... i'm guessing the team members are
		// not coordinating on a common message format
		if (m.ints != null && m.ints.length == 3 &&
				m.ints[2] == br.curRound &&
				m.strings == null || m.strings.length == 0 &&
				m.locations == null || m.locations.length == 0) {
			enemyTeam = 53;
			return true;
		}
		
		// no match :(
		return false;
	}
	
	/**
	 * Returns whether a guess has been made about the enemy team.
	 * @return True if we have already set a guess on the enemy team, False
	 * otherwise.
	 */
	public boolean isEnemyTeamKnown() {
		return enemyTeam != -1;
	}
	
	/**
	 * Initialize the message attack system based on the enemy team guess.
	 */
	public void load() {
		
		// if we haven't guessed the enemy team, return
		if (enemyTeam == -1) {
			return;
		}
		
		// if we already loaded message data, return
		if (loaded) {
			return;
		}
		
		// load message data if appropriate
		switch (enemyTeam) {
			case 16:
				load016();
				break;
			case 53:
				break;
			default:
				break;
		}
		
		loaded = true;
	}
	
	/**
	 * Returns whether the message attack system has been initialized and is
	 * ready to generate enemy messages.
	 * @return True if the system is loaded, False otherwise.
	 */
	public boolean isLoaded() {
		return loaded;
	}
	
	/**
	 * Returns an enemy message to send to the enemy team.
	 * @return An enemy message. May return null.
	 */
	public Message getMessage() {
		
		// if we haven't loaded message data, return null
		if (!loaded) {
			return null;
		}
		
		// return enemy message
		Message m = null;
		switch (enemyTeam) {
			case 16:
				int[] data = messageData[br.curRound];
				// if we don't have any message data for this round, return null
				if (data != null) {
					m = new Message();
					m.ints = new int[] {data[0], br.curRound, data[1]};
					m.locations = new MapLocation[] {new MapLocation(data[2], data[3])};
				}
				break;
			case 53:
				m = new Message();
				m.ints = new int[] {653608212, 0, br.curRound};
				break;
			default:
				break;
		}
		return m;
	}
	
	/**
	 * Loads previous messages from team016 into messageData.
	 */
	private void load016() {
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
	}
}
