package glt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Accession {

	private String id = null; // the unique ID
	private Gene gene = null; // the Gene containing this Accession

	// all CCDS in this Accession
	private final Map<String, CCDS> ccdss = new HashMap<String, CCDS>();

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

	public Gene.Strand getStrand() {
		return this.gene.getStrand();
	}

	public boolean hasCCDS(String id) {
		return this.ccdss.containsKey(id);
	}

	public boolean hasCCDS(CCDS ccds) {
		return this.hasCCDS(ccds.getId());
	}

	public Accession add(CCDS ccds) {
		String id = ccds.getId();
		if (!this.hasCCDS(id)) {
			this.ccdss.put(id, ccds);
		}
		return this;
	}

	public Collection<CCDS> getCCDSs() {
		return this.ccdss.values();
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
