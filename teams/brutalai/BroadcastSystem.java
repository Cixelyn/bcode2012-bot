package brutalai;

import java.util.Arrays;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.RobotType;

/**
 *
 * The radio class encapsulates the sending of messages between robots
 * TODO: defend against message replay DoS attacks
 * 
 */
public class BroadcastSystem {
	
	public static char CHANHEADER_C = ((char) -1);
	public static char TERMINATOR_C = ((char) -2);
	public static char METADATA_C = ((char) -3);
	
	public static String CHANHEADER_S = String.valueOf(CHANHEADER_C);
	public static String TERMINATOR_S = String.valueOf(TERMINATOR_C);
	public static String METADATA_S = String.valueOf(METADATA_C);
	
	private String[] boundChannelHeaders;
	private BaseRobot br;
	public int teamkey;
	
	private boolean shouldSendWakeup;

	StringBuilder msgContainer;
	

	private BroadcastSystem() {
		System.out.println("WARNING: RADIO NOT BOUND TO BASEROBOT");
		msgContainer = new StringBuilder();
		teamkey = 1;
	}

	
	
	public BroadcastSystem(BaseRobot br) {
		this.br = br;
		boundChannelHeaders = new String[0];
		teamkey = (br.myHome.x * 0xFFFF) + br.myHome.y;
		
		msgContainer = new StringBuilder();
		shouldSendWakeup = false;
	}

	/**
	 * Set the addresses a robot should listen for.
	 * Addresses are two characters in the form of #x,
	 * where the # is invariable, and the x can be any
	 * string which you wish to listen on
	 * @param addrs
	 */
	public void setChannels(BroadcastChannel[] channels) {
		boundChannelHeaders = new String[channels.length];
		for(int i=0; i < channels.length; i++) {
			boundChannelHeaders[i] = channels[i].chanHeader;
		}
	}


	/**
	 * Returns whether a robot is already bound
	 * to a particular address
	 * @param chn
	 * @return
	 */
	public boolean hasChannel(BroadcastChannel chn) {
		return findChannel(chn)!=-1;
	}
	
	private int findChannel(BroadcastChannel chn) {
		for(int i=boundChannelHeaders.length; --i>=0;) {
			if(boundChannelHeaders[i] == chn.chanHeader) {
				return i;
			}
		}
		return -1;
	}
	

	/**
	 * Adds a particular channel to the robot's active ports.
	 * Internally checks whether the address is already bound or not
	 * @param chn
	 * @return whether address was bound or not
	 */
	public boolean addChannel(BroadcastChannel chn) {
		if(!hasChannel(chn)) {
			
			int oldlen = boundChannelHeaders.length;
			
			String[] newAddrs = new String[oldlen + 1];
			System.arraycopy(boundChannelHeaders, 0, newAddrs, 0, oldlen);
			newAddrs[oldlen] = chn.chanHeader;
			boundChannelHeaders = newAddrs;
			
			return true;
		} else {
			return false;
		}
	}
	
	
	/**
	 * Removes a robot's binding to a channel
	 * Internally checks whether the address is already bound or not
	 * @param channel
	 */
	public boolean removeChannel(BroadcastChannel chn) {
		int pos=findChannel(chn);
		if(pos >= 0) {
			int oldlen = boundChannelHeaders.length;
			String[] newChans = new String[oldlen - 1];
			System.arraycopy(boundChannelHeaders, 0, newChans, 0, pos);
			System.arraycopy(boundChannelHeaders, pos+1, newChans, pos, oldlen - pos -1);
			boundChannelHeaders = newChans;
			return true;
			
		} else {
			return false;
		}
	}
	
	
	
	/**
	 * Next broadcast will have a wakeup note appended to it
	 */
	public void sendWakeupCall() {
		shouldSendWakeup = true;
	}
	

	/**
	 * Queue a 15-bit unsigned short for broadcasting.
	 * Headers are an address followed by a message type
	 * eg: #xy, where the address to send it to is ^x and the
	 * type of the message is y.
	 */
	public void sendUShort(BroadcastChannel bChan, BroadcastType bType, int data) {
		sendUShort(bChan.chanHeader + bType.header_c, data);
	}
	private void sendUShort(String header, int data) {
		msgContainer.append(header + (char)data);
	}
	
	/**
	 * Decodes the next 15-bit unsigned short from the stream
	 * @param message data stream
	 * @return short value
	 */
	public static int decodeShort(StringBuilder msg) {
		return msg.charAt(0);
	}
	

	/**
	 * Sends a single map location to a unit
	 */
	public void sendMapLoc(BroadcastChannel bChan, BroadcastType bType, MapLocation loc) {
		sendMapLoc(bChan.chanHeader.concat(bType.header_s), loc);
	}
	
	private void sendMapLoc(String header, MapLocation loc) {
		msgContainer.append(header + (char)loc.x + (char)loc.y);
	}
	
	public static MapLocation decodeMapLoc(StringBuilder msg) {
		return new MapLocation(
			msg.charAt(0),
			msg.charAt(1)
		);
	}
	
	
	/**
	 * Send a variable-sized array of unsigned 15-bit integers.
	 * Make sure each element in the array is LESS THAN 32768,
	 * otherwise deserialization will fail
	 * @param bChan - broadcast channel
	 * @param bType - message type
	 * @param ints - array of 15-bit ints
	 * @see BroadcastSystem#sendUShort(String, int)
	 */
	public void sendUShorts(BroadcastChannel bChan, BroadcastType bType, int[] ints) {
		sendUShorts(bChan.chanHeader.concat(bType.header_s), ints);
	}
	private void sendUShorts(String header, int[] ints) {
		for (int i : ints) {
			header = header.concat(String.valueOf((char) i ));
		}
		msgContainer.append(header.concat(TERMINATOR_S));
		
	}
	
	/**
	 * Sends a raw string over the channel. Same limitations apply as shorts,
	 * "unicode chars" must be \u7fff or below.
	 * @param bChan - broadcast channel
	 * @param bType - message type
	 * @param data - string to send
	 * @see BroadcastSystem#sendUShorts(String, int[])
	 */
	public void sendRaw(BroadcastChannel bChan, BroadcastType bType, String data) {
		msgContainer.append(bChan.chanHeader.concat(bType.header_s).concat(data));
	}
	
	
	

	/**
	 * Decodes the next 15-bit unsigned integer array in the message
	 * @param msg - the message
	 * @return deserialized array
	 */
	public static int[] decodeUShorts(StringBuilder msg) {
		int end = msg.indexOf(TERMINATOR_S);
		int[] ints = new int[end];
		
		for(int i=end; --i >= 0; ) {
			ints[i] = msg.charAt(i);
		}
		return ints;
	}
	

	/**
	 * @param msg
	 * @return timestamp of the current message
	 */
	public static int decodeSenderTimestamp(StringBuilder msg) {
		return msg.charAt(msg.indexOf(METADATA_S) + 1);
	}

	
	/**
	 * @param msg
	 * @return robot id of the of the current message's sender
	 */
	public static int decodeSenderID(StringBuilder msg) {
		return msg.charAt(msg.indexOf(METADATA_S) + 2);
	}
	
	/**
	 * @param msg
	 * @return MapLocation of message's origin
	 */
	public static MapLocation decodeSenderLoc(StringBuilder msg) {
		int metaIdx = msg.indexOf(METADATA_S);
		return new MapLocation(
				msg.charAt(metaIdx+3),
				msg.charAt(metaIdx+4)
		);
	}
	
	
	
	
	/**
	 * Send a variable-sized array of unsigned 30-bit integers.
	 * Make sure each element in the array is ONLY 30-bits long
	 * otherwise you'll get raped by wrap-around
	 * <br/>
	 * If your number is only 15-bits long, consider sending
	 * a short instead, as it's much cheaper
	 * @param bChan - channel to braodcast
	 * @param bType - type of message
	 * @param ints - array of 31-bit ints
	 * @see BroadcastSystem#sendUShorts(String, int[])
	 */
	
	public void sendUInts(BroadcastChannel bChan, BroadcastType bType, int[] ints) {
		sendUInts(bChan.chanHeader + bType.header_c, ints);
	}
	
	private void sendUInts(String header, int[] ints) {
		for (int i : ints ) {
			header = header.concat(
				String.valueOf((char) ( i & 0x00007FFF ))).concat( // LO BITS
				String.valueOf((char) ((i & 0x3FFF8000 )  >> 15))); // HI BITS
		}
		msgContainer.append(header.concat(TERMINATOR_S));
	}
	
	/**
	 * Decodes the next 30-bit integer array from the stream
	 * @param msg
	 * @return deserialized array
	 */
	public static int[] decodeInts(StringBuilder msg) {
		int num = msg.indexOf(TERMINATOR_S) / 2;
		int[] ints = new int[num];
		
		for(int i=num; --i >= 0;) {	
			ints[i] = 
				(msg.charAt(i*2  )    +      // lo bits
				(msg.charAt(i*2+1) << 15));  // hi bits
		}
		
		return ints;
	}
		
	

	/**
	 * Queue a list of MapLocations
	 * @param bChan - channel to use
	 * @param bType - type of message
	 * @param locs - Locations to send
	 * @see BroadcastSystem#sendUShort(String, int) sendInt
	 */
	public void sendMapLocs(BroadcastChannel bChan, BroadcastType bType, MapLocation[] locs) {
		sendMapLocs(bChan.chanHeader + bType.header_c, locs);
	}
	
	
	private void sendMapLocs(String header, MapLocation[] locs) {
		msgContainer.append(header);
		
		for (MapLocation l : locs) {
			msgContainer.append((char) l.x );
			msgContainer.append((char) l.y );
		}
		
		msgContainer.append(TERMINATOR_C);
	}
	
	
	/**
	 * Sends a bunch of map locations
	 * @param msg
	 * @return
	 */
	public static MapLocation[] decodeMapLocs(StringBuilder msg) {
		
		// calc number of locations
		int num = msg.indexOf(TERMINATOR_S) / 2;
		
		MapLocation[] locs = new MapLocation[num];
	
		for (int i=0; i < num; i++) {
			locs[i] = new MapLocation(
					msg.charAt(i*2  ),
					msg.charAt(i*2+1)
					);
		}
		return locs;
	}
	

	/**
	 * forces a message send. Use at your own risk
	 * @param m - message to send
	 */
	public void forceSend(Message m) {
		try{
			br.rc.broadcast(m);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	

	public void sendAll() {
		
		// normal message sending
		if(msgContainer.length() > 0 && !br.rc.hasBroadcasted()) {
		
			// append message metadata
			msgContainer.append(generateMetadata());
			
			// build message
			Message m = new Message();
			m.strings = new String[]{msgContainer.toString()};
			
			
			// build wakeup call
			if(shouldSendWakeup) {
				m.ints = new int[3];
				m.ints[2] = locToInt(br.curLoc);
				shouldSendWakeup = false;
			} else {
				m.ints = new int[2];
			}
			
			// sign message
			m.ints[0] = teamkey;
			m.ints[1] = hashMessage(msgContainer);
			
			try {
				br.rc.broadcast(m);
			} catch (GameActionException e) {
				System.out.println("Broadcasting threw an error. Maybe called more than once?");
				e.printStackTrace();
			}
		
			// wipe container
			msgContainer = new StringBuilder();
			
			return;
		}
		
		
		// build a pure wakeup call if we had nothing to send
		if(shouldSendWakeup && !br.rc.hasBroadcasted()) {
			Message m = new Message();
			m.ints = new int[3];
			m.ints[0] = teamkey;
			m.ints[2] = locToInt(br.curLoc);
			shouldSendWakeup = false;
			try {
				br.rc.broadcast(m);
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
		
	}

	/** Completely dumps your whole message queue */
	public void flushIncomingQueue() {
		br.rc.getAllMessages();
	}
	
	/** Dumps your whole message send queue queue */
	public void flushSendQueue() {
		msgContainer = new StringBuilder();
	}
	
	
	public int hashMessage(StringBuilder msg) {
		String tmp = new String();
	
		int endpoint = msg.length();
		int midpoint = endpoint / 2;
		
		return tmp.concat(msg.substring(midpoint,endpoint)).concat(
					(msg.substring(0,midpoint))).hashCode() * this.teamkey;
		
	}
	
	
	public String generateMetadata() {
		return METADATA_S.concat(
				String.valueOf((char)Clock.getRoundNum())).concat(
				String.valueOf((char)br.myID)).concat(
				String.valueOf((char)br.curLoc.x)).concat(
				String.valueOf((char)br.curLoc.y));		
	}
	
	
	
	public void receive() throws GameActionException {
		
		// check for active listeners
		if(boundChannelHeaders.length > 0) { 
			
			//Message Receive Loop
			StringBuilder sb = new StringBuilder();
			for (Message m: br.rc.getAllMessages()) {
			
				int[] mints;
				String data;
				
				// ints validity check
				if((mints = m.ints) == null || mints.length < 2 || mints.length > 3) {memoEnemy(m); continue;}
				if(m.strings == null || m.strings.length != 1) {memoEnemy(m); continue;}
				if(m.locations != null) {memoEnemy(m); continue;}
				
				// team check
				if(mints[0] != teamkey) { memoEnemy(m); continue;}
				
				// hash check
				if((data = m.strings[0]) == null) {memoEnemy(m); continue;}
				if(mints[1] != hashMessage(new StringBuilder(data))) { memoEnemy(m); continue; }
				
				sb.append(data);
			}
			
			
			//Message Process Loop
			int i=0;
			for(String addr: boundChannelHeaders) {
			    while((i = sb.indexOf(addr, i)) != -1) {
			        br.processMessage(
			        	BroadcastType.decode(sb.charAt(i + 2)),
			        	new StringBuilder(sb.substring(i + 3))
			        );
			        i++;
			    }	
			}
		}
	}
	
	private void memoEnemy(Message m) {
		// log message to match observation
		if (br.myType == RobotType.ARCHON) {
			br.mos.rememberMessage(m, false);
		}
		// if a scout, try to identify enemy team
		if (br.myType == RobotType.SCOUT && br.mas.guessEnemyTeam() == -1 &&
				Util.randDouble() < 0.01) {
			// the boolean determines whether we encrypt or not
			br.mas.detectTeam(m);
		}
	}
	
	/**
	 * @param data - packed maplocation where high bits are x loc, low bits are y loc
	 * @return unpacked MapLocation
	 */
	public static MapLocation intToLoc(int data) {
		return new MapLocation(
				data >> 16,
				data & 0xFFFF
		);
	}

	/**
	 * Packs a map location into an int where high bits are x loc, low bits are y loc
	 * @param loc - maplocation
	 * @return encoded location
	 */
	public static int locToInt(MapLocation loc) {
		return (loc.x << 16) + loc.y;
	}
	

	/**
	 * Test code to ensure serialization / deserialization works
	 */
	public static void main(String args[]) {
		BroadcastSystem io = new BroadcastSystem();
		
		int[] a;
		a = new int[]{555,10000,20000,30000,40000,500000,1073741823};
		
		io.sendUShorts("",a);
		System.out.println((Arrays.toString(BroadcastSystem.decodeUShorts(io.msgContainer))));
		io.msgContainer = new StringBuilder();
		
		io.sendUInts("",a);
		System.out.println((Arrays.toString(BroadcastSystem.decodeInts(io.msgContainer))));
		io.msgContainer = new StringBuilder();
		
		MapLocation[] locs;
		locs = new MapLocation[]{new MapLocation(3,20), new MapLocation(200,234), new MapLocation(4,9000)};
		
		io.sendMapLocs("", locs);
		System.out.println((Arrays.toString(BroadcastSystem.decodeMapLocs(io.msgContainer))));
		io.msgContainer = new StringBuilder();

		io.setChannels(new BroadcastChannel[] {});
		System.out.println((Arrays.toString(io.boundChannelHeaders)));
		io.addChannel(BroadcastChannel.ALL);
		System.out.println((Arrays.toString(io.boundChannelHeaders)));
		
		
		// Test hashing
		StringBuilder msg1 = new StringBuilder("abcdefg");
		StringBuilder msg2 = new StringBuilder("woieruw");
		StringBuilder msg3 = new StringBuilder("weoiruwaeral;sfaas;dlfjxc2#$@#$@#$");
		System.out.println(io.hashMessage(msg1));
		System.out.println(io.hashMessage(msg1));
		System.out.println(io.hashMessage(msg2));
		System.out.println(io.hashMessage(msg2));
		System.out.println(io.hashMessage(msg3));
		System.out.println(io.hashMessage(msg3));
		
		MapLocation b = new MapLocation(23414, 23);
		System.out.println(BroadcastSystem.locToInt(b));
		System.out.println(BroadcastSystem.intToLoc(BroadcastSystem.locToInt(b)));
		
		System.out.println(BroadcastType.decode(BroadcastType.ENEMY_ARCHON_KILL.header_c).toString());
	}
	
	
}
