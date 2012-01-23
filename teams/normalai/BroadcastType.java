package normalai;

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
	
	// swarm code
	ANNOUNCE_ENEMY,
	// (temporary)
	SWARM_DETAILS,
	// ushorts(15-bit), [seek_or_swarm, target_loc.x, target_loc.y, sender_loc.x, sender_loc.y]
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
