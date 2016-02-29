package asan1008;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Base;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

/**
 * Aggressive client that leaves no survivors
 * 
 */
public class NoSurvivorsTeamClient extends spacesettlers.clients.TeamClient {
	PropositionalRepresentation propositionalKnowledge;
	RelationalRepresentation relationalKnowledge;
	ArrayList<SpacewarGraphics> graphicsToAdd = new ArrayList<SpacewarGraphics>();
	LinkedList<GridNode> currentPath;
	AbstractObject currentGoalObject;
	Grid grid;
	
	// Powerups
	double weaponsProbability = 1;
	boolean willShoot = false;

	/**
	 * Generates the action that must be executed in this time step based on
	 * current knowledge of the environment
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		for (AbstractObject actionable : actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;

				AbstractAction action = getAggressiveAction(space, ship);
				if(propositionalKnowledge.shouldPlan()) {
					//log("should plan");
					if (currentGoalObject != null) {
						//log("is planning");
						grid = new Grid(space, ship, currentGoalObject, ship.getEnergy() > 500);
						currentPath = grid.getPathToGoal(space);
						if (currentPath != null) graphicsToAdd = grid.drawPath(currentPath, space);
					}
				}
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
	 * Gets the action for the our aggressive ship, setting priorities in the
	 * order: 1) Staying alive 2) Buying weapon or health upgrades 3) Targeting
	 * enemy ships 4) Targeting mineable asteroids
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 * 
	 * @return
	 */
	private AbstractAction getAggressiveAction(Toroidal2DPhysics space, Ship ship) {
		// Update current knowledge of the environment
		relationalKnowledge.updateRepresentation(space, ship);
		propositionalKnowledge.updateRepresentation(relationalKnowledge, space, ship);

		AbstractAction newAction = null;
		//log("Position: " + ship.getPosition().getX() + ", " + ship.getPosition().getY());
		
		//log("X Velocity: " + ship.getPosition().getxVelocity() + "Y Velocity: " + ship.getPosition().getyVelocity());

		if (!ship.isAlive()) {
			log("But I died");
			ship.setCurrentAction(null);
			return new DoNothingAction();
		}
		
		if (grid != null && currentPath != null && !currentPath.isEmpty() && grid.getNodeByObject(ship).getPosition().equals(currentPath.getLast().getPosition())) {
			currentPath.removeLast();
		}

		// If we don't have enough fuel, locate nearest fuel source
		if (ship.getEnergy() < 1500 && ship.isAlive()) {
			if (relationalKnowledge.getNearestBeacon() != null) {

				// Going to recharge, release target enemy
				relationalKnowledge.setCurrentTargetEnemy(null);

				// Find nearest enemy within short distance to target beacon
				if (propositionalKnowledge
						.getDistanceBetweenTargetBeaconAndEnemy() < propositionalKnowledge.SHORT_DISTANCE
						|| propositionalKnowledge.getDistanceToBeacon() > propositionalKnowledge
								.getDistanceBetweenTargetBeaconAndEnemy()) {
					willShoot = true;
				} else {
					willShoot = false;
				}

				if (propositionalKnowledge.getDistanceToBeacon() <= propositionalKnowledge.SHORT_DISTANCE
						|| propositionalKnowledge.getDistanceToBeacon() <= propositionalKnowledge.getDistanceToBase()
						|| relationalKnowledge.getNearestBase().getEnergy() < 1000) {
					// Beacon is within short distance, or it is closer than the nearest base,
					// or the base doesn't have enough energy to satisfy our hunger
					newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestBeacon(), ship);
//					 log("Moving toward beacon at: " + relationalKnowledge.getNearestBeacon().getPosition().getX()
//					 + ", " + relationalKnowledge.getNearestBeacon().getPosition().getY());
					return newAction;
				}
			}

			// There is no beacon, or the base is closer and has enough energy
			newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestBase(), ship);
			log("Moving toward base");
			return newAction;
		}
		
		// Previous action was to go to base, but we don't need to do that anymore
		if (ship.getCurrentAction() instanceof FasterMoveToObjectAction) {
			if (((FasterMoveToObjectAction)ship.getCurrentAction()).getGoalObject() instanceof Base) {
				ship.setCurrentAction(null);
			}
		}

		// if the ship has enough resourcesAvailable, take it back to base
		if (ship.getResources().getTotal() > 1000) {
			newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestBase(), ship);
			willShoot = false;
//			log("Going toward base, with loot");
			return newAction;
		}

		// We have a current target enemy, so keep aiming for that
		if (relationalKnowledge.getCurrentTargetEnemy() != null) {
			willShoot = shouldShootAtEnemy(space, ship);
			newAction = fasterMoveToObjectAction(space, relationalKnowledge.getCurrentTargetEnemy(), ship);
//			log("Hunting target: " + relationalKnowledge.getCurrentTargetEnemy().getTeamName());
			return newAction;
		}

		// if we do not already have a current target enemy, decide on a new enemy or asteroid
		if (ship.getCurrentAction() == null || ship.getCurrentAction().isMovementFinished(space)) {

			// If nothing exists, do nothing
			if (relationalKnowledge.getNearestAsteroid() == null && relationalKnowledge.getNearestEnemy() == null) {
				willShoot = false;
				
				// If nothing to do, go to nearest beacon to heal
				if (relationalKnowledge.getNearestBeacon() != null) {
					newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestBeacon(), ship);
					return newAction;
				}
									
				// Go to base if nothing else, to drop off resources and heal
				if (relationalKnowledge.getNearestBase() != null) {
					newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestBase(), ship);
					return newAction;
				}
				
				newAction = new DoNothingAction();
				
				log("Doing nothing");
				return newAction;
			}

			// Asteroid is much more convenient than enemy at this time
			if (propositionalKnowledge.getDistanceToAsteroid() < propositionalKnowledge.SHORT_DISTANCE
					&& propositionalKnowledge.getDistanceToEnemy() > propositionalKnowledge.SHORT_DISTANCE
					// || propositionalKnowledge.getDistanceToEnemy() -
					// propositionalKnowledge.getDistanceToAsteroid() < 250
					|| relationalKnowledge.getNearestEnemy() == null) {
				willShoot = false;
				newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestAsteroid(), ship);
				log("Moving toward asteroid. Gonna get me some money.");
				return newAction;
			}

			// Go for the enemy!
			willShoot = shouldShootAtEnemy(space, ship);
			newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestEnemy(), ship);
			relationalKnowledge.setCurrentTargetEnemy(relationalKnowledge.getNearestEnemy());
			log("Moving toward new enemy, attempting to annihilate new target: " + relationalKnowledge.getCurrentTargetEnemy().getTeamName());
			return newAction;
		}
		
		 if (ship.getCurrentAction() instanceof FasterMoveToObjectAction) { 
			 log("Going to old goal object: " + ((FasterMoveToObjectAction)ship.getCurrentAction()).getGoalObject()); 
		 } else { 
			 log("Performing same old action: " + ship.getCurrentAction());
		 }
		 
		// return the current action if we cannot determine a new action
		ship.setCurrentAction(null);
		return new DoNothingAction();
	}
	
	private boolean shouldShootAtEnemy(Toroidal2DPhysics space, Ship ship) {
		return relationalKnowledge.enemyOnPath(space, ship) && propositionalKnowledge.getDistanceToEnemy() <= propositionalKnowledge.SHOOTING_DISTANCE;
	}

	/**
	 * Convenience method to move at a higher velocity than other ships. This is
	 * done by essentially multiplying the distance vector by a certain scale,
	 * and aiming for a "ghost object" at the new position, which causes the
	 * ship to move more quickly. Adjustments are made to the scale when new
	 * positions go off the map, which would otherwise cause erratic behavior.
	 * 
	 * 
	 * @param space Current space instance
	 * @param goalObject Object toward which we are moving
	 * 
	 * @return
	 */
	private AbstractAction fasterMoveToObjectAction(Toroidal2DPhysics space, AbstractObject goalObject, Ship ship) {
		currentGoalObject = goalObject;
				
		// The magnitude of our velocity vector. If we are dangerously low on energy, slow down
		final int VELOCITY_MAGNITUDE = ship.getEnergy() > 500 ? 85 : 50;
		
		// Next node we are targeting on the path
		Position targetPosition = currentPath != null && !currentPath.isEmpty() && 
				space.findShortestDistance(propositionalKnowledge.getCurrentPosition(), goalObject.getPosition()) > propositionalKnowledge.SHORT_DISTANCE ? 
						currentPath.getLast().getPosition() : goalObject.getPosition();
		
		// Distance to target position
		Vector2D distance = space.findShortestDistanceVector(propositionalKnowledge.getCurrentPosition(), targetPosition);
		
		// Scale by which to multiply our distance vectors to get the desired velocity magnitude
		double velocityScale = VELOCITY_MAGNITUDE / Math.sqrt(Math.pow(distance.getXValue(), 2) + Math.pow(distance.getYValue(), 2));
		
		// If our target is an enemy ship, and we are within short distance, slow down
		Vector2D targetVelocity;
		
		// If we are within short distance of enemy, slow down and attack!
		if ((relationalKnowledge.getCurrentTargetEnemy() != null && goalObject.getId() == relationalKnowledge.getCurrentTargetEnemy().getId()) && 
				propositionalKnowledge.getDistanceToEnemy() < propositionalKnowledge.SHORT_DISTANCE*2) {
			targetPosition = goalObject.getPosition();
			targetVelocity = new Vector2D(0, 0);
		} else {
			targetVelocity = new Vector2D(velocityScale*distance.getXValue(), velocityScale*distance.getYValue());
		}
							
		return new FasterMoveToObjectAction(space, propositionalKnowledge.getCurrentPosition(), goalObject, targetPosition, targetVelocity);
	}

	@Override
	/**
	 * If we have the resources, upgrade weapons, energy capacity, in that order
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, ResourcePile resourcesAvailable,
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

	/**
	 * Helper method for logging messages
	 * 
	 * @param logMessage Message to be logged
	 */
	private void log(String logMessage) {
		System.out.println(logMessage);
	}

	@Override
	/**
	 * Upon initialization, also initialize objects used for representing knowledge of the world
	 * 
	 * @param space Current space instance
	 */
	public void initialize(Toroidal2DPhysics space) {
		propositionalKnowledge = new PropositionalRepresentation();
		relationalKnowledge = new RelationalRepresentation();
	}

	/**
	 * The No Survivors client shoots if there is an enemy nearby
	 * 
	 * @param space Current space instance
	 * @param actionableObjects Set of actionable objects
	 * 
	 * @return Map of UUID of actionable object to SpaceSettlersPoweropEnum
	 */
	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, SpaceSettlersPowerupEnum> powerUps = new HashMap<UUID, SpaceSettlersPowerupEnum>();

		Random random = new Random();
		for (AbstractActionableObject actionableObject : actionableObjects) {
			SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.values()[random
					.nextInt(SpaceSettlersPowerupEnum.values().length)];
			if (actionableObject.isValidPowerup(powerup) && random.nextDouble() < weaponsProbability && willShoot) {
				powerUps.put(actionableObject.getId(), powerup);
			}
		}

		return powerUps;
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// ...
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
		graphics.addAll(graphicsToAdd);
		return graphics;
	}
}
