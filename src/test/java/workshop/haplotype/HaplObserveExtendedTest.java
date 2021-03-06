/**
 * This is a test case for DriverForHaplObserveExtended
 */
package workshop.haplotype;

import java.io.File;
import java.io.FileFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import junit.framework.TestCase;
import workshop.haplotype.collective.AddFamRelationCountry;
import workshop.haplotype.collective.CombineGLFile;
import workshop.haplotype.collective.FamSamRelationCountry;
import workshop.haplotype.collective.SeparateHapAndAllele;
import workshop.haplotype.frequency.ranking.HapTarget;
import workshop.haplotype.frequency.write.GenerateCountryHapGLstring;
import workshop.haplotype.frequency.write.GenerateGlobalGroupsHapCountTable;
import workshop.haplotype.frequency.write.GenerateHapFrequencyTableForHLAHapV;
import workshop.haplotype.organize.file.CreateDirectoryList;
import workshop.haplotype.write.GenerateFamSampleRelationHapValidCountryTable;
import workshop.haplotype.write.GenerateHaplotypeGLStringSummary;
import workshop.haplotype.write.GenerateHaplotypeSummary;
import workshop.haplotype.write.RunHaplObserveForMultipleFamilies;

/**
 * @author kazu
 * @version July 5 2018
 *
 */
public class HaplObserveExtendedTest extends TestCase {
	private static final DateFormat dateFormat;
	private static final Date date;		
	private static final String today;	
	private static final String global = "src/test/resources/";
	private static final String collective = global + "collective/";
	private static final String haplotype = global + "haplotype/";
	private static final CombineGLFile combined;
	private static final FamSamRelationCountry fsrc;
	
	static {
		dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		date = new Date();	
		today = dateFormat.format(date);
		combined = new CombineGLFile(collective);
		fsrc = new FamSamRelationCountry(combined);
	}
	
	@Test
	public void testHaplObserveExtended() {
		//testing core of HaplObserve, but testing multiple families
		String famcsv = global + "FAMCSV/";
		new RunHaplObserveForMultipleFamilies(collective, combined, famcsv, haplotype, today);
		
		// test SeparateHapAndAllele
		File hap = new File(haplotype);
		assertTrue(hap.isDirectory());
		
		File[] famFiles = hap.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().startsWith("FAM") 
                    && pathname.isDirectory();
            }
        });
		
		// Expecting FAM0001, FAM0002, FAM0003, FAM0009, FAM0013, FAM0222
		assertTrue(famFiles.length == 6);
		
		for (File famFile : famFiles) {
			File[] validationFiles = famFile.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.getName().contains("Validation");
				}
			});
			
			// Expecting at least one validation file...multiple if test directories not cleaned between runs...
			assertTrue(validationFiles.length >= 1);
			
			// Since inputs are always the same...just check the first one - outputs should be the same
			
			System.out.println(validationFiles[0].getAbsolutePath());
			SeparateHapAndAllele saa = new SeparateHapAndAllele(validationFiles[0].getAbsolutePath(), fsrc);
			
			assertNotNull(saa);
							
			switch (famFile.getName()) {
				case "FAM0001": validateFamSamRelation(saa, true, "OTHER"); break;
				case "FAM0002": validateFamSamRelation(saa, true, "EUR"); break;
				case "FAM0003": validateFamSamRelation(saa, true, "HIS"); break;
				case "FAM0009": validateFamSamRelation(saa, true, "ASI"); break;
				case "FAM0013": validateFamSamRelation(saa, true, "AFA"); break;
				case "FAM0222": validateFamSamRelation(saa, false, "EUR"); break;
				default: fail("Unexpected Family: " + famFile.getName()); break;
			}
		}
		
		
		// generate table that is easily reviewed by human
		// per family table
		new GenerateFamSampleRelationHapValidCountryTable(haplotype, "Validation", fsrc,
						"_Haplotype_Summary_Table_" + today + ".csv");		
		// combined summary table
		new GenerateHaplotypeSummary(haplotype, "Validation", fsrc, today);	
		
		//test AddFamRelationCountry
		for (File file : hap.listFiles()) {
			if (file.isDirectory()) {
				String fam = file.getAbsolutePath();
				File glFile = new File(fam);
				for (File test : glFile.listFiles()) {					
					if (test.getName().contains("Validation")) {
						System.out.println(test.getAbsolutePath());		// print absolute path
						AddFamRelationCountry arc = new AddFamRelationCountry(test.getAbsolutePath(), fsrc);
						for (List<String> list : arc.getFamSamRelationHapCountry()) {
							System.out.println(list);
						}	
					}
				}
			}
		}	
				
		// generate GL String table
		// this is for haplotype frequency
		new GenerateHaplotypeGLStringSummary(haplotype, haplotype , "Validation", fsrc, today);
		new GenerateHaplotypeGLStringSummary(haplotype, haplotype, "UnambiguousAllele", fsrc, today);
		new GenerateHaplotypeGLStringSummary(haplotype, haplotype, "TwoFieldAllele", fsrc, today);	
		
		
		// haplotype frequency
		// input files are generated above steps
		// separate by ethnic groups or countries
		new GenerateCountryHapGLstring(global, 
					haplotype + "FAM_Haplotype_Summary_GL_String_" + today + ".csv", fsrc, "FAM", today);
		new GenerateCountryHapGLstring(global, 
						haplotype + "UnambiguousAllele_Haplotype_Summary_GL_String_" + today + ".csv", 
						fsrc, "UnambiguousAllele", today);
		new GenerateCountryHapGLstring("src/test/resources/", 
						haplotype + "TwoFieldAllele_Haplotype_Summary_GL_String_" + today + ".csv",
						fsrc, "TwoFieldAllele", today);
		
		
		
		CreateDirectoryList cdl = new CreateDirectoryList(global);
		
		// Expecting EUR, HIS, OTHER, ASI, AFA
		List<String> expectedCountries = Arrays.asList("EUR", "HIS", "OTHER", "ASI", "AFA");
		assertTrue(cdl.getDirList().containsAll(expectedCountries));
		
		for (String str : cdl.getDirList()) {
			System.out.println(str);
		}
		
		for (String str : cdl.getInputFileNameList()) {
			System.out.println(str);
		}	
		System.out.println(cdl.getGlobalFam());
		
		
		HapTarget ht = new HapTarget();		// target haplotype		
		new File(global + "summary").mkdir();	// make summary dir
		for (int index = 0; index < ht.getHapTargetList().size(); index++) {	// go through target	
			String output = global + "summary/Global_" + ht.getNameList().get(index) + "_Haplotype_Summary_" + today + ".csv";
			new GenerateGlobalGroupsHapCountTable(global, 
					ht.getHapTargetList().get(index), output);
			
			// HLAHapV format
			new GenerateHapFrequencyTableForHLAHapV(global, ht.getHapTargetList().get(index), 
					ht.getNameList().get(index), today);	
		}
		
	}

	public void validateFamSamRelation(SeparateHapAndAllele saa, boolean validExpectation, String countryExpectation) {
		for (List<String> list : saa.getFamSamRelationHapCountry()) {	
			for (int count = 0; count < 2; count++) {	// family, sample, relation
				for (int index = 0; index < 3; index++) {
					System.out.println(list.get(index) + ",");										
				}
				for (String type : saa.getSampleAllele().get(list.get(1)).get(count)) {
					System.out.println(type + ",");
				}
				
				Boolean valid = new Boolean(list.get(4)); // validation
				assertTrue(valid == validExpectation);
				System.out.println(valid + ",");
				
				String country = list.get(5); // country
				
				assertTrue(country.equals(countryExpectation));
				System.out.println(country + "\n");	
			}							
		}
	}



}
