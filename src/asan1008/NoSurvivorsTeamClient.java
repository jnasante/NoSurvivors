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
		
		for (AbstractObject actionable : actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;

				AbstractAction action = getAggressiveAction(space, ship);
				actions.put(ship.getId(), action);
				
			} else {
				// it is a base. Do nothing with bases (for now)
				actions.put(actionable.getId(), new DoNothingAction());
			}
		} 
		
		return actions;
	}
	

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	}
	
	
	/**
	 * Gets the action for the asteroid collecting ship (while being aggressive towards the other ships)
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getAggressiveAction(Toroidal2DPhysics space, Ship ship) {
		propKnowledge.updateRepresentation(space, ship);
		
		AbstractAction newAction = null;
				
		/*if (propKnowledge.getCurrentTargetBase() != null) {
			log("Going after target base");
			return ship.getCurrentAction();
		}
		
		if (propKnowledge.getCurrentTargetBeacon() != null) {
			log("Going after target beacon");
			return ship.getCurrentAction();
		}*/
		
		// If we don't have enough fuel, locate nearest fuel source
		if (ship.getEnergy() < 2000) {
			if (propKnowledge.getNearestBeacon() != null) {
				
				// Going to recharge, release target enemy
				propKnowledge.setCurrentTargetEnemy(null);
				
				// Find nearest enemy within short distance to target beacon
				if (propKnowledge.getDistanceBetweenTargetBeaconAndEnemy() < propKnowledge.SHORT_DISTANCE) {
					willShoot = true;
				} else {
					willShoot = false;
				}
				
				if (propKnowledge.getDistanceToBeacon() <= propKnowledge.SHORT_DISTANCE || propKnowledge.getDistanceToBeacon() <= propKnowledge.getDistanceToBase() || propKnowledge.getNearestBase().getEnergy() < 1000) {
					// Beacon is within short distance, or it is closer than the nearest base, or the base doesn't have enough energy to satisfy our hunger
					newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), propKnowledge.getNearestBeacon());
					//newAction = new MoveAction(space, propKnowledge.getCurrentPosition(), propKnowledge.getNearestBeacon().getPosition(), new Vector2D(ship.getPosition().getxVelocity(), ship.getPosition().getyVelocity()));
					propKnowledge.setCurrentTargetBeacon(propKnowledge.getNearestBeacon());
					log("Moving toward beacon");
					return newAction;
				}
			}
			
			// There is no beacon, or the base is closer and has enough energy
			newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), propKnowledge.getNearestBase());
			propKnowledge.setCurrentTargetBase(propKnowledge.getNearestBase());
			log("Moving toward base");
			return newAction;
		} else {
			propKnowledge.setCurrentTargetBeacon(null);
			propKnowledge.setCurrentTargetBase(null); // TODO: fix
		}
		
		if (propKnowledge.getCurrentTargetEnemy() != null) {
			willShoot = true;
			newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), propKnowledge.getCurrentTargetEnemy());
			log("Hunting target: " + propKnowledge.getCurrentTargetEnemy().getTeamName());
			return newAction;
		}

		/*if (propKnowledge.getCurrentTargetAsteroid() != null) {
			willShoot = false;
			newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), propKnowledge.getCurrentTargetAsteroid());
			log("Chasing asteroid with resources: " + propKnowledge.getCurrentTargetAsteroid().getResources() + "  " + propKnowledge.getCurrentTargetAsteroid().isAlive());
			return newAction;
		}*/
		
		// if we do not already have a current target enemy, decide on a new enemy or asteroid
		if (ship.getCurrentAction().isMovementFinished(space) || ship.getCurrentAction() == null) {
			
			// Both asteroid and enemy don't exist, do nothing
			if (propKnowledge.getNearestAsteroid() == null && propKnowledge.getNearestEnemy() == null) {
				willShoot = false;
				newAction = new DoNothingAction();
				System.err.println("Doing nothing");
				return newAction;
			}
			
			// Asteroid is much more convenient than enemy at this time
			if (propKnowledge.getDistanceToAsteroid() < propKnowledge.SHORT_DISTANCE && propKnowledge.getDistanceToEnemy() > propKnowledge.SHORT_DISTANCE
					//|| propKnowledge.getDistanceToEnemy() - propKnowledge.getDistanceToAsteroid() < 250
					|| propKnowledge.getNearestEnemy() == null) {
				willShoot = false;
				newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), propKnowledge.getNearestAsteroid());
				propKnowledge.setCurrentTargetAsteroid(propKnowledge.getNearestAsteroid());
				log("Moving toward asteroid. Gonna get me some money.");
				return newAction;
			} 
			
			// Go for the enemy!
			willShoot = propKnowledge.getDistanceToEnemy() <= propKnowledge.LARGE_DISTANCE ? true : false;
			newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), propKnowledge.getNearestEnemy());
			propKnowledge.setCurrentTargetEnemy(propKnowledge.getNearestEnemy());
			log("Moving toward new enemy, attempting to annihilate new target: " + propKnowledge.getCurrentTargetEnemy().getTeamName());
			return newAction;
		} 
		
		// if the ship has enough resourcesAvailable, take it back to base
		if (ship.getResources().getTotal() > 500) {
			Base base = propKnowledge.getNearestBase();
			newAction = new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), base);
			willShoot = false;
			log("Going toward base, with loot");
			return newAction;
		}

		// return the current if new goals haven't formed
		log("Performing same old action: " + ship.getCurrentAction().toString());
		return ship.getCurrentAction();
	}

	private void log(String logMessage) {
		System.out.println(logMessage);
	}

	@Override
	public void initialize(Toroidal2DPhysics space) {
		propKnowledge = new PropositionalRepresentation();
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

}
