package asan1008;

import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

/** 
 * Propositional knowledge representation of the environment
 *
 */
public class PropositionalRepresentation {
	
	// Current given information
	private Position currentPosition;
	private int tick;
	
	// Calculated distances
	private double distanceToEnemy;
	private double distanceToBase;
	private double distanceToBeacon;
	private double distanceToAsteroid;
	private double distanceBetweenTargetBeaconAndEnemy;

	// Constants
	public final int SPEED_NAVIGATION = 40;
	public final int SPEED_BASE_ARRIVAL = 5;
	public final int SPEED_CHEAT_DEATH = Integer.MAX_VALUE;
	public final int PLANNING_FREQUENCY = 20;
	public final int LOW_BASE_ENERGY = 1000;
	public final int CRITICAL_HEALTH = 500;
	public final double MINIMUM_ASTEROID_SEARCH_RADIUS = 200;
	public final double MAXIMUM_ASTEROID_SEARCH_RADIUS = 800;
	public final double ASTEROID_COLLECTION_PROBABILITY_THRESHOLD = 0.3;

	/**
	 *  Update our knowledge about the current environment
	 *  
	 * @param relationalRepresentation Representation of the environment from a relational perspective
	 * @param space Current space instance
	 * @param ship Our ship
	 */
	public void updateRepresentation(RelationalRepresentation relationalRepresentation, Toroidal2DPhysics space, Ship ship) {
		currentPosition = ship.getPosition();
		tick = space.getCurrentTimestep();
		distanceToEnemy = relationalRepresentation.getNearestEnemy(ship) == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(currentPosition, relationalRepresentation.getNearestEnemy(ship).getPosition());
		distanceToBase = relationalRepresentation.getNearestBase(ship) == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(ship.getPosition(), relationalRepresentation.getNearestBase(ship).getPosition());
		distanceToBeacon = relationalRepresentation.getNearestBeacon(ship) == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(ship.getPosition(), relationalRepresentation.getNearestBeacon(ship).getPosition());
		distanceToAsteroid = relationalRepresentation.getNearestAsteroid(ship) == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(ship.getPosition(), relationalRepresentation.getNearestAsteroid(ship).getPosition());
		distanceBetweenTargetBeaconAndEnemy = relationalRepresentation.getNearestEnemy(ship) == null || relationalRepresentation.getNearestBeacon(ship) == null ? 
				Double.POSITIVE_INFINITY : space.findShortestDistance(relationalRepresentation.getNearestEnemy(ship).getPosition(), relationalRepresentation.getNearestBeacon(ship).getPosition());
	}
	
	/**
	 * Getter for currentPosition
	 */
	protected Position getCurrentPosition() {
		return currentPosition;
	}
	
	/**
	 * Getter for distanceToEnemy
	 */
	protected double getDistanceToEnemy() {
		return distanceToEnemy;
	}

	/**
	 * Getter for distanceToBase
	 */
	protected double getDistanceToBase() {
		return distanceToBase;
	}

	/**
	 * Getter for distanceToBeacon
	 */
	protected double getDistanceToBeacon() {
		return distanceToBeacon;
	}
	
	/**
	 * Getter for distanceToAsteroid
	 */
	protected double getDistanceToAsteroid() {
		return distanceToAsteroid;
	}

	/**
	 * Getter for distanceBetweenTargetBeaconAndEnemy
	 */
	protected double getDistanceBetweenTargetBeaconAndEnemy() {
		return distanceBetweenTargetBeaconAndEnemy;
	}
	
	/**
	 * Determine whether or not to plan on the current time step
	 * 
	 * @return
	 */
	protected boolean shouldPlan() {
		return (tick % PLANNING_FREQUENCY == 0) ? true : false;
	}
	
	/**
	 * Determine whether or not we should keep collecting asteroids based on 
	 * the time step set on the agent's chromosome
	 * 
	 * @param ASTEROID_COLLECTING_TIMESTEP
	 * @return
	 */
	protected boolean shouldCollectResources(double ASTEROID_COLLECTING_TIMESTEP) {
		return (tick < ASTEROID_COLLECTING_TIMESTEP) ? true : false;
	}
}
