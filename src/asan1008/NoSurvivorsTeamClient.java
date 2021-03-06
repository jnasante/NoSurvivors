package asan1008;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.ImmutableTeamInfo;
import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.graphics.StarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.AbstractWeapon;
import spacesettlers.objects.weapons.Missile;
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
	HashMap<UUID, ArrayList<SpacewarGraphics>> graphicsToAdd;
	HashMap<UUID, LinkedList<Vertex>> currentPath;
	HashMap<UUID, LinkedList<Asteroid>> asteroidPlan;
	HashMap<UUID, AbstractObject> currentGoalObject;
	HashMap<UUID, Position> interceptPosition;
	HashMap <UUID, Graph> graphByShip;
	HashMap<UUID, Boolean> shipDied;
	ResourceDelivery resourceDelivery;
	Individual agent;
	Chromosome chromosome;
	HashSet<UUID> asteroidCollectorIDs;
	String teamName;
	boolean pathClear = false;
	boolean shouldUseAStar = true;
	
	// Set to true only when we are performing our two learning strategies
	boolean shouldLearn = false;
	boolean shouldSaveResourceCollectionData = false;

	// Powerups
	double weaponsProbability = 1;
	HashMap<UUID, Boolean> shouldShoot;
	private final int GAMES_PER_ROUND = 2;

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
				
//				if (space.getCurrentTimestep() == 19998) {
//					log("Ship stats: " + ship.getDamageInflicted() + " - " + ship.getDamageReceived());
//				}
				
				// the first time we initialize, decide which ship is the asteroid collector
				if (asteroidCollectorIDs.size() < 1 && ship.getTeamName().equalsIgnoreCase("NoSurvivorsTeamClient")) {
					if( !asteroidCollectorIDs.contains(ship.getId())) {
						asteroidCollectorIDs.add(ship.getId());
						//if (ship.getTeamName().equalsIgnoreCase("NoSurvivorsTeamClient")) log("Asteroid collector id: " + ship.getId());
					}
				}
				
				// Maintain a hashmap of deaths (to handle multiple time steps of ship.isAlive == false
				if (shipDied.get(ship.getId()) == null) {
					shipDied.put(ship.getId(), new Boolean(false));
				}
				
				// Maintain hashmap for shouldShoot for each ship
				if (shouldShoot.get(ship.getId()) == null) {
					shouldShoot.put(ship.getId(), new Boolean(false));
				}
				
				teamName = ship.getTeamName();
				
				AbstractAction action = getNextAction(space, ship);
				
				if(propositionalKnowledge.shouldPlan()) {
					//log("should plan");
					if (currentGoalObject != null) {
						getAStarPathToGoal(space, ship, currentGoalObject.get(ship.getId()).getPosition());
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
	 * Get action for our ships
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getNextAction(Toroidal2DPhysics space, Ship ship) {
		// Update current knowledge of the environment
		relationalKnowledge.updateRepresentation(space, ship);
		propositionalKnowledge.updateRepresentation(relationalKnowledge, space, ship);

		// Reset hashmaps and actions based on ship death
		if (!ship.isAlive()) {
			//log("But I died");
			if (shouldTrackResourceDeliveries(ship, space.getCurrentTimestep()) && 
					(!shipDied.get(ship.getId()).booleanValue())) {
				writeResourceDeliveriesToCsv(ship.getTeamName().equals("agent1"), 0);
				shipDied.put(ship.getId(), new Boolean(true));
			}
			
			ship.setCurrentAction(null);
			
		} else if (ship.getResources().getTotal() == 0 && ship.getEnergy() > agent.LOW_ENERGY &&
				ship.getCurrentAction() instanceof FasterMoveToObjectAction && 
				((FasterMoveToObjectAction)ship.getCurrentAction()).goalObject instanceof Base) {
			// We deposited resources at base
			if (shouldTrackResourceDeliveries(ship, space.getCurrentTimestep())) {
				writeResourceDeliveriesToCsv(ship.getTeamName().equals("agent1"), 1);
			}
		}
		
		// Ship is alive, reset shipDied map
		if (shipDied.get(ship.getId()).booleanValue()) {
			shipDied.put(ship.getId(), new Boolean(false));
		}
		
		// Update the current map if we have reached a vertex
		if (reachedVertex(space, ship)) {
			currentPath.get(ship.getId()).removeLast();
		}

		// Previous action was to go to base, but we don't need to do that anymore
		if (ship.getCurrentAction() instanceof FasterMoveToObjectAction) {
			if (((FasterMoveToObjectAction)ship.getCurrentAction()).getGoalObject() instanceof Base) {
				ship.setCurrentAction(null);
			}
		}

		if (isAsteroidCollector(ship)) {
			// Asteroid collecting ship
			return getThatPaperAction(space, ship);
		} else {
			// Attacking ship
			return bangBangAction(space, ship);
		}
	}
	
	/**
	 * Action for asteroid collectors
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getThatPaperAction(Toroidal2DPhysics space, Ship ship) {
		AbstractAction newAction = null;

		// if the ship is holding enough resources, take it back to base
		if (ship.getResources().getTotal() > agent.HIGH_RESOURCES) {
			newAction = goHome(space, ship);
			if (newAction != null) return newAction;
		}
		
		// If we don't have enough fuel, locate nearest fuel source
		if (ship.getEnergy() < agent.LOW_ENERGY && ship.isAlive()) {
			newAction = goHeal(space, ship);
			if (newAction != null) return newAction;
		}
		
		// Go mining (new or target asteroid)
		planMining(space, ship);
		newAction = goMining(space, ship);
		if (newAction != null) return newAction;

		// If can't find a nearby asteroid, just go home
		newAction = goHome(space, ship);
		if (newAction != null) return newAction;
		
		return new DoNothingAction();
	}

	/**
	 * Gets the action for the our aggressive ships, setting priorities in the
	 * order: 1) Buying weapon or health upgrades 3) Targeting enemy ships
	 * 
	 * @param space Current space instance
	 * @param ship Current ship
	 * 
	 * @return
	 */
	private AbstractAction bangBangAction(Toroidal2DPhysics space, Ship ship) {
		AbstractAction newAction = null;
		
		// if the ship is holding enough resources (for some reason), take it back to base
		if (ship.getResources().getTotal() > 3*agent.HIGH_RESOURCES) {
			newAction = goHome(space, ship);
			if (newAction != null) return newAction;
		}

		// Hunt enemy (current target or new enemy)
		newAction = huntEnemy(space, ship);
		if (newAction != null) return newAction;
		
		// Nothing exists, just go home
		newAction = goHome(space, ship);
		if (newAction != null) return newAction;
		
		// Do nothing if we cannot determine a new action
		ship.setCurrentAction(null);
		return new DoNothingAction();
	}
	
	/**
	 * Plan out asteroid collection sequence
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	public void planMining(Toroidal2DPhysics space, Ship ship) {
		if(asteroidPlan.get(ship.getId()) == null || asteroidPlan.get(ship.getId()).isEmpty()) {
			double radius;
			List<Asteroid> asteroids;
			
			radius = propositionalKnowledge.MINIMUM_ASTEROID_SEARCH_RADIUS;
			
			do {
				asteroids = relationalKnowledge.findAsteroidsWithinRadius(space, ship, radius);
				radius += 100;
			} while( sumAsteroids(asteroids) < agent.HIGH_RESOURCES - ship.getResources().getTotal() && radius < propositionalKnowledge.MAXIMUM_ASTEROID_SEARCH_RADIUS);

			asteroids = QuickSort.quickSort(asteroids, 0, asteroids.size()-1);			
			asteroidPlan.put(ship.getId(), getAsteroidPlan(space, ship, asteroids));
		}
	}
	
	/**
	 * Plan out asteroid collection sequence
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	public LinkedList<Asteroid> getAsteroidPlan(Toroidal2DPhysics space, Ship ship, List<Asteroid> asteroids) {
		LinkedList<Asteroid> chosenAsteroids = new LinkedList<>();
		double resouces = 0.0;
		for( Asteroid asteroid : asteroids ) {
			if( !chosenAsteroids.contains(asteroid) ) {
				// Add asteroids in sequence until goal condition has been met
				chosenAsteroids.add(asteroid);
				// Sum resources for the sequence so far
				resouces += asteroid.getResources().getTotal();
				/*
				 * Check that the precondition of the goal state (returning to the home base) has been met
				 * Then return the sequence of asteroids that leads to that the goal state
				 */
				if(resouces >= agent.HIGH_RESOURCES){
					return chosenAsteroids;
				}
			}
		}
		
		// Return null if there is no sequence of actions that lead to the goal state
		return chosenAsteroids;
	}
	
	
	/**
	 * Sum the total resources of a set of asteroids
	 * 
	 * @param asteroids
	 * @return
	 */
	private double sumAsteroids(List<Asteroid> asteroids) {
		double sum = 0.0;
		for( Asteroid asteroid : asteroids){ 
			sum += asteroid.getResources().getTotal();
		}
		return sum;
	}
	
	/**
	 * Go for asteroid
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction goMining(Toroidal2DPhysics space, Ship ship) {
		setShouldShoot(ship, false);
		
		if (relationalKnowledge.getCurrentTargetAsteroid(ship) != null) {
			return fasterMoveToObjectAction(space, relationalKnowledge.getCurrentTargetAsteroid(ship), ship);
		} else {
			Asteroid targetAsteroid = null;
			do {
				if (asteroidPlan.get(ship.getId()) == null || asteroidPlan.get(ship.getId()).isEmpty()) {
					break;
				}
				
				targetAsteroid = asteroidPlan.get(ship.getId()).removeFirst();
			} while ( (targetAsteroid == null || !targetAsteroid.isAlive()) && asteroidPlan.get(ship.getId()) != null && !asteroidPlan.get(ship.getId()).isEmpty() );
			
			if (targetAsteroid != null && targetAsteroid.isAlive()) {
				return fasterMoveToObjectAction(space, targetAsteroid, ship);
			}
		}
		
		return goHome(space, ship);
	}
	
	/**
	 * Target an enemy and go after them
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction huntEnemy(Toroidal2DPhysics space, Ship ship) {
		setShouldShoot(ship, shouldShootAtEnemy(space, ship));
		
		// Go after current target, if we have one
		if (relationalKnowledge.getCurrentTargetEnemy(ship) != null) {
			return fasterMoveToObjectAction(space, relationalKnowledge.getCurrentTargetEnemy(ship), ship);
		}
		
		// Go for new target (if one exists)
		if (relationalKnowledge.getNearestEnemy(ship) != null) {
			relationalKnowledge.setCurrentTargetEnemy(relationalKnowledge.getNearestEnemy(ship), ship);
			return fasterMoveToObjectAction(space, relationalKnowledge.getNearestEnemy(ship), ship);
		}
		
		return null;
	}
	
	/**
	 * Decide whether or not to go to beacon or base
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction goHeal(Toroidal2DPhysics space, Ship ship) {
		// Going to recharge, release target enemy
		releaseTargetEnemy(ship);

		if (relationalKnowledge.getNearestBeacon(ship) != null) {
			if (isBeaconMoreConvenientThanBase(ship)) {
				return getBeacon(space, ship);
			}
		}

		// There is no beacon, or the base is closer and has enough energy
		return goHome(space, ship);
	}
	
	/**
	 * Go to nearest beacon (and set it as target so we don't deviate unless it dies or we die)
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction getBeacon(Toroidal2DPhysics space, Ship ship) {
		setShouldShoot(ship, false);
		if (relationalKnowledge.getNearestBeacon(ship) != null) {
			relationalKnowledge.setCurrentTargetBeacon(relationalKnowledge.getNearestBeacon(ship), ship);
			return fasterMoveToObjectAction(space, relationalKnowledge.getNearestBeacon(ship), ship);
		}
		
		return null;
	}
	
	/**
	 * Go to base
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private AbstractAction goHome(Toroidal2DPhysics space, Ship ship) {
		setShouldShoot(ship, false);
		
		if (relationalKnowledge.getNearestBase(ship) != null) {
			return fasterMoveToObjectAction(space, relationalKnowledge.getNearestBase(ship), ship);
		}
		
		// Can't find base
		return null;
	}
	
	/**
	 * Calculate if given position is in free space (with a buffer or ship's radius around it)
	 * 
	 * @param pos
	 * @param space
	 * @return
	 */
	public static boolean positionIsInFreeSpace(Toroidal2DPhysics space, Position position) {
		// loop through the obstacles
		for (AbstractObject object : space.getAllObjects()) {
			double dist = space.findShortestDistance(position, object.getPosition());
			
			if (object instanceof Asteroid && ((Asteroid)object).isMineable()) {
				continue;
			}

			if (dist < (object.getRadius() + Ship.SHIP_RADIUS)) {
				return false;
			}
		}

		return true;
	}
	
	/**
	 * Generate a random free point (where ship can be...uh..."put down")
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private Position digOwnGrave(Toroidal2DPhysics space, Ship ship) {
		Random random = new Random();
		Position position = null;
		for (int dist = 1000; dist >= 0; dist -= 50) {
			for (int v = 0; v < 20; v++) {
				double newX = random.nextFloat() * dist;
				double newY = random.nextFloat() * dist;
								
				position = new Position(newX % space.getWidth(), newY % space.getHeight());
				
				if (positionIsInFreeSpace(space, position) && AStarSearch.isFreeLine(ship.getPosition(), position, space)) {
					return position;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Let go of a target enemy
	 * 
	 * @param ship
	 */
	private void releaseTargetEnemy(Ship ship) {
		relationalKnowledge.setCurrentTargetEnemy(null, ship);
	}
	
	/**
	 * Check if a ship is an asteroid collector currently
	 * 
	 * @param ship
	 * @return
	 */
	private boolean isAsteroidCollector(Ship ship) {
		return asteroidCollectorIDs.contains(ship.getId()) && propositionalKnowledge.shouldCollectResources(agent.ASTEROID_COLLECTING_TIMESTEP);
	}
	
	/**
	 * Check if we should track resource collection results for learning
	 * 
	 * @param ship
	 * @param timeStep
	 * @return
	 */
	private boolean shouldTrackResourceDeliveries(Ship ship, int timeStep) {
		if ((ship.getTeamName().equals("agent1") || ship.getTeamName().equals("agent2")) && isAsteroidCollector(ship) && 
				propositionalKnowledge.shouldCollectResources(agent.ASTEROID_COLLECTING_TIMESTEP) && shouldSaveResourceCollectionData) {
				return true;			
		}
		
		return false;
	}
	
	/**
	 * Check if beacon would be more convenient that going to base
	 * 
	 * @param ship
	 * @return
	 */
	private boolean isBeaconMoreConvenientThanBase(Ship ship) {
		// If beacon is within short distance, or it is closer than the nearest base,
		// or the base doesn't have enough energy to satisfy our burning hunger
		return propositionalKnowledge.getDistanceToBeacon() <= agent.SHORT_DISTANCE
				|| propositionalKnowledge.getDistanceToBeacon() <= propositionalKnowledge.getDistanceToBase()
				|| relationalKnowledge.getNearestBase(ship).getEnergy() < propositionalKnowledge.LOW_BASE_ENERGY;
	}
	
	/**
	 * Follow an aStar path to the goal
	 * 
	 * @param space
	 * @param ship
	 * @param goalPosition
	 * @return
	 */
	private void getAStarPathToGoal(Toroidal2DPhysics space, Ship ship, Position goalPosition) {
		// If path is clear of obstructions, don't use A*
		pathClear = !shouldUseAStar || space.isPathClearOfObstructions(ship.getPosition(), interceptPosition.get(ship.getId()), getObstructions(space, ship), 10);

		if (!pathClear) {
			Graph graph = AStarSearch.createGraphToGoalWithBeacons(space, ship, goalPosition, new Random());
			currentPath.put(ship.getId(), graph.findAStarPath(space));
		} else {
			if (currentPath.get(ship.getId()) != null) {
				 currentPath.get(ship.getId()).clear();
			} else {
				currentPath.put(ship.getId(), new LinkedList<Vertex>());
			}
			
			currentPath.get(ship.getId()).add(new Vertex(interceptPosition.get(ship.getId())));
		}
		
		/* Draw path as planning takes place */
		if (currentPath.get(ship.getId()) != null) graphicsToAdd.put(ship.getId(), drawPath(currentPath.get(ship.getId()), space, ship));
	}
	
	/**
	 * Get all objects we consider obstructions
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private Set<AbstractObject> getObstructions(Toroidal2DPhysics space, Ship ship) {
		Set<AbstractObject> obstructions = new HashSet<AbstractObject>();
		
		// don't add an asteroid if it is the goal, or if it is mineable and we want to hit them
		for (Asteroid asteroid : space.getAsteroids()) {
			if (asteroid.isMineable() || asteroid.getId() == currentGoalObject.get(ship.getId()).getId()) {
				continue;
			}
			
			obstructions.add(asteroid);
		}
		
		// Avoid all ships except current ship
		for (Ship otherShip : space.getShips()) {
			if (otherShip.getId() == ship.getId() || otherShip.getId() == currentGoalObject.get(ship.getId()).getId()) {
				continue;
			}
			
			obstructions.add(otherShip);
		}
		
		// Avoid all bases
		for (Base base : space.getBases()) {
			if (base.getId() == currentGoalObject.get(ship.getId()).getId()) {
				continue;
			}

			obstructions.add(base);
		}
		
		return obstructions;
	}
	
	/**
	 * Check if a ship has reached a vertex
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private boolean reachedVertex(Toroidal2DPhysics space, Ship ship) {
		return 
				currentPath.get(ship.getId()) != null && 
				!currentPath.get(ship.getId()).isEmpty() && 
				space.findShortestDistance(ship.getPosition(), currentPath.get(ship.getId()).getLast().getPosition()) < agent.SHORT_DISTANCE;
	}

	/**
	 * Draw path created from A* search (for debugging in simulator)
	 * 
	 * @param path
	 * @param space
	 * @return
	 */
	public ArrayList<SpacewarGraphics> drawPath(LinkedList<Vertex> path, Toroidal2DPhysics space, Ship ship) {
		Iterator<Vertex> iterator = path.iterator();
		Position prev = iterator.next().getPosition();
		
		if (graphicsToAdd.get(ship.getId()) != null) {
			graphicsToAdd.get(ship.getId()).clear();
			graphicsToAdd.get(ship.getId()).add(new StarGraphics(3, ship.getTeamColor(), path.get(0).getPosition()));
			
			while(iterator.hasNext()) {
				Position current = iterator.next().getPosition();
				LineGraphics line = new LineGraphics(prev, current, space.findShortestDistanceVector(prev, current));
				line.setLineColor(ship.getTeamColor());
				graphicsToAdd.get(ship.getId()).add(line);
				prev = current;
			}
			
			return graphicsToAdd.get(ship.getId());
		}
		
		return new ArrayList<SpacewarGraphics>();
	}

	/**
	 * Setter for shouldShoot for each ship
	 * 
	 * @param ship
	 * @param shoot
	 */
	private void setShouldShoot(Ship ship, boolean shoot) {
		shouldShoot.put(ship.getId(), new Boolean(shoot));
	}

	/** 
	 * Decide whether or not to shoot (true if we are oriented toward the enemy)
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private boolean shouldShootAtEnemy(Toroidal2DPhysics space, Ship ship) {
		Position enemyActualPosition = relationalKnowledge.getCurrentTargetEnemy(ship) != null ? relationalKnowledge.getCurrentTargetEnemy(ship).getPosition() : null;
		Position enemyInterceptPosition = enemyActualPosition != null ? interceptPosition.get(ship.getId()) : null;
		return relationalKnowledge.enemyOnPath(space, ship, enemyInterceptPosition) 
					&& propositionalKnowledge.getDistanceToEnemy() <= agent.SHOOTING_DISTANCE;
	}
	
	/** 
	 * TODO: documentation
	 * 
	 * @param space
	 * @return
	 */
	private Position getInterceptPosition(Toroidal2DPhysics space, Position goalPosition, Ship ship, double velocity) {
		if (!teamName.equalsIgnoreCase("NoSurvivorsTeamClient") || !isAsteroidCollector(ship)) return goalPosition;
		
		double goalVelocityX = goalPosition.getTranslationalVelocityX();
		double goalVelocityY = goalPosition.getTranslationalVelocityY();
		if(goalVelocityX == 0 && goalVelocityY == 0) {
			return goalPosition;
		}
		double goalSpeed = Math.sqrt(Math.pow(goalVelocityX, 2) + Math.pow(goalVelocityY, 2));
		Vector2D relativePosition = space.findShortestDistanceVector(ship.getPosition(), goalPosition);
		double postitionNorm = Math.sqrt(Math.pow(relativePosition.getXValue(), 2) + Math.pow(relativePosition.getYValue(), 2));
		Vector2D normalizedRelative = new Vector2D(relativePosition.getXValue()/postitionNorm, relativePosition.getYValue()/postitionNorm);
		Vector2D normalizedVelocity = new Vector2D(goalVelocityX/goalSpeed, goalVelocityY/goalSpeed);
		double cosTheta = normalizedRelative.getXValue()*normalizedVelocity.getXValue() + normalizedRelative.getYValue()*normalizedVelocity.getYValue();
		double distance = space.findShortestDistance(goalPosition, ship.getPosition());
		double a = Math.pow(velocity, 2) - Math.pow(goalSpeed, 2);
		double b = 2 * goalSpeed * distance * cosTheta;
		double c = -Math.pow(distance, 2);
		double quadraticTemp = Math.sqrt(Math.pow(b, 2) - 4*a*c);
		double testSign = (-b + quadraticTemp)/(2*a);
		double timeToIntercept = ( testSign > 0 ) ? testSign : (-b - quadraticTemp)/(2*a);
		double x = (goalPosition.getX() + timeToIntercept*goalVelocityX) % space.getWidth();
		double y = (goalPosition.getY() + timeToIntercept*goalVelocityY) % space.getHeight();
		
//		if (Double.isNaN(x) || Double.isNaN(y)) {
//			log("Avoided wormhole!");
//			log("\tgoalPositionX: " + goalPosition.getX() + "\n\tgoalPositionY: " + goalPosition.getY() + 
//					"\n\tshipPositionX: " + shipPosition.getX() + "\n\tshipPositionY: " + shipPosition.getY() + 
//					"\n\trelativePositionX: " + 
//					relativePosition.getXValue() + "\n\trelativePositionY: " + relativePosition.getYValue() + "\n\ta: " + a + "\n\tb: " + b + 
//					"\n\tc: " + c + "\n\tquadraticTemp: " + quadraticTemp + "\n\ttestSign: " + testSign + 
//					"\n\ttimeToIntercept: " + timeToIntercept + "\n\tx: " + x + "\n\ty: " + y);
//			return goalPosition;
//		}
		
		return new Position(x, y);
	}

	/**
	 * Convenience method to move at a higher velocity than other ships.
	 * 
	 * @param space Current space instance
	 * @param goalObject Object toward which we are moving
	 * 
	 * @return
	 */
	private AbstractAction fasterMoveToObjectAction(Toroidal2DPhysics space, AbstractObject goalObject, Ship ship) {
		currentGoalObject.put(ship.getId(), goalObject);

		// Calculate target intercept position
		if (propositionalKnowledge.shouldRecalculateIntercept() || interceptPosition.get(ship.getId()) == null) {
			double targetInterceptVelocity = Missile.INITIAL_VELOCITY;
			if (isAsteroidCollector(ship)) {
				targetInterceptVelocity = ship.getEnergy() < agent.LOW_ENERGY ? agent.SPEED_SLOW : agent.SPEED_FAST;
			}
			
			interceptPosition.put(ship.getId(), getInterceptPosition(space, goalObject.getPosition(), ship, targetInterceptVelocity));
		}
		
		// The magnitude of our velocity vector. If we are dangerously low on energy, slow down
		double VELOCITY_MAGNITUDE = agent.SPEED_FAST;
		
		if (ship.getEnergy() < agent.LOW_ENERGY) {
			VELOCITY_MAGNITUDE = agent.SPEED_SLOW;
		}
		
		if (goalObject instanceof Base && space.findShortestDistance(ship.getPosition(), goalObject.getPosition()) < agent.SHORT_DISTANCE) {  
			VELOCITY_MAGNITUDE = propositionalKnowledge.SPEED_BASE_ARRIVAL;
		} 
		
		if (!pathClear) {
			VELOCITY_MAGNITUDE = propositionalKnowledge.SPEED_NAVIGATION;
		}
				
		// Next node we are targeting on the path
		Position targetPosition = currentPath.get(ship.getId()) != null && !currentPath.get(ship.getId()).isEmpty() && 
				space.findShortestDistance(propositionalKnowledge.getCurrentPosition(), interceptPosition.get(ship.getId())) > agent.SHORT_DISTANCE ? 
			currentPath.get(ship.getId()).getLast().getPosition() : interceptPosition.get(ship.getId());
		
		// Distance to target position
		Vector2D distance = space.findShortestDistanceVector(propositionalKnowledge.getCurrentPosition(), targetPosition);
		
		Vector2D targetVelocity;
		
		// If we are within shooting distance of enemy, slow down and attack!
		if ((relationalKnowledge.getCurrentTargetEnemy(ship) != null && goalObject.getId() == relationalKnowledge.getCurrentTargetEnemy(ship).getId()) && 
				propositionalKnowledge.getDistanceToEnemy() < agent.SHORT_DISTANCE) {
			targetPosition = goalObject.getPosition();
			targetVelocity = new Vector2D(0, 0);
		} else {
			if (ship.getEnergy() < propositionalKnowledge.CRITICAL_HEALTH) {
				// Set insane (deadly) speed
				VELOCITY_MAGNITUDE = propositionalKnowledge.SPEED_CHEAT_DEATH;
				
				// Generate random point, path to which is free
				Position grave = digOwnGrave(space, ship);
				if (grave != null) {
					targetPosition = grave;
				}
			}
			
			// Scale by which to multiply our distance vectors to get the desired velocity magnitude
			double velocityScale = VELOCITY_MAGNITUDE / Math.sqrt(Math.pow(distance.getXValue(), 2) + Math.pow(distance.getYValue(), 2));
			targetVelocity = new Vector2D(velocityScale*distance.getXValue(), velocityScale*distance.getYValue());
		}
		
		// Asteroid coordination
		if (goalObject instanceof Asteroid) {
			resourceDelivery.setValues(ship.getEnergy(), 
					space.findShortestDistance(ship.getPosition(), interceptPosition.get(ship.getId())), 
					space.findShortestDistance(interceptPosition.get(ship.getId()), relationalKnowledge.getNearestBase(ship).getPosition()), 
					space.findShortestDistance(ship.getPosition(), relationalKnowledge.getNearestBase(ship).getPosition()));
		}
		
		return new FasterMoveToObjectAction(space, propositionalKnowledge.getCurrentPosition(), 
				goalObject, targetPosition, targetVelocity, 
				relationalKnowledge.getTargetOrientationToEnemy(space, ship, targetPosition));
	}
	
	/**
	 * Helper method for logging messages
	 * 
	 * @param logMessage Message to be logged
	 */
	public static void log(String logMessage) {
		System.out.println(logMessage);
	}
	
	/**
	 * Writing results from attempt at collecting asteroid for learning later
	 * 
	 * @param test
	 * @param success
	 */
	public void writeResourceDeliveriesToCsv(Boolean test, int success) {
		resourceDelivery.setSuccess(success);
		writeResourceDeliveriesToCsv(test);
	}
	
	/**
	 * Writing results from attempt at collecting asteroid for learning later
	 * 
	 * @param test
	 * @param success
	 */
	public void writeResourceDeliveriesToCsv(boolean test) {
		try {
			FileWriter writer = new FileWriter(test ? "asan1008/resource_delivery_test.csv" : "asan1008/resource_delivery_training.csv", true);
						
			writer.append(String.valueOf(resourceDelivery.getEnergy()));
		    writer.append(',');
			writer.append(String.valueOf(resourceDelivery.getShipToAsteroid()));
		    writer.append(',');
			writer.append(String.valueOf(resourceDelivery.getAsteroidToBase()));
		    writer.append(',');
			writer.append(String.valueOf(resourceDelivery.getShipToBase()));
		    writer.append(',');
			writer.append(String.valueOf(resourceDelivery.getSuccess()));
		    writer.append('\n');
			
							
		    writer.flush();
		    writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		graphicsToAdd = new HashMap<UUID, ArrayList<SpacewarGraphics>>();
		currentPath = new HashMap<UUID, LinkedList<Vertex>>();
		asteroidPlan = new HashMap<>();
		currentGoalObject = new HashMap<UUID, AbstractObject>();
		interceptPosition = new HashMap<UUID, Position>();
		graphByShip = new HashMap<UUID, Graph>();
		shipDied = new HashMap<UUID, Boolean>();
		shouldShoot = new HashMap<UUID, Boolean>();
		resourceDelivery = new ResourceDelivery();
		asteroidCollectorIDs = new HashSet<>();
		
		XStream xstream = new XStream();
		xstream.alias("Individual", Individual.class);

		try { 
			agent = (Individual) xstream.fromXML(new File(shouldLearn ? getKnowledgeFile() : "asan1008/og.xml"));
			chromosome = new Chromosome(agent);
		} catch (XStreamException e) {
			// if you get an error, handle it other than a null pointer because
			// the error will happen the first time you run
			agent = new Individual();
		}
	}
	
	/**
	 * Demonstrates saving out to the xstream file
	 * You can save out other ways too.  This is a human-readable way to examine
	 * the knowledge you have learned.
	 */
	@Override
	public void shutDown(Toroidal2DPhysics space) {
		if (!shouldLearn) { 
			return; 
		}
		
		if(shouldLearn){
			Iterator<ImmutableTeamInfo> iterator = space.getTeamInfo().iterator();
			while (iterator.hasNext()) {
				ImmutableTeamInfo teamInfo = iterator.next();
				if (teamInfo.getTeamName() == teamName) {
					chromosome.calculateFitness(teamInfo.getScore());
					break;
				}
			}
		}
		
		XStream xstream = new XStream();
		xstream.alias("Individual", Individual.class);

		try { 
			// if you want to compress the file, change FileOuputStream to a GZIPOutputStream
			agent.READY = true;
			xstream.toXML(agent, new FileOutputStream(new File(getKnowledgeFile())));
		} catch (XStreamException e) {
			// if you get an error, handle it somehow as it means your knowledge didn't save
			// the error will happen the first time you run
			agent = new Individual();
		} catch (FileNotFoundException e) {
			agent = new Individual();
		}
		
		try {
			//log("trying to evolve");
			ArrayList<File> files = new ArrayList<>();
			ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
			for (int i = 1; i <= Population.SIZE; i++) {
				files.add(new File("asan1008/chromosome" + i + ".xml"));
			}

			for( File f : files) {
				chromosomes.add(new Chromosome((Individual) xstream.fromXML(f)));
			}
			
			boolean shouldEvolve = true;
			for( Chromosome chromosome : chromosomes ){
				if( !chromosome.agent.READY ) {
					//log("not all files written out");
					shouldEvolve = false;
				}
			}
			
			if(shouldEvolve) {
				xstream.alias("Game", Game.class);
				Game game = (Game) xstream.fromXML(new File("asan1008/game_stats.xml"));
				if( game.GAME_NUMBER % GAMES_PER_ROUND == 0) {
					Population population = new Population(chromosomes);
					population.performRankSelection(space);
					Chromosome parentChromosome = population.performCrossover();
					List<Chromosome> newPop = population.performMutations(parentChromosome);
					int i = 1;
					log("Setting new chromosome values");
					for( Chromosome chromosome : newPop) {
						// set up the xml files for the next set of games
						chromosome.agent.READY = false;
						chromosome.agent.FITNESS = 0.0;
						xstream.toXML( chromosome.agent, new FileOutputStream(new File("asan1008/chromosome" + i + ".xml")));
						i++;
					}
				} else {
					int i = 1;
					for( Chromosome chromosome : chromosomes) {
						// set up the xml files for the next set of games
						chromosome.agent.READY = false;
						xstream.toXML( chromosome.agent, new FileOutputStream(new File("asan1008/chromosome" + i + ".xml")));
						i++;
					}
				}
				
				game.GAME_NUMBER++;
				xstream.toXML(game, new FileOutputStream(new File("asan1008/game_stats.xml")));		
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	/**
	 * If we have the resources, upgrade weapons, energy capacity, in that order
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, ResourcePile resourcesAvailable,
			PurchaseCosts purchaseCosts) {
		
		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		
		// can we buy another ship?
		if (purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;
					if (!base.getTeamName().equalsIgnoreCase("NoSurvivorsTeamClient")) break;
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					//log(space.getCurrentTimestep() + "\t" + base.getTeamName() + " is increasing the size of its army");
					break;
				}
			}
		}
		
		// Don't buy anything else until we're done collecting asteroids
		if (!propositionalKnowledge.shouldCollectResources(agent.ASTEROID_COLLECTING_TIMESTEP)) {
			// can we upgrade weapon capacity?
			if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_DOUBLE_WEAPON_CAPACITY, resourcesAvailable)) {
				for (AbstractActionableObject actionableObject : actionableObjects) {
					if (actionableObject instanceof Ship) {
						Ship ship = (Ship) actionableObject;
						if (!ship.isValidPowerup(PurchaseTypes.POWERUP_DOUBLE_WEAPON_CAPACITY.getPowerupMap())) {
							purchases.put(ship.getId(), PurchaseTypes.POWERUP_DOUBLE_WEAPON_CAPACITY);
							//log(space.getCurrentTimestep() + "\t" + ship.getTeamName() + " is upgrading weapon capacity for ship: " + ship.getId());
						}
					}
				}
			}

			// can we buy EMP launcher?
			if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_EMP_LAUNCHER, resourcesAvailable)) {
				for (AbstractActionableObject actionableObject : actionableObjects) {
					if (actionableObject instanceof Ship) {
						Ship ship = (Ship) actionableObject;
						if (!ship.isValidPowerup(PurchaseTypes.POWERUP_EMP_LAUNCHER.getPowerupMap())) {
							purchases.put(ship.getId(), PurchaseTypes.POWERUP_EMP_LAUNCHER);
							//log(space.getCurrentTimestep() + "\t" + ship.getTeamName() + " is buying an EMP launcher");
						}
					}
				}		
			}
		}
		
		return purchases;
	}

	/**
	 * The No Survivors client shoots if there is an enemy nearby
	 * 
	 * @param space Current space instance
	 * @param actionableObjects Set of actionable objects
	 * 
	 * @return Map of UUID of actionable object to SpaceSettlersPowerupEnum
	 */
	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, SpaceSettlersPowerupEnum> powerUps = new HashMap<UUID, SpaceSettlersPowerupEnum>();

		Random random = new Random();
		for (AbstractActionableObject actionableObject : actionableObjects) {
			SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.values()[random.nextInt(SpaceSettlersPowerupEnum.values().length)];
			if (actionableObject.isValidPowerup(powerup) && shouldShoot.get(actionableObject.getId())) {
				powerUps.put(actionableObject.getId(), powerup);
			}
		}
		
		return powerUps;
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
		for( ArrayList<SpacewarGraphics> draw : graphicsToAdd.values()){
			graphics.addAll(draw);
		}
		return graphics;
	}
}
