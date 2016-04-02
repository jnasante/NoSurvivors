package asan1008;

public class ResourceDelivery {
	private double energy;
	private double shipToAsteroid;
	private double asteroidToBase;
	private double shipToBase;
	private double resourcesHeld;
	private int success;
	
	// Weights learned from regression
	private final double[] w = { -0.379713587974, 0.00013225887486, 0.000901991331468, 0.00124808731749, -5.76578406546e-05, -6.42946494996e-05 };
	
	public ResourceDelivery() {
		
	}
	
	public ResourceDelivery( double energy, double shipToAsteroid, double asteroidToBase, double shipToBase, double resourcesHeld ) {
		setValues(energy, shipToAsteroid, asteroidToBase, shipToBase, resourcesHeld);
	}
	
	public void setValues( double energy, double shipToAsteroid, double asteroidToBase, double shipToBase, double resourcesHeld ) {
		this.energy = energy;
		this.shipToAsteroid = shipToAsteroid;
		this.asteroidToBase = asteroidToBase;
		this.shipToBase = shipToBase;
		this.resourcesHeld = resourcesHeld;
	}
	
	public double predictSurvivalProbability( double energy, double shipToAsteroid, double asteroidToBase, double shipToBase, double resourcesHeld ) {
		return w[0] + w[1]*energy + w[2]*shipToAsteroid + w[3]*asteroidToBase + w[4]*shipToBase + w[5]*resourcesHeld;
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
