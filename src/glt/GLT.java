package glt;

import glt.CDS.MatchType;
import glt.CDS.Status;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.MatchResult;

import ml.options.OptionSet;
import ml.options.Options;
import ml.options.Options.Multiplicity;
import ml.options.Options.Separator;

public class GLT {

	public static Logger logger = Logger.getLogger("com.GLT");

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

	/**
	 * Converts the .fa files into .bases files needed for the Exon export.
	 */
	static private void convertFasta(Config config, String chromosomeId) {

		logger.info("Converting .fasta files");

		try {

			String fastaFileName = config.getInputChromosomeFastaFileName(chromosomeId);
			String basesFileName = config.getOutputChromosomeBasesFileName(chromosomeId);

			FileReader fr = new FileReader(fastaFileName);
			BufferedReader br = new BufferedReader(fr);

			FileWriter fw = new FileWriter(basesFileName);
			BufferedWriter bw = new BufferedWriter(fw);

			logger.info("Reading from:" + fastaFileName);
			logger.info("Writing to:" + basesFileName);

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

		logger.info("Converting .fasta files finished");
	}

	/**
	 * Converts the .fa files into .bases files needed for the Exon export.
	 */
	static private void convertReferenceFasta(Config config, String chromosomeId) {

		String in = config.getInputChromosomeFastaFileName("?");
		String out = config.getOutputChromosomeBasesFileName("?");
		
		// set the input and output files to be the reference ones
		config.setInputChromosomeFastaFileName(config.getReferenceChromosomeFastaFileName("?"));
		config.setOutputChromosomeBasesFileName(config.getReferenceChromosomeBasesFileName("?"));
		
		convertFasta(config, chromosomeId);
		
		config.setInputChromosomeFastaFileName(in);
		config.setOutputChromosomeBasesFileName(out);
	}

	/**
	 * Exports the exons into two files per chromosome.
	 * One containing the bases and the other one the locations
	 */
	static private void exportReferenceExons(Config config, Chromosome chromosome) {

		GLT.logger.info("Exporting reference exon-data");

		RandomAccessFile in = null;
		RandomAccessFile outBin = null;
		RandomAccessFile outLocations = null;

		String chromosomeId = chromosome.getId();
		
		try {

			// open the bases-file for that chromosome
			String chromosomeFileName = config.getReferenceChromosomeBasesFileName(chromosomeId);

			in = new RandomAccessFile(chromosomeFileName, "r");

			// create the output files
			String binFilename = config.getOutputChromosomeBasesFileName(chromosomeId);
			String locationsFilename = config.getOutputExonLocationsFileName(chromosomeId);

			outBin = new RandomAccessFile(binFilename, "rw");
			outLocations = new RandomAccessFile(locationsFilename, "rw");

			GLT.logger.info("Exporting Chromosome " + chromosomeId);

			// get all genes for the chromosome
			Collection<Gene> genes = chromosome.getGenes();

			for (Gene gene : genes) {

				// get all accessions for this gene
				Collection<Accession> accessions = gene.getAccessions();

				for (Accession accession : accessions) {

					// get all CCDSs for the accession
					Collection<CDS> cdss = accession.getCCDSs();

					for (CDS cds : cdss) {

						// only export the public ones
						// and disregard the withdrawn ones
						if (cds.isPublic()) {

							// get all exons for the ccds
							Collection<Exon> exons = cds.getExons();

							int currentWritePosition = 0;

							// loop over all the exons
							for (Exon exon : exons) {

								exon.setSourceFile(in);

								// always get the positive strand,
								// because that is the one we are going to compare with other FASTA files
								Bases bases = exon.getBases(Strand.Positive);
								
								// write the strand to the new file
								outBin.writeBytes(bases.getLetters());

								int length = bases.length();
								Strand strand = exon.getStrand();
								
								// write [from],[length] into index file
								String location = "" + currentWritePosition + "," + length + "," + strand.getSymbol() + "\n";
								outLocations.writeBytes(location);

								currentWritePosition += length;
							}
							
							int numberExons = exons.size();
							GLT.logger.info("Exported " + numberExons + " exons");
						}
					}
				}
			}
			
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
		GLT.logger.info("Exporting exon-data finished");
	}

	/**
	 * Compiles all exons from the file
	 * 
	 * @return ArrayList of Exon-objects
	 */
	static private ArrayList<Exon> compileExons(Config config, String chromosomeId) {

		logger.info("Compiling exon-data");

		String fileName = config.getExonsFileName();

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
					String chrId = columns[GLT.CCDSColumnIds.CHROMOSOME];
					
					if (chromosomeId == chrId) {
						
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
							logger.warning("CDS(" + ccdsId + " MatchType:'" + s
									+ "' unknoen.");
							break;
						}
	
						CDS cds = new CDS(ccdsId, status, accession, matchType);
	
						// if it is anything but partial,
						// then it contains some exons
						if (!cds.isPartial()) {
	
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
								Exon exon = Exon.factory(start, stop, cds);
	
								exons.add(exon);
							}
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
	 * Locates the exons in the input bases file.
	 */
	static private void locateExons(Config config, Chromosome chromosome) {

		GLT.logger.info("Locating exons");

		String chromosomeId = chromosome.getId();

		try {

			// open the reference genome FASTA for reading
			String referenceChromosomeFileName = config.getReferenceChromosomeBasesFileName(chromosomeId);
			RandomAccessFile referenceChromosomeIn = new RandomAccessFile(referenceChromosomeFileName, "r");

			// open the to be scanned .bases file
			String chromosomeFileName = config.getInputChromosomeBasesFileName(chromosomeId);

			GLT.logger.info("Scanning Chromosome " + chromosomeId);

			// get all genes for the chromosome
			Collection<Gene> genes = chromosome.getGenes();

			int foundNumberExons = 0;
			int geneN = 0;
			int genesCount = genes.size();
			
			for (Gene gene : genes) {

				geneN++;
				
				// get all accessions for this gene
				Collection<Accession> accessions = gene.getAccessions();

				for (Accession accession : accessions) {

					// store all the exons that could not be found,
					// so we can remove them from the CDS
					Set<Exon> notFoundExons = new HashSet<Exon>();
					
					// get all CCDSs for the accession
					Collection<CDS> cdss = accession.getCCDSs();

					for (CDS cds : cdss) {

						// only work the public ones
						// and disregard the withdrawn ones
						if (cds.isPublic()) {

							// get all exons for the ccds
							Collection<Exon> exons = cds.getExons();

							String msg = null;
							int exonN = 0;
							int exonsCount = exons.size();
							
							// loop over all the exons
							for (Exon exon : exons) {

								exonN++;

								exon.setSourceFile(referenceChromosomeIn);

								// always get the positive strand,
								// because that is the one we are going to compare with other FASTA files
								Bases positiveBases = exon.getBases(Strand.Positive);
								String letters = positiveBases.getLetters();
								
								msg = String.format("Scanning chromosome(%s):Gene(%s) %d/%d:Exon %d/%d", 
										chromosome.getId(), gene.getName(), geneN, genesCount, exonN, exonsCount);
								GLT.logger.info(msg);

								// @TODO: there must be a better way than opening the file all the time??
								// we could load it completely into memory first,
								// however this won't work on small machines, because the chromosomes are fairly huge
								File chromosomeFile = new File(chromosomeFileName);  
								FileInputStream chromosomeFIS = new FileInputStream(chromosomeFile);  
								FileChannel chromosomeFC = chromosomeFIS.getChannel();  

								// see: http://stackoverflow.com/a/4338841
								// apparently .findWithinHorizon() already uses BoyerMoore internally.
								Scanner s = new Scanner(chromosomeFile);
								
						        if (s.findWithinHorizon(letters, 0) != null) {
						        	
						            MatchResult mr = s.match();
						            int startInChromosome = mr.start();

						            // set the new start based on where it is in the input file
						            exon.setFrom(startInChromosome);

						            foundNumberExons++;
						            
						        } else {
						        	
						        	notFoundExons.add(exon);
						        	
						        	msg = "Could not find exon:" + letters;
						        	logger.info(msg);
						        }
						        
						        s.close();
								
								chromosomeFC.close();
								chromosomeFIS.close();
							}
						}
					}
					
					// anything to throw away?
					if (!notFoundExons.isEmpty()) {
						
						logger.info("Could not locate " + notFoundExons.size() + " exons. Removing them from the CDS");
						
						// remove all the ones we could not find,
						// so the data is clean
						for (Exon exon : notFoundExons) {
							exon.remove();
						}
					}
				}
			}
			
			GLT.logger.info("Found " + foundNumberExons + " exons");
			
		} catch (FileNotFoundException e) {
			System.err.println("Could not open file");
			System.err.println(e.getLocalizedMessage());
		} catch (IOException e) {
			System.err.println("I/O error");
			System.err.println(e.getLocalizedMessage());
		}

		GLT.logger.info("Locating exons finished");
	}
	
	/**
	 * Exports the exons into two files per chromosome.
	 * One containing the bases and the other one the locations
	 */
	static private void exportExons(Config config, Chromosome chromosome) {

		GLT.logger.info("Exporting exon-data");

		RandomAccessFile referenceChromosomeIn = null;
		RandomAccessFile outBases = null;
		RandomAccessFile outLocations = null;

		String chromosomeId = chromosome.getId();

		try {

			// open the reference genome FASTA for reading
			String referenceChromosomeFileName = config.getReferenceChromosomeBasesFileName(chromosomeId);
			referenceChromosomeIn = new RandomAccessFile(referenceChromosomeFileName, "r");

			// open the to be scanned .bases file
			String chromosomeFileName = config.getInputChromosomeBasesFileName(chromosomeId);

			File chromosomeFile = new File(chromosomeFileName);  
			FileInputStream chromosomeFIS = new FileInputStream(chromosomeFile);  
			FileChannel chromosomeFC = chromosomeFIS.getChannel();  
			
			// create the output files
			String outputBasesFilename = config.getOutputExonBasesFileName(chromosomeId);
			String outputLocationsFilename = config.getOutputExonLocationsFileName(chromosomeId);

			outBases = new RandomAccessFile(outputBasesFilename, "rw");
			outLocations = new RandomAccessFile(outputLocationsFilename, "rw");

			GLT.logger.info("Exporting Chromosome " + chromosomeId);

			int exportedNumberExons = 0;

			// get all genes for the chromosome
			Collection<Gene> genes = chromosome.getGenes();

			for (Gene gene : genes) {

				// get all accessions for this gene
				Collection<Accession> accessions = gene.getAccessions();

				for (Accession accession : accessions) {

					// get all CCDSs for the accession
					Collection<CDS> cdss = accession.getCCDSs();

					for (CDS cds : cdss) {

						// only export the public ones
						// and disregard the withdrawn ones
						if (cds.isPublic()) {

							// get all exons for the ccds
							Collection<Exon> exons = cds.getExons();

							int currentWritePosition = 0;

							// loop over all the exons
							for (Exon exon : exons) {

								exon.setSourceFile(referenceChromosomeIn);

								Bases bases = exon.getBases();

								// write the strand to the new file
								outBases.writeBytes(bases.getLetters());

								int from = exon.getFrom();
								int length = bases.length();
								
								// write [from],[length] into index file
								String location = "" + currentWritePosition + "," + length + "," + from + "\n";
								outLocations.writeBytes(location);

								currentWritePosition += length;
								
								exportedNumberExons++;
							}
						}
					}
				}
			}
			
			chromosomeFC.close();
			chromosomeFIS.close();
			referenceChromosomeIn.close();
			outLocations.close();
			outBases.close();

			GLT.logger.info("Exported " + exportedNumberExons + " exons");

		} catch (FileNotFoundException e) {
			System.err.println("Could not open file");
			e.getLocalizedMessage();
		} catch (IOException e) {
			System.err.println("I/O error");
			e.getLocalizedMessage();
		}

		GLT.logger.info("Exporting exon-data finished");
	}

	/**
	 * Exports all exons from the file provided by RB
	 * 
	 * @return ArrayList of Exon-objects
	 */
	static private void exportRBExons(Config config, String chromosomeId) {

		logger.info("Exporting exon-data");

		int numberExons = 0;

		try {

			String fastaFileName = config.getInputChromosomeFastaFileName(chromosomeId);

			FileReader fr = new FileReader(fastaFileName);
			BufferedReader br = new BufferedReader(fr);

			// create the output files
			String outputBasesFilename = config.getOutputExonBasesFileName(chromosomeId);
			String outputLocationsFilename = config.getOutputExonLocationsFileName(chromosomeId);

			RandomAccessFile outBases = new RandomAccessFile(outputBasesFilename, "rw");
			RandomAccessFile outLocations = new RandomAccessFile(outputLocationsFilename, "rw");
			
			logger.info("Reading from:" + fastaFileName);

			boolean first = true;
			
			// @TODO: really not the best code.
			// we should have a class holding this details, but it's one day before leaving to the desert
			List<String> outLines = new ArrayList<String>();
			// the position in the exon.bases file
			List<Integer> startPositions = new ArrayList<Integer>();
			// the length of the exon
			List<Integer> lengths = new ArrayList<Integer>();
			// the position where it has been found in the chromosome
			List<Integer> sourcePositions = new ArrayList<Integer>();
			
			String outLine = "";
			String location = "";
			int startPosition = 0;
			int length = 0;
			int sourcePosition = 0;
			
			while (br.ready()) {

				String inLine = br.readLine();

				// only use it if it is a proper letter
				if ((inLine.length() > 0) && Character.isLetter(inLine.codePointAt(0))) {
					outLine = outLine + inLine;
				} else {

					// ignore the first comment
					if (!first && (outLine.length() > 0)) {

						length = outLine.length();

						outLines.add(outLine);
						startPositions.add(startPosition);
						lengths.add(length);
						sourcePositions.add(sourcePosition);
						
						startPosition += length;
						
						outLine = "";
						numberExons++;
					}

					// it is a comment like
					// >AVC9O:00150:00849
					// so get the numbers from there
					String[] meta = inLine.split(":");
					// make up a fictional position in the chromosome
					sourcePosition = Integer.parseInt(meta[1]) * 10000 + Integer.parseInt(meta[2]);

					first = false;
				}
			}

			// write them all out in one go
			for (int i = 0; i < numberExons; i++) {

				// write the strand to the new file
				outBases.writeBytes(outLines.get(i));

				// write [from],[length] into index file
				location = "" + startPositions.get(i) + "," + lengths.get(i) + "," + sourcePositions.get(i) + "\n";
				outLocations.writeBytes(location);
			}
			
			br.close();
			fr.close();
			outBases.close();
			outLocations.close();

		} catch (FileNotFoundException e) {
			System.err.println("Could not open file");
			e.getLocalizedMessage();
		} catch (IOException e) {
			System.err.println("I/O error");
			e.getLocalizedMessage();
		}

		GLT.logger.info("Found " + numberExons + " exons in chromosome " + chromosomeId);
		GLT.logger.info("Exporting exon-data finished");
	}

	/**
	 * Remove any duplicate exons which might be there due to the CCDS file
	 */
	static private void removeDuplicateExons(Config config, Chromosome chromosome) {

		GLT.logger.info("Removing duplicate exons");

		// get all genes for the chromosome
		Collection<Gene> genes = chromosome.getGenes();

		// a container holding all Exons for the Chromosome
		// SortedMap<Exon, Exon> exportableExons = new TreeMap<Exon,
		// Exon>();
		SortedSet<Exon> uniqueExons = new TreeSet<Exon>();
		Set<Exon> duplicateExons = new HashSet<Exon>();

		for (Gene gene : genes) {

			// get all accessions for this gene
			Collection<Accession> accessions = gene.getAccessions();

			for (Accession accession : accessions) {

				// get all CCDSs for the accession
				Collection<CDS> cdss = accession.getCCDSs();

				for (CDS cds : cdss) {

					// get all exons for the ccds
					Collection<Exon> exons = cds.getExons();

					// loop over all the exons
					for (Exon exon : exons) {

						// do we have this exon already?
						if (uniqueExons.contains(exon)) {

							duplicateExons.add(exon);
							
						} else {

							// add it, so we know we have already come across it
							uniqueExons.add(exon);
						}
					}
				}
			}
		}
		
		// throw away all the duplicated ones
		for (Exon exon : duplicateExons) {
			exon.remove();
		}
		
		GLT.logger.info("Removed " + duplicateExons.size() + " duplicates; " + uniqueExons.size() + " remaining");
	}
	
	/**
	 * Shows the help and usage
	 * 
	 * @param operations
	 *            All supported operations
	 */
	static private void showHelp() {

		ArrayList<String> help = new ArrayList<String>();

		help.add("GenomeLaserTools v2013.08.20");
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
		help.add("java GLT [OPTIONS] arg1 arg2 ...");
		help.add("(Note: generated files are saved in the working directory)");
		help.add("");
		help.add("-cf - convert FASTA to the fileformat required by the tool. Suggested is \".bases\".");
		help.add("      arg1: chromosome FASTA-filename template");
		help.add("      arg2: chromomsome GLT-filename template");
		help.add("      A ? in the filename template is replaced with the chromosome ID being processed");
		help.add("-ee - extract Exons.");
		help.add("      arg1: Reference chromosome GLT-filename template");
		help.add("      arg2: source chromosome GLT-filename templatee");
		help.add("      arg3: output exon.bases filename template");
		help.add("      arg4: output exon.locations filename template");
		help.add("      A ? in the filename template is replaced with the chromosome ID being processed");
		help.add("-rb - extract Exons from RB files.");
		help.add("      arg1: source chromosome FASTA-filename templatee");
		help.add("      arg2: output exon.GLT filename template");
		help.add("      arg3: output exon.locations filename template");
		help.add("      A ? in the filename template is replaced with the chromosome ID being processed");
		help.add("");
		help.add("-d - set the working directory where to find/store the files.");
		help.add("     Wrap it with quotation marks to be on the safe side.");
		help.add("     Add a trailing /");
		help.add("     Defaults to the current directory.");
		help.add("-c - set the list of chromosomes that should be worked on. Defaults to all chromosomes.");
		help.add("     Wrap it with quotation marks to be on the safe side.");
		help.add("     e.g. \"1,2,20,21,X\"");
		help.add("     Defaults to all chomosomes 1..22,X,Y.");
		help.add("");
		help.add("Examples:");
		help.add("Convert FASTA to internal format. Files are in a particular directory. Process chromosomes 1, 3 and X only:");
		help.add("-d \"/home/michael/GLT/resources/\" -c \"1,3,X\" -cf \"hs_ref_GRCh37.p10_chr?.fa\" \"hs_ref_GRCh37.p10_chr?.bases\"");
		help.add("");
		help.add("Extract convert FASTA to internal format. Files are in a particular directory. Process all chromosomes:");
		help.add("-d \"/home/michael/GLT/resources/\" -ee \"hs_ref_GRCh37.p10_chr?.bases\" \"chr?.bases\" \"chr?.exon.bases\" \"chr?.locations\"");
		help.add("");
		help.add("Extract data from the RB files:");
		help.add("-d \"/home/michael/GLT/resources/\" -rb \"chr?.fa\" \"chr?.exon.bases\" \"chr?.locations\"");

		help.add("");
		help.add("No sharks are harmed running this software.");

		for (String h : help) {
			System.out.println(h);
		}
	}

	static public void main(String[] arguments) {

		String msg = null;

		// create the configuration
		Config config = new Config();

		// see:
		// http://www.javaworld.com/javaworld/jw-08-2004/jw-0816-command.html?page=5
		Options opt = new Options(arguments, 2);
		
		// convert fasta: arg1 = fasta files, arg2 = bases files
		// c = comma separated list of chromosomes e.g. "1,3,18,X" 
		opt.addSet("cfset", 2).addOption("cf").addOption("c", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
		// extract exons: arg1 = reference bases, arg2 = input bases, arg3 = output exon.bases, arg4 = output exon.locations
		// c = comma separated list of chromosomes e.g. "1,3,18,X"
		opt.addSet("eeset", 4).addOption("ee").addOption("c", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
		// extracts exons from the file provided by RB
		// arg2 = input .fa, arg2 = output exon.bases, arg3 = output exon.locations
		opt.addSet("rbset", 3).addOption("rb").addOption("c", Separator.BLANK, Multiplicity.ZERO_OR_ONE);

		// add -d to all of them
		opt.addOptionAllSets("d", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
		
		OptionSet set = opt.getMatchingSet();
		if (set == null) {
			GLT.showHelp();
			// Print usage hints
			System.exit(1);
		}
		
		// set the directory
		if (set.isSet("d")) {
			String directory = set.getOption("d").getResultValue(0);
			config.setDirectory(directory);
		}

		msg = "Working directory:" + config.getDirectory();
		GLT.logger.info(msg);

		// set the chromosomeIds
		if (set.isSet("c")) {
			String chromosomeIds = set.getOption("c").getResultValue(0);
			List<String> ids = new ArrayList<String>(Arrays.asList(chromosomeIds.split(",")));
			config.setChromosomeIds(ids);
		}

		msg = "Working with chromosomes " + config.getChromosomeIds().toString();
		GLT.logger.info(msg);
		
		int i = 0;

		// Evaluate the different option sets
		if (set.getSetName().equals("cfset")) {
		  
			// set the different file name masks
			config.setInputChromosomeFastaFileName(set.getData().get(i++));
			config.setOutputChromosomeBasesFileName(set.getData().get(i++));

			for (String id : config.getChromosomeIds()) {
				GLT.convertFasta(config, id);
			}
		}
		
		if (set.getSetName().equals("eeset")) {
			
			// set the different file name masks
			config.setReferenceChromosomeBasesFileName(set.getData().get(i++));
			config.setInputChromosomeBasesFileName(set.getData().get(i++));
			config.setOutputExonBasesFileName(set.getData().get(i++));
			config.setOutputExonLocationsFileName(set.getData().get(i++));

			for (String id : config.getChromosomeIds()) {
	
				GLT.compileExons(config, id);
				
				Chromosome chromosome = Chromosome.get(id);
				
				// we may have picked up duplicate ones, so throw those away first,
				// because it is unnecessary to process them twice
				GLT.removeDuplicateExons(config, chromosome);
				
				// find them in the input file
				GLT.locateExons(config, chromosome);
				
				// and throw the duplicated ones away again for good measure
				GLT.removeDuplicateExons(config, chromosome);
				
				GLT.exportExons(config, chromosome);
			}
		}
		
		if (set.getSetName().equals("rbset")) {
			
			// set the different file name masks
			config.setInputChromosomeFastaFileName(set.getData().get(i++));
			config.setOutputExonBasesFileName(set.getData().get(i++));
			config.setOutputExonLocationsFileName(set.getData().get(i++));

			for (String id : config.getChromosomeIds()) {
	
				GLT.exportRBExons(config, id);
			}
		}
	}
}
