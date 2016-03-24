package asan1008;

import java.util.Set;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;

/** 
 * Relational knowledge representation of the environment
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
	
	/**
	 * Find out if an enemy is within the line of fire
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 * 
	 * @return Boolean value for whether or not we should shoot
	 */
	public boolean enemyOnPath(Toroidal2DPhysics space, Ship ship){
		// prepare the necessary variables for the algorithm
		double shipX = ship.getPosition().getX();
		double shipY = ship.getPosition().getY();
		double orientation = ship.getPosition().getOrientation();
		if( currentTargetEnemy != null) {
			double radius = currentTargetEnemy.getRadius();
			double enemyX = currentTargetEnemy.getPosition().getX();
			double enemyY = currentTargetEnemy.getPosition().getY();
			double x1, y1, x2, y2;
			// check if the x and y distances are of the same sign
			if((enemyX - shipX) * (enemyY - shipY) > 0) {
				// if x and y distances have the same sign, use the bottom left and top right corners
				// bottom left corner
				x1 = enemyX - radius;
				y1 = enemyY + radius;
				// top right corner
				x2 = enemyX + radius;
				y2 = enemyY - radius;
			} else {
				// if x and y distances have different signs, use the top left and bottom right corners
				// top left corner
				x1 = enemyX - radius;
				y1 = enemyY - radius;
				// bottom right corner
				x2 = enemyX + radius;
				y2 = enemyY + radius;
			}
			
			// now we can get the x and y distances to the proper corners
			double xDist1 = x1 - shipX;
			double yDist1 = y1 - shipY;
			double xDist2 = x2 - shipX;
			double yDist2 = y2 - shipY;
			// turn the cartesian distances to degrees using arctan
			double degree1 = Math.toDegrees(Math.atan2(yDist1, xDist1));
			// turn the degrees into radians, accounting for the odd radian distribution in spacesettlers
			double radian1 = -((-degree1)*Math.PI/180);
			double degree2 = Math.toDegrees(Math.atan2(yDist2, xDist2));
			double radian2 = -((-degree2)*Math.PI/180);
			
			// check that it is not a pi/-pi overlap			
			if (radian1*radian2 > 0 || Math.max(radian1, radian2) < Math.PI/2) {
				// check that our ship’s orientation lies between the radians
				if((orientation <= Math.max(radian1, radian2)) && (orientation >= Math.min(radian1, radian2))) {
					// enemy is on path
					return true;
				}
			} else {
				// check that our ship’s orientation lies between the radians that involve pi/-pi overlap
				if((orientation >= Math.max(radian1, radian2)) && (orientation <= Math.min(radian1, radian2))) {
					// enemy is on path
					return true;
				}
			}
		}
		// enemy is not on path
		return false;
	}
	
	/**
	 * Find the target orientation to the current enemy
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 * 
	 * @return Double value of orientation to enemy from ship
	 */
	public double getTargetOrientationToEnemy(Toroidal2DPhysics space, Ship ship) {
		if(currentTargetEnemy != null) {
			double shipX = ship.getPosition().getX();
			double shipY = ship.getPosition().getY();
			double enemyX = currentTargetEnemy.getPosition().getX();
			double enemyY = currentTargetEnemy.getPosition().getY();		
			double xDist = enemyX - shipX;
			double yDist = enemyY - shipY;
			double degree = Math.toDegrees(Math.atan2(yDist, xDist));
			double radian = -((-degree)*Math.PI/180);
			return radian;
		}
		return 10.0;
	}

}
