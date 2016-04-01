package asan1008;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
 */
public class NoSurvivorsTeamClient extends spacesettlers.clients.TeamClient {
	PropositionalRepresentation propositionalKnowledge;
	RelationalRepresentation relationalKnowledge;
	HashMap<UUID, ArrayList<SpacewarGraphics>> graphicsToAdd;
	HashMap<UUID, LinkedList<Vertex>> currentPath;
	HashMap<UUID, AbstractObject> currentGoalObject;
	HashMap <UUID, Graph> graphByShip;
	Individual agent;
	Chromosome chromosome;
	UUID asteroidCollectorID;
	String teamName;
	boolean pathClear = false;
	boolean shouldUseAStar = true;
	boolean shouldLearn = true;

	// Powerups
	double weaponsProbability = 1;
	boolean shouldShoot = false;
	private final int GAMES_PER_ROUND = 5;

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
				
				// the first time we initialize, decide which ship is the asteroid collector
				if (asteroidCollectorID == null) {
					asteroidCollectorID = ship.getId();
				}
				
				teamName = ship.getTeamName();

				AbstractAction action = getAggressiveAction(space, ship);
				if(propositionalKnowledge.shouldPlan()) {
					//log("should plan");
					if (currentGoalObject != null) {
						HashSet<AbstractObject> obstructions = new HashSet<AbstractObject>();
						for(AbstractObject object : space.getAllObjects()){
							if(object instanceof Beacon || object.getId() == currentGoalObject.get(ship.getId()).getId() || object.getId() == ship.getId()){
								continue;
							}
							obstructions.add(object);
						}
						
						pathClear = !shouldUseAStar || space.isPathClearOfObstructions(ship.getPosition(), currentGoalObject.get(ship.getId()).getPosition(), obstructions, 10);
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
			//log("But I died");
			ship.setCurrentAction(null);
			return new DoNothingAction();
		}
		
		if (reachedVertex(space, ship)) {
			currentPath.get(ship.getId()).removeLast();
		}

		// If we don't have enough fuel, locate nearest fuel source
		if (ship.getEnergy() < agent.LOW_ENERGY && ship.isAlive()) {
			if (relationalKnowledge.getNearestBeacon(ship) != null) {

				// Going to recharge, release target enemy
				relationalKnowledge.setCurrentTargetEnemy(null, ship);

				// Find nearest enemy within short distance to target beacon
//				if (propositionalKnowledge
//						.getDistanceBetweenTargetBeaconAndEnemy() < propositionalKnowledge.SHORT_DISTANCE
//						|| propositionalKnowledge.getDistanceToBeacon() > propositionalKnowledge
//								.getDistanceBetweenTargetBeaconAndEnemy()) {
//					shouldShoot = true;
//				} else {
					shouldShoot = false;
//				}

				if (propositionalKnowledge.getDistanceToBeacon() <= agent.SHORT_DISTANCE
						|| propositionalKnowledge.getDistanceToBeacon() <= propositionalKnowledge.getDistanceToBase()
						|| relationalKnowledge.getNearestBase(ship).getEnergy() < propositionalKnowledge.LOW_BASE_ENERGY) {
					// Beacon is within short distance, or it is closer than the nearest base,
					// or the base doesn't have enough energy to satisfy our hunger
					newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestBeacon(ship), ship);
//					 log("Moving toward beacon at: " + relationalKnowledge.getNearestBeacon().getPosition().getX()
//					 + ", " + relationalKnowledge.getNearestBeacon().getPosition().getY());
					return newAction;
				}
			}

			// There is no beacon, or the base is closer and has enough energy
			newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestBase(ship), ship);
			//log("Moving toward base");
			return newAction;
		}
		
		// Previous action was to go to base, but we don't need to do that anymore
		if (ship.getCurrentAction() instanceof FasterMoveToObjectAction) {
			if (((FasterMoveToObjectAction)ship.getCurrentAction()).getGoalObject() instanceof Base) {
				ship.setCurrentAction(null);
			}
		}

		// if the ship has enough resourcesAvailable, take it back to base
		if (ship.getResources().getTotal() > agent.HIGH_RESOURCES) {
			newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestBase(ship), ship);
			shouldShoot = false;
//			log("Going toward base, with loot");
			return newAction;
		}
		
		// Asteroid collecting ship
		if (asteroidCollectorID == ship.getId() && propositionalKnowledge.shouldCollectResources(agent.ASTEROID_COLLECTING_TIMESTEP)) {
			for (double radius = propositionalKnowledge.ASTEROID_COLLECTING_RADIUS; radius < space.getHeight(); radius += 100) {
				Asteroid asteroid = relationalKnowledge.findHighestValueAsteroidWithinRadius(space, ship, radius);
				if (asteroid != null) {
					shouldShoot = false;
					newAction = fasterMoveToObjectAction(space, asteroid, ship);
					return newAction;
				}
			}
		}

		// Asteroid is much more convenient than enemy at this time
		if (propositionalKnowledge.getDistanceToAsteroid() < agent.SHORT_DISTANCE
				&& propositionalKnowledge.getDistanceToEnemy() > agent.SHORT_DISTANCE
				&& propositionalKnowledge.shouldCollectResources(agent.ASTEROID_COLLECTING_TIMESTEP) 
				// || propositionalKnowledge.getDistanceToEnemy() -
				// propositionalKnowledge.getDistanceToAsteroid() < 250
				|| relationalKnowledge.getNearestEnemy(ship) == null) {
			shouldShoot = false;
			newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestAsteroid(ship), ship);
			relationalKnowledge.setCurrentTargetAsteroid(relationalKnowledge.getNearestAsteroid(ship), ship);
			relationalKnowledge.setCurrentTargetEnemy(null, ship);
			//log("Moving toward asteroid. Mineable? " + relationalKnowledge.getNearestAsteroid(ship).isMineable());
			return newAction;
		}

		// We have a current target enemy, so keep aiming for that
		if (relationalKnowledge.getCurrentTargetEnemy(ship) != null) {
			shouldShoot = shouldShootAtEnemy(space, ship);
			newAction = fasterMoveToObjectAction(space, relationalKnowledge.getCurrentTargetEnemy(ship), ship);
//			log("Hunting target: " + relationalKnowledge.getCurrentTargetEnemy().getTeamName());
			return newAction;
		}
		
		// We have a current target asteroid, so keep aiming for that
		if (relationalKnowledge.getCurrentTargetAsteroid(ship) != null) {
			shouldShoot = false;
			newAction = fasterMoveToObjectAction(space, relationalKnowledge.getCurrentTargetAsteroid(ship), ship);
//			log("Hunting asteroid);
			return newAction;
		}

		// if we do not already have a current target enemy, decide on a new enemy or asteroid
		if (ship.getCurrentAction() == null || ship.getCurrentAction().isMovementFinished(space)) {

			// If nothing exists, do nothing
			if (relationalKnowledge.getNearestAsteroid(ship) == null && relationalKnowledge.getNearestEnemy(ship) == null) {
				shouldShoot = false;
				
				// If nothing to do, go to nearest beacon to heal
				if (relationalKnowledge.getNearestBeacon(ship) != null) {
					newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestBeacon(ship), ship);
					return newAction;
				}
									
				// Go to base if nothing else, to drop off resources and heal
				if (relationalKnowledge.getNearestBase(ship) != null) {
					newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestBase(ship), ship);
					return newAction;
				}
				
				newAction = new DoNothingAction();
				
				//log("Doing nothing");
				return newAction;
			}

			// Go for the enemy!
			shouldShoot = shouldShootAtEnemy(space, ship);
			newAction = fasterMoveToObjectAction(space, relationalKnowledge.getNearestEnemy(ship), ship);
			relationalKnowledge.setCurrentTargetEnemy(relationalKnowledge.getNearestEnemy(ship), ship);
			//log("Moving toward new enemy, attempting to annihilate new target: " + relationalKnowledge.getCurrentTargetEnemy().getTeamName());
			return newAction;

		}
		
//		 if (ship.getCurrentAction() instanceof FasterMoveToObjectAction) { 
//			 log("Going to old goal object: " + ((FasterMoveToObjectAction)ship.getCurrentAction()).getGoalObject()); 
//		 } else { 
//			 log("Performing same old action: " + ship.getCurrentAction());
//		 }
		 
		// return the current action if we cannot determine a new action
		ship.setCurrentAction(null);
		return new DoNothingAction();
	}
	
	/**
	 * Follow an aStar path to the goal
	 * @param space
	 * @param ship
	 * @param goalPosition
	 * @return
	 */
	private void getAStarPathToGoal(Toroidal2DPhysics space, Ship ship, Position goalPosition) {
		if (!pathClear) {
			Graph graph = AStarSearch.createGraphToGoalWithBeacons(space, ship, goalPosition, new Random(), ship.getEnergy() > propositionalKnowledge.IGNORE_ASTEROIDS_ENERGY);
			currentPath.put(ship.getId(), graph.findAStarPath(space));
		} else {
			if (currentPath.get(ship.getId()) != null) currentPath.get(ship.getId()).clear(); else currentPath.put(ship.getId(), new LinkedList<Vertex>());
			currentPath.get(ship.getId()).add(new Vertex(currentGoalObject.get(ship.getId()).getPosition()));
		}
		
		/* Draw path as planning takes place */
		if (currentPath.get(ship.getId()) != null) graphicsToAdd.put(ship.getId(), drawPath(currentPath.get(ship.getId()), space, ship));
	}
	
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
			graphicsToAdd.get(ship.getId()).add(new StarGraphics(3, Color.CYAN, path.get(0).getPosition()));
			
			while(iterator.hasNext()) {
				Position current = iterator.next().getPosition();
				LineGraphics line = new LineGraphics(prev, current, space.findShortestDistanceVector(prev, current));
				line.setLineColor(Color.CYAN);
				graphicsToAdd.get(ship.getId()).add(line);
				prev = current;
			}
			
			return graphicsToAdd.get(ship.getId());
		}
		
		return new ArrayList<SpacewarGraphics>();
	}

	/** 
	 * Decide whether or not to shoot (true if we are oriented toward the enemy)
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	private boolean shouldShootAtEnemy(Toroidal2DPhysics space, Ship ship) {
		return relationalKnowledge.enemyOnPath(space, ship) && propositionalKnowledge.getDistanceToEnemy() <= agent.SHOOTING_DISTANCE;
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
		currentGoalObject.put(ship.getId(), goalObject);
				
		// The magnitude of our velocity vector. If we are dangerously low on energy, slow down
		double VELOCITY_MAGNITUDE = agent.SPEED_FAST;
		
		if (ship.getEnergy() < agent.LOW_ENERGY) {
			VELOCITY_MAGNITUDE = agent.SPEED_SLOW;
		} else if (!pathClear) {
			VELOCITY_MAGNITUDE = propositionalKnowledge.SPEED_MEDIUM;
		}
				
		// Next node we are targeting on the path
		Position targetPosition = currentPath.get(ship.getId()) != null && !currentPath.get(ship.getId()).isEmpty() && 
				space.findShortestDistance(propositionalKnowledge.getCurrentPosition(), goalObject.getPosition()) > agent.SHORT_DISTANCE ? 
			currentPath.get(ship.getId()).getLast().getPosition() : goalObject.getPosition(); 
		
		// Distance to target position
		Vector2D distance = space.findShortestDistanceVector(propositionalKnowledge.getCurrentPosition(), targetPosition);
		
		// Scale by which to multiply our distance vectors to get the desired velocity magnitude
		double velocityScale = VELOCITY_MAGNITUDE / Math.sqrt(Math.pow(distance.getXValue(), 2) + Math.pow(distance.getYValue(), 2));
		
		// If our target is an enemy ship, and we are within short distance, slow down
		Vector2D targetVelocity;
		
		// If we are within short distance of enemy, slow down and attack!
		if ((relationalKnowledge.getCurrentTargetEnemy(ship) != null && goalObject.getId() == relationalKnowledge.getCurrentTargetEnemy(ship).getId()) && 
				propositionalKnowledge.getDistanceToEnemy() < agent.SHORT_DISTANCE*2) {
			targetPosition = goalObject.getPosition();
			targetVelocity = new Vector2D(0, 0);
		} else {
			targetVelocity = new Vector2D(velocityScale*distance.getXValue(), velocityScale*distance.getYValue());
		}
							
		return new FasterMoveToObjectAction(space, propositionalKnowledge.getCurrentPosition(), goalObject, targetPosition, targetVelocity, relationalKnowledge.getTargetOrientationToEnemy(space, ship));
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
					//log("The people's champion is increasing the size of its army");
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					break;
				}
			}
		}
		
		// can we upgrade weapons?
		if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_DOUBLE_WEAPON_CAPACITY, resourcesAvailable) && !propositionalKnowledge.shouldCollectResources(agent.ASTEROID_COLLECTING_TIMESTEP)) {
			//log("Can afford double weapon capacity");
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					if (!ship.isValidPowerup(PurchaseTypes.POWERUP_DOUBLE_WEAPON_CAPACITY.getPowerupMap())) {
						purchases.put(ship.getId(), PurchaseTypes.POWERUP_DOUBLE_WEAPON_CAPACITY);
						//log("The people's champion is upgrading weapon capacity");
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
	public static void log(String logMessage) {
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
		graphicsToAdd = new HashMap<UUID, ArrayList<SpacewarGraphics>>();
		currentPath = new HashMap<UUID, LinkedList<Vertex>>();
		currentGoalObject = new HashMap<UUID, AbstractObject>();
		graphByShip = new HashMap<UUID, Graph>();
		
		XStream xstream = new XStream();
		xstream.alias("Individual", Individual.class);

		try { 
			agent = (Individual) xstream.fromXML(new File(getKnowledgeFile()));
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
			// TODO Auto-generated catch block
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
//						log("new chromosome " + i + " shooting: " + chromosome.agent.SHOOTING_DISTANCE);
//						log("new chromosome large: " + chromosome.agent.LARGE_DISTANCE);
//						log("new chromosome short: " + chromosome.agent.SHORT_DISTANCE);
//						log("new chromosome fast: " + chromosome.agent.SPEED_FAST);
//						log("new chromosome slow: " + chromosome.agent.SPEED_SLOW);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
			SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.values()[random
					.nextInt(SpaceSettlersPowerupEnum.values().length)];
			if (actionableObject.isValidPowerup(powerup) && random.nextDouble() < weaponsProbability && shouldShoot) {
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
