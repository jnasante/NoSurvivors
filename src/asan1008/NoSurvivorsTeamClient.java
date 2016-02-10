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
		
		if (!ship.isAlive()) {
			ship.setCurrentAction(null);
		}
		
		// If we don't have enough fuel, locate nearest fuel source
		if (ship.getEnergy() < 2000 && ship.isAlive()) {
			if (propKnowledge.getNearestBeacon() != null) {
				
				// Going to recharge, release target enemy
				propKnowledge.setCurrentTargetEnemy(null);
				
				// Find nearest enemy within short distance to target beacon
				if (propKnowledge.getDistanceBetweenTargetBeaconAndEnemy() < propKnowledge.SHORT_DISTANCE || 
						propKnowledge.getDistanceToBeacon() > propKnowledge.getDistanceBetweenTargetBeaconAndEnemy()) {
					willShoot = true;
				} else {
					willShoot = false;
				}
				
				if (propKnowledge.getDistanceToBeacon() <= propKnowledge.SHORT_DISTANCE || propKnowledge.getDistanceToBeacon() <= propKnowledge.getDistanceToBase() || propKnowledge.getNearestBase().getEnergy() < 1000) {
					// Beacon is within short distance, or it is closer than the nearest base, or the base doesn't have enough energy to satisfy our hunger
					newAction = fasterMoveToObjectAction(space, propKnowledge.getNearestBeacon());
					//log("Moving toward beacon at: " + propKnowledge.getNearestBeacon().getPosition().getX() + ", " + propKnowledge.getNearestBeacon().getPosition().getY());
					return newAction;
				}
			}
			
			// There is no beacon, or the base is closer and has enough energy
			newAction = fasterMoveToObjectAction(space, propKnowledge.getNearestBase());
			//log("Moving toward base");
			return newAction;
		}
		
		// if the ship has enough resourcesAvailable, take it back to base
		if (ship.getResources().getTotal() > 1000) {
			newAction = fasterMoveToObjectAction(space, propKnowledge.getNearestBase());
			willShoot = false;
			//log("Going toward base, with loot");
			return newAction;
		}
		
		if (propKnowledge.getCurrentTargetEnemy() != null) {
			willShoot = true;
			newAction = fasterMoveToObjectAction(space, propKnowledge.getCurrentTargetEnemy());
			//log("Hunting target: " + propKnowledge.getCurrentTargetEnemy().getTeamName());
			return newAction;
		}
		
		// if we do not already have a current target enemy, decide on a new enemy or asteroid
		if (ship.getCurrentAction().isMovementFinished(space) || ship.getCurrentAction() == null) {
			
			// Both asteroid and enemy don't exist, do nothing
			if (propKnowledge.getNearestAsteroid() == null && propKnowledge.getNearestEnemy() == null) {
				willShoot = false;
				newAction = new DoNothingAction();
				//log("Doing nothing");
				return newAction;
			}
			
			// Asteroid is much more convenient than enemy at this time
			if (propKnowledge.getDistanceToAsteroid() < propKnowledge.SHORT_DISTANCE && propKnowledge.getDistanceToEnemy() > propKnowledge.SHORT_DISTANCE
					//|| propKnowledge.getDistanceToEnemy() - propKnowledge.getDistanceToAsteroid() < 250
					|| propKnowledge.getNearestEnemy() == null) {
				willShoot = false;
				newAction = fasterMoveToObjectAction(space, propKnowledge.getNearestAsteroid());
				//log("Moving toward asteroid. Gonna get me some money.");
				return newAction;
			} 
			
			// Go for the enemy!
			willShoot = propKnowledge.getDistanceToEnemy() <= propKnowledge.LARGE_DISTANCE ? true : false;
			newAction = fasterMoveToObjectAction(space, propKnowledge.getNearestEnemy());
			propKnowledge.setCurrentTargetEnemy(propKnowledge.getNearestEnemy());
			//log("Moving toward new enemy, attempting to annihilate new target: " + propKnowledge.getCurrentTargetEnemy().getTeamName());
			return newAction;
		} 

		// return the current action if we cannot determine a new action
		/*if (ship.getCurrentAction() instanceof MoveToObjectAction) {
			log("Going to old goal object: " + ((MoveToObjectAction)ship.getCurrentAction()).getGoalObject());
		} else {
			log("Performing same old action: " + ship.getCurrentAction());
		}*/
		return ship.getCurrentAction();
	}
	
	private AbstractAction fasterMoveToObjectAction(Toroidal2DPhysics space, AbstractObject goalObject) {
		double MOVEMENT_COMPENSATION_FACTOR = goalObject instanceof Beacon ? 3.0 : 4.0;

		final Vector2D distanceToGoalObject = space.findShortestDistanceVector(propKnowledge.getCurrentPosition(), goalObject.getPosition());
		
		Vector2D distanceToGhostObject = distanceToGoalObject;
		AbstractObject ghostObject = goalObject.deepClone();
		Position ghostPosition = ghostObject.getPosition();

		do {
			MOVEMENT_COMPENSATION_FACTOR -= 0.5;
			
			distanceToGhostObject.setX(distanceToGoalObject.getXValue() * MOVEMENT_COMPENSATION_FACTOR);
			distanceToGhostObject.setY(distanceToGoalObject.getYValue() * MOVEMENT_COMPENSATION_FACTOR);
			
			ghostPosition.setX(goalObject.getPosition().getX() + distanceToGhostObject.getXValue());
			ghostPosition.setY(goalObject.getPosition().getY() + distanceToGhostObject.getYValue());
			
			ghostObject.setPosition(ghostPosition);
			
			if (MOVEMENT_COMPENSATION_FACTOR == 0) {
				return new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), goalObject);
			}
			
		} while (ghostPosition.getX() > space.getWidth() || ghostPosition.getX() < 0 || 
				ghostPosition.getY() > space.getHeight() || ghostPosition.getY() < 0 || 
				space.findShortestDistance(propKnowledge.getCurrentPosition(), ghostPosition) < 
				(MOVEMENT_COMPENSATION_FACTOR+1) * space.findShortestDistance(propKnowledge.getCurrentPosition(), goalObject.getPosition()));
		
		if (goalObject instanceof Ship && space.findShortestDistance(propKnowledge.getCurrentPosition(), goalObject.getPosition()) < propKnowledge.LARGE_DISTANCE) {
			return new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), goalObject);
		}
			
		// If goal object's position is closer than ghost object's position, just target goal object
		return new MoveToObjectAction(space, propKnowledge.getCurrentPosition(), ghostObject);
	}
	
	/**
	 * Helper method for logging messages
	 * 
	 * @param logMessage Message to be logged
	 */
	private void log(String logMessage) {
		System.out.println(logMessage);
	}

	@Override
	public void initialize(Toroidal2DPhysics space) {
		propKnowledge = new PropositionalRepresentation();
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// ...

	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		// ...
		return null;
	}

	@Override
	/**
	 * If we have the resources, buy weapons, energy capacity, bases, healing, emp, in that order
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {

		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		
		// can we upgrade weapons?
		if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_DOUBLE_WEAPON_CAPACITY, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					if (ship.isValidPowerup(PurchaseTypes.POWERUP_DOUBLE_WEAPON_CAPACITY.getPowerupMap())) {
						purchases.put(ship.getId(), PurchaseTypes.POWERUP_DOUBLE_WEAPON_CAPACITY);
					}
				}
			}		
		} 
		
		// can we upgrade max energy?
		if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_DOUBLE_MAX_ENERGY, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					if (ship.isValidPowerup(PurchaseTypes.POWERUP_DOUBLE_MAX_ENERGY.getPowerupMap())) {
						purchases.put(ship.getId(), PurchaseTypes.POWERUP_DOUBLE_MAX_ENERGY);
					}
				}
			}
		}
		
		return purchases;
	}


	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		// ...
		return null;
	}
}
