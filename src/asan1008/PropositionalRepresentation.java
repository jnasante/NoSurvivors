package asan1008;

import java.util.Set;

import com.sun.org.apache.xerces.internal.parsers.CachingParserPool;

import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class PropositionalRepresentation {
	
	// 
	private Position currentPosition;
	private Ship nearestEnemy;
	private Ship currentTargetEnemy;
	private Base nearestBase;
	private Base currentTargetBase;
	private Beacon nearestBeacon;
	private Beacon currentTargetBeacon;
	private Asteroid nearestAsteroid;
	private Asteroid currentTargetAsteroid;
	private spacesettlers.actions.AbstractAction currentAction;
	
	// Calculated distances
	private double distanceToEnemy;
	private double distanceToBase;
	private double distanceToBeacon;
	private double distanceToAsteroid;
	private double distanceBetweenTargetBeaconAndEnemy;

	// Static constants
	public final int LARGE_DISTANCE = 400;
	public final int SHORT_DISTANCE = 60;
	public final int BEACON_BASE_DIFFERENCE_THRESHOLD = 100;

	public PropositionalRepresentation() {
		// ...
	}

	public void updateRepresentation(Toroidal2DPhysics space, Ship ship) {
		currentPosition = ship.getPosition();
		nearestEnemy = findNearestEnemy(space, ship);
		nearestBase = findNearestBase(space, ship);
		nearestBeacon = findNearestBeacon(space, ship);
		nearestAsteroid = findNearestAsteroid(space, ship);
		currentAction = ship.getCurrentAction();
		
		updateTargetEnemy(space, ship);
		updateTargetBeacon(space, ship);
		updateTargetAsteroid(space, ship);
		updateTargetBase(space, ship);
		
		distanceToEnemy = nearestEnemy == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(currentPosition, nearestEnemy.getPosition());
		distanceToBase = nearestBase == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(ship.getPosition(), nearestBase.getPosition());
		distanceToBeacon = nearestBeacon == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(ship.getPosition(), nearestBeacon.getPosition());
		distanceToAsteroid = nearestAsteroid == null ? Double.POSITIVE_INFINITY : space.findShortestDistance(ship.getPosition(), nearestAsteroid.getPosition());
		distanceBetweenTargetBeaconAndEnemy = nearestEnemy == null || nearestBeacon == null ? 
				Double.POSITIVE_INFINITY : space.findShortestDistance(nearestEnemy.getPosition(), nearestBeacon.getPosition());
		
	}
	
	protected Position getCurrentPosition() {
		return currentPosition;
	}

	protected Ship getNearestEnemy() {
		return nearestEnemy;
	}
	
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
	
	public void updateTargetAsteroid(Toroidal2DPhysics space, Ship ship) {
		if (currentTargetAsteroid != null) {
			if (!currentTargetAsteroid.isAlive()) {
				currentTargetAsteroid = null;
				return;
			}
						
			for (Asteroid updatedTargetAsteroid : space.getAsteroids()) {
				if (updatedTargetAsteroid.getId() == currentTargetAsteroid.getId()) {
					currentTargetAsteroid = updatedTargetAsteroid;
					return;
				}				
			}
		}
	}

	public void updateTargetBeacon(Toroidal2DPhysics space, Ship ship) {
		if (currentTargetBeacon != null) {
			if (!currentTargetBeacon.isAlive()) {
				currentTargetBeacon = null;
			} else if (distanceToBeacon < space.findShortestDistance(ship.getPosition(), currentTargetBeacon.getPosition())) {
				currentTargetBeacon = nearestBeacon;
			}
		} 
	}

	public void updateTargetBase(Toroidal2DPhysics space, Ship ship) {
		if (currentTargetBase != null) {
			if (!currentTargetBase.isAlive()) {
				currentTargetBase = null;
			} else if (distanceToBase < space.findShortestDistance(ship.getPosition(), currentTargetBase.getPosition())) {
				currentTargetBase = nearestBase;
			}
		} 
	}

	protected double getDistanceToEnemy() {
		return distanceToEnemy;
	}

	protected Base getNearestBase() {
		return nearestBase;
	}
	
	protected double getDistanceToBase() {
		return distanceToBase;
	}

	protected Beacon getNearestBeacon() {
		return nearestBeacon;
	}
	
	protected double getDistanceToBeacon() {
		return distanceToBeacon;
	}
	
	protected Asteroid getNearestAsteroid() {
		return nearestAsteroid;
	}
	
	protected double getDistanceToAsteroid() {
		return distanceToAsteroid;
	}

	protected spacesettlers.actions.AbstractAction getCurrentAction() {
		return currentAction;
	}
	
	protected Ship getCurrentTargetEnemy() {
		return currentTargetEnemy;
	}
	
	protected Beacon getCurrentTargetBeacon() {
		return currentTargetBeacon;
	}
	
	protected Asteroid getCurrentTargetAsteroid() {
		return currentTargetAsteroid;
	}
	
	protected Base getCurrentTargetBase() {
		return currentTargetBase;
	}

	protected double getDistanceBetweenTargetBeaconAndEnemy() {
		return distanceBetweenTargetBeaconAndEnemy;
	}
	
	protected void setCurrentTargetEnemy(Ship enemy) {
		currentTargetEnemy = enemy;
	}
	
	protected void setCurrentTargetBeacon(Beacon beacon) {
		currentTargetBeacon = beacon;
	}
	
	protected void setCurrentTargetAsteroid(Asteroid asteroid) {
		currentTargetAsteroid = asteroid;
	}
	
	protected void setCurrentTargetBase(Base base) {
		currentTargetBase = base;
	}

	/**
	 * Find the nearest ship on another team and aim for it
	 * @param space
	 * @param ship
	 * @return
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
	 * @param space
	 * @param ship
	 * @param beacon
	 * @return
	 */
	protected Ship findNearestEnemyWithinShortDistanceToBeacon(Toroidal2DPhysics space, Ship ship, Beacon beacon) {
		double minDistance = Double.POSITIVE_INFINITY;
		Ship nearestShip = null;
		for (Ship otherShip : space.getShips()) {
			// don't aim for our own team (or ourself)
			if (otherShip.getTeamName().equals(ship.getTeamName())) {
				continue;
			}
			
			double distance = space.findShortestDistance(beacon.getPosition(), otherShip.getPosition());
			if (distance < minDistance) {
				minDistance = distance;
				nearestShip = otherShip;
			}
		}
		
		return space.findShortestDistance(nearestShip.getPosition(), beacon.getPosition()) <= SHORT_DISTANCE ? nearestShip : null;
	}

	/**
	 * Find the base for this team nearest to this ship
	 * 
	 * @param space
	 * @param ship
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
	 * Returns the nearest, most convenient asteroid
	 * 
	 * @return
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
	
	/**
	 * 
	 */

	/**
	 * Find the nearest beacon to this ship
	 * @param space
	 * @param ship
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


}
