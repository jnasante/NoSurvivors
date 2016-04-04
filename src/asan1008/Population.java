package asan1008;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

import spacesettlers.simulator.Toroidal2DPhysics;

/** 
 * Population representation for evolutionary computation
 */
public class Population {
	
	public static final int SIZE = 4;
	private final int GAMES_PER_ROUND = 2;
	public List<Chromosome> chromosomes;
	private ArrayList<Chromosome> crossoverParents;
	
	public Population() {
		this.chromosomes = new ArrayList<Chromosome>(SIZE);
		crossoverParents = new ArrayList<>();
	}
	
	public Population(ArrayList<Chromosome> chromosomes) {
		this.chromosomes = chromosomes;
		crossoverParents = new ArrayList<>();
	}
	
	/**
	 * Create chromosome list of predetermined size 
	 */
	public void initializeChromosomes() {
		chromosomes = new ArrayList<Chromosome>();
		for (int i = 0; i < SIZE; i++) {
			chromosomes.add(new Chromosome());
		}
	}
	
	/** 
	 * Rank selection. Essentially, always select the top two performing teams.
	 * 
	 * @param space
	 */
	public void performRankSelection(Toroidal2DPhysics space) {
		Collections.sort(chromosomes);
		
		Chromosome bestThisLadder = chromosomes.get(chromosomes.size()-1);
		Individual bestAgent;
		XStream xstream = new XStream();
		xstream.alias("Individual", Individual.class);
		
		// Update the best performing agent so far, if the best this ladder has a higher score
		try { 
			bestAgent = (Individual) xstream.fromXML(new File("asan1008/best.xml"));
			if( bestAgent.FITNESS < bestThisLadder.agent.FITNESS ){
				xstream.toXML(bestThisLadder.agent, new FileOutputStream(new File("asan1008/best.xml")));
			}
		} catch (XStreamException | FileNotFoundException e) {
			// if you get an error, handle it other than a null pointer because
			// the error will happen the first time you run
			bestAgent = new Individual();
		}
		
		// Write out to the learning curve csv
		try {
			FileWriter writer = new FileWriter("asan1008/learning.csv", true);
			
			xstream.alias("Game", Game.class);
			Game game = (Game) xstream.fromXML(new File("asan1008/game_stats.xml"));
			
			writer.append(String.valueOf(bestThisLadder.agent.FITNESS/GAMES_PER_ROUND));
		    writer.append(',');
		    writer.append(String.valueOf(game.GAME_NUMBER));
		    writer.append('\n');
							
		    writer.flush();
		    writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Iterate twice
		for (int i = 0; i < 2; i++) {
			crossoverParents.add(chromosomes.remove(chromosomes.size()-1));
		}
		
		chromosomes.addAll(crossoverParents);
	}
	
	/** 
	 * Perform crossover 
	 * Create new chromosome by alternating which fields we select from each one
	 * 
	 * @return New chromosome after crossover
	 */
	public Chromosome performCrossover() {
		Chromosome newChromosome = new Chromosome();
		Field[] fields = newChromosome.agent.getClass().getDeclaredFields();
		
		for (int i = 0; i < fields.length; i++) {
			Individual parent = crossoverParents.get(i % 2).agent;
			try {
				Object value = parent.getClass().getField(fields[i].getName()).get(parent);
				fields[i].set(newChromosome.agent, value);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				System.out.println("Failed to do crossover");
				e.printStackTrace();
				return null;
			}
		}
		
		return newChromosome;
	}
	
	/**
	 * Perform mutations of chromosome to create next generation
	 * 
	 * @param chromosome
	 * @return
	 */
	public List<Chromosome> performMutations(Chromosome chromosome) {
		for (int i = 0; i < SIZE; i++) {
			Chromosome newChromosome = chromosome.mutate();
			chromosomes.set(i, newChromosome);
		}	
		return chromosomes;
	}

}
