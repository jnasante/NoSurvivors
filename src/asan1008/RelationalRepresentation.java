package asan1008;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import sun.util.logging.resources.logging;

/** 
 * Relational knowledge representation of the environment
 *
 */
public class RelationalRepresentation {
	
	// Nearest and target objects
	private HashMap<UUID, Ship> nearestEnemy = new HashMap<UUID, Ship>();
	private HashMap<UUID, Ship> currentTargetEnemy = new HashMap<UUID, Ship>();
	private HashMap<UUID, Asteroid> currentTargetAsteroid = new HashMap<UUID, Asteroid>();
	private HashMap<UUID, Base> nearestBase = new HashMap<UUID, Base>();
	private HashMap<UUID, Beacon> nearestBeacon = new HashMap<UUID, Beacon>();
	private HashMap<UUID, Asteroid> nearestAsteroid = new HashMap<UUID, Asteroid>();
	
	/**
	 *  Update our knowledge about the current environment
	 *  
	 * @param space Current space instance
	 * @param ship Our ship
	 */
	public void updateRepresentation(Toroidal2DPhysics space, Ship ship) {
		nearestEnemy.put(ship.getId(), findNearestEnemy(space, ship));
		nearestBase.put(ship.getId(), findNearestBase(space, ship));
		nearestBeacon.put(ship.getId(), findNearestBeacon(space, ship));
		nearestAsteroid.put(ship.getId(), findNearestAsteroid(space, ship));		
		updateTargetEnemy(space, ship);
		updateTargetAsteroid(space, ship);
	}
	
	/**
	 * If current target enemy is still alive, update to object to reflect changes in state
	 * Otherwise, release target so we can perform another action
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 */
	public void updateTargetEnemy(Toroidal2DPhysics space, Ship ship) {
		if (currentTargetEnemy.get(ship.getId()) != null) {
			if (!currentTargetEnemy.get(ship.getId()).isAlive()) {
				currentTargetEnemy.put(ship.getId(), null);
				return;
			}
						
			for (Ship updatedTargetShip : space.getShips()) {
				if (updatedTargetShip.getId() == currentTargetEnemy.get(ship.getId()).getId()) {
					currentTargetEnemy.put(ship.getId(), updatedTargetShip);
					return;
				}				
			}
		}
	}
	
	/**
	 * If current target asteroid is still alive, update to object to reflect changes in state
	 * Otherwise, release target so we can perform another action
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 */
	public void updateTargetAsteroid(Toroidal2DPhysics space, Ship ship) {
		if (currentTargetAsteroid.get(ship.getId()) != null) {
			if (!currentTargetAsteroid.get(ship.getId()).isAlive()) {
				currentTargetAsteroid.put(ship.getId(), null);
				return;
			}
						
			for (Asteroid updatedTargetAsteroid : space.getAsteroids()) {
				if (updatedTargetAsteroid.getId() == currentTargetAsteroid.get(ship.getId()).getId()) {
					currentTargetAsteroid.put(ship.getId(), updatedTargetAsteroid);
					return;
				}				
			}
			
			currentTargetAsteroid.put(ship.getId(), null);
		}
	}

	/**
	 * Getter for nearestEnemy
	 */
	protected Ship getNearestEnemy(Ship ship) {
		return nearestEnemy.get(ship.getId());
	}

	/**
	 * Getter for nearestBase
	 */
	protected Base getNearestBase(Ship ship) {
		return nearestBase.get(ship.getId());
	}
	
	/**
	 * Getter for nearestBeacon
	 */
	protected Beacon getNearestBeacon(Ship ship) {
		return nearestBeacon.get(ship.getId());
	}
	
	/**
	 * Getter for nearestAsteroid
	 */
	protected Asteroid getNearestAsteroid(Ship ship) {
		return nearestAsteroid.get(ship.getId());
	}
	
	/**
	 * Getter for currentTargetEnemy
	 */
	protected Ship getCurrentTargetEnemy(Ship ship) {
		return currentTargetEnemy.get(ship.getId());
	}
	
	/**
	 * Getter for currentTargetAsteroid
	 */
	protected Asteroid getCurrentTargetAsteroid(Ship ship) {
		return currentTargetAsteroid.get(ship.getId());
	}

	/**
	 * Setter for currentTargetEnemy
	 */
	protected void setCurrentTargetEnemy(Ship enemy, Ship ship) {
		currentTargetEnemy.put(ship.getId(), enemy);
	}
	
	/**
	 * Setter for currentTargetAsteroid
	 */
	protected void setCurrentTargetAsteroid(Asteroid asteroid, Ship ship) {
		currentTargetAsteroid.put(ship.getId(), asteroid);
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
	 * Find the nearest, most convenient mineable asteroid
	 * 
	 * @param space Current space instance
	 * @param ship Our ship
	 * 
	 * @return Nearest, most convenient asteroid
	 */
	private Asteroid findNearestAsteroid(Toroidal2DPhysics space, Ship ship) {
		Asteroid closestAsteroid = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (Asteroid asteroid : space.getAsteroids()) {
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
	 * Returns the asteroid of highest value within specified radius of our base
	 * 
	 * @return
	 */
	public Asteroid findHighestValueAsteroidWithinRadius(Toroidal2DPhysics space, Ship ship, double radius) {
		Set<Asteroid> asteroids = space.getAsteroids();
		int bestMoney = Integer.MIN_VALUE;
		Asteroid bestAsteroid = null;

		for (Asteroid asteroid : asteroids) {
			if (asteroid.isMineable() && asteroid.getResources().getTotal() > bestMoney && 
					space.findShortestDistance(findNearestBase(space, ship).getPosition(), asteroid.getPosition()) < radius) {
				bestMoney = asteroid.getResources().getTotal();
				bestAsteroid = asteroid;
			}
		}
		return bestAsteroid;
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
		if( currentTargetEnemy.get(ship.getId()) != null) {
			double radius = currentTargetEnemy.get(ship.getId()).getRadius();
			double enemyX = currentTargetEnemy.get(ship.getId()).getPosition().getX();
			double enemyY = currentTargetEnemy.get(ship.getId()).getPosition().getY();
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
		if(currentTargetEnemy.get(ship.getId()) != null) {
			Position currentLoc = ship.getPosition();
			Position goalLoc = currentTargetEnemy.get(ship.getId()).getPosition();
			double shipX = currentLoc.getX();
			double shipY = currentLoc.getY();
			double enemyX = goalLoc.getX();
			double enemyY = goalLoc.getY();			
			double xDist = enemyX - shipX;
			double yDist = enemyY - shipY;
			
			double halfWidth = space.getWidth()/2;
			double halfHeight = space.getHeight()/2;
			double absXDist = Math.abs(xDist);
			double absYDist = Math.abs(yDist);
			
			if(absXDist > halfWidth || absYDist > halfHeight) {
				return 10.0;
			}
			
			double degree = Math.toDegrees(Math.atan2(yDist, xDist));
			double radian = -((-degree)*Math.PI/180);
			return radian;
		}
		return 10.0;
	}

}
