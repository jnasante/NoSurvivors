package asan1008;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.activation.UnsupportedDataTypeException;

import org.omg.CORBA.INTERNAL;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveAction;
import spacesettlers.actions.MoveToObjectAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

/**
 * Aggressive client that leaves no survivors
 * 
 * 
 */
public class NoSurvivorsTeamClient extends spacesettlers.clients.TeamClient {
	PropositionalRepresentation propKnowledge;
	UUID asteroidCollectorID;
	double weaponsProbability = 1;
	boolean willShoot = false;

	/**
	 * Assigns ships to asteroids and beacons, as described above
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();
		
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;

				AbstractAction action = getAggressiveAction(space, ship);
				actions.put(ship.getId(), action);
				
			} else {
				// it is a base.  Heuristically decide when to use the shield (TODO)
				actions.put(actionable.getId(), new DoNothingAction());
			}
		} 
		return actions;
	}
	
	/**
	 * Gets the action for the asteroid collecting ship (while being aggressive towards the other ships)
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getAggressiveAction(Toroidal2DPhysics space, Ship ship) {
		propKnowledge.updateRepresentation(space, ship);
		log(String.valueOf(ship.getPosition().getOrientation()));
		log(String.valueOf(ship.getPosition().getxVelocity()) + ", " + String.valueOf(ship.getPosition().getyVelocity()));
		
		AbstractAction newAction = null;
		
		// Once we are 
		if (propKnowledge.getCurrentAction() instanceof MoveToObjectAction) {
			if (((MoveToObjectAction)propKnowledge.getCurrentAction()).getGoalObject() instanceof Beacon) {
				// Find nearest enemy
				Ship enemy = propKnowledge.findNearestEnemy(space, ship);
			}
		}

		// If we don't have enough fuel, locate nearest fuel source
		if (ship.getEnergy() < 2000) {
			// Find nearest beacon
			Beacon beacon = propKnowledge.findNearestBeacon(space, ship);
			
			// Find nearest base
			Base base = propKnowledge.findNearestBase(space, ship);

			if (beacon != null) {
				// Find nearest enemy to target beacon
				Ship enemy = propKnowledge.findNearestEnemyToBeacon(space, ship, beacon);
				
				if (enemy != null && space.findShortestDistance(enemy.getPosition(), beacon.getPosition()) < propKnowledge.SHORT_DISTANCE) {
					willShoot = true;
					log("Will shoot = true");
				} else {
					willShoot = false;
					log("Will shoot = false");
				}
				
				if (propKnowledge.getDistanceToBeacon() <= propKnowledge.SHORT_DISTANCE || propKnowledge.getDistanceToBeacon() <= propKnowledge.getDistanceToBeacon() || base.getEnergy() < 1000) {
					// Beacon is within short distance, so go to that
					//newAction = new MoveAction(space, currentPosition, base.getPosition(), new Vector2D(beacon.getPosition())); 
					newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), beacon);
					log("Moving toward beacon");
					return newAction;
				}
			}
			
			// There is no beacon, or the base is closer and has enough energy
			newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), base);
			log("Moving toward base");
			return newAction;
		}
		
		// otherwise either for an asteroid or an enemy ship (depending on who is closer and what we need)
		if (propKnowledge.getCurrentAction() == null || propKnowledge.getCurrentAction().isMovementFinished(space)) {

			// see if there is an enemy ship nearby
			Ship enemy = propKnowledge.findNearestEnemy(space, ship);
			
			// find the highest valued nearby asteroid
			Asteroid asteroid = propKnowledge.pickHighestValueFreeAsteroid(space, ship);

			// if there is no enemy nearby, go for an asteroid
			if (enemy == null) {
				willShoot = false;
				
				if (asteroid != null) {
					newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), asteroid);
					log("Moving toward asteroid, not shooting");
					return newAction;
				} else {
					// no enemy and no asteroid, just skip this turn (shouldn't happen often)
					log("No enemy, no asteroid. I'm just gonna sit here.");
					newAction = new DoNothingAction();
				}
			}
			
			// now decide which one to aim for
			double enemyDistance = space.findShortestDistance(ship.getPosition(), enemy.getPosition());
						
			if (asteroid == null || enemyDistance - space.findShortestDistance(ship.getPosition(), asteroid.getPosition()) < 100) {
				willShoot = enemyDistance <= propKnowledge.LARGE_DISTANCE ? true : false;
				newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), enemy);
				log("Moving toward enemy, attempting to annihilate them");
			} else {
				willShoot = false;
				newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), asteroid);
				log("Moving toward asteroid. Gonna get me some money.");
			}
						
			return newAction;
		}
		
		// if the ship has enough resourcesAvailable, take it back to base
		if (ship.getResources().getTotal() > 500) {
			Base base = propKnowledge.findNearestBase(space, ship);
			newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), base);
			willShoot = false;
			log("Going toward base, not shooting");
			return newAction;
		}

		// return the current if new goals haven't formed
		return ship.getCurrentAction();
	}

	private void log(String logMessage) {
		System.out.println(logMessage);
	}

	@Override
	public void initialize(Toroidal2DPhysics space) {
		propKnowledge = new PropositionalRepresentation(space);
		asteroidCollectorID = null;
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	/**
	 * If there is enough resourcesAvailable, buy a base.  Place it by finding a ship that is sufficiently
	 * far away from the existing bases
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {

		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		double BASE_BUYING_DISTANCE = 200;
		boolean bought_base = false;

		if (purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					Set<Base> bases = space.getBases();

					// how far away is this ship to a base of my team?
					double maxDistance = Double.MIN_VALUE;
					for (Base base : bases) {
						if (base.getTeamName().equalsIgnoreCase(getTeamName())) {
							double distance = space.findShortestDistance(ship.getPosition(), base.getPosition());
							if (distance > maxDistance) {
								maxDistance = distance;
							}
						}
					}

					if (maxDistance > BASE_BUYING_DISTANCE) {
						purchases.put(ship.getId(), PurchaseTypes.BASE);
						bought_base = true;
						//System.out.println("Buying a base!!");
						break;
					}
				}
			}		
		} 
		
		// see if you can buy EMPs
		if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_EMP_LAUNCHER, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					
					if (!ship.getId().equals(asteroidCollectorID) && ship.isValidPowerup(PurchaseTypes.POWERUP_EMP_LAUNCHER.getPowerupMap())) {
						purchases.put(ship.getId(), PurchaseTypes.POWERUP_EMP_LAUNCHER);
					}
				}
			}		
		} 
		

		// can I buy a ship?
		if (purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable) && bought_base == false) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;
					
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					break;
				}

			}

		}


		return purchases;
	}

	/**
	 * The aggressive asteroid collector shoots if there is an enemy nearby! 
	 * 
	 * @param space
	 * @param actionableObjects
	 * @return
	 */
	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, SpaceSettlersPowerupEnum> powerUps = new HashMap<UUID, SpaceSettlersPowerupEnum>();

		Random random = new Random();
		for (AbstractActionableObject actionableObject : actionableObjects){
			SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.values()[random.nextInt(SpaceSettlersPowerupEnum.values().length)];
			if (actionableObject.isValidPowerup(powerup) && random.nextDouble() < weaponsProbability && willShoot){
				powerUps.put(actionableObject.getId(), powerup);
			}
		}
		
		
		return powerUps;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		// TODO Auto-generated method stub
		
	}

}
