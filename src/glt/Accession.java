package glt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Accession {

	private String id = null; // the unique ID
	private Gene gene = null; // the Gene containing this Accession

	// all CDS in this Accession
	private final Map<String, CDS> cdss = new HashMap<String, CDS>();

	private Accession(String id, Gene gene) {
		this.id = id;

		// add it to the gene
		gene.add(this);
		this.gene = gene;
	}

	public String getId() {
		return this.id;
	}

	public Gene getGene() {
		return this.gene;
	}

	public Strand getStrand() {
		return this.gene.getStrand();
	}

	public boolean hasCCDS(String id) {
		return this.cdss.containsKey(id);
	}

	public boolean hasCCDS(CDS ccds) {
		return this.hasCCDS(ccds.getId());
	}

	public Accession add(CDS ccds) {
		String id = ccds.getId();
		if (!this.hasCCDS(id)) {
			this.cdss.put(id, ccds);
		}
		return this;
	}

	public Collection<CDS> getCCDSs() {
		return this.cdss.values();
	}

	/**
	 * Returns a new Accession from scratch or returns the already existing one
	 * 
	 * @param id
	 *            The unique Accession ID
	 * @param gene
	 *            The Gene containing the Accession
	 * @return a new Accession from scratch or returns the already existing one
	 */
	static public Accession factory(String id, Gene gene) {

		Accession accession = gene.hasAccession(id) ? gene.getAccession(id)
				: new Accession(id, gene);

		return accession;
	}
}
