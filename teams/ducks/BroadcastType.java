package ducks;

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

	// rally code
	RALLY,
	
	// extended radar
	/** ushorts(15-bit), [sender_robot_ID, sender_loc.x, sender_loc.y, sender_energon, robot_ID_1, loc_1.x, loc_1.y, energon_1(rounded up), robot_ID_2, loc_2.x, etc...] */
	ENEMY_INFO,
	/** ushort(15-bit), robot_ID_of_robot_that_I_killed */
	ENEMY_KILL,
	
	// swarm code
	ANNOUNCE_ENEMY,
	// (temporary)
	SWARM_DETAILS,
	/** ushorts(15-bit), [seek_or_swarm, target_loc.x, target_loc.y, sender_loc.x, sender_loc.y] */
	SWARM_TARGET,

	// single ushort
	HIBERNATE,
	
	
	
	
	
	
//	ending semicolon
	;
	
	public char header;
	BroadcastType() {
		header = (char)(this.ordinal() + 0x100);
	}
	
	public static BroadcastType decode(char header) {
		return BroadcastType.values()[header - 0x100];
	}
}
