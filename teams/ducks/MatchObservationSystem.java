package ducks;

import battlecode.common.Clock;
import battlecode.common.MapLocation;
import battlecode.common.Message;

public class MatchObservationSystem {
	
	// TODO(jven): throw out messages with strings containing the delimiter
	
	private static final String DELIMITER = "#~#";
	private BaseRobot br;
	
	public MatchObservationSystem(BaseRobot myBR) {
		br = myBR;
	}
	
	/**
	 * Sets a match observation for the fun gamers to review later. Useful for
	 * looking at enemy messages!
	 * @param m The message to remember
	 * @param shouldEncrypt Whether to encrypt the message or not. Right now,
	 * takes about 2000 bytecodes per message. Not sure why it's so many. :(
	 */
	public void rememberMessage(Message m, boolean shouldEncrypt) {
		String s = serializeMessageToString(m);
		if (shouldEncrypt) {
			s = encryptString(s);
		}
		br.rc.addMatchObservation(s);
	}
	
	private static String serializeMessageToString(Message m) {
		if (m == null) {
			return "";
		}
		String s = DELIMITER;
		if (m.ints != null) {
			s = s.concat(m.ints.length + DELIMITER);
			for (int myInt : m.ints) {
				s = s.concat(myInt + DELIMITER);
			}
		} else {
			s = s.concat(0 + DELIMITER);
		}
		if (m.strings != null) {
			s = s.concat(m.strings.length + DELIMITER);
			for (String myString : m.strings) {
				s = s.concat(myString + DELIMITER);
			}
		} else {
			s = s.concat(0 + DELIMITER);
		}
		if (m.locations != null) {
			s = s.concat(m.locations.length + DELIMITER);
			for (MapLocation myLoc : m.locations) {
				s = s.concat(myLoc.x + "," + myLoc.y + DELIMITER);
			}
		} else {
			s = s.concat(0 + DELIMITER);
		}
		return s;
	}
	
	private static Message deserializeMessageFromString(String s) {
		int[] myInts;
		String[] myStrings;
		MapLocation[] myLocs;
		try {
			s = s.substring(s.indexOf(DELIMITER) + DELIMITER.length());
			int numInts = Integer.parseInt(s.substring(0, s.indexOf(DELIMITER)));
			myInts = new int[numInts];
			for (int i = 0; i < numInts; i++) {
				s = s.substring(s.indexOf(DELIMITER) + DELIMITER.length());
				myInts[i] = Integer.parseInt(s.substring(0, s.indexOf(DELIMITER)));
			}
			s = s.substring(s.indexOf(DELIMITER) + DELIMITER.length());
			int numStrings = Integer.parseInt(s.substring(0, s.indexOf(DELIMITER)));
			myStrings = new String[numStrings];
			for (int i = 0; i < numStrings; i++) {
				s = s.substring(s.indexOf(DELIMITER) + DELIMITER.length());
				myStrings[i] = s.substring(0, s.indexOf(DELIMITER));
			}
			s = s.substring(s.indexOf(DELIMITER) + DELIMITER.length());
			int numLocs = Integer.parseInt(s.substring(0, s.indexOf(DELIMITER)));
			myLocs = new MapLocation[numLocs];
			for (int i = 0; i < numLocs; i++) {
				s = s.substring(s.indexOf(DELIMITER) + DELIMITER.length());
				int myLocX = Integer.parseInt(s.substring(0, s.indexOf(",")));
				int myLocY = Integer.parseInt(s.substring(s.indexOf(",") + 1,
						s.indexOf(DELIMITER)));
				myLocs[i] = new MapLocation(myLocX, myLocY);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		Message m = new Message();
		m.ints = myInts;
		m.strings = myStrings;
		m.locations = myLocs;
		return m;
	}
	
	private static String encryptString(String s) {
		String t = "";
		for (int i = 0; i < s.length(); i++) {
			t = t.concat(Integer.toHexString(
					(7 * i + (int)s.charAt(i)) % 65536) + 'x');
		}
		return t;
	}
	
	private static String decryptString(String t) {
		String s = "";
		int i = 0;
		while (t.indexOf('x') != -1) {
			s = s.concat("" + (char)(((65536 - 7) * i + Integer.parseInt(
					t.substring(0, t.indexOf('x')), 16)) % 65536));
			t = t.substring(t.indexOf('x') + 1);
			i++;
		}
		return s;
	}
	
	private static void test() {
		Message m = new Message();
		m.ints = new int[] {10,11,12};
		m.strings = new String[] {"sup", "hi", "enemy over here!", "yoyoyoyo"};
		m.locations = new MapLocation[] {new MapLocation(22, 55)};
		String s = serializeMessageToString(m);
		System.out.println(s);
		s = encryptString(s);
		System.out.println(s);
		s = decryptString(s);
		System.out.println(s);
		Message m2 = deserializeMessageFromString(s);
		for (int a : m2.ints) System.out.println(a);
		for (String b : m2.strings) System.out.println(b);
		for (MapLocation c : m2.locations) System.out.println(c);
	}
	
	public static void main(String[] args) {
//		test();
		
		/**
		 *  PUT MATCH OBSERVATION STRING HERE TO GET THE OPPONENT'S MESSAGE
		 */
		String matchObservationString = "23x85x31x48x3fxa1x4dx62x6ax62xc4x70x8ax91x92x99x93xf5xa1xbexc5xc5xcfxd2xdcxe0xd9x13bxe7xfbxf5x157x103x119x111x173x11fx13cx143x148x148x14bx158x164x16ax172x176x16cx1cex17ax197x19ex1a3x1a6x1a6x1b3x1bfx1c5x1ccx1d6x1c7x229x1d5x";
		boolean wasEncrypted = true;
		
		
		// DON'T CHANGE ANYTHING BELOW
		if (wasEncrypted) {
			matchObservationString = decryptString(matchObservationString);
		}
		Message m = deserializeMessageFromString(matchObservationString);
		for (int a : m.ints) System.out.println(a);
		for (String b : m.strings) System.out.println(b);
		for (MapLocation c : m.locations) System.out.println(c);
	}
}
