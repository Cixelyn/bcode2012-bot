package ducks;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

/**
 * Handles the distribution of flux amongst units.
 * 
 * @author jven
 */
public class FluxBalanceSystem {
	
	private class FluxConstants {
		public static final double BATTLE_FLUX_RATIO = 0.47;
	}
	
	private enum FluxManagerMode {
		BATTERY,
		BATTLE
	}
	
	private BaseRobot br;
	private RobotController rc;
	private RadarSystem radar;
	
	private FluxManagerMode mode;
	
	public FluxBalanceSystem(BaseRobot myBR) {
		br = myBR;
		rc = myBR.rc;
	}
	
	/**
	 * Non-archon units should be kept filled.
	 */
	public void setBatteryMode() {
		mode = FluxManagerMode.BATTERY;
	}
	
	/**
	 * Non-archon units should be kept at very low capacity.
	 */
	public void setBattleMode() {
		mode = FluxManagerMode.BATTLE;
	}
	
	/**
	 * Distribute flux depending on unit type and flux manager mode.
	 */
	public void manageFlux() {
		// we catch GameActionException to avoid trying to manage flux with stale
		// information
		if (radar==null)
			radar = br.radar;
		try {
			radar.scan(true, false);
			
			if (mode == FluxManagerMode.BATTERY &&
					br.myType == RobotType.ARCHON) {
				distributeArchonBattery();
			} else if (mode == FluxManagerMode.BATTERY &&
					br.myType != RobotType.ARCHON) {
				distributeUnitBattery();
			} else if (mode == FluxManagerMode.BATTLE &&
					br.myType == RobotType.ARCHON) {
				distributeArchonBattle();
			} else if (mode == FluxManagerMode.BATTLE &&
					br.myType != RobotType.ARCHON) {
				distributeUnitBattle();
			}
		} catch (GameActionException e) {
			e.printStackTrace();
			rc.addMatchObservation(e.toString());
			return;
		}
	}
	
	private void distributeArchonBattery() throws GameActionException {
		
		if (br.rc.getFlux() < Constants.MIN_ROBOT_FLUX) {
			return;
		}
		
		for (int x=0; x<radar.numAdjacentAllies; x++)
		{
			RobotInfo ri = radar.adjacentAllies[x];
			
			//TODO we might want to give flux to archons
			if (ri.type == RobotType.TOWER || ri.type == RobotType.ARCHON) {
				continue;
			}
			
			//TODO in the future, perhaps don't use greedy method
			if (ri.flux < ri.type.maxFlux) {
				double fluxToTransfer = Math.min(ri.type.maxFlux - ri.flux,
						br.rc.getFlux() - Constants.MIN_ROBOT_FLUX);
				if (fluxToTransfer > 0) {
					rc.transferFlux(
							ri.location, ri.robot.getRobotLevel(), fluxToTransfer);
				}
			}
		}
	}
	
	private void distributeArchonBattle() throws GameActionException {
		
		if (br.rc.getFlux() < Constants.MIN_ROBOT_FLUX) {
			return;
		}
		
		for (int x=0; x<radar.numAdjacentAllies; x++)
		{
			RobotInfo ri = radar.adjacentAllies[x];
			
			//TODO we might want to give flux to archons
			if (ri.type == RobotType.TOWER || ri.type == RobotType.ARCHON) {
				continue;
			}
			
			//TODO in the future, perhaps don't use greedy method
			//TODO may want to consider energon of units
			if (ri.flux < Constants.MIN_UNIT_BATTLE_FLUX_RATIO *
					ri.type.maxFlux) {
				double fluxToTransfer = Math.min(
						Constants.MIN_UNIT_BATTLE_FLUX_RATIO *
						ri.type.maxFlux - ri.flux,
						br.rc.getFlux() - Constants.MIN_ROBOT_FLUX);
				if (fluxToTransfer > 0) {
					rc.transferFlux(
							ri.location, ri.robot.getRobotLevel(), fluxToTransfer);
				}
			}
		}
	}
	
	private void distributeUnitBattery() throws GameActionException {
		// don't do anything
		return;
	}
	
	private void distributeUnitBattle() throws GameActionException {
		
		if (br.rc.getFlux() < Constants.MIN_ROBOT_FLUX) {
			return;
		}
		
		for (int x=0; x<radar.numAdjacentAllies; x++)
		{
			RobotInfo ri = radar.adjacentAllies[x];
			
			//TODO we might want to give flux to archons
			if (ri.type == RobotType.TOWER || ri.type == RobotType.ARCHON) {
				continue;
			}
			
			//TODO in the future, perhaps don't use greedy method
			//TODO may want to consider energon of units
			if (ri.flux < ri.type.maxFlux) {
				double fluxToTransfer = Math.min(ri.type.maxFlux - ri.flux,
						br.rc.getFlux() - FluxConstants.BATTLE_FLUX_RATIO *
						br.myType.maxFlux);
				if (fluxToTransfer > 0) {
					rc.transferFlux(
							ri.location, ri.robot.getRobotLevel(), fluxToTransfer);
				}
			}
		}
	}
}
