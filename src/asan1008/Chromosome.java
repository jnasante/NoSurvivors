package asan1008;

import java.lang.reflect.Field;
import java.util.Comparator;

/**
 * Chromosome representation for Evolutionary Computation
 */
public class Chromosome implements Comparator<Chromosome>, Comparable<Chromosome>{
	
	// Ship information
	public Individual agent;
	
	public Chromosome() {
		// Empty constructor
		agent = new Individual();
	}
	
	public Chromosome(Individual agent) {
		this.agent = agent;
	}
	
	public void calculateFitness(double score) {
		// Use score to keep it simple
		agent.FITNESS += score;
	}
	
	/**
	 * Mutate the current chromosome.
	 * Mutate 1/6 of the time. Half of those 1/6, add to the field. The other half, subtract from the field.
	 * Mutation of each field is always by 10%.
	 * 
	 * @return A mutated copy of the current chromosome
	 */
	public Chromosome mutate() {
		// Create a new chromosome to return later
		Chromosome chromosome = new Chromosome(new Individual());
		
		for (Field field : agent.getClass().getDeclaredFields()) {
			if(field.getName().equals("READY") || field.getName().equals("FITNESS")){
				continue;
			}
			
			int mutationConstant = (int) (Math.random() * 12);
			switch (mutationConstant) {
			
				case 1:
					// Increase value of field by 10%
					try {
						field.set(chromosome.agent, ((double)field.get(agent)) * (1.10));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
						NoSurvivorsTeamClient.log("Failed to mutate");
						return null;
					}
					break;
				
				case 2:
					// Decrease value of field by 10%
					try {
						field.set(chromosome.agent, ((double)field.get(agent)) * (0.90));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
						NoSurvivorsTeamClient.log("Failed to mutate");
						return null;
					}
					break;
	
				default:
					// Don't mutate. Just set the fields t
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
