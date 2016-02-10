package asan1008;

import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class PropositionalRepresentation {
	
	// Current given information
	private Position currentPosition;
	private double currentEnergy;
	
	// Calculated distances
	private double distanceToEnemy;
	private double distanceToBase;
	private double distanceToBeacon;
	private double distanceToAsteroid;
	private double distanceBetweenTargetBeaconAndEnemy;

	// Static constants
	public final int LARGE_DISTANCE = 400;
	public final int SHORT_DISTANCE = 60;

	/**
	 *  Update our knowledge about the current environment
	 *  
	 * @param relationalRepresentation Representation of the environment from a relational perspective
	 * @param space Current space instance
	 * @param ship Our ship
	 */
	public void updateRepresentation(RelationalRepresentation relationalRepresentation, Toroidal2DPhysics space, Ship ship) {
		currentPosition = ship.getPosition();
		currentEnergy = ship.getEnergy();

		distanceToEnemy = relationalRepresentation.getNearestEnemy() == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(currentPosition, relationalRepresentation.getNearestEnemy().getPosition());
		distanceToBase = relationalRepresentation.getNearestBase() == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(ship.getPosition(), relationalRepresentation.getNearestBase().getPosition());
		distanceToBeacon = relationalRepresentation.getNearestBeacon() == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(ship.getPosition(), relationalRepresentation.getNearestBeacon().getPosition());
		distanceToAsteroid = relationalRepresentation.getNearestAsteroid() == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(ship.getPosition(), relationalRepresentation.getNearestAsteroid().getPosition());
		distanceBetweenTargetBeaconAndEnemy = relationalRepresentation.getNearestEnemy() == null || relationalRepresentation.getNearestBeacon() == null ? 
				Double.POSITIVE_INFINITY : space.findShortestDistance(relationalRepresentation.getNearestEnemy().getPosition(), relationalRepresentation.getNearestBeacon().getPosition());
	}
	
	/**
	 * Getter for currentPosition
	 */
	protected Position getCurrentPosition() {
		return currentPosition;
	}
	
	/**
	 * Getter for currentEnergy
	 */
	protected double getCurrentEnergy() {
		return currentEnergy;
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
	
}
