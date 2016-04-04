package asan1008;

public class ResourceDelivery {
	private double energy;
	private double shipToAsteroid;
	private double asteroidToBase;
	private double shipToBase;
	private int success;
	
	// Weights learned from regression
	private final double[] w = { 0.610862369323, 8.10469543238e-05, -0.000599327181399, -0.000795335405885, 0.000626560550786 };
	
	public ResourceDelivery() {
		
	}
	
	public ResourceDelivery( double energy, double shipToAsteroid, double asteroidToBase, double shipToBase ) {
		setValues(energy, shipToAsteroid, asteroidToBase, shipToBase);
	}
	
	public void setValues( double energy, double shipToAsteroid, double asteroidToBase, double shipToBase ) {
		this.energy = energy;
		this.shipToAsteroid = shipToAsteroid;
		this.asteroidToBase = asteroidToBase;
		this.shipToBase = shipToBase;
	}
	
	public double predictSurvivalProbability( double energy, double shipToAsteroid, double asteroidToBase, double shipToBase ) {
		return w[0] + w[1]*energy + w[2]*shipToAsteroid + w[3]*asteroidToBase + w[4]*shipToBase;
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

	public int getSuccess() {
		return success;
	}

	public void setSuccess(int success) {
		this.success = success;
	}
	
}
