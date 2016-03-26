package asan1008;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import spacesettlers.simulator.Toroidal2DPhysics;

public class Population {
	
	private final int SIZE = 5;
	private List<Chromosome> chromosomes;
	private ArrayList<Chromosome> crossoverParents;
	private final double TOURNAMENT_SELECTION_PROBABILITY = 0.75;
	
	public Population() {
		this.chromosomes = new ArrayList<Chromosome>(SIZE);
		
		// Read from file to set values for each chromosome
	}
	
	public void initializeChromosomes() {
		chromosomes = new ArrayList<Chromosome>();
		for (int i = 0; i < SIZE; i++) {
			chromosomes.add(new Chromosome());
		}
	}
	
	// Tournament Selection
	public void performTournamentSelection(Toroidal2DPhysics space) {
		Collections.sort(chromosomes);
		
		// Iterate twice
		for (int i = 0; i < 2; i++) {
			double random = Math.random();
			if (random > TOURNAMENT_SELECTION_PROBABILITY) {
				crossoverParents.add(chromosomes.remove((int) Math.random() * chromosomes.size()));
			} else {
				crossoverParents.add(chromosomes.remove(chromosomes.size()-1));
			}
		}
	}
	
	// Crossover
	public Chromosome performCrossover() {
		Chromosome newChromosome = new Chromosome();
		Field[] fields = newChromosome.getClass().getFields();
		
		for (int i = 0; i < fields.length; i++) {
			Chromosome parent = crossoverParents.get(i % 2);
			try {
				double value = parent.getClass().getField(fields[i].getName()).getDouble(parent);
				fields[i].setDouble(newChromosome, value);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				System.out.println("Failed to do crossover");
				e.printStackTrace();
				return null;
			}
		}
		
		return newChromosome;
	}
	
	// Mutation
	public void performMutations(Chromosome chromosome) {
		for (int i = 0; i < SIZE; i++) {
			chromosomes.set(i, chromosome.mutate());
		}		
	}

}
