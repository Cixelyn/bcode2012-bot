package ducks;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

/** Handles the distribution of flux amongst units. <br>
 * <br>
 * Goals: <br>
 *   - never waste archon flux income (archons sitting at 300 flux). <br>
 * 	 - do not waste too much upkeep swarming large armies across the map. <br>
 *   - dying units waste as little flux as possible. <br>
 */
public class FluxBalanceSystem {
	
	private enum FluxManagerMode {
		/** No flux transfer happens here. This mode is not recommended. */
		NO_TRANSFER,
		/** Archons try to give swarm units their flux. Good for low upkeep. */
		BATTERY,
		/** Units give their flux back to archon so it can build stuff. */
		POOL_ARCHON
	}
	
	private final BaseRobot br;
	private final RobotController rc;
	private final RadarSystem radar;
	
	private FluxManagerMode mode;
	
	public FluxBalanceSystem(BaseRobot myBR) {
		br = myBR;
		rc = myBR.rc;
		radar = br.radar;
		mode = FluxManagerMode.POOL_ARCHON;
	}
	
	/** Set the robot to manage its flux in battery mode. */
	public void setBatteryMode() {
		mode = FluxManagerMode.BATTERY;
	}
	
	/** Set the robot to pool the archons all their excess flux. */
	public void setPoolMode() {
		mode = FluxManagerMode.POOL_ARCHON;
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
		switch (mode) {
		case BATTERY:
			if(br.myType == RobotType.ARCHON) {
				distributeArchonBattery();
			}
			break;
		case POOL_ARCHON:
			if(br.myType == RobotType.ARCHON) {
				distributeArchonPool();
			} else if(br.myType == RobotType.SCOUT) {
				distributeScoutPool();
			} else {
				distributeUnitPool();
			}
			break;
		default:
			break;
		}
	}
	
	private void distributeArchonBattery() throws GameActionException {
		if(rc.getFlux() <= 150) 
			return;
		double fluxToTransfer = rc.getFlux()-0.1;
		for(int n=0; n<radar.numAdjacentAllies && fluxToTransfer>0; n++) {
			RobotInfo ri = radar.adjacentAllies[n];
			if (ri.type == RobotType.TOWER)
				continue;
			
			double canHold = ri.type.maxFlux - ri.flux;
			if (ri.type == RobotType.ARCHON) 
				canHold -= 200;
			if (canHold > 1) {
				double x = Math.min(canHold, fluxToTransfer);
				rc.transferFlux(ri.location, ri.type.level, x);
				fluxToTransfer -= x;
			}
		}
	}
	
	private void distributeArchonPool() throws GameActionException {
		if (rc.getFlux() > 10)
			distributeFluxBattle(rc.getFlux()-0.1); // Save 0.1 flux for messaging
	}
	
	private void distributeScoutPool() throws GameActionException {
		// TODO implement moving to low flux units and giving them flux
		
		double myUpperFluxThreshold = br.curEnergon < br.myMaxEnergon ? 
				br.curEnergon/2 : br.curEnergon*2/3;
			
		distributeFluxBattle(rc.getFlux()-myUpperFluxThreshold);
	}
	
	private void distributeUnitPool() throws GameActionException {
		double myUpperFluxThreshold = br.curEnergon < br.myMaxEnergon ? 
				br.curEnergon/2 : br.curEnergon*2/3;
			
		distributeFluxBattle(rc.getFlux()-myUpperFluxThreshold);
	}
	
	private void distributeFluxBattle(double fluxToTransfer) throws GameActionException {
//		rc.setIndicatorString(0, fluxToTransfer<=0 ? "No flux to distribute" : 
//			"Trying to distribute "+String.format("%.1f", fluxToTransfer)+" flux");
		if(fluxToTransfer<=0) return;
		
		br.radar.scan(true, false);
		
		double amountBelowLowerThreshold = 0;
		for (int n=0; n<radar.numAdjacentAllies; n++) {
			RobotInfo ri = radar.adjacentAllies[n];
			if (ri.type == RobotType.TOWER || ri.type == RobotType.ARCHON)
				continue;
			double lowerFluxThreshold = ri.energon < ri.type.maxEnergon ? 
					ri.energon/4 : ri.energon/3;
			if(ri.flux < lowerFluxThreshold)
				amountBelowLowerThreshold += lowerFluxThreshold - ri.flux;
		}
		// scouts shouldn't transfer to archons or other scouts
		if(br.myType != RobotType.SCOUT && amountBelowLowerThreshold == 0) {
			for (int n=0; n<radar.numAdjacentAllies && fluxToTransfer>0; n++) {
				RobotInfo ri = radar.adjacentAllies[n];
				if (ri.type == RobotType.ARCHON && ri.flux < 280) {
					double x = Math.min(fluxToTransfer, 280-ri.flux);
					rc.transferFlux(ri.location, ri.type.level, x);
					fluxToTransfer -= x;
				}
			}
			for (int n=0; n<radar.numAdjacentAllies && fluxToTransfer>0; n++) {
				RobotInfo ri = radar.adjacentAllies[n];
				if (ri.type == RobotType.SCOUT && ri.flux<50) {
					double x = Math.min(fluxToTransfer, 50-ri.flux);
					rc.transferFlux(ri.location, ri.type.level, x);
					fluxToTransfer -= x;
				}
			}
		} else {
			for (int n=0; n<radar.numAdjacentAllies && fluxToTransfer>0; n++) {
				RobotInfo ri = radar.adjacentAllies[n];
				if (ri.type == RobotType.TOWER || ri.type == RobotType.ARCHON)
					continue;
				double lowerFluxThreshold = ri.energon < ri.type.maxEnergon ? 
						ri.energon/4 : ri.energon/3;
				if(ri.flux < lowerFluxThreshold) {
					double upperFluxThreshold = ri.energon < ri.type.maxEnergon ? 
							ri.energon/2 : ri.energon*2/3;
					double x = Math.min(fluxToTransfer, upperFluxThreshold - ri.flux);
					rc.transferFlux(ri.location, ri.type.level, x);
					if (br.myType == RobotType.SCOUT) {
						br.dbg.println('j', "Giving flux.");
					}
					fluxToTransfer -= x;
				}
			}
		}
	}
}
