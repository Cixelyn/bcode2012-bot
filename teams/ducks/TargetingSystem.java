package ducks;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

/**
 * abstract class representing a targeting system which selects the best unit to attack
 * TODO how this would work for robots w/o attack power or ones not requiring a target
 * @author YP
 *
 */
public abstract class TargetingSystem
{
	BaseRobot br;
	RobotController rc;
	RadarSystem radar;
	
	public TargetingSystem(BaseRobot br) {
		this.br = br;
		rc = br.rc;
		radar = br.radar;
	}
	
	/**
	 * Scans and then returns the best target to attack
	 */
	public abstract RobotInfo getBestTarget();
}
