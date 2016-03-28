package asan1008;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.UUID;

public class Chromosome implements Comparator<Chromosome>, Comparable<Chromosome>{
	
	// Ship information
	public UUID shipId;
	public Double fitness;
	public double score;
	public int damageDealt;
	public int resourcesDeposited;
	public int resourcesCollected;
	public int deaths;
	public int resourcesDropped;
	public int damageTaken;

	// Properties
	public double SHOOTING_DISTANCE = 250;
	public double LARGE_DISTANCE = 400;
	public double SHORT_DISTANCE = 60;
	public double SPEED_FAST = 90;
	public double SPEED_SLOW = 40;
	public double LOW_ENERGY = 1500;
	public double HIGH_RESOURCES = 500;
	public double ASTEROID_COLLECTING_TIMESTEP = 15000;
	
	// Constants
	public final double MUTATION_RATE = 0.00001;
	
	public Chromosome() {
		// Empty constructor
	}
	
	public Chromosome(UUID shipId, double shootingDistance, double largeDistance, double shortDistance, double fastSpeed, double slowSpeed, double lowEnergy, double highResources, double asteroidCollectingProbability) {
		this.shipId = shipId;
		SHOOTING_DISTANCE = shootingDistance;
		LARGE_DISTANCE = largeDistance;
		SHORT_DISTANCE = shortDistance;
		SPEED_FAST = fastSpeed;
		SPEED_SLOW = slowSpeed;
		LOW_ENERGY = lowEnergy;
		HIGH_RESOURCES = highResources;
		ASTEROID_COLLECTING_PROBABILITY = asteroidCollectingProbability;
	}
	
	public void recordGameObservations(double score, int damageDealt, int resourcesDeposited, int resourcesCollected, int deaths, int resourcesDropped, int damageTaken) {
		this.score = score;
		this.damageDealt = damageDealt;
		this.resourcesDeposited = resourcesDeposited;
		this.resourcesCollected = resourcesCollected;
		this.deaths = deaths;
		this.resourcesDropped = resourcesDropped;
		this.damageTaken = damageTaken;
		
		calculateFitness();
	}
	
	public void calculateFitness() {
		fitness = (damageDealt + 2*resourcesDeposited + resourcesCollected) - (10*deaths + 0.5*resourcesDropped + damageTaken);
	}
	
	public Chromosome mutate() {
		Chromosome chromosome = new Chromosome(shipId, SHOOTING_DISTANCE, LARGE_DISTANCE, SHORT_DISTANCE, SPEED_FAST, SPEED_SLOW, LOW_ENERGY, HIGH_RESOURCES, ASTEROID_COLLECTING_PROBABILITY);
		for (Field field : getClass().getDeclaredFields()) {
			int mutationConstant = (int) Math.random() * 3;
			
			switch (mutationConstant) {
			case 1:
				try {
					field.setDouble(chromosome, field.getDouble(this)*(1 + MUTATION_RATE));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
					NoSurvivorsTeamClient.log("Failed to mutate");
					return null;
				}
				break;
				
			case 2:
				try {
					field.setDouble(chromosome, field.getDouble(this)*(1 - MUTATION_RATE));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
					NoSurvivorsTeamClient.log("Failed to mutate");
					return null;
				}
				break;

			default:
				// Don't mutate
				break;
			}
		}
		
		return chromosome;
		
	}

	@Override
	public int compare(Chromosome ch1, Chromosome ch2) {
		return ch1.fitness < ch2.fitness ? -1 : ch1.fitness > ch2.fitness ? 1 : 0;
	}

	@Override
	public int compareTo(Chromosome ch) {
		return fitness.compareTo(ch.fitness);
	}	
}
