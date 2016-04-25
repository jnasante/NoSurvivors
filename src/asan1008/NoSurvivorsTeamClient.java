package asan1008;

import java.awt.Color;
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
				if (asteroidCollectorIDs.size() < 2 ) {
					if( !asteroidCollectorIDs.contains(ship.getId())){
						asteroidCollectorIDs.add(ship.getId());
						log("Asteroid collector id: " + ship.getId());
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
			
			return new DoNothingAction();
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

		if (asteroidCollectorIDs.contains(ship.getId()) && propositionalKnowledge.shouldCollectResources(agent.ASTEROID_COLLECTING_TIMESTEP)) {
			// Asteroid collecting ship
			return getThatPaperAction(space, ship);
		} else {
			// Attacking ship
			return bangBangAction(space, ship);
		}
	}
	
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

		// TODO: Refactor this
		for (double radius = propositionalKnowledge.MINIMUM_ASTEROID_SEARCH_RADIUS; radius <= propositionalKnowledge.MAXIMUM_ASTEROID_SEARCH_RADIUS; radius += 100) {
			Asteroid asteroid = relationalKnowledge.findHighestValueAsteroidWithinRadius(space, ship, radius);
			if (asteroid != null && relationalKnowledge.getCurrentTargetAsteroid(ship) == null) {
				if (resourceDelivery.predictSurvivalProbability(ship.getEnergy(),
						space.findShortestDistance(ship.getPosition(), asteroid.getPosition()),
						space.findShortestDistance(asteroid.getPosition(), relationalKnowledge.getNearestBase(ship).getPosition()),
						space.findShortestDistance(ship.getPosition(), relationalKnowledge.getNearestBase(ship).getPosition())) > propositionalKnowledge.ASTEROID_COLLECTION_PROBABILITY_THRESHOLD) {

					// We will probably survive the trip if we go for another asteroid.
					setShouldShoot(ship, false);
					relationalKnowledge.setCurrentTargetAsteroid(asteroid, ship);
					newAction = fasterMoveToObjectAction(space, asteroid, ship);
					return newAction;
				} else {
					// We probably won't survive going for another asteroid. Go back to base to deposit what we have and heal.
					newAction = goHome(space, ship);
					if (newAction != null) return newAction;
				}
			} else {
				if(relationalKnowledge.getCurrentTargetAsteroid(ship) != null) {
					newAction = fasterMoveToObjectAction(space, relationalKnowledge.getCurrentTargetAsteroid(ship), ship);
					return newAction;
				}
			}
		}
		
		// If can't find a nearby asteroid, just go home
		newAction = goHome(space, ship);
		if (newAction != null) return newAction;
		
		return new DoNothingAction();
	}

	/**
	 * Gets the action for the our aggressive ship, setting priorities in the
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
		if (ship.getResources().getTotal() > agent.HIGH_RESOURCES) {
			newAction = goHome(space, ship);
			if (newAction != null) return newAction;
		}

		// Hunt enemy (current target or new enemy)
		newAction = huntEnemy(space, ship);
		if (newAction != null) return newAction;
		
		// Nothing exists, just go home
		newAction = goHome(space, ship);
		if (newAction != null) return newAction;

		// If for some reason we can't go home, go mining!
		// This is the only time an aggressive ship goes for an asteroid.
		// TODO: perhaps this should be above going home if we are near asteroids?
		newAction = goMiningNearby(space, ship);
		if (newAction != null) return newAction;
		
		// Do nothing if we cannot determine a new action
		ship.setCurrentAction(null);
		return new DoNothingAction();
	}
	
	private AbstractAction goMiningNearby(Toroidal2DPhysics space, Ship ship) {
		setShouldShoot(ship, false);
				
		// Go after current target asteroid, if we have one
		if (relationalKnowledge.getCurrentTargetAsteroid(ship) != null) {
			return fasterMoveToObjectAction(space, relationalKnowledge.getCurrentTargetAsteroid(ship), ship);
		}
		
		// Go for new asteroid (if one exists)
		if (relationalKnowledge.getNearestAsteroid(ship) != null) {
			return fasterMoveToObjectAction(space, relationalKnowledge.getNearestAsteroid(ship), ship);
		}

		return null;
	}
	
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
	
	private AbstractAction goToBeacon(Toroidal2DPhysics space, Ship ship) {
		setShouldShoot(ship, false);
		if (relationalKnowledge.getNearestBeacon(ship) != null) {
			return fasterMoveToObjectAction(space, relationalKnowledge.getNearestBeacon(ship), ship);
		}
		
		return null;
	}
	
	private AbstractAction goHeal(Toroidal2DPhysics space, Ship ship) {
		// Going to recharge, release target enemy
		releaseTargetEnemy(ship);

		if (relationalKnowledge.getNearestBeacon(ship) != null) {
			if (isBeaconMoreConvenientThanBase(ship)) {
				return goToBeacon(space, ship);
			}
		}

		// There is no beacon, or the base is closer and has enough energy
		return goHome(space, ship);
	}
	
	private AbstractAction goHome(Toroidal2DPhysics space, Ship ship) {
		setShouldShoot(ship, false);
		return fasterMoveToObjectAction(space, relationalKnowledge.getNearestBase(ship), ship);
	}
	
	private void releaseTargetEnemy(Ship ship) {
		relationalKnowledge.setCurrentTargetEnemy(null, ship);
	}
	
	private boolean shouldTrackResourceDeliveries(Ship ship, int timeStep) {
		if ((ship.getTeamName().equals("agent1") || ship.getTeamName().equals("agent2")) && asteroidCollectorIDs.contains(ship.getId()) && 
				propositionalKnowledge.shouldCollectResources(agent.ASTEROID_COLLECTING_TIMESTEP) && 
				timeStep < 5000 && shouldSaveResourceCollectionData) {
				return true;			
		}
		
		return false;
	}
	
	private boolean isBeaconMoreConvenientThanBase(Ship ship) {
		// If beacon is within short distance, or it is closer than the nearest base,
		// or the base doesn't have enough energy to satisfy our burning hunger
		return propositionalKnowledge.getDistanceToBeacon() <= agent.SHORT_DISTANCE
				|| propositionalKnowledge.getDistanceToBeacon() <= propositionalKnowledge.getDistanceToBase()
				|| relationalKnowledge.getNearestBase(ship).getEnergy() < propositionalKnowledge.LOW_BASE_ENERGY;
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
			Graph graph = AStarSearch.createGraphToGoalWithBeacons(space, ship, goalPosition, new Random());
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
		
		// If we are within shooting distance of enemy, slow down and attack!
		if ((relationalKnowledge.getCurrentTargetEnemy(ship) != null && goalObject.getId() == relationalKnowledge.getCurrentTargetEnemy(ship).getId()) && 
				propositionalKnowledge.getDistanceToEnemy() < agent.SHOOTING_DISTANCE-10) {
			targetPosition = goalObject.getPosition();
			targetVelocity = new Vector2D(0, 0);
		} else {
			targetVelocity = new Vector2D(velocityScale*distance.getXValue(), velocityScale*distance.getYValue());
		}
		
		if (goalObject instanceof Asteroid) {
			resourceDelivery.setValues(ship.getEnergy(), 
					space.findShortestDistance(ship.getPosition(), goalObject.getPosition()), 
					space.findShortestDistance(goalObject.getPosition(), relationalKnowledge.getNearestBase(ship).getPosition()), 
					space.findShortestDistance(ship.getPosition(), relationalKnowledge.getNearestBase(ship).getPosition()));
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
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					//log("The people's champion is increasing the size of its army");
					break;
				}
			}
		}
		
		// can we upgrade weapons?
		if (purchaseCosts.canAfford(PurchaseTypes.POWERUP_DOUBLE_WEAPON_CAPACITY, resourcesAvailable) && !propositionalKnowledge.shouldCollectResources(agent.ASTEROID_COLLECTING_TIMESTEP)) {
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
	
	public void writeResourceDeliveriesToCsv(Boolean test, int success) {
		resourceDelivery.setSuccess(success);
		writeResourceDeliveriesToCsv(test);
	}
	
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
		currentGoalObject = new HashMap<UUID, AbstractObject>();
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
			if (!(actionableObject instanceof Ship)) {
				continue;
			}
			
			SpaceSettlersPowerupEnum powerup = SpaceSettlersPowerupEnum.values()[random
					.nextInt(SpaceSettlersPowerupEnum.values().length)];
			if (actionableObject.isValidPowerup(powerup) && random.nextDouble() < weaponsProbability && shouldShoot.get(actionableObject.getId()).booleanValue()) {
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
