package veryhardai;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;

/**
 * System used to create scout wires, used for sensing large areas of the
 * map and communicating the information quickly across the map.
 * 
 * TODO(jven): To ensure stability, scouts should not receive multiple wire
 * requests at once. :) I have an idea for how to do this but don't know if
 * it's worth the trouble.
 * 
 * TODO(jven): Reset wire stats method?
 * 
 * STAGES OF WIRE FORMATION:
 * (i) Archon requests a wire from nearby scouts.
 * (ii) Nearby scouts accept request by sending their ID to the archon.
 * (iii) Archon confirms wire by broadcasting the IDs of the wire's scouts.
 * 
 * @author jven
 */
public class ScoutWireSystem {

	/**
	 * TODO(jven): Move me out of here
	 * Constants for the ScoutWireSystem
	 */
	private class ScoutWireSystemConstants {
		public static final int MAX_SCOUTS_PER_WIRE = 10;
//		public static final int MAX_WIRE_ID = 255;
	}
	
	//----------------- COMMON FIELDS ---------------------
	
	/** The Robot using the system. */
	private BaseRobot br;
	
	/** The IDs of all of the Scouts in the current wire. -1 means no Robot. */
	private int[] partnerIDs;
	
	/** The ID of the Archon requesting the wire. */
	private int archonID;
	
	/** The starting location of the wire. */
	private MapLocation startLoc;
	/** The ending location of the wire. */
	private MapLocation endLoc;
	
	/** The ID of the next confirm to send. */
	private int confirmID = 0;
	
	// ----------------- ARCHON FIELDS ---------------------
	
	
	// ----------------- SCOUT FIELDS ---------------------
	
	/** The ID of the last confirm rebroadcasted. */
	private int lastConfirmID = 0;
	
	//----------------- CONSTRUCTOR ---------------------
	
	/**
	 * Constructor.
	 * @param myBR The Robot using the system.
	 */
	public ScoutWireSystem(BaseRobot myBR) {
		br = myBR;
		reset();
	}
	
	//----------------- STATE STUFF ---------------------
	
	/**
	 * Reset wire information.
	 */
	private void reset() {
		partnerIDs = new int[(
				ScoutWireSystemConstants.MAX_SCOUTS_PER_WIRE)];
		for (int idx = 0; idx < partnerIDs.length; idx++) {
			partnerIDs[idx] = -1;
		}
		startLoc = br.myHome;
		endLoc = br.myHome;
		archonID = -1;
		lastConfirmID = 0;
	}
	
	/**
	 * Returns whether the Scout is on a wire. Should only be called by Scouts.
	 */
	public boolean isOnWire() {
		// make sure a Scout is calling this
		if (br.myType != RobotType.SCOUT) {
			return false;
		}
		// make sure I've at least requested a wire
		if (archonID == -1) {
			return false;
		}
		// make sure I've been confirmed
		for (int idx = 0; idx < partnerIDs.length; idx++) {
			if (br.myID == partnerIDs[idx]) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns whether the Archon owns a wire. Should only be called by Archons.
	 */
	public boolean ownsWire() {
		// make sure an Archon is calling this
		if (br.myType != RobotType.ARCHON) {
			return false;
		}
		return archonID == br.myID;
	}
	
	//----------------- BROADCAST/PROCESSING STUFF ---------------------
	
	/**
	 * Request a wire from nearby scouts if the Archon doesn't already have one,
	 * does nothing otherwise. Should be called only by Archons.
	 */
	public void broadcastWireRequest() {
		// make sure an Archon is calling this and that he doesn't already own
		// a wire
		if (br.myType != RobotType.ARCHON || ownsWire()) {
			return;
		}
		// send my ID
		int msgArchonID = br.myID;
		br.io.sendUShort(
				BroadcastChannel.SCOUTS, BroadcastType.WIRE_REQUEST, msgArchonID);
	}
	
	/**
	 * Accepts a wire request from an Archon if the Scout isn't already on one,
	 * does nothing otherwise. Should be called only by Scouts in their
	 * processMessage
	 * @param msgArchonID The Archon of the wire request to accept
	 */
	public void broadcastWireAccept(int msgArchonID) {
		// make sure a Scout is calling this and isn't already on a wire
		if (br.myType != RobotType.SCOUT || archonID != -1) {
			return;
		}
		// get wire request information
		archonID = msgArchonID;
		// send wire accept
		int[] wireAccept = new int[] {archonID, br.myID};
		br.io.sendUShorts(
				BroadcastChannel.ARCHONS, BroadcastType.WIRE_ACCEPT, wireAccept);
	}
	
	/**
	 * Processes a wire accept from a Scout. Should only be called by Archons
	 * in their processMessage
	 * @param wireAccept
	 */
	public void processWireAccept(int[] wireAccept) {
		// make sure an Archon is calling this and that he doesn't already own
		// a wire
		if (br.myType != RobotType.ARCHON || ownsWire()) {
			return;
		}
		// make sure the Archon is the intended recipient of the wire accept
		if (br.myID != wireAccept[0]) {
			return;
		}
		// check if Scout ID is already in partner IDs
		for (int idx = 0; idx < partnerIDs.length; idx++) {
			if (partnerIDs[idx] == wireAccept[1]) {
				// we've already processed this wire accept
				return;
			}
		}
		// add Scout ID to partner IDs if there's room
		for (int idx = 0; idx < partnerIDs.length; idx++) {
			if (partnerIDs[idx] == -1) {
				partnerIDs[idx] = wireAccept[1];
				return;
			}
		}
	}
	
	/**
	 * Sends the final wire information to surrounding scouts. After this is
	 * called, the Archon will ignore incoming wire accepts. Also notifies these
	 * Scouts of the endpoints of the wire. Can be called multiple times with
	 * different inputs. Should only be called by Archons.
	 */
	public void broadcastWireConfirm() {
		// make sure an Archon is calling this
		if (br.myType != RobotType.ARCHON) {
			return;
		}
		// the Archon owns a wire
		archonID = br.myID;
		// make wire confirm
		int[] wireConfirm = new int[6 + partnerIDs.length];
		wireConfirm[0] = br.myID;
		wireConfirm[1] = startLoc.x;
		wireConfirm[2] = startLoc.y;
		wireConfirm[3] = endLoc.x;
		wireConfirm[4] = endLoc.y;
		wireConfirm[5] = ++confirmID;
		for (int idx = 0; idx < partnerIDs.length; idx++) {
			wireConfirm[6 + idx] = partnerIDs[idx];
		}
		// send wire confirm
		br.io.sendUShorts(
				BroadcastChannel.SCOUTS, BroadcastType.WIRE_CONFIRM, wireConfirm);
	}
	
	/**
	 * Checks if the Scout is on the given wire, updates the Scout's wire
	 * information if so, and rebroadcasts it. Should only be called by Scouts in
	 * their processMessage
	 * @param wireConfirm The wire confirm to process and rebroadcast
	 */
	public void rebroadcastWireConfirm(int[] wireConfirm) {
		// make sure a Scout is calling this and was requesting a wire
		if (br.myType != RobotType.SCOUT || archonID == -1) {
			return;
		}
		// ignore the message if I've already received it in the past
		if (wireConfirm[5] <= lastConfirmID) {
			return;
		}
		// ignore the message if I did not request this wire
		if (archonID != wireConfirm[0]) {
			return;
		}
		// set last confirm ID
		lastConfirmID = wireConfirm[5];
		// make sure I'm on the wire, ignore if not
		boolean amOnWire = false;
		for (int idx = 6; idx < wireConfirm.length; idx++) {
			if (br.myID == wireConfirm[idx]) {
				amOnWire = true;
				break;
			}
		}
		if (!amOnWire) {
			return;
		}
		// update wire information
		startLoc = new MapLocation(wireConfirm[1], wireConfirm[2]);
		endLoc = new MapLocation(wireConfirm[3], wireConfirm[4]);
		for (int idx = 0; idx < partnerIDs.length; idx++) {
			partnerIDs[idx] = -1;
		}
		for (int idx = 6; idx < wireConfirm.length; idx++) {
			partnerIDs[idx - 6] = wireConfirm[idx];
		}
		// rebroadcast
		br.io.sendUShorts(
				BroadcastChannel.SCOUTS, BroadcastType.WIRE_CONFIRM, wireConfirm);
	}
	
	//----------------- WIRE MAINTENANCE STUFF ---------------------
	
	/**
	 * Set the starting location of the wire. Should only be called by an
	 * Archon who owns a wire.
	 */
	public void setWireStartLoc(MapLocation loc) {
		// make sure an Archon is calling this who owns a wire
		if (br.myType != RobotType.ARCHON || !ownsWire()) {
			return;
		}
		startLoc = loc;
	}
	
	/**
	 * Set the ending location of the wire. Should only be called by an
	 * Archon who owns a wire.
	 */
	public void setWireEndLoc(MapLocation loc) {
		// make sure an Archon is calling this who owns a wire
		if (br.myType != RobotType.ARCHON || !ownsWire()) {
			return;
		}
		endLoc = loc;
	}
	
	/**
	 * Disbands the wire. Should be called only by the owner of a wire or a
	 * Scout already on a wire.
	 */
	public void broadcastAbortWire() {
		// make sure a Scout is calling this and that he is already on a wire
		// or an Archon is calling this and he owns a wire
		if (br.myType != RobotType.SCOUT &&
				br.myType != RobotType.ARCHON ||
				archonID == -1) {
			return;
		}
		// send abort
		br.io.sendUShort(
				BroadcastChannel.ALL, BroadcastType.WIRE_ABORT, archonID);
		// reset
		reset();
	}
	
	/**
	 * Processes a wire abort message. Should be called only by the owner of a
	 * wire or a Scout already on a wire in their processMessages.
	 */
	public void processAbortWire(int msgArchonID) {
		// make sure a Scout is calling this and that he is already on a wire
		// or an Archon is calling this and he owns a wire
		if (br.myType != RobotType.SCOUT &&
				br.myType != RobotType.ARCHON ||
				archonID == -1) {
			return;
		}
		// check that this message is about this unit's wire
		if (msgArchonID != archonID) {
			return;
		}
		// send abort
		broadcastAbortWire();
	}
	
	/**
	 * Returns the location the Scout should be in on his wire. Should only be
	 * called by Scouts already on a wire. Returns null if method is
	 * inappropriately called.
	 */
	public MapLocation getMyWireLocation() {
		// make sure Scout is calling this and that he is already on a wire
		if (br.myType != RobotType.SCOUT || archonID == -1) {
			return null;
		}
		// get my location in partnerIDs
		int myIndex = -1;
		for (int idx = 0; idx < partnerIDs.length; idx++) {
			if (br.myID == partnerIDs[idx]) {
				myIndex = idx;
				break;
			}
		}
		// if i'm not in partnerIDs, reset
		if (myIndex == -1) {
			reset();
			return null;
		}
		// get my location
		int sx = startLoc.x;
		int sy = startLoc.y;
		int tx = endLoc.x;
		int ty = endLoc.y;
		int dx = tx - sx;
		int dy = ty - sy;
		int n = getNumScoutsOnWire();
		return new MapLocation(
				(int)(sx + ((1.0 + myIndex) / (n + 1)) * dx),
				(int)(sy + ((1.0 + myIndex) / (n + 1)) * dy));
	}
	
	//----------------- WIRE UTILITIES ---------------------
	
	/**
	 * Get the number of Scouts on the wire.
	 */
	public int getNumScoutsOnWire() {
		// TODO(jven): cache this?
		int ans = 0;
		for (int idx = 0; idx < partnerIDs.length; idx++) {
			if (partnerIDs[idx] != -1) {
				ans++;
			}
		}
		return ans;
	}
}
