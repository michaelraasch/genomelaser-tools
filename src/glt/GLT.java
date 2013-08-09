package glt;

import glt.CCDS.MatchType;
import glt.CCDS.Status;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

public class GLT {

	public static Logger logger = Logger.getLogger("com.GLT");

	/**
	 * Helper-class for one option e.g. convert the FASTA files
	 * 
	 * @author michael
	 * 
	 */
	static private class Option {

		private String s = null; // the short command
		private String l = null; // the long command
		public String help = null;

		public Option(String s, String l, String help) {
			this.s = "-" + s;
			this.l = "--" + l;
			this.help = help;
		}

		public String getShort() {
			return this.s;
		}

		public String getLong() {
			return this.l;
		}

		public String getHelp() {
			return this.help;
		}

		public boolean isOption(String arg) {
			return this.s.equals(arg) || this.l.equals(arg);
		}

		public boolean isOption(Set<String> args) {
			boolean isOption = false;
			for (String arg : args) {
				isOption |= this.isOption(arg);
			}
			return isOption;
		}
	}

	/**
	 * The CCDS file structure
	 * 
	 * @author michael
	 * 
	 */
	static private class CCDSColumnIds {
		static private final int CHROMOSOME = 0;
		static private final int NC_ACCESSION = CHROMOSOME + 1;
		static private final int GENE = NC_ACCESSION + 1;
		static private final int GENE_ID = GENE + 1;
		static private final int CCDS_ID = GENE_ID + 1;
		static private final int CCDS_STATUS = CCDS_ID + 1;
		static private final int CDS_STRAND = CCDS_STATUS + 1;
		static private final int CDS_FROM = CDS_STRAND + 1;
		static private final int CDS_TO = CDS_FROM + 1;
		static private final int CDS_LOCATIONS = CDS_TO + 1;
		static private final int MATCH_TYPE = CDS_LOCATIONS + 1;
	}

	// Directory containing the FASTA and CCDS source files
	static private String directory = null;

	static private String exonsFileName() {
		return directory + "CCDS.current.txt";
	}

	static private String chromosomeFastaFileName(String id) {
		return "chromosome." + id.toLowerCase() + ".fa";
	}

	static private String chromosomeBinFileName(String id) {
		return "chromosome." + id.toLowerCase() + ".bases";
	}

	static private String exonBinFileName(String id) {
		return "exons." + id.toLowerCase() + ".bases";
	}

	static private String exonLocationsFileName(String id) {
		return "exons." + id.toLowerCase() + ".locations";
	}

	/**
	 * Converts the .fasta files into .bin files needed for the Exon export.
	 */
	static private void convertFasta() {

		logger.info("Converting .fasta files");

		ArrayList<String> chromosomeIds = new ArrayList<String>();
		for (int i = 1; i <= 22; i++) {
			chromosomeIds.add(Integer.toString(i));
		}
		chromosomeIds.add("X");
		chromosomeIds.add("Y");

		for (String id : chromosomeIds) {

			try {

				String fastaFileName = directory
						+ GLT.chromosomeFastaFileName(id);
				String binFileName = directory + GLT.chromosomeBinFileName(id);

				FileReader fr = new FileReader(fastaFileName);
				BufferedReader br = new BufferedReader(fr);

				FileWriter fw = new FileWriter(binFileName);
				BufferedWriter bw = new BufferedWriter(fw);

				logger.info("Reading from:" + fastaFileName);
				logger.info("Writing to:" + binFileName);

				while (br.ready()) {

					String line = br.readLine();

					// only use it if it is a proper letter
					if ((line.length() > 0)
							&& Character.isLetter(line.codePointAt(0))) {
						bw.write(line);
					}
				}

				bw.close();
				br.close();
				fw.close();
				fr.close();

			} catch (FileNotFoundException e) {
				System.err.println("Could not open file");
				e.getLocalizedMessage();
			} catch (IOException e) {
				System.err.println("I/O error");
				e.getLocalizedMessage();
			}
		}

		logger.info("Converting .fasta files finished");
	}

	/**
	 * Compiles all exons from the file
	 * 
	 * @return ArrayList of Exon-objects
	 */
	static private ArrayList<Exon> compileExons() {

		logger.info("Compiling exon-data");

		String fileName = exonsFileName();

		ArrayList<Exon> exons = new ArrayList<Exon>();

		try {

			FileReader fr = new FileReader(fileName);
			BufferedReader in = new BufferedReader(fr);

			String s = null;

			while (in.ready()) {

				String line = in.readLine();

				// disregard any comments
				if (!line.startsWith("#")) {

					String[] columns = line.split("\\t");

					// get the Chromosome
					String chromosomeId = columns[GLT.CCDSColumnIds.CHROMOSOME];
					Chromosome chromosome = Chromosome.factory(chromosomeId);

					// get the Strand
					String strand = columns[GLT.CCDSColumnIds.CDS_STRAND];

					// get the Gene
					String geneName = columns[GLT.CCDSColumnIds.GENE];
					String geneId = columns[GLT.CCDSColumnIds.GENE_ID];
					Gene gene = Gene.factory(geneId, geneName, strand,
							chromosome);

					// get the Accession
					String accessionId = columns[GLT.CCDSColumnIds.NC_ACCESSION];
					Accession accession = Accession.factory(accessionId, gene);

					// get the CCDS ID
					String ccdsId = columns[GLT.CCDSColumnIds.CCDS_ID];

					// get the Status
					Status status = null;
					s = columns[GLT.CCDSColumnIds.CCDS_STATUS].toLowerCase();
					switch (s) {
					case "public":
						status = Status.Public;
						break;
					case "withdrawn":
						status = Status.Withdrawn;
						break;
					default:
						status = Status.Reviewed;
						break;
					}

					// get the MatchType
					MatchType matchType = null;
					s = columns[GLT.CCDSColumnIds.MATCH_TYPE].toLowerCase();
					switch (s) {
					case "identical":
						matchType = MatchType.Identical;
						break;
					case "partial":
						matchType = MatchType.Partial;
						break;
					default:
						logger.warning("CCDS(" + ccdsId + " MatchType:'" + s
								+ "' unknoen.");
						break;
					}

					CCDS ccds = new CCDS(ccdsId, status, accession, matchType);

					// if it is anything but partial,
					// then it contains some exons
					if (!ccds.isPartial()) {

						// get all exons
						String locations = columns[GLT.CCDSColumnIds.CDS_LOCATIONS];

						// cut off the leading "[" and trailing "]"
						locations = locations.substring(1,
								locations.length() - 1);

						// throw away any blanks
						locations = locations.replace(" ", "");

						// split them into individual locations
						String[] startstops = locations.split(",");

						for (String startstop : startstops) {

							String[] ss = startstop.split("-");
							int start = Integer.parseInt(ss[0]);
							int stop = Integer.parseInt(ss[1]);

							// create the exon
							Exon exon = Exon.factory(start, stop, ccds);

							exons.add(exon);
						}
					}
				}
			}

			in.close();
			fr.close();

		} catch (FileNotFoundException e) {
			System.err.println("Could not open file");
			e.getLocalizedMessage();
		} catch (IOException e) {
			System.err.println("I/O error");
			e.getLocalizedMessage();
		}

		GLT.logger.info("Found " + exons.size() + " exons in all chromosomes");
		GLT.logger.info("Compiling exon-data finished");

		return exons;
	}

	/**
	 * Exports the exons into two files per chromosome exons.[n].bin
	 * exons.[n].locations where n is the chromosome ID 1..22,X,Y
	 */
	static private void exportExons() {

		GLT.logger.info("Exporting exon-data");

		// get all Chromosomes
		Collection<Chromosome> chromosomes = Chromosome.getAll();

		// loop over all the chromosomes
		for (Chromosome chromosome : chromosomes) {

			String chromosomeId = chromosome.getId();

			if (chromosomeId.equals("Y")) {
				int a = 0;
				a=a+3;
			}
			

			GLT.logger.info("Exporting Chromosome " + chromosomeId);

			// get all genes for the chromosome
			Collection<Gene> genes = chromosome.getGenes();

			// a container holding all Exons for the Chromosome
			// SortedMap<Exon, Exon> exportableExons = new TreeMap<Exon,
			// Exon>();
			SortedSet<Exon> exportableExons = new TreeSet<Exon>();
			for (Gene gene : genes) {

				// get all accessions for this gene
				Collection<Accession> accessions = gene.getAccessions();

				for (Accession accession : accessions) {

					// get all CCDSs for the accession
					Collection<CCDS> ccdss = accession.getCCDSs();

					for (CCDS ccds : ccdss) {

						// only export the public ones
						// and disregard the withdrawn ones
						if (ccds.isPublic()) {

							// get all exons for the ccds
							Collection<Exon> exons = ccds.getExons();

							// loop over all the exons
							for (Exon exon : exons) {

								// there are multiple CCDSs which
								// contain the same Exon,
								// so ignore any Exons which we have
								// already picked up
								/*
								 * String exonId = exon.getId(); if
								 * (!exportableExons.containsKey(exon)) {
								 * exportableExons.put(exonId, exon); }
								 */
								exportableExons.add(exon);
							}
						}
					}
				}
			}

			RandomAccessFile in = null;
			RandomAccessFile outBin = null;
			RandomAccessFile outLocations = null;

			try {

				// open the bin-file for that chromosome
				String chromosomeFileName = directory
						+ GLT.chromosomeBinFileName(chromosomeId);

				in = new RandomAccessFile(chromosomeFileName, "r");

				// create the output files
				String binFilename = directory
						+ GLT.exonBinFileName(chromosomeId);
				String locationsFilename = directory
						+ GLT.exonLocationsFileName(chromosomeId);

				outBin = new RandomAccessFile(binFilename, "rw");
				outLocations = new RandomAccessFile(locationsFilename, "rw");

				int currentWritePosition = 0;

				// loop over all the exportable Exons.
				// the values are returned already sorted
				for (Exon exon : exportableExons) {

					exon.setSourceFile(in);
					Bases bases = exon.getBases();
					
					// write the strand to the new file
					outBin.writeBytes(bases.getLetters());

					int length = bases.length();
					
					// write [from],[length] into index file
					String location = "" + currentWritePosition + "," + length
							+ "\n";
					outLocations.writeBytes(location);

					currentWritePosition += length;
				}

				int numberExons = exportableExons.size();
				GLT.logger.info("Exported " + numberExons + " exons");

				in.close();
				outLocations.close();
				outBin.close();

			} catch (FileNotFoundException e) {
				System.err.println("Could not open file");
				e.getLocalizedMessage();
			} catch (IOException e) {
				System.err.println("I/O error");
				e.getLocalizedMessage();
			}
		}

		GLT.logger.info("Exporting exon-data finished");
	}

	/**
	 * Shows the help and usage
	 * 
	 * @param operations
	 *            All supported operations
	 */
	static private void showHelp(ArrayList<Option> operations) {

		ArrayList<String> help = new ArrayList<String>();

		help.add("GenomeLaserTools v2013.08.09");
		help.add("the cool stuff for Burning Man 2013 by Alex, Michael, Neil and Vincent");
		help.add("");
		help.add("Get the reference human genome fasta files from");
		help.add("ftp://ftp.ncbi.nlm.nih.gov/genomes/H_sapiens/Assembled_chromosomes/seq/hs_ref_GRCh37.p10_chr?.fa.gz");
		help.add("with ? = 1..22 and X and Y");
		help.add("and unzip them into the source-directory");
		help.add("");
		help.add("Get the CDDS meta-data from");
		help.add("ftp://ftp.ncbi.nlm.nih.gov/pub/CCDS/current_human/CCDS.current.txt");
		help.add("and save it in the source-directory");
		help.add("");
		help.add("Usage:");
		help.add("java GLT [OPTIONS]... DIRECTORY-CONTAINING-FASTA-AND-CCDS-SOURCE-FILES");
		help.add("(Note: generated files are saved in the source-directory)");
		help.add("");

		for (Option op : operations) {
			help.add(op.getShort() + ", " + op.getLong() + " " + op.getHelp());
		}

		help.add("");
		help.add("No sharks are harmed running this software.");

		for (String h : help) {
			System.out.println(h);
		}
	}

	static public void main(String[] arguments) {

		ArrayList<Option> options = new ArrayList<Option>();

		// the different opttions we support
		Option opExtractExons = new Option("ee", "extractExons",
				"Extract the exons into one file and the indices into another");
		Option opConvertFasta = new Option("cf", "convertFasta",
				"Converts the .fasta files to the .bin files required to extract the exons");
		Option opHelp = new Option("h", "help", "Show the help");

		options.add(opConvertFasta);
		options.add(opExtractExons);
		options.add(opHelp);

		// line up the arguments
		Set<String> args = new HashSet<String>();
		for (String arg : arguments) {
			args.add(arg);

			// just always set the directory,
			// because it is the last argument
			directory = arg;
		}

		boolean executedOp = false;

		// loop over all the options,
		// so we can make sure we execute them in the right order
		// e.g. comvert Fasta before extracting the data
		for (Option option : options) {

			if (option.equals(opHelp) && opHelp.isOption(args)) {
				GLT.showHelp(options);
				executedOp = true;
			}

			if (option.equals(opConvertFasta) && opConvertFasta.isOption(args)) {
				GLT.convertFasta();
				executedOp = true;
			}

			if (option.equals(opExtractExons) && opExtractExons.isOption(args)) {
				GLT.compileExons();
				GLT.exportExons();
				executedOp = true;
			}
		}

		// did we execute anything? if not, then print out the help
		if (!executedOp) {
			GLT.showHelp(options);
		}
	}
}
