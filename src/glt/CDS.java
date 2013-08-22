package glt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The CDS containing all Exons and the reference to the parent Accession
 * 
 * @author michael
 * 
 */
public class CDS {

	static public enum Status {
		Public, Withdrawn, Reviewed
	}

	static public enum MatchType {
		Identical, Partial
	}

	private Accession accession = null; // reference to the Accession containing
										// thie CCDS
	private String id = null;
	private Status status = null;
	private MatchType matchType = null;

	// the Exons for this CCDS
	final private Map<String, Exon> exons = new HashMap<String, Exon>();

	public CDS(String id, Status status, Accession accession,
			MatchType matchType) {

		this.id = id;
		this.status = status;
		this.matchType = matchType;

		this.accession = accession;
		accession.add(this);
	}

	public String getId() {
		return this.id;
	}

	public Accession getAccession() {
		return this.accession;
	}

	public Strand getStrand() {
		return this.accession.getStrand();
	}
	
	public Status getStatus() {
		return this.status;
	}

	public boolean isPublic() {
		return this.status == Status.Public;
	}

	public boolean isWithdrawn() {
		return this.status == Status.Withdrawn;
	}

	public boolean isReviewed() {
		return this.status == Status.Reviewed;
	}

	public MatchType getMatchType() {
		return this.matchType;
	}

	public boolean isPartial() {
		return this.matchType == MatchType.Partial;
	}

	public boolean isIdentical() {
		return this.matchType == MatchType.Identical;
	}

	public boolean hasExon(String id) {
		return this.exons.containsKey(id);
	}

	public boolean hasExon(Exon exon) {
		return this.hasExon(exon.getId());
	}

	public Exon getExon(String id) {
		return this.exons.get(id);
	}

	/**
	 * Adds the exon to the colletion
	 * @param exon The exon
	 * @return this
	 */
	public CDS add(Exon exon) {
		String id = exon.getId();
		if (!this.hasExon(id)) {
			this.exons.put(id, exon);
		}
		return this;
	}

	/**
	 * Removes the object from the collection again
	 * @return The CDS
	 */
	public CDS remove(Exon exon) {
		String id = exon.getId();
		this.exons.remove(id);
		return this;
	}

	public Collection<Exon> getExons() {
		return this.exons.values();
	}
}