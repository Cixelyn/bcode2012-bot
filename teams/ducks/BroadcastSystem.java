package ducks;

import java.util.Arrays;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.Team;

/**
 *
 * The radio class encapsulates the sending of messages between robots
 * TODO: defend against message replay DoS attacks
 * 
 */
public class BroadcastSystem {

	private String[] boundChannelHeaders;
	private BaseRobot br;
	public int teamkey;
	
	private boolean shouldSendWakeup;

	StringBuilder msgContainer;
	

	private BroadcastSystem() {
		System.out.println("WARNING: RADIO NOT BOUND TO BASEROBOT");
		msgContainer = new StringBuilder();
	}

	
	
	public BroadcastSystem(BaseRobot br) {
		this.br = br;
		boundChannelHeaders = new String[0];
		teamkey = (br.myTeam == Team.A ?
				Constants.RADIO_TEAM_KEYS[0] : Constants.RADIO_TEAM_KEYS[1]);
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
		sendUShort(bChan.chanHeader + bType.header, data);
	}
	private void sendUShort(String header, int data) {
		msgContainer.append(header);
		msgContainer.append((char) (data + 0x100));
	}
	
	/**
	 * Decodes the next 15-bit unsigned short from the stream
	 * @param message data stream
	 * @return short value
	 */
	public static int decodeShort(StringBuilder msg) {
		return msg.charAt(0) - 0x100;
	}
	

	/**
	 * Sends a single map location to a unit
	 */
	public void sendMapLoc(BroadcastChannel bChan, BroadcastType bType, MapLocation loc) {
		sendMapLoc(bChan.chanHeader + bType.header, loc);
	}
	
	private void sendMapLoc(String header, MapLocation loc) {
		msgContainer.append(header);
		msgContainer.append((char) (loc.x + 0x100));
		msgContainer.append((char) (loc.y + 0x100));
	}
	
	public static MapLocation decodeMapLoc(StringBuilder msg) {
		return new MapLocation(
			msg.charAt(0) - 0x100,
			msg.charAt(1) - 0x100
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
		sendUShorts(bChan.chanHeader + bType.header, ints);
	}
	private void sendUShorts(String header, int[] ints) {
		msgContainer.append(header);
		for (int i : ints) {
			msgContainer.append((char)( i + 0x100));
		}
		msgContainer.append('!');
	}

	/**
	 * Decodes the next 15-bit unsigned integer array in the message
	 * @param msg - the message
	 * @return deserialized array
	 */
	public static int[] decodeUShorts(StringBuilder msg) {
		int end = msg.indexOf("!");
		int[] ints = new int[end];
		
		for(int i=end; --i >= 0; ) {
			ints[i] = msg.charAt(i) - 0x100;
		}
		return ints;
	}
	

	/**
	 * @param msg
	 * @return timestamp of the current message
	 */
	public static int decodeSenderTimestamp(StringBuilder msg) {
		return msg.charAt(msg.indexOf(":") + 1) - 0x100;
	}

	
	/**
	 * @param msg
	 * @return robot id of the of the current message's sender
	 */
	public static int decodeSenderID(StringBuilder msg) {
		return msg.charAt(msg.indexOf(":") + 2) - 0x100;
	}
	
	/**
	 * @param msg
	 * @return MapLocation of message's origin
	 */
	public static MapLocation decodeSenderLoc(StringBuilder msg) {
		int metaIdx = msg.indexOf(":");
		return new MapLocation(
				msg.charAt(metaIdx+3) - 0x100,
				msg.charAt(metaIdx+4) - 0x100
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
		sendUInts(bChan.chanHeader + bType.header, ints);
	}
	
	private void sendUInts(String header, int[] ints) {
		msgContainer.append(header);
		for (int i : ints ) {
			
			int lo = (i & 0x00007FFF )  + 0x100;
			int hi = ((i & 0x3FFF8000 )  >> 15) + 0x100;
			
			msgContainer.append((char) lo);
			msgContainer.append((char) hi);
		}
		msgContainer.append('!');
		
	}
	
	/**
	 * Decodes the next 30-bit integer array from the stream
	 * @param msg
	 * @return deserialized array
	 */
	public static int[] decodeInts(StringBuilder msg) {
		int num = msg.indexOf("!") / 2;
		int[] ints = new int[num];
		
		for(int i=num; --i >= 0;) {	
			ints[i] = 
				((msg.charAt(i*2  )      ) - 0x100) +  // lo bits
				((msg.charAt(i*2+1) - 0x100) << 15);   // hi bits
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
		sendMapLocs(bChan.chanHeader + bType.header, locs);
	}
	
	
	private void sendMapLocs(String header, MapLocation[] locs) {
		msgContainer.append(header);
		
		for (MapLocation l : locs) {
			msgContainer.append((char)( l.x + 0x100));
			msgContainer.append((char)( l.y + 0x100));
		}
		
		msgContainer.append('!');
	}
	
	
	/**
	 * Untested code. Ping Cory if you're trying to use it
	 * @param msg
	 * @return
	 */
	public static MapLocation[] decodeMapLocs(StringBuilder msg) {
		
		// calc number of locations
		int num = msg.indexOf("!") / 2;
		
		MapLocation[] locs = new MapLocation[num];
	
		for (int i=0; i < num; i++) {
			locs[i] = new MapLocation(
					msg.charAt(i*2  ) - 0x100,
					msg.charAt(i*2+1) - 0x100
					);
		}
		return locs;
	}
	

	public void sendAll() {
		
		// normal message sending
		if(msgContainer.length() > 0) {
		
			// append message metadata
			msgContainer.append(':');
			msgContainer.append((char)(Clock.getRoundNum() + 0x100));
			msgContainer.append((char)(br.myID + 0x100));
			msgContainer.append((char)(br.curLoc.x + 0x100));
			msgContainer.append((char)(br.curLoc.y + 0x100));
			
			// build message
			Message m = new Message();
			m.strings = new String[]{msgContainer.toString()};
			
			
			// build wakeup call
			if(shouldSendWakeup) {
				m.ints = new int[3];
				m.ints[2] = Clock.getRoundNum();
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
		
		// build a pure wakeup call if we have nothign to send
		if(shouldSendWakeup) {
			Message m = new Message();
			m.ints = new int[3];
			m.ints[0] = teamkey;
			m.ints[2] = Clock.getRoundNum();
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
	
	
	private static int hashMessage(StringBuilder msg) {
		StringBuilder tmp = new StringBuilder();
	
		int endpoint = msg.length();
		int midpoint = endpoint / 2;
		tmp.append(msg.substring(midpoint,endpoint));
		tmp.append(msg.substring(0,midpoint));
		
		return tmp.toString().hashCode();
	}
	
	
	
	public void receive() throws GameActionException {
		
		// check for active listeners
		if(boundChannelHeaders.length > 0) { 
			
			//Message Receive Loop
			StringBuilder sb = new StringBuilder();
			for (Message m: br.rc.getAllMessages()) {
				
				
				// validity check
				if(m.ints == null) continue;
				if(m.ints.length < 2 || m.ints.length > 3) continue;
				if(m.strings == null || m.strings.length != 1 ) continue;
				if(m.locations != null) continue;
				
				// team check
				if(m.ints[0] != teamkey) continue;
				
				// hash check
				if(m.strings[0] == null) continue;
				if(m.ints[1] != hashMessage(new StringBuilder(m.strings[0]))) continue;
				
				sb.append(m.strings[0]);
			}
			
			
			//Message Process Loop
			int i=0;
			for(String addr: boundChannelHeaders) {
			    while((i = sb.indexOf(addr, i)) != -1) {
			        br.processMessage(
			        	BroadcastType.decode(sb.charAt(i+addr.length())),
			        	new StringBuilder(sb.substring(i + addr.length() + 1))
			        );
			        i++;
			    }	
			}
		}
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
		
		
		System.out.println(BroadcastType.decode(BroadcastType.ENEMY_ARCHON_KILL.header).toString());
	}
	
	
	
}
