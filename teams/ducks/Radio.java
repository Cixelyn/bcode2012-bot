package ducks;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.Team;

public class Radio {

	private String[] listenAddrs;
	private BaseRobot br;
	public int teamkey;
	
	private boolean shouldSendWakeup;

	StringBuilder msgContainer;

	public Radio(BaseRobot br) {
		this.br = br;
		listenAddrs = new String[]{};
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
	public void setAddresses(String[] addrs) {
		listenAddrs = addrs;
	}
	
	
	
	/**
	 * Next broadcast will have a wakeup note appended to it
	 */
	public void sendWakeupCall() {
		shouldSendWakeup = true;
	}
	

	/**
	 * Queue an integer for broadcasting.
	 * Headers are an address followed by a message type
	 * eg: #xy, where the address to send it to is ^x and the
	 * type of the message is y.
	 * @param header
	 * @param data
	 */
	public void sendInt(String header, int data) {
		msgContainer.append(header);
		msgContainer.append((char) (data + 0x100));
	}
	
	public static int decodeInt(StringBuilder s) {
		return s.charAt(0) - 0x100;
	}
	
	

	/**
	 * Sends a single map location to a unit
	 * @param header
	 * @param loc
	 */
	public void sendMapLoc(String header, MapLocation loc) {
		msgContainer.append(header);
		msgContainer.append((char) (loc.x + 0x100));
		msgContainer.append((char) (loc.y + 0x100));
	}
	
	public static MapLocation decodeMapLoc(StringBuilder s) {
		return new MapLocation(
			s.charAt(0) - 0x100,
			s.charAt(1) - 0x100
		);
	}
	

	/**
	 * Send a variable-sized array of 16-bit integers.
	 * Make sure each element in the array is LESS THAN 32000,
	 * otherwise deserialization will fail
	 * @param header - "#[addr][type]"
	 * @param ints - array of 16-bit ints
	 * @see Radio#sendInt(String, int) sendInt
	 */
	public void sendInts(String header, int[] ints) {
		msgContainer.append(header);
		for (int i : ints) {
			msgContainer.append((char)( i + 0x100));
		}
		msgContainer.append('!');
	}

	/**
	 * Decodes the next 16-bit integer array in the message
	 * @param msg - the message
	 * @return deserialized array
	 */
	public static int[] decodeInts(StringBuilder msg) {
		int end = msg.indexOf("!");
		int[] ints = new int[end];
		
		for(int i=0; i < end; i++ ) {
			ints[i] = msg.charAt(i) - 0x100;
		}
		return ints;
	}
	
	

	/**
	 * Queue a list of integers
	 * @param header
	 * @param locs
	 * @see Radio#sendInt(String, int) sendInt
	 */
	public void sendMapLocs(String header, MapLocation[] locs) {
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
		int end = msg.indexOf("!");
		
		int num = end/2;
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
		
		if(msgContainer.length() > 0) {
		
			// timestamp outgoing
			msgContainer.append((char)Clock.getRoundNum());
			
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
		}
	}
	
	
	private static int hashMessage(StringBuilder msg) {
		StringBuilder tmp = new StringBuilder();
	
		int endpoint = msg.length();
		int midpoint = endpoint / 2;
		tmp.append(msg.substring(midpoint,endpoint));
		tmp.append(msg.substring(0,midpoint));
		
		return tmp.toString().hashCode();
	}
	
	
	
	public void receive() {
		
		// check for active listeners
		if(listenAddrs.length > 0) { 
			
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
			for(String addr: listenAddrs) {
			    while((i = sb.indexOf(addr, i)) != -1) {
			        br.processMessage(
			        	sb.charAt(i+addr.length()),
			        	new StringBuilder(sb.substring(i + addr.length() + 1))
			        );
			        i++;
			    }	
			}
		}
	}
}
