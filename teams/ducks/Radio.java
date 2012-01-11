package ducks;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.Team;

public class Radio {

	String[] listenAddrs;
	BaseRobot br;
	int teamkey;

	StringBuilder msgContainer;

	public Radio(BaseRobot br) {
		this.br = br;
		listenAddrs = new String[]{};
		teamkey = (br.myTeam == Team.A ?
				Constants.RADIO_TEAM_KEYS[0] : Constants.RADIO_TEAM_KEYS[1]);
		msgContainer = new StringBuilder();
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
	 * Queue an integer for broadcasting.
	 * Headers are an address followed by a message type
	 * eg: #xy, where the address to send it to is ^x and the
	 * type of the message is y.
	 * @param header
	 * @param data
	 */
	public void sendInt(String header, int data) {
		msgContainer.append(header);
		msgContainer.append((char) data + 0x100);
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
		
		msgContainer.append("!");
	}
	
	
	
	public static MapLocation[] decodeMapLocs(StringBuilder s) {
		
		// calc number of locations
		int end = s.indexOf("!");
		System.out.println(s);
		System.out.println(end);
		
		int num = (end-3)/2;
		MapLocation[] locs = new MapLocation[num];
	
		for (int i=0; i < num; i++) {
			locs[i] = new MapLocation(
					s.charAt(i*2) - 0x100,
					s.charAt(i*2) - 0x100
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
			m.ints = new int[2];
			
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
				if(m.ints == null || m.ints.length != 2 ) continue;
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
