package glt;

import java.util.ArrayList;
import java.util.List;

public class Config {

	private String directory = "./";

	private String referenceChromosomeFastaFileName = "hs_ref_GRCh37.p13_chr?.fa";
	private String referenceChromosomeBasesFileName = "hs_ref_GRCh37.p13_chr?.bases";
	private String referenceExonBasesFileName = "hs_ref_GRCh37.p13_chr?.exons.bases";
	private String inputChromosomeFastaFileName = "chromosome?.fa";
	private String inputChromosomeBasesFileName = "chromosome?.bases";
	private String outputChromosomeBasesFileName = "chromosome?.bases";
	private String outputExonBasesFileName = "chromosome?.exons.bases";
	private String outputExonLocationsFileName = "chromosome?.exons.locations";

	// list of all ChromosomeIds that should be processed
	private List<String> chromosomeIds = new ArrayList<String>();
	
	public Config() {

		for (int i = 1; i <= 22; i++) {
			this.chromosomeIds.add(Integer.toString(i));
		}
		this.chromosomeIds.add("X");
		this.chromosomeIds.add("Y");
	}

	/**
	 * Sets all Chromosome IDs that should be processed
	 * @return Config
	 */
	public Config setChromosomeIds(List<String> ids) {

		this.chromosomeIds = ids;
		return this;
	}

	/**
	 * Returns all Chromosome IDs that should be processed
	 * @return An ArrayList of all Chromosome IDs
	 */
	public List<String> getChromosomeIds() {

		return this.chromosomeIds;
	}

	public void setDirectory(String dir) {
		directory = dir;
	}

	public String getDirectory() {
		return this.directory;
	}
	
	public String getExonsFileName() {
		return directory + "CCDS.current.txt";
	}

	public Config setReferenceChromosomeFastaFileName(String fn) {
		referenceChromosomeFastaFileName = fn;
		return this;
	}

	public String getReferenceChromosomeFastaFileName(String id) {
		return directory + referenceChromosomeFastaFileName.replace("?", id);
	}

	public Config setInputChromosomeFastaFileName(String fn) {
		inputChromosomeFastaFileName = fn;
		return this;
	}

	public String getInputChromosomeFastaFileName(String id) {
		return directory + inputChromosomeFastaFileName.replace("?", id);
	}

	public Config setReferenceChromosomeBasesFileName(String fn) {
		referenceChromosomeBasesFileName = fn;
		return this;
	}

	public String getReferenceChromosomeBasesFileName(String id) {
		return directory + referenceChromosomeBasesFileName.replace("?", id);
	}

	public Config setReferenceExonBasesFileName(String fn) {
		referenceExonBasesFileName = fn;
		return this;
	}

	public String getReferenceExonBasesFileName(String id) {
		return directory + referenceExonBasesFileName.replace("?", id);
	}

	public Config setInputChromosomeBasesFileName(String fn) {
		inputChromosomeBasesFileName = fn;
		return this;
	}

	public String getInputChromosomeBasesFileName(String id) {
		return directory + inputChromosomeBasesFileName.replace("?", id);
	}

	public Config setOutputChromosomeBasesFileName(String fn) {
		outputChromosomeBasesFileName = fn;
		return this;
	}

	public String getOutputChromosomeBasesFileName(String id) {
		return directory + outputChromosomeBasesFileName.replace("?", id);
	}

	public Config setOutputExonBasesFileName(String fn) {
		outputExonBasesFileName = fn;
		return this;
	}

	public String getOutputExonBasesFileName(String id) {
		return directory + outputExonBasesFileName.replace("?", id);
	}

	public Config setOutputExonLocationsFileName(String fn) {
		outputExonLocationsFileName = fn;
		return this;
	}

	public String getOutputExonLocationsFileName(String id) {
		return directory + outputExonLocationsFileName.replace("?", id);
	}
	
	static public Config fromArgs(String[] args) {
		
		Config config = new Config();
		
		return config;
	}
}
