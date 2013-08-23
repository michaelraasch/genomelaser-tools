package glt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Gene {

	private String id = null; // the unique Gene ID
	private String name = null; // The Gene's name
	private Strand strand = null;
	private Chromosome chromosome = null; // the Chromosome containing the Gene

	// a list of all Accessions within the Gene
	private final Map<String, Accession> accessions = new HashMap<String, Accession>();

	private Gene(String id, String name, String strand, Chromosome chromosome) {

		this.id = id;
		this.name = name;
		this.strand = strand.equals("+") ? Strand.Positive : Strand.Negative;

		chromosome.add(this);
		this.chromosome = chromosome;
	}

	public String getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public Strand getStrand() {
		return this.strand;
	}

	public boolean isPositive() {
		return this.strand == Strand.Positive;
	}

	public boolean isNegative() {
		return !this.isPositive();
	}

	public Chromosome getChromosome() {
		return this.chromosome;
	}

	public boolean hasAccession(String id) {
		return this.accessions.containsKey(id);
	}

	public boolean hasAccession(Accession accession) {
		return this.hasAccession(accession.getId());
	}

	public Accession getAccession(String id) {
		return this.accessions.get(id);
	}

	/**
	 * Adds the Accession to the Gene.
	 * 
	 * @param accession
	 *            The Accession.
	 * @return The Gene.
	 */
	public Gene add(Accession accession) {
		String id = accession.getId();

		if (!this.hasAccession(id)) {
			this.accessions.put(id, accession);
		}

		return this;
	}

	/**
	 * Returns all Accessions within this Gene.
	 * 
	 * @return all Accessions within this Gene.
	 */
	public Collection<Accession> getAccessions() {
		return this.accessions.values();
	}

	/**
	 * Returns a new Gene or an already existing one.
	 * 
	 * @param id
	 *            The Gene's unique ID.
	 * @param name
	 *            The Gene's name.
	 * @param strand
	 *            The Strand whether + or -
	 * @param chromosome
	 *            The Chromosome containing the Gene
	 * @return a new Gene or an already existing one.
	 */
	static public Gene factory(String id, String name, String strand,
			Chromosome chromosome) {

		Gene gene = chromosome.hasGene(id) ? chromosome.getGene(id) : new Gene(
				id, name, strand, chromosome);

		return gene;
	}
}
