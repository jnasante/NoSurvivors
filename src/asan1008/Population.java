package asan1008;

public class Population {
	
	private final int SIZE;
	private Chromosome[] chromosomes;
	
	public Population(Chromosome[] chromosomes) {
		SIZE = chromosomes.length;
		this.chromosomes = chromosomes;
	}

}
