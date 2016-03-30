package asan1008;

import java.lang.reflect.Field;
import java.util.Comparator;

public class Chromosome implements Comparator<Chromosome>, Comparable<Chromosome>{
	
	// Ship information
//	public Double fitness;
//	public int damageDealt;
//	public int resourcesDeposited;
//	public int resourcesCollected;
//	public int deaths;
//	public int resourcesDropped;
//	public int damageTaken;

	public Individual agent;
	
	// Constants
	//public final double MUTATION_RATE = 0.01;
	
	public Chromosome() {
		// Empty constructor
		agent = new Individual();
	}
	
	public Chromosome(Individual agent) {
		this.agent = agent;
	}
	
	public void calculateFitness(double score) {
		agent.FITNESS += score;
		//(damageDealt + 2*resourcesDeposited + resourcesCollected) - (10*deaths + 0.5*resourcesDropped + damageTaken);
	}
	
	public Chromosome mutate() {
		Chromosome chromosome = new Chromosome(new Individual());
		for (Field field : agent.getClass().getDeclaredFields()) {
			if(field.getName().equals("READY") || field.getName().equals("FITNESS")){
				continue;
			}
			int mutationConstant = (int) (Math.random() * 3);
			switch (mutationConstant) {
			case 1:
				try {
					double MUTATION_RATE = Math.random() * 0.1;
					field.set(chromosome.agent, ((double)field.get(agent)) * (1 + MUTATION_RATE));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
					NoSurvivorsTeamClient.log("Failed to mutate");
					return null;
				}
				break;
				
			case 2:
				try {
					double MUTATION_RATE = Math.random() * 0.1;
					field.set(chromosome.agent, ((double)field.get(agent)) * (1 - MUTATION_RATE));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
					NoSurvivorsTeamClient.log("Failed to mutate");
					return null;
				}
				break;

			default:
				// Don't mutate
				try {
					field.set(chromosome.agent, ((double)field.get(agent)));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
					NoSurvivorsTeamClient.log("Failed to mutate");
					return null;
				}
				break;
			}
		}
		
		return chromosome;
		
	}

	@Override
	public int compare(Chromosome ch1, Chromosome ch2) {
		return ch1.agent.FITNESS < ch2.agent.FITNESS ? -1 : ch1.agent.FITNESS > ch2.agent.FITNESS ? 1 : 0;
	}

	@Override
	public int compareTo(Chromosome ch) {
		return agent.FITNESS.compareTo(ch.agent.FITNESS);
	}	
}
