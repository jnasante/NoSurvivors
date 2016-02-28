package asan1008;

import java.util.Set;

import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

/** 
 * 
 *
 */
public class RelationalRepresentation {
	
	// Nearest and target objects
	private Ship nearestEnemy;
	private Ship currentTargetEnemy;
	private Base nearestBase;
	private Beacon nearestBeacon;
	private Asteroid nearestAsteroid;
	
	/**
	 *  Update our knowledge about the current environment
	 *  
	 * @param space Current space instance
	 * @param ship Our ship
	 */
	public void updateRepresentation(Toroidal2DPhysics space, Ship ship) {
		nearestEnemy = findNearestEnemy(space, ship);
		nearestBase = findNearestBase(space, ship);
		nearestBeacon = findNearestBeacon(space, ship);
		nearestAsteroid = findNearestAsteroid(space, ship);
		
		updateTargetEnemy(space, ship);
	}
	
	/**
	 * If current target enemy is still alive, update to object to reflect changes in state
	 * Otherwise, release target so we can perform another action
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 */
	public void updateTargetEnemy(Toroidal2DPhysics space, Ship ship) {
		if (currentTargetEnemy != null) {
			if (!currentTargetEnemy.isAlive()) {
				currentTargetEnemy = null;
				return;
			}
						
			for (Ship updatedTargetShip : space.getShips()) {
				if (updatedTargetShip.getId() == currentTargetEnemy.getId()) {
					currentTargetEnemy = updatedTargetShip;
					return;
				}				
			}
		}
	}

	/**
	 * Getter for nearestEnemy
	 */
	protected Ship getNearestEnemy() {
		return nearestEnemy;
	}

	/**
	 * Getter for nearestBase
	 */
	protected Base getNearestBase() {
		return nearestBase;
	}
	
	/**
	 * Getter for nearestBeacon
	 */
	protected Beacon getNearestBeacon() {
		return nearestBeacon;
	}
	
	/**
	 * Getter for nearestAsteroid
	 */
	protected Asteroid getNearestAsteroid() {
		return nearestAsteroid;
	}
	
	/**
	 * Getter for currentTargetEnemy
	 */
	protected Ship getCurrentTargetEnemy() {
		return currentTargetEnemy;
	}

	/**
	 * Setter for currentTargetEnemy
	 */
	protected void setCurrentTargetEnemy(Ship enemy) {
		currentTargetEnemy = enemy;
	}
	
	/**
	 * Find the nearest ship on another team and aim for it
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 * 
	 * @return The nearest enemy
	 */
	private Ship findNearestEnemy(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.POSITIVE_INFINITY;
		Ship nearestShip = null;
		for (Ship otherShip : space.getShips()) {
			// don't aim for our own team (or ourself)
			if (otherShip.getTeamName().equals(ship.getTeamName())) {
				continue;
			}
			
			double distance = space.findShortestDistance(ship.getPosition(), otherShip.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestShip = otherShip;
			}
		}
		
		return nearestShip;
	}
	
	/**
	 * Find the ship to our target beacon, within SHORT_DISTANCE of the beacon
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 * @param beacon Target beacon
	 * @param shortDistance Distance radius within which to detect ships
	 * 
	 * @return Enemy closest to our target beacon within given radius
	 */
	protected Ship findNearestEnemyWithinShortDistanceToBeacon(Toroidal2DPhysics space, Ship ship, Beacon beacon, double shortDistance) {
		double minDistance = Double.POSITIVE_INFINITY;
		Ship nearestShip = null;
		for (Ship otherShip : space.getShips()) {
			// avoid aiming for our own team
			if (otherShip.getTeamName().equals(ship.getTeamName())) {
				continue;
			}
			
			double distance = space.findShortestDistance(beacon.getPosition(), otherShip.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestShip = otherShip;
			}
		}
		
		return space.findShortestDistance(nearestShip.getPosition(), beacon.getPosition()) <= shortDistance ? nearestShip : null;
	}

	/**
	 * Find the base for this team nearest to this ship
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 * @return
	 */
	private Base findNearestBase(Toroidal2DPhysics space, Ship ship) {
		double minDistance = Double.MAX_VALUE;
		Base nearestBase = null;

		for (Base base : space.getBases()) {
			if (base.getTeamName().equalsIgnoreCase(ship.getTeamName())) {
				double dist = space.findShortestDistance(ship.getPosition(), base.getPosition());
				if (dist < minDistance) {
					minDistance = dist;
					nearestBase = base;
				}
			}
		}
		
		return nearestBase;
	}

	/**
	 * Find the nearest beacon to this ship
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 * 
	 * @return
	 */
	private Beacon findNearestBeacon(Toroidal2DPhysics space, Ship ship) {
		// get the current beacons
		Set<Beacon> beacons = space.getBeacons();

		Beacon closestBeacon = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (Beacon beacon : beacons) {
			double dist = space.findShortestDistance(ship.getPosition(), beacon.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestBeacon = beacon;
			}
		}

		return closestBeacon;
	}

	/**
	 * Find the nearest, most convenient asteroid
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 * 
	 * @return Nearest, most convenient asteroid
	 */
	private Asteroid findNearestAsteroid(Toroidal2DPhysics space, Ship ship) {
		// get the current asteroids
		Set<Asteroid> asteroids = space.getAsteroids();

		Asteroid closestAsteroid = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (Asteroid asteroid : asteroids) {
			if (!asteroid.isMineable()) {
				continue;
			}
			
			double dist = space.findShortestDistance(ship.getPosition(), asteroid.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestAsteroid = asteroid;
			}
		}

		return closestAsteroid;
	}	
}
