package ducks;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

/**
 * Handles the distribution of flux amongst units.
 * 
 * @author jven
 */
public class FluxBalanceSystem {
	
	private enum FluxManagerMode {
		BATTERY,
		BATTLE
	}
	
	private BaseRobot br;
	private RobotController rc;
	
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
		try {
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
			return;
		}
	}
	
	private void distributeArchonBattery() throws GameActionException {
		// check all directions around you, ground and air
		for (Direction d : Direction.values()) {
			// ignore none direction
			if (d == Direction.NONE) {
				continue;
			}
			for (RobotLevel level : RobotLevel.values()) {
				// if we don't have flux to give, abort
				if (br.rc.getFlux() < Constants.MIN_ROBOT_FLUX) {
					return;
				}
				// ignore power node level
				if (level == RobotLevel.POWER_NODE) {
					continue;
				}
				// can't give flux to yourself, silly!
				if (d == Direction.OMNI && level == RobotLevel.ON_GROUND) {
					continue;
				}
				GameObject obj = br.dc.getAdjacentGameObject(d, level);
				if (obj instanceof Robot && obj.getTeam() == br.myTeam) {
					// TODO(jven): data cache this?
					RobotInfo rInfo = rc.senseRobotInfo((Robot)obj);
					// don't give flux to towers or archons
					if (rInfo.type == RobotType.TOWER || rInfo.type == RobotType.ARCHON) {
						continue;
					}
					if (rInfo.flux < rInfo.type.maxFlux) {
						double fluxToTransfer = Math.min(rInfo.type.maxFlux - rInfo.flux,
								br.rc.getFlux() - Constants.MIN_ROBOT_FLUX);
						if (fluxToTransfer > 0) {
							rc.transferFlux(
									rInfo.location, rInfo.robot.getRobotLevel(), fluxToTransfer);
						}
					}
				}
			}
		}
	}
	
	private void distributeArchonBattle() throws GameActionException {
		// check all directions around you, ground and air
		for (Direction d : Direction.values()) {
			// ignore none direction
			if (d == Direction.NONE) {
				continue;
			}
			for (RobotLevel level : RobotLevel.values()) {
				// if we don't have flux to give, abort
				if (br.rc.getFlux() < Constants.MIN_ROBOT_FLUX) {
					return;
				}
				// ignore power node level
				if (level == RobotLevel.POWER_NODE) {
					continue;
				}
				// can't give flux to yourself, silly!
				if (d == Direction.OMNI && level == RobotLevel.ON_GROUND) {
					continue;
				}
				GameObject obj = br.dc.getAdjacentGameObject(d, level);
				if (obj instanceof Robot && obj.getTeam() == br.myTeam) {
					// TODO(jven): data cache this?
					RobotInfo rInfo = rc.senseRobotInfo((Robot)obj);
					// don't give flux to towers or archons
					if (rInfo.type == RobotType.TOWER || rInfo.type == RobotType.ARCHON) {
						continue;
					}
					if (rInfo.flux < Constants.MIN_UNIT_BATTLE_FLUX_RATIO *
							rInfo.type.maxFlux) {
						double fluxToTransfer = Math.min(
								Constants.MIN_UNIT_BATTLE_FLUX_RATIO *
								rInfo.type.maxFlux - rInfo.flux,
								br.rc.getFlux() - Constants.MIN_ROBOT_FLUX);
						if (fluxToTransfer > 0) {
							rc.transferFlux(
									rInfo.location, rInfo.robot.getRobotLevel(), fluxToTransfer);
						}
					}
				}
			}
		}
	}
	
	private void distributeUnitBattery() throws GameActionException {
		// don't do anything
		return;
	}
	
	private void distributeUnitBattle() throws GameActionException {
		// check all directions around you, ground and air
		for (Direction d : Direction.values()) {
			// ignore none direction
			if (d == Direction.NONE) {
				continue;
			}
			for (RobotLevel level : RobotLevel.values()) {
				// if we don't have flux to give, abort
				if (br.rc.getFlux() < Constants.MIN_ROBOT_FLUX) {
					return;
				}
				// ignore power node level
				if (level == RobotLevel.POWER_NODE) {
					continue;
				}
				// can't give flux to yourself, silly!
				if (d == Direction.OMNI && level == RobotLevel.ON_GROUND) {
					continue;
				}
				GameObject obj = br.dc.getAdjacentGameObject(d, level);
				if (obj instanceof Robot && obj.getTeam() == br.myTeam) {
					// TODO(jven): data cache this?
					RobotInfo rInfo = rc.senseRobotInfo((Robot)obj);
					// only give flux to archons
					if (rInfo.type != RobotType.ARCHON) {
						continue;
					}
					if (rInfo.flux < rInfo.type.maxFlux) {
						double fluxToTransfer = Math.min(rInfo.type.maxFlux - rInfo.flux,
								br.rc.getFlux() - Constants.MIN_UNIT_BATTLE_FLUX_RATIO *
								br.myType.maxFlux);
						if (fluxToTransfer > 0) {
							rc.transferFlux(
									rInfo.location, rInfo.robot.getRobotLevel(), fluxToTransfer);
						}
					}
				}
			}
		}
	}
}
