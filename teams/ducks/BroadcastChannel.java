package ducks;

/** Channels available for broadcasting messages */
public enum BroadcastChannel {

	/** Channel used for common announces */
	ALL,
	/** Channel used for army swarm targets */
	FIGHTERS,
	/** Channel used by low flux units to call for scout help */
	SCOUTS,
	/** Channel used to pass all extended radar information */
	EXTENDED_RADAR,
	/** Channel used to pass all shared exploration data */
	EXPLORERS,
	;

	/** The 2 char channel header */
	public final String chanHeader;
	
	private BroadcastChannel() {
		chanHeader = BroadcastSystem.CHANHEADER_S + ((char)this.ordinal());
	}
}
