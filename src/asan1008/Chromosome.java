package asan1008;

import java.lang.reflect.Field;

public class Chromosome {

	// Properties
	public double SHOOTING_DISTANCE = 250;
	public double LARGE_DISTANCE = 400;
	public double SHORT_DISTANCE = 60;
	public double SPEED_FAST = 90;
	public double SPEED_SLOW = 40;
	public double LOW_ENERGY = 1500;
	public double HIGH_RESOURCES = 500;
	public double ASTEROID_COLLECTING_PROBABLITIY = 0.0;
	
	// Constants
	public final double MUTATION_RATE = 0.00001;
	
	private Chromosome() {}

	public Chromosome(int shootingDistance, int largeDistance, int shortDistance, int fastSpeed, int slowSpeed, int lowEnergy, int highResources, double asteroidCollectingProbability) {
		SHOOTING_DISTANCE = shootingDistance;
		LARGE_DISTANCE = largeDistance;
		SHORT_DISTANCE = shortDistance;
		SPEED_FAST = fastSpeed;
		SPEED_SLOW = slowSpeed;
		LOW_ENERGY = lowEnergy;
		HIGH_RESOURCES = highResources;
		ASTEROID_COLLECTING_PROBABLITIY = asteroidCollectingProbability;
	}
	
	public void mutate() {
		for (Field field : getClass().getDeclaredFields()) {
			int mutationConstant = (int) Math.random() * 3;
			
			switch (mutationConstant) {
			case 1:
				try {
					field.setDouble(this, field.getDouble(this)*(1 + MUTATION_RATE));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
				
			case 2:
				try {
					field.setDouble(this, field.getDouble(this)*(1 - MUTATION_RATE));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;

			default:
				break;
			}
		}
		
	}
	
	
}
