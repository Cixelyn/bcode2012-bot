package normalai;

import battlecode.common.GameConstants;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class TargetingSystemSoldier extends TargetingSystem {

	public TargetingSystemSoldier(BaseRobot br) {
		super(br);
	}

	public static final double ATTACK = RobotType.SOLDIER.attackPower;
	RobotInfo lastTarget;
	
	@Override
	public RobotInfo getBestTarget() {
		
		RobotInfo backupTarget = null;
		RobotInfo bestTarget = null;
		boolean isKill = false;
		radar.scan(false, true);
		
		if (lastTarget!=null)
		{
			if (rc.canSenseObject(lastTarget.robot))
			{
				lastTarget = radar.enemyInfos[lastTarget.robot.getID()];
				if (rc.canAttackSquare(lastTarget.location))
				{
					bestTarget = lastTarget;
					isKill = lastTarget.energon <= ATTACK;
					if (!isKill)
					{
						lastTarget.robot.getID();
					} else
					{
					}
				}
			} else
			{
				lastTarget = null;
			}
		}
		
		if (radar.numEnemyRobots == 0) return null;
		
		bestTarget = radar.closestEnemy;
		bestTarget.robot.getID();
		
		
//		if (radar.numEnemyRobots>0) return radar.closestEnemy;
		
		for (int x=0; x<radar.numEnemyRobots; x++)
		{
			RobotInfo ri = radar.enemyInfos[radar.enemyRobots[x]];
			if (!rc.canAttackSquare(ri.location)) continue;
			if (backupTarget == null) backupTarget = ri;
			if (ri.flux < GameConstants.UNIT_UPKEEP) continue;
			
			if (ri.energon < ATTACK)
			{
				bestTarget = ri;
				isKill = true;
			}
			
//			if (bestTarget == null)
//			{
//				bestTarget = ri;
//				isKill = ri.energon <= ATTACK;
//				if (!isKill)
//				{
//					energon = ri.energon;
//					robotid = ri.robot.getID();
//					maxflux = ri.flux;
//				} else
//				{
////					robotid = ri.robot.getID();
//					maxflux = ri.flux;
//				}
//			} else if (bestTarget.type == RobotType.ARCHON)
//			{
//				if (ri.type != RobotType.ARCHON) continue;
//				if (isKill)
//				{
//					if (ri.energon > ATTACK) continue;
//					if (maxflux < ri.flux) {
//						maxflux = ri.flux;
////						robotid = ri.robot.getID();
//						bestTarget = ri;
//					}
//				} else if (ri.energon < energon)
//				{
//					if (ri.energon < ATTACK)
//					{
//						bestTarget = ri;
//						isKill = true;
////						robotid = ri.robot.getID();
//						maxflux = ri.flux;
//					} else
//					{
//						bestTarget = ri;
//						energon = ri.energon;
//						robotid = ri.robot.getID();
////						maxflux = ri.flux;
//						
//					}
//				} else if (ri.energon == energon)
//				{
////					if (robotid > br.myID)
////					{
////						
////					} else 
//						
//					if (ri.robot.getID() < robotid)
//					{
//						bestTarget = ri;
//						energon = ri.energon;
//						robotid = ri.robot.getID();
////						maxflux = ri.flux;
//					}
//				}
//			} else if (isKill)
//			{
//				if (ri.energon > ATTACK) continue;
//				if (maxflux < ri.flux) {
//					maxflux = ri.flux;
////					robotid = ri.robot.getID();
//					bestTarget = ri;
//				}
//			} else if (ri.energon < energon)
//			{
//				if (ri.energon < ATTACK)
//				{
//					bestTarget = ri;
//					isKill = true;
////					robotid = ri.robot.getID();
//					maxflux = ri.flux;
//				} else
//				{
//					bestTarget = ri;
//					energon = ri.energon;
//					robotid = ri.robot.getID();
////					maxflux = ri.flux;
//					
//				}
//			} else if (ri.energon == energon)
//			{
////				if (robotid > br.myID)
////				{
////					
////				} else 
//					
//				if (ri.robot.getID() < robotid)
//				{
//					bestTarget = ri;
//					energon = ri.energon;
//					robotid = ri.robot.getID();
////					maxflux = ri.flux;
//				}
//			}
		}
		
		lastTarget = bestTarget;
		{
//			lastTarget = bestTarget;
			return bestTarget;
		}
	}

}
