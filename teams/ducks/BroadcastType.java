package ducks;

public enum BroadcastType {
	
	// ------- TEST CODE
	/** blank */
	NONE,
	
	// ------- SHARED EXPLORATION CODE
	/** shared exploration powernodes */
	POWERNODE_FRAGMENTS,
	/** shared exploration map fragments */
	MAP_FRAGMENTS,
	/** shared exploration map edges */
	MAP_EDGES,

	// ------ EXTENDED RADAR
	/** ushorts(15-bit), [sender_robot_ID, sender_loc.x, sender_loc.y, 
	 * sender_energon (+10000 if sender info is included at all), sender_type, 
	 * robot_ID_1, loc_1.x, loc_1.y, energon_1(rounded up), robot_type_1, 
	 * robot_ID_2, loc_2.x, etc...] */
	ENEMY_INFO,
	/** ushort(15-bit), robot_ID_of_robot_that_I_killed */
	ENEMY_KILL,
	
	// ------ SWARM MODE
	/** ushorts(15-bit), [moving_target?1:0, target_loc.x, target_loc.y] */
	SWARM_TARGET,
	/** ushorts(15-bit), [round_spotted, enemy_loc.x, enemy_loc.y] */
	ENEMY_SPOTTED,

	// ------ HIBERNATION MODE
	/** blank message. Sent by units with no flux */
	LOW_FLUX_HELP,
	
	// ------ GAME END MODE
	/** ushort, [gameEndTime] */
	DETECTED_GAME_END,
	
	// --------------------
	;

	public final char header_c;
	public final String header_s;
	BroadcastType() {
		header_c = (char)(this.ordinal());
		header_s = String.valueOf(header_c);
	}
	
	public static BroadcastType decode(char header) {
		return BroadcastType.values()[header];
	}
}
