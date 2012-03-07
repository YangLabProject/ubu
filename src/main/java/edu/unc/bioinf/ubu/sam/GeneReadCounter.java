package edu.unc.bioinf.ubu.sam;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.samtools.SAMRecord;

/**
 * Counts total and unique hits for reads against a given gene.
 * The input bam file must be generated by GenomeToTranscriptome with XG tags included.
 * 
 * @author Lisle Mose (lmose at unc dot edu)
 */
public class GeneReadCounter {
	
	private Map<String, Long> totalGeneCounts = new HashMap<String, Long>(); 
	private Map<String, Long> uniqueGeneCounts = new HashMap<String, Long>();
	
	private IsoformGeneMap isoformGeneMap = new IsoformGeneMap();
	
	private static final Long ONE = (long) 1;
	private static final Long ZERO = (long) 0;

	public void count(IsoformGeneMap isoformGeneMap, String inputSam, String outputFile) throws IOException {
		this.isoformGeneMap = isoformGeneMap;
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false));
		
		countGenes(inputSam);
		outputCounts(writer);
		
		writer.close();
	}
	
	private void countGenes(String inputFile) {
		SamMultiMappingReader reader = new SamMultiMappingReader(inputFile);
		
		// For each read
		for (List<SAMRecord> reads : reader) {
			
			Set<String> genes = new HashSet<String>();
			boolean isUnique = true;
			String prevGenomeCoords = null;

			for (SAMRecord read : reads) {
				String isoform = read.getReferenceName();
				genes.add(isoformGeneMap.getGene(isoform));
				String genomeCoords = read.getStringAttribute("XG");
				
				if ((prevGenomeCoords != null) && (!genomeCoords.equals(prevGenomeCoords))) {
					isUnique = false;
				}
				
				prevGenomeCoords = genomeCoords;
			}

			for (String gene : genes) {
				incrementCount(totalGeneCounts, gene);
				
				if (isUnique) {
					incrementCount(uniqueGeneCounts, gene);
				}
			}
		}
	}
	
	private void outputCounts(BufferedWriter writer) throws IOException {
		List<String> genes = isoformGeneMap.getSortedGeneList();
		
		for (String gene : genes) {
			StringBuffer line = new StringBuffer();
			
			long totalCount = getCount(totalGeneCounts, gene);
			long uniqueCount = getCount(uniqueGeneCounts, gene);
			
			line.append(gene);
			line.append('\t');
			line.append(totalCount);
			line.append('\t');
			line.append(uniqueCount);
			
			line.append('\n');
			
			writer.write(line.toString());
		}
	}
	
	private void incrementCount(Map<String, Long> counts, String gene) {
		Long count = counts.get(gene);
		if (count == null) {
			counts.put(gene, ONE);
		} else {
			counts.put(gene, count + 1);
		}
	}
	
	private long getCount(Map<String, Long> counts, String gene) {
		Long count = counts.get(gene);
		if (count == null) {
			count = ZERO;
		}
		return count;
	}
	
	public static void main(String[] args) throws IOException {
		String isoformGeneFile = args[0];
		String input = args[1];
		String output = args[2];
		
//		String isoformGeneFile = "/home/lisle/gaf/ref/gaf.knownToLocus";
//		String input = "/home/lisle/data/gene_counts/small.bam";
//		String output = "/home/lisle/data/gene_counts/output.tsv";

		long s = System.currentTimeMillis();
				
		IsoformGeneMap isoformGeneMap = new IsoformGeneMap();
		isoformGeneMap.init(isoformGeneFile);
		
		new GeneReadCounter().count(isoformGeneMap, input, output);
		
		long e = System.currentTimeMillis();
		
		System.out.println("Done.  Elapsed secs: " + (e-s)/1000);
	}
}
