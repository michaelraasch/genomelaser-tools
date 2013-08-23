package glt;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Chromosome {

	static final private Map<String, Chromosome> chromosomes = new HashMap<String, Chromosome>();

	// 1..22,X,Y
	private String id = null;
	private final Map<String, Gene> genes = new HashMap<String, Gene>();

	private Chromosome(String id) {
		GLT.logger.info("Created Chromosome " + id);
		this.id = id;

		// add it to our list, so we know it has already been created
		Chromosome.chromosomes.put(id, this);
	}

	static public Chromosome get(String id) {
		return Chromosome.chromosomes.get(id);
	}
	
	public String getId() {
		return this.id;
	}

	public boolean hasGene(String id) {
		return this.genes.containsKey(id);
	}

	public boolean hasGene(Gene gene) {
		return this.hasGene(gene.getId());
	}

	public Gene getGene(String id) {
		return this.genes.get(id);
	}

	/**
	 * Adds a Gene to this Chromosome.
	 * 
	 * @param gene
	 *            The Gene to be added to the Chromosome.
	 * @return The Chromosome.
	 */
	public Chromosome add(Gene gene) {

		String id = gene.getId();

		if (!this.hasGene(id)) {
			this.genes.put(id, gene);
		}
		return this;
	}

	/**
	 * Returns all Genes for this Chromosome.
	 * 
	 * @return all Genes for this Chromosome.
	 */
	public Collection<Gene> getGenes() {
		return this.genes.values();
	}

	/**
	 * Returns all Chromosomes.
	 * 
	 * @return all Chromosomes.
	 */
	static public Collection<Chromosome> getAll() {
		return Chromosome.chromosomes.values();
	}

	/**
	 * Returns a new Chromosome or returns the already existing one.
	 * 
	 * @param id
	 *            The unique Chromosome ID
	 * @return a new Chromosome or returns the already existing one.
	 */
	static public Chromosome factory(String id) {

		Chromosome chromosome = Chromosome.chromosomes.containsKey(id) ? Chromosome.chromosomes
				.get(id) : new Chromosome(id);

		return chromosome;
	}
}
