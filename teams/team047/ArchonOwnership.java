package team047;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

/**
 * Class used to tie units to archons. Archons claim ownership via broadcast
 * and units accept them. 
 * 
 * @author jven -cory
 */
public class ArchonOwnership {

	private BaseRobot br;
	/** {{birthday, birthplace.x, birthplace.y}} */
	private int[][] buffer;
	private int timeUntilBroadcast;
	
	private int archonOwnerID;
//	private int archonRobotID;

	public ArchonOwnership(BaseRobot myBR) {
		br = myBR;
		buffer = new int[Constants.ARCHON_OWNERSHIP_BUFFER_LENGTH][3];
		for (int idx = 0; idx < buffer.length; idx++) {
			buffer[idx][0] = -1;
			buffer[idx][1] = -1;
			buffer[idx][2] = -1;
		}
		timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
		archonOwnerID = -1;
//		archonRobotID = -1;
	}
	
	// ----------------------- ARCHON METHODS ---------------------
	
	/**
	 * Claim ownership of the unit right in front of you. 
	 * Only archons should call this.
	 */
	public void claimOwnership() {
		// ignore if not an archon
		if (br.myType != RobotType.ARCHON) {
			return;
		}
		// get information to set in ownership
		int birthday = br.curRound;
		MapLocation birthplace = br.curLocInFront;
		// store ownership in buffer
		for (int idx = 0; idx < buffer.length; idx++) {
			if (buffer[idx][0] == -1) {
				buffer[idx][0] = birthday;
				buffer[idx][1] = birthplace.x;
				buffer[idx][2] = birthplace.y;
				@SuppressWarnings("unused")
				int cory = 5; // -cory
				break; 
			}
		}
	}
	
	/**
	 * Broadcast ownership information. Only archons should call this.
	 */
	public void broadcastOwnerships(int trueArchonID) {
		// ignore if not an archon
		if (br.myType != RobotType.ARCHON) {
			return;
		}
		// broadcast buffer
		if (--timeUntilBroadcast <= 0) {
			for (int[] ownership : buffer) {
				if (ownership[0] != -1) {
					br.io.sendUShorts(BroadcastChannel.ALL, BroadcastType.OWNERSHIP_CLAIM,
							new int[] {ownership[0], ownership[1],
							ownership[2], trueArchonID});
				}
			}
			timeUntilBroadcast = Constants.ARCHON_BROADCAST_FREQUENCY;
		}
	}
	
	/**
	 * See if a unit acknowledged your ownership broadcast, 
	 * remove from buffer if so.
	 */
	public void processAcknowledgement(int[] ack) {
		// ignore if not an archon
		if (br.myType != RobotType.ARCHON) {
			return;
		}
		// check if a unit has acknowledged one of my ownerships
		for (int idx = 0; idx < buffer.length; idx++) {
			if (ack[0] == buffer[idx][0] &&
					ack[1] == buffer[idx][1] &&
					ack[2] == buffer[idx][2]) {
				buffer[idx][0] = -1;
				buffer[idx][1] = -1;
				buffer[idx][2] = -1;
				break;
			}
		}
	}
	
	/**
	 * For debugging. Get number of unacknowledged ownerships.
	 */
	public int getNumUnacknowledgedOwnerships() {
		int ans = 0;
		for (int[] ownership: buffer) {
			if (ownership[0] != -1) {
				ans++;
			}
		}
		return ans;
	}
	
	// --------------------- OTHER UNIT METHODS ----------------------
	
	/**
	 * See if the ownership refers to me, broadcast acknowledgment if so.
	 */
	public boolean processOwnership(int[] ownership) {
		// make sure I'm not an archon
		if (br.myType == RobotType.ARCHON) {
			return false;
		}
		// ignore if I already have an owner
		if (archonOwnerID != -1) {
			return false;
		}
		// check ownership
		if (br.birthday - ownership[0] <= GameConstants.WAKE_DELAY &&
				ownership[1] == br.birthplace.x &&
				ownership[2] == br.birthplace.y) {
			archonOwnerID = ownership[3];
//			archonRobotID = ownership[4];
			br.io.sendUShorts(BroadcastChannel.ARCHONS, BroadcastType.OWNERSHIP_CLAIM, new int[] {ownership[0], ownership[1],
					ownership[2]});
			
			throw new RuntimeException("ArchonOwnership[processOwnership]: THIS IS BROKEN FIX ME LATER");
//			br.io.addChannel("#" + archonOwnerID);
//			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get true ID of owner archon, -1 if not set.
	 */
	public int getArchonOwnerID() {
		// make sure I'm not an archon
		if (br.myType == RobotType.ARCHON) {
			return -1;
		}
		return archonOwnerID;
	}
	
//	/**
//	 * Get robot ID of owner archon, -1 if not set.
//	 */
//	public int getArchonRobotID() {
//		// make sure I'm not an archon
//		if (br.myType == RobotType.ARCHON) {
//			br.debug.println(
//					"Archon " + br.myID + " tried to process ownership!");
//			return -1;
//		}
//		return archonRobotID;
//	}
}
