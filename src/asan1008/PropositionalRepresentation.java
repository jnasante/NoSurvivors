package asan1008;

import java.util.Set;

import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class PropositionalRepresentation {
	
	private Position currentPosition;
	private Position nearestEnemyPosition;
	private double distanceToEnemy;
	private Position nearestBasePosition;
	private double distanceToBase;
	private Position nearestBeaconPosition;
	private double distanceToBeacon;
	private spacesettlers.actions.AbstractAction currentAction;
	private Ship currentTargetEnemy;
	private double distanceBetweenTargetBeaconAndEnemy;
	public static final int LARGE_DISTANCE = 350;
	public static final int SHORT_DISTANCE = 60;
	public static final int BEACON_BASE_DIFFERENCE_THRESHOLD = 100;

	public PropositionalRepresentation(Toroidal2DPhysics space) {
		
	}

	public void updateRepresentation(Toroidal2DPhysics space, Ship ship) {
		
	}

	protected Position getCurrentPosition() {
		return currentPosition;
	}

	protected Position getNearestEnemyPosition() {
		return nearestEnemyPosition;
	}
	
	protected double getDistanceToEnemy() {
		return distanceToEnemy;
	}

	protected Position getNearestBasePosition() {
		return nearestBasePosition;
	}
	
	protected double getDistanceToBase() {
		return distanceToBase;
	}

	protected Position getNearestBeaconPosition() {
		return nearestBeaconPosition;
	}
	
	protected double getDistanceToBeacon() {
		return distanceToBeacon;
	}

	protected spacesettlers.actions.AbstractAction getCurrentAction() {
		return currentAction;
	}
	public Ship getCurrentTargetEnemy() {
		return currentTargetEnemy;
	}

	protected double getDistanceBetweenTargetBeaconAndEnemy() {
		return distanceBetweenTargetBeaconAndEnemy;
	}
	

	/**
	 * Find the nearest ship on another team and aim for it
	 * @param space
	 * @param ship
	 * @return
	 */
	protected Ship findNearestEnemy(Toroidal2DPhysics space, Ship ship) {
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
	 * Find the nearest ship to our target beacon
	 * @param space
	 * @param ship
	 * @param beacon
	 * @return
	 */
	protected Ship findNearestEnemyToBeacon(Toroidal2DPhysics space, Ship ship, Beacon beacon) {
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
		
		return nearestShip;
	}

	/**
	 * Find the base for this team nearest to this ship
	 * 
	 * @param space
	 * @param ship
	 * @return
	 */
	protected Base findNearestBase(Toroidal2DPhysics space, Ship ship) {
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
	 * Returns the asteroid of highest value that isn't already being chased by this team
	 * 
	 * @return
	 */
	protected Asteroid pickHighestValueFreeAsteroid(Toroidal2DPhysics space, Ship ship) {
		Set<Asteroid> asteroids = space.getAsteroids();
		int bestMoney = Integer.MIN_VALUE;
		Asteroid bestAsteroid = null;

		for (Asteroid asteroid : asteroids) {
			if (asteroid.isMineable() && asteroid.getResources().getTotal() > bestMoney) {
				bestMoney = asteroid.getResources().getTotal();
				bestAsteroid = asteroid;
			}
		}
		//System.out.println("Best asteroid has " + bestMoney);
		return bestAsteroid;
	}

	/**
	 * Find the nearest beacon to this ship
	 * @param space
	 * @param ship
	 * @return
	 */
	protected Beacon findNearestBeacon(Toroidal2DPhysics space, Ship ship) {
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
