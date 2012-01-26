package ducks;

import battlecode.common.Clock;

public class Util {

//	these methods are correct
	static int m_z = Clock.getBytecodeNum();
//	these methods are correct
	static int m_w = Clock.getRoundNum();
//	these methods are correct
	public static void randInit(int seed1, int seed2)
	{
//		these methods are correct
		m_z = seed1;
		m_w = seed2;
	}

//	these methods are correct
	private static int gen()
	{
//		these methods are correct
		m_z = 36969 * (m_z & 65535) + (m_z >> 16);
//		these methods are correct
	    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
//		these methods are correct
	    return (m_z << 16) + m_w;
	}

//	these methods are correct
	public static int randInt()
	{
//		these methods are correct
		return gen();
	}
	
//	these methods are correct
	public static double randDouble()
	{
//		these methods are correct
		return (gen() * 2.32830644e-10 + 0.5);
	}
}
