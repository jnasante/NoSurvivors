package asan1008;

public class ResourceDelivery {
	private double energy;
	private double shipToAsteroid;
	private double asteroidToBase;
	private double shipToBase;
	private double resourcesHeld;
	private int success;
	
	public ResourceDelivery() {
		
	}
	
	public ResourceDelivery( double energy, double shipToAsteroid, double asteroidToBase, double shipToBase, double resourcesHeld) {
		setValues(energy, shipToAsteroid, asteroidToBase, shipToBase, resourcesHeld);
	}
	
	public void setValues( double energy, double shipToAsteroid, double asteroidToBase, double shipToBase, double resourcesHeld) {
		this.energy = energy;
		this.shipToAsteroid = shipToAsteroid;
		this.asteroidToBase = asteroidToBase;
		this.shipToBase = shipToBase;
		this.resourcesHeld = resourcesHeld;
	}
		
	public double getEnergy() {
		return energy;
	}

	public void setEnergy(double energy) {
		this.energy = energy;
	}

	public double getShipToAsteroid() {
		return shipToAsteroid;
	}

	public void setShipToAsteroid(double shipToAsteroid) {
		this.shipToAsteroid = shipToAsteroid;
	}

	public double getAsteroidToBase() {
		return asteroidToBase;
	}

	public void setAsteroidToBase(double asteroidToBase) {
		this.asteroidToBase = asteroidToBase;
	}

	public double getShipToBase() {
		return shipToBase;
	}

	public void setShipToBase(double shipToBase) {
		this.shipToBase = shipToBase;
	}

	public double getResourcesHeld() {
		return resourcesHeld;
	}

	public void setResourcesHeld(double resourcesHeld) {
		this.resourcesHeld = resourcesHeld;
	}

	public int getSuccess() {
		return success;
	}

	public void setSuccess(int success) {
		this.success = success;
	}
	
}
