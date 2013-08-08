package glt;

import java.io.IOException;
import java.io.RandomAccessFile;

public class Exon implements Comparable<Exon> {

	private String id = null; // the unique ID
	private int from = 0; // the start position within the Chromosoome
	private int to = 0; // the stop position within the Chromosome
	private CCDS ccds = null; // the CCDS containing this Exon

	private RandomAccessFile file = null;
	private Bases bases = null;  
	
	private Exon(String id, int from, int to, CCDS ccds) {

		this.id = id;
		this.from = from;
		this.to = to;

		this.ccds = ccds;

		// add ourselves to the CCDS
		ccds.add(this);
	}

	public CCDS getCCDS() {
		return this.ccds;
	}

	public String getId() {
		return this.id;
	}

	public int getFrom() {
		return this.from;
	}

	public int getTo() {
		return this.to;
	}

	public int getLength() {
		return this.to - this.from + 1;
	}

	public Gene.Strand getStrand() {
		return this.ccds.getStrand();
	}
	
	public Exon setSourceFile(RandomAccessFile file) {
		this.file = file;
		return this;
	}
	
	public Bases getBases() {
		
		if (this.bases == null) {
			
			int length = this.getLength();
			
			this.bases = new Bases();

			try {
				
				byte[] bases = new byte[length];
				
				this.file.seek(this.getFrom());
				this.file.read(bases, 0, length);

				// add all the letters
				this.bases.add(new String(bases));

				// by default the data in the FASTA file is on the positive strand.
				// so if it is on the negative one, then we have to reverse-complement it
				if (this.getStrand() == Gene.Strand.Negative) {

					this.bases = this.bases.reverseComplement();
				}

			} catch (IOException e) {
				System.err.println("I/O error");
				e.getLocalizedMessage();
			}
		}
		
		return this.bases;
	}
	
	/**
	 * Returns a new Exon or returns an already existing one.
	 * 
	 * @param from
	 *            The start position within the Chromosome.
	 * @param to
	 *            The stop position within the Chromosome.
	 * @param ccds
	 *            The CCDS containing the Exon
	 * @return a new Exon or returns an already existing one.
	 */
	static public Exon factory(int from, int to, CCDS ccds) {

		// an Exon does not have an ID, so we make it up
		String id = Integer.toString(from) + ":" + Integer.toString(to);

		Exon exon = ccds.hasExon(id) ? ccds.getExon(id) : new Exon(id, from,
				to, ccds);

		return exon;
	}

	/**
	 * Implementation of compareTo() to allow sorting with the TreeMap. Sorting
	 * is based on the Exon's from and to positions.
	 */
	@Override
	public int compareTo(Exon o) {

		int ret = 0;

		if (this.from == o.getFrom() && this.to == o.getTo()) {
			ret = 0;
		} else {
			if (this.from == o.getFrom()) {
				ret = this.to < o.getTo() ? -1 : 1;
			} else {
				ret = this.from < o.getFrom() ? -1 : 1;
			}
		}

		return ret;
	}
}
