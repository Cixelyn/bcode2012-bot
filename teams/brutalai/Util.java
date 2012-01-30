package brutalai;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Util {

	static int m_z = Clock.getBytecodeNum();
	static int m_w = Clock.getRoundNum();
	
	/**
	 * sets up our RNG given two seeds
	 * @param seed1
	 * @param seed2
	 */
	public static void randInit(int seed1, int seed2)
	{
		m_z = seed1;
		m_w = seed2;
	}

	private static int gen()
	{
		m_z = 36969 * (m_z & 65535) + (m_z >> 16);
	    m_w = 18000 * (m_w & 65535) + (m_w >> 16);
	    return (m_z << 16) + m_w;
	}

	/** @return an integer between 0 and MAX_INT */
	public static int randInt()
	{
		return gen();
	}

	/** @return a double between 0 - 1.0 */
	public static double randDouble()
	{
		return (gen() * 2.32830644e-10 + 0.5);
	}
	
	public static double getStrengthEstimate(RobotInfo ri) {
		double strengthEstimate;
		switch(ri.type) {
		case SOLDIER:
			strengthEstimate = ri.energon+10;
			break;
		case DISRUPTER:
			strengthEstimate = ri.energon*0.7+10;
			break;
		case SCORCHER:
			strengthEstimate = ri.energon*1.5+10;
			break;
		case SCOUT:
			strengthEstimate = 2;
			break;
		default:
			strengthEstimate = 0;
			break;
		}
		if(ri.flux < 0.15) 
			strengthEstimate *= 0.25;
		if(ri.roundsUntilAttackIdle > 5) 
			strengthEstimate *= 20/(15+ri.roundsUntilAttackIdle);
		return strengthEstimate;
	}
	public static double getOwnStrengthEstimate(RobotController rc) {
		double strengthEstimate;
		switch(rc.getType()) {
		case SOLDIER:
			strengthEstimate = rc.getEnergon()+10;
			break;
		case DISRUPTER:
			strengthEstimate = rc.getEnergon()*0.7+10;
			break;
		case SCORCHER:
			strengthEstimate = rc.getEnergon()*1.5+10;
			break;
		case SCOUT:
			strengthEstimate = 2;
			break;
		default:
			strengthEstimate = 0;
			break;
		}
		if(rc.getFlux() < 0.15) 
			strengthEstimate *= 0.25;
		if(rc.roundsUntilAttackIdle() > 5) 
			strengthEstimate *= 20/(15+rc.roundsUntilAttackIdle());
		return strengthEstimate;
	}
}
