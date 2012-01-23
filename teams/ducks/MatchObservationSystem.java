package ducks;

import battlecode.common.MapLocation;
import battlecode.common.Message;

public class MatchObservationSystem {
	
	// TODO(jven): throw out messages with strings containing the delimiter
	
	private static final String DELIMITER = "#~#";
	private BaseRobot br;
	
	public MatchObservationSystem(BaseRobot myBR) {
		br = myBR;
	}
	
	public void rememberMessage(Message m) {
		br.rc.addMatchObservation(encryptString(serializeMessageToString(m)));
	}
	
	private static String serializeMessageToString(Message m) {
		if (m == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(DELIMITER);
		if (m.ints != null) {
			sb.append(m.ints.length + DELIMITER);
			for (int myInt : m.ints) {
				sb.append(myInt + DELIMITER);
			}
		} else {
			sb.append(0 + DELIMITER);
		}
		if (m.strings != null) {
			sb.append(m.strings.length + DELIMITER);
			for (String myString : m.strings) {
				sb.append(myString + DELIMITER);
			}
		} else {
			sb.append(0 + DELIMITER);
		}
		if (m.locations != null) {
			sb.append(m.locations.length + DELIMITER);
			for (MapLocation myLoc : m.locations) {
				sb.append(myLoc.x + "," + myLoc.y + DELIMITER);
			}
		} else {
			sb.append(0 + DELIMITER);
		}
		return sb.toString();
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
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			sb.append(Integer.toHexString((7 * i + (int)s.charAt(i)) % 65536) + 'x');
		}
		return sb.toString();
	}
	
	private static String decryptString(String s) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (s.indexOf('x') != -1) {
			sb.append((char)(((65536 - 7) * i + Integer.parseInt(
					s.substring(0, s.indexOf('x')), 16)) % 65536));
			s = s.substring(s.indexOf('x') + 1);
			i++;
		}
		return sb.toString();
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
		// PUT MATCH OBSERVATION STRING HERE TO GET THE OPPONENT'S MESSAGE
		String matchObservationString = "23x85x31x48x3fxa1x4dx62x6ax62xc4x70x8ax91x92x99x93xf5xa1xbexc5xc5xcfxd2xdcxe0xd9x13bxe7xfbxf5x157x103x119x111x173x11fx13cx143x148x148x14bx158x164x16ax172x176x16cx1cex17ax197x19ex1a3x1a6x1a6x1b3x1bfx1c5x1ccx1d6x1c7x229x1d5x";
		Message m = deserializeMessageFromString(decryptString(
				matchObservationString));
		for (int a : m.ints) System.out.println(a);
		for (String b : m.strings) System.out.println(b);
		for (MapLocation c : m.locations) System.out.println(c);
	}
}
