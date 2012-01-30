package ducks;

public enum BroadcastChannel {

	ALL,
	ARCHONS,
	FIGHTERS,
	SCOUTS,
	EXTENDED_RADAR,
	EXPLORERS,
	
	;
	
	public final String chanHeader;
	
	BroadcastChannel() {
		chanHeader = BroadcastSystem.CHANHEADER_S + ((char)this.ordinal());
	}
}
