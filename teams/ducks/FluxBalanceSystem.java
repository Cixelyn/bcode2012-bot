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
	
	private enum FluxManagerMode {
		NO_TRANSFER,
		BATTERY,
		BATTLE
	}
	
	private final BaseRobot br;
	private final RobotController rc;
	private final RadarSystem radar;
	
	private FluxManagerMode mode;
	
	public FluxBalanceSystem(BaseRobot myBR) {
		br = myBR;
		rc = myBR.rc;
		radar = br.radar;
		mode = FluxManagerMode.BATTLE;
	}
	
	/** Non-archon units should be kept filled. */
	public void setBatteryMode() {
		mode = FluxManagerMode.BATTERY;
	}
	
	/** Non-archon units should be kept at very low capacity. */
	public void setBattleMode() {
		mode = FluxManagerMode.BATTLE;
	}

	/** Disables running the flux balance management each turn */
	public void disable() {
		mode = FluxManagerMode.NO_TRANSFER;
	}
	
	
	/**
	 * Distribute flux depending on unit type and flux manager mode.
	 * @throws GameActionException 
	 */
	public void manageFlux() throws GameActionException {
		if(mode == FluxManagerMode.NO_TRANSFER)
			return;
		
		br.radar.scan(true, false);
		
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
						br.rc.getFlux() - 0.47 * br.myType.maxFlux);
				if (fluxToTransfer > 0) {
					rc.transferFlux(ri.location, ri.robot.getRobotLevel(), fluxToTransfer);
				}
			}
		}
	}
}
