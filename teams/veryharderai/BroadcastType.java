package veryharderai;

public enum BroadcastType {
	
	// shared exploration
	POWERNODE_FRAGMENTS,
	MAP_FRAGMENTS,
	MAP_EDGES,

	// random shit
	OWNERSHIP_CLAIM,
	ENEMY_ARCHON_KILL,
	
	// scout wire system
	WIRE_REQUEST,
	WIRE_ACCEPT,
	WIRE_CONFIRM,
	WIRE_ABORT,

	// jven
	INITIAL_REPORT,
	INITIAL_REPORT_ACK,
	RALLY,
	
	// extended radar
	/** ushorts(15-bit), [sender_robot_ID, sender_loc.x, sender_loc.y, 
	 * sender_energon (+10000 if sender info is included at all), sender_type, 
	 * robot_ID_1, loc_1.x, loc_1.y, energon_1(rounded up), robot_type_1, 
	 * robot_ID_2, loc_2.x, etc...] 
	 */
	ENEMY_INFO,
	/** ushort(15-bit), robot_ID_of_robot_that_I_killed */
	ENEMY_KILL,
	
	// swarm code
	ANNOUNCE_ENEMY,
	// (temporary)
	SWARM_DETAILS,
	
	/** ushorts(15-bit), [seek_or_swarm, target_loc.x, target_loc.y, sender_loc.x, sender_loc.y] */
	SWARM_TARGET,
	/** ushorts(15-bit), [round_spotted, enemy_loc.x, enemy_loc.y] */
	ENEMY_SPOTTED,

	/** blank message. Sent by units with no flux */
	LOW_FLUX_HELP,
	
	
	
//	ending semicolon
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
