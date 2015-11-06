package rca;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.IntegerGene;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.openqa.selenium.Point;
import org.xml.sax.SAXException;

import util.Util;
import websee.HtmlElement;
import websee.ImageProcessing;
import config.Constants;

public class GAFitnessFunction extends FitnessFunction
{
	private Logger log;

	private static final long serialVersionUID = 1L;
	private HtmlElement element;
	private String visualProperty; // CSS -> style:<property_name>:<unit>, HTML attribute -> <attribute_name>
	private String[] testPagePathAndName;
	private String newGADir;
	private String oracleFullPath;
	private double visualPropertyValueWithoutUnits;
	private String visualPropertyValue;

	private Map<String, Integer> cloneDPMap; // contains the difference pixels in the given element
	private Map<String, Integer> cloneFullDPMap;  // contains all the difference pixels
	private Map<String, Double> cloneDPColorMap;

	private String analysisType;
	
	private String oracleAverageColor;

	private List<Point> pixelsInElement;

	private List<Point> originalDifferencePixels;
	
	public List<Point> getPixelsInElement()
	{
		return pixelsInElement;
	}

	public void setPixelsInElement(List<Point> pixelsInElement)
	{
		this.pixelsInElement = pixelsInElement;
	}

	public void setOracleColor(String oracleColor)
	{
		this.oracleAverageColor = oracleColor;
	}

	public String getOracleColor()
	{
		return oracleAverageColor;
	}

	public GAFitnessFunction(HtmlElement element, String visualProperty, String oracleFullPath, String testFileFullPath, double visualPropertyValueWithoutUnits, Logger log)
	{
		this.element = element;
		this.visualProperty = visualProperty;
		this.oracleFullPath = oracleFullPath;
		this.visualPropertyValueWithoutUnits = visualPropertyValueWithoutUnits;
		this.log = log;

		// create new directory
		this.testPagePathAndName = Util.getPathAndFileNameFromFullPath(testFileFullPath);
		this.newGADir = testPagePathAndName[0] + File.separatorChar + "GA";
		File gaDir = new File(newGADir);
		gaDir.mkdir();

		cloneDPMap = new HashMap<String, Integer>();
		cloneFullDPMap = new HashMap<String, Integer>();
		cloneDPColorMap = new HashMap<String, Double>();
	}
	
	// numeric
	public GAFitnessFunction(HtmlElement element, String visualProperty, String oracleFullPath, String testFileFullPath, List<Point> originalDifferencePixels, double visualPropertyValueWithoutUnits, Logger log)
	{
		this.element = element;
		this.visualProperty = visualProperty;
		this.oracleFullPath = oracleFullPath;
		this.visualPropertyValueWithoutUnits = visualPropertyValueWithoutUnits;
		this.log = log;

		// create new directory
		this.testPagePathAndName = Util.getPathAndFileNameFromFullPath(testFileFullPath);
		this.newGADir = testPagePathAndName[0] + File.separatorChar + "GA";
		File gaDir = new File(newGADir);
		gaDir.mkdir();

		cloneDPMap = new HashMap<String, Integer>();
		cloneFullDPMap = new HashMap<String, Integer>();
		cloneDPColorMap = new HashMap<String, Double>();
		
		this.originalDifferencePixels = originalDifferencePixels;
	}

	// color
	public GAFitnessFunction(HtmlElement element, String visualProperty, String oracleFullPath, String testFileFullPath, List<Point> originalDifferencePixels, String visualPropertyValue, Logger log)
	{
		this.element = element;
		this.visualProperty = visualProperty;
		this.oracleFullPath = oracleFullPath;
		this.visualPropertyValue = visualPropertyValue;
		this.log = log;

		// create new directory
		this.testPagePathAndName = Util.getPathAndFileNameFromFullPath(testFileFullPath);
		this.newGADir = testPagePathAndName[0] + File.separatorChar + "GA";
		File gaDir = new File(newGADir);
		gaDir.mkdir();

		cloneDPColorMap = new HashMap<String, Double>();
		
		this.originalDifferencePixels = originalDifferencePixels;
	}

	@Override
	protected double evaluate(IChromosome e)
	{
		double fitnessScore = -1;

		if (analysisType.equalsIgnoreCase("NUMERIC"))
		{
			// run visual invariants tool on new solution and get number of
			// difference pixels in element
			try
			{
				int numberOfDifferencePixelsInElement = getNumberOfDifferencePixels(e);

				// if number of difference pixels = 0, implies there is no more
				// visual difference in that element.
				// Hence, give it the highest fitness value
				if (numberOfDifferencePixelsInElement == 0)
				{
					fitnessScore = Double.MAX_VALUE;
				}
				else
				{
					fitnessScore = Integer.MAX_VALUE - numberOfDifferencePixelsInElement;
					if(fitnessScore < 0)
					{
						fitnessScore = 0;
					}
				}
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
			catch (SAXException e1)
			{
				e1.printStackTrace();
			}
		}
		else if (analysisType.equalsIgnoreCase("COLOR"))
		{
			try
			{
				//fitnessScore = getDifferenceByColorHistogram(e);
				fitnessScore = Integer.MAX_VALUE - getDifferenceByColorDistance(e, oracleAverageColor);
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
		return fitnessScore;
	}

	private int getNumberOfDifferencePixels(IChromosome aPotentialSolution) throws IOException, SAXException
	{
		int value = (Integer) aPotentialSolution.getGene(0).getAllele();

		if (value == (int) visualPropertyValueWithoutUnits)
		{
			return Integer.MAX_VALUE;
		}

		String cloneFileName;
		String cloneFile;
		int numberOfDifferencePixelsInElement = 0;

		// create test page clone
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + value);
		cloneFileName = newName[0] + newName[1];
		cloneFile = newGADir + File.separatorChar + cloneFileName;

		if (!new File(cloneFile).exists() || cloneDPMap.get(cloneFileName) == null || cloneFullDPMap.get(cloneFileName) == null)
		{
			FileUtils.copyFile(new File(testPagePathAndName[0] + File.separatorChar + testPagePathAndName[1]), new File(cloneFile));
			Document document = Jsoup.parse(new File(cloneFile), null);
			Element e = Util.getElementFromXPathJava(element.getXpath(), document);

			// check if CSS property or HTML attribute to modify
			if (visualProperty.contains("style"))
			{
				// CSS property (style:<property_name>:<unit>)
				String css[] = visualProperty.split(":");
				String style = e.attr("style");
				style = style + "; " + css[1] + ":" + value + css[2]; 
				e.attr(css[0], style);
			}
			else
			{
				// HTML attribute
				e.attr(visualProperty, "" + value);
			}

			document.outputSettings().prettyPrint(false);
			//document.outputSettings().escapeMode(Entities.EscapeMode.extended);
			String html = document.html();		
			PrintWriter out = new PrintWriter(new File(cloneFile));
			out.print(html);
			out.close();

			// run detection to see if the problem is resolved
			/*VisualInvariantsTool vit = new VisualInvariantsTool(oracleFullPath, cloneFile);
			List<Point> diffPixels = vit.detection(false);
			cloneFullDPMap.put(cloneFileName, diffPixels.size());*/

			// get number of difference pixels in element
			/*for (Point p : diffPixels)
			{
				if (Util.isPointInRectangle(p.x, p.y, element.getX(), element.getY(), element.getWidth(), element.getHeight(), true))
				{
					numberOfDifferencePixelsInElement++;
				}
			}*/

			//cloneDPMap.put(cloneFileName, numberOfDifferencePixelsInElement);
			//cloneDPMap.put(cloneFileName, diffPixels.size());
			//return numberOfDifferencePixelsInElement;
			//return diffPixels.size();
			
			int diffPixels = RootCauseAnalysis.fitnessFunction(oracleFullPath, cloneFile, originalDifferencePixels);
			cloneDPMap.put(cloneFileName, diffPixels);
			cloneFullDPMap.put(cloneFileName, diffPixels);
		}
/*		else
		{
			if(cloneDPMap.get(cloneFileName) == null)
			{
				// read from filtered diff text file
				VisualInvariantsTool vit = new VisualInvariantsTool(oracleFullPath, cloneFile);
				List<Point> diffPixels = vit.extract(true);
				cloneFullDPMap.put(cloneFileName, diffPixels.size());
				cloneDPMap.put(cloneFileName, diffPixels.size());
			}
			return cloneDPMap.get(cloneFileName);
		}
*/		
		return cloneFullDPMap.get(cloneFileName);
	}

	public Map<String, String> runGAForNumericAnalysisForSBST(int low, int high) throws InvalidConfigurationException
	{
		Map<String, String>  returnMap = new HashMap<String, String>();
		
		analysisType = "NUMERIC";

		Configuration.reset();
		Configuration conf = new DefaultConfiguration();
		conf.setFitnessFunction(this);
		conf.setPreservFittestIndividual(true);
		conf.setKeepPopulationSizeConstant(false);
		conf.getNaturalSelectors(false).clear();
		BestChromosomesSelector bcs = new BestChromosomesSelector(conf, 1.0d);
		bcs.setDoubletteChromosomesAllowed(false);
		conf.addNaturalSelector(bcs, false);

		Gene[] sampleGenes = new Gene[1];
		sampleGenes[0] = new IntegerGene(conf, low, high);

		Chromosome sampleChromosome = new Chromosome(conf, sampleGenes);
		conf.setSampleChromosome(sampleChromosome);

		int populationSize = Math.abs((int) (high - low + 1));
		if(populationSize > Constants.GENETIC_ALGORITHM_MAX_POPLUATION_SIZE)
		{
			populationSize = Constants.GENETIC_ALGORITHM_MAX_POPLUATION_SIZE;
		}
		else
		{
			populationSize = populationSize * 2;
		}
		conf.setPopulationSize(populationSize);
		Genotype population = Genotype.randomInitialGenotype(conf);
		
		/*List<IChromosome> chromosomes = population.getPopulation().getChromosomes();
		IChromosome chrom = (IChromosome) chromosomes.get(0);
		Gene gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(-10));
		chrom = (IChromosome) chromosomes.get(1);
		gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(10));
		chrom = (IChromosome) chromosomes.get(2);
		gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(5));
		chrom = (IChromosome) chromosomes.get(3);
		gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(-5));*/



		boolean fixFound = false;
		int fixValue = 0;

		long startTime = System.nanoTime();
		int maxEvolutions = Constants.GENETIC_ALGORITHM_MAX_EVOLUTIONS;
		int previousBestSolution = Integer.MAX_VALUE;
		int previousBestValue = Integer.MAX_VALUE;
		int constantRateOfChangeCount = 0;
		
		IChromosome bestSolutionSoFar = null;
		for (int i = 0; i < maxEvolutions && !fixFound && (Util.convertNanosecondsToSeconds(System.nanoTime() - startTime) <= Constants.GENETIC_ALGORITHM_TIMEOUT_IN_MINS * 60); i++)
		{
			log.info("    Evolution " + (i + 1) + ", population size = " + population.getPopulation().size() + ", range = [" + low + ", " + high + "], original DP = " + originalDifferencePixels.size());
			population.evolve();
			
			bestSolutionSoFar = population.getFittestChromosome();

			for (IChromosome c : population.getPopulation().getChromosomes())
			{
				log.info("        value (GA) = " + c.getGene(0).getAllele() + ", DP = " + (c.getFitnessValue() == Double.MAX_VALUE?0:(Integer.MAX_VALUE - c.getFitnessValue())));
				if (c.getFitnessValue() == Double.MAX_VALUE || Util.isCurrentInAcceptableReductionThreshold((Integer.MAX_VALUE - c.getFitnessValue()), originalDifferencePixels.size()))
				{
					if(!fixFound)
					{
						fixFound = true;
						fixValue = (Integer) c.getGene(0).getAllele();
						bestSolutionSoFar = c;
					}
				}
			}
		}

		if (!fixFound)
		{
			fixValue = (Integer) bestSolutionSoFar.getGene(0).getAllele();
		}

		String isFixFound = "";
		int fixDiffPixels = -1;
		if(bestSolutionSoFar.getFitnessValue() == Double.MAX_VALUE)
		{
			isFixFound = " => exact root cause found!";
			fixDiffPixels = 0;
		}
		else if(fixFound && Util.isCurrentInAcceptableReductionThreshold((Integer.MAX_VALUE - bestSolutionSoFar.getFitnessValue()), originalDifferencePixels.size()))
		{
			isFixFound = " => acceptable root cause found!";
			fixDiffPixels = (int) (Integer.MAX_VALUE - bestSolutionSoFar.getFitnessValue());
		}
		log.info("        value = " + fixValue + (visualProperty.contains("style") ? "px" : "") + isFixFound);
		
		if (fixFound)
		{
			// problem solved
			returnMap.put(Constants.RCA_FIX_FOUND, fixDiffPixels + "#" + fixValue + (visualProperty.contains("style") ? "px" : ""));
			return returnMap;
		}
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + fixValue);
		String cloneFileName = newName[0] + newName[1];
		int diffPixels = cloneFullDPMap.get(cloneFileName);
		returnMap.put(Constants.RCA_FIX_NOT_FOUND, diffPixels + "#" + fixValue + (visualProperty.contains("style") ? "px" : ""));
		return returnMap;
	}

	
	public Map<String, String> runGAForNumericAnalysis(int low, int high) throws InvalidConfigurationException
	{
		Map<String, String>  returnMap = new HashMap<String, String>();
		
		analysisType = "NUMERIC";

		Configuration.reset();
		Configuration conf = new DefaultConfiguration();
		conf.setFitnessFunction(this);
		conf.setPreservFittestIndividual(true);
		conf.setKeepPopulationSizeConstant(false);
		conf.getNaturalSelectors(false).clear();
		BestChromosomesSelector bcs = new BestChromosomesSelector(conf, 1.0d);
		bcs.setDoubletteChromosomesAllowed(false);
		conf.addNaturalSelector(bcs, false);

		Gene[] sampleGenes = new Gene[1];
		sampleGenes[0] = new IntegerGene(conf, low, high);

		Chromosome sampleChromosome = new Chromosome(conf, sampleGenes);
		conf.setSampleChromosome(sampleChromosome);

		int populationSize = Math.abs((int) (high - low + 1));
		if(populationSize > Constants.GENETIC_ALGORITHM_MAX_POPLUATION_SIZE)
		{
			populationSize = Constants.GENETIC_ALGORITHM_MAX_POPLUATION_SIZE;
		}
		else
		{
			populationSize = populationSize * 2;
		}
		conf.setPopulationSize(populationSize);
		Genotype population = Genotype.randomInitialGenotype(conf);
		
		/*List<IChromosome> chromosomes = population.getPopulation().getChromosomes();
		IChromosome chrom = (IChromosome) chromosomes.get(0);
		Gene gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(-10));
		chrom = (IChromosome) chromosomes.get(1);
		gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(10));
		chrom = (IChromosome) chromosomes.get(2);
		gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(5));
		chrom = (IChromosome) chromosomes.get(3);
		gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(-5));*/



		boolean fixFound = false;
		int fixValue = 0;

		long startTime = System.nanoTime();
		int maxEvolutions = Constants.GENETIC_ALGORITHM_MAX_EVOLUTIONS;
		int previousBestSolution = Integer.MAX_VALUE;
		int previousBestValue = Integer.MAX_VALUE;
		int constantRateOfChangeCount = 0;
		
		IChromosome bestSolutionSoFar = null;
		//for (int i = 0; i < maxEvolutions && !fixFound && (Util.convertNanosecondsToSeconds(System.nanoTime() - startTime) <= Constants.GENETIC_ALGORITHM_TIMEOUT_IN_MINS * 60); i++)
		for (int i = 0; i < maxEvolutions && !fixFound; i++)
		{
			log.info("    Evolution " + (i + 1) + ", population size = " + population.getPopulation().size() + ", range = [" + low + ", " + high + "], original DP = " + originalDifferencePixels.size());
			population.evolve();
			
			bestSolutionSoFar = population.getFittestChromosome();

			for (IChromosome c : population.getPopulation().getChromosomes())
			{
				log.info("        value (GA) = " + c.getGene(0).getAllele() + ", DP = " + (c.getFitnessValue() == Double.MAX_VALUE?0:(Integer.MAX_VALUE - c.getFitnessValue())));
				if (c.getFitnessValue() == Double.MAX_VALUE || Util.isCurrentInAcceptableReductionThreshold((Integer.MAX_VALUE - c.getFitnessValue()), originalDifferencePixels.size()))
				{
					if(!fixFound)
					{
						fixFound = true;
						fixValue = (Integer) c.getGene(0).getAllele();
						bestSolutionSoFar = c;
					}
				}
			}
			
			if(bestSolutionSoFar.getFitnessValue() < previousBestSolution)
			{
				previousBestSolution = (int) bestSolutionSoFar.getFitnessValue();
				previousBestValue = (Integer) bestSolutionSoFar.getGene(0).getAllele();
				
				if(i == Constants.GENETIC_ALGORITHM_MAX_EVOLUTIONS-1)
				{
					// reset number of evolutions
					i = 0;
				}
			}
			else if(bestSolutionSoFar.getFitnessValue() == previousBestSolution)
			{
				constantRateOfChangeCount++;
			}
			
			if(constantRateOfChangeCount == Constants.RCA_NUMERIC_ANALYSIS_RATE_OF_CHANGE_WINDOW_SIZE)
			{
				// rate of change is constant, indicating that there is no convergence and this cannot be the correct faulty property
				log.info("Constant rate of change. Hence, terminating.");
				break;
			}
		}

		if (!fixFound)
		{
			fixValue = (Integer) bestSolutionSoFar.getGene(0).getAllele();
		}

		String isFixFound = "";
		int fixDiffPixels = -1;
		if(bestSolutionSoFar.getFitnessValue() == Double.MAX_VALUE)
		{
			isFixFound = " => exact root cause found!";
			fixDiffPixels = 0;
		}
		else if(fixFound && Util.isCurrentInAcceptableReductionThreshold((Integer.MAX_VALUE - bestSolutionSoFar.getFitnessValue()), originalDifferencePixels.size()))
		{
			isFixFound = " => acceptable root cause found!";
			fixDiffPixels = (int) (Integer.MAX_VALUE - bestSolutionSoFar.getFitnessValue());
		}
		log.info("        value = " + fixValue + (visualProperty.contains("style") ? "px" : "") + isFixFound);
		
		if (fixFound)
		{
			// problem solved
			returnMap.put(Constants.RCA_FIX_FOUND, fixDiffPixels + "#" + fixValue + (visualProperty.contains("style") ? "px" : ""));
			return returnMap;
		}
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + fixValue);
		String cloneFileName = newName[0] + newName[1];
		int diffPixels = cloneFullDPMap.get(cloneFileName);
		returnMap.put(Constants.RCA_FIX_NOT_FOUND, diffPixels + "#" + fixValue + (visualProperty.contains("style") ? "px" : ""));
		return returnMap;
	}

	private double getDifferenceByColorHistogram(IChromosome aPotentialSolution) throws IOException
	{
		String value = (String) aPotentialSolution.getGene(0).getAllele();

		if (value.equalsIgnoreCase(visualPropertyValue))
		{
			return 0;
		}

		String cloneFileName;
		String cloneFile;

		// create test page clone
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + value.replace("#", ""));
		cloneFileName = newName[0] + newName[1];
		cloneFile = newGADir + File.separatorChar + cloneFileName;

		if (!new File(cloneFile).exists())
		{
			FileUtils.copyFile(new File(testPagePathAndName[0] + File.separatorChar + testPagePathAndName[1]), new File(cloneFile));
			Document document = Jsoup.parse(new File(cloneFile), null);
			Element e = Util.getElementFromXPathJava(element.getXpath(), document);

			// check if CSS property or HTML attribute to modify
			e.attr("style", visualProperty + ": " + value);
			
			document.outputSettings().prettyPrint(false);
			document.outputSettings().escapeMode(Entities.EscapeMode.extended);
			String html = document.html();		
			PrintWriter out = new PrintWriter(new File(cloneFile));
			out.print(html);
			out.close();

			// run detection to see if the problem is resolved
			ImageProcessing ip = new ImageProcessing();
			Util.getScreenshot(cloneFileName, newGADir, newName[0] + Constants.SCREENSHOT_FILE_EXTENSION, null, false);
			double result = ip.compareImagesByHistogram(oracleFullPath, newGADir + File.separatorChar + newName[0] + Constants.SCREENSHOT_FILE_EXTENSION);
			cloneDPColorMap.put(cloneFileName, result);

			return result;
		}
		else
		{
			return cloneDPColorMap.get(cloneFileName);
		}
	}

	private double getDifferenceByColorDistance(IChromosome aPotentialSolution, String oracleAverageColor) throws IOException
	{
		int value = (Integer) aPotentialSolution.getGene(0).getAllele();

		String cloneFileName;
		String cloneFile;

		// create test page clone
		String[] newName = Util.getFileNameAndExtension(testPagePathAndName[1], "_clone_" + Util.getHexFromDecimal(value).replace("#", ""));
		cloneFileName = newName[0] + newName[1];
		cloneFile = newGADir + File.separatorChar + cloneFileName;

		if (!new File(cloneFile).exists())
		{
			FileUtils.copyFile(new File(testPagePathAndName[0] + File.separatorChar + testPagePathAndName[1]), new File(cloneFile));
			Document document = Jsoup.parse(new File(cloneFile), null);
			Element e = Util.getElementFromXPathJava(element.getXpath(), document);

			// CSS property (style:<property_name>:<unit>)
			//String css[] = visualProperty.split(":");
			String style = e.attr("style");
			style = style + "; " + visualProperty + ":" + Util.getHexFromDecimal(value); 
			e.attr("style", style);
			
			document.outputSettings().prettyPrint(false);
			document.outputSettings().escapeMode(Entities.EscapeMode.extended);
			String html = document.html();		
			PrintWriter out = new PrintWriter(new File(cloneFile));
			out.print(html);
			out.close();
			
			// take screenshot of test page
			Util.getScreenshot(cloneFileName, newGADir, newName[0] + Constants.SCREENSHOT_FILE_EXTENSION, null, true);
			
			// compute average color from test page
			String testAverageColor = computeAverageColor(newGADir + File.separatorChar + newName[0] + Constants.SCREENSHOT_FILE_EXTENSION);
			double distance = getColorDistance(Color.decode(oracleAverageColor), Color.decode(testAverageColor));
			
			// run detection to check if the two images are same
			double returnValue = distance + RootCauseAnalysis.fitnessFunction(oracleFullPath, cloneFile, originalDifferencePixels);
			cloneDPColorMap.put(cloneFileName, returnValue);
			return returnValue;
		}
		else
		{
			return cloneDPColorMap.get(cloneFileName);
		}
	}

	public String computeAverageColor(String imageWithFullPath) throws NumberFormatException, IOException
	{
		SortedSet<Map.Entry<String, Integer>> sortedColorset = getAllColorsInElement(imageWithFullPath);

		// get average color
		int totalDecColors = 0;
		for (Map.Entry<String, Integer> entry : sortedColorset)
		{
			totalDecColors = totalDecColors + Util.getDecimalFromHex(entry.getKey());
		}
		int averageColor = totalDecColors/sortedColorset.size();
		String averageColorHex = Util.getHexFromDecimal(averageColor);
		//String averageColorHex = sortedColorset.first().getKey();
		
		return averageColorHex;
	}

	private double getColorDistance(Color c1, Color c2)
	{
	    double rmean = ( c1.getRed() + c2.getRed() )/2;
	    int r = c1.getRed() - c2.getRed();
	    int g = c1.getGreen() - c2.getGreen();
	    int b = c1.getBlue() - c2.getBlue();
	    double weightR = 2 + rmean/256;
	    double weightG = 4.0;
	    double weightB = 2 + (255-rmean)/256;
	    return Math.sqrt(weightR*r*r + weightG*g*g + weightB*b*b);
	} 
	
	private SortedSet<Map.Entry<String, Integer>> getAllColorsInElement(String imageWithFullPath) throws NumberFormatException, IOException
	{
		// get colors for all difference pixels
		Map<String, Integer> colors = new HashMap<String, Integer>();
		for (int i = 0; i < originalDifferencePixels.size(); i++)
		{
			Point p = originalDifferencePixels.get(i);
			String c = getPixelColorInHex(imageWithFullPath, p.x, p.y);
			Integer cnt = 0;
			if (c != null && colors.get(c) != null)
			{
				cnt = colors.get(c);
			}
			colors.put(c, ++cnt);
		}

		// sort the colors map in descending order of frequency count
		SortedSet<Map.Entry<String, Integer>> sortedset = new TreeSet<Map.Entry<String, Integer>>(new Comparator<Map.Entry<String, Integer>>()
		{
			public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2)
			{
				return e2.getValue().compareTo(e1.getValue());
			}
		});
		sortedset.addAll(colors.entrySet());
		
		return sortedset;
	}

	private String getPixelColorInHex(String imagePath, int x, int y) throws IOException
	{
		File file = new File(imagePath);
		BufferedImage image = ImageIO.read(file);

		int clr = 0; 
		try
		{
			clr = image.getRGB(x, y);
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			return null;
		}
		int red = (clr >> 16) & 0xff;
		int green = (clr >> 8) & 0xff;
		int blue = (clr) & 0xff;
		
		String hex = String.format("#%02x%02x%02x", red, green, blue);
		return hex;
	}
	
	public Map<String, String> runGAForColorAnalysis(int low, int high) throws InvalidConfigurationException, NumberFormatException, IOException
	{
		Map<String, String>  returnMap = new HashMap<String, String>();
		
		analysisType = "COLOR";

		log.info("ORACLE COLOR = " + oracleAverageColor);
		
		Configuration.reset();
		Configuration conf = new DefaultConfiguration();
		conf.setFitnessFunction(this);
		conf.setPreservFittestIndividual(true);
		conf.setKeepPopulationSizeConstant(false);
		conf.getNaturalSelectors(false).clear();
		BestChromosomesSelector bcs = new BestChromosomesSelector(conf, 1.0d);
		bcs.setDoubletteChromosomesAllowed(false);
		conf.addNaturalSelector(bcs, false);

		Gene[] sampleGenes = new Gene[1];
		sampleGenes[0] = new IntegerGene(conf, low, high);

		Chromosome sampleChromosome = new Chromosome(conf, sampleGenes);
		conf.setSampleChromosome(sampleChromosome);

		int populationSize = Math.abs((int) (high - low + 1));
		if(populationSize > Constants.GENETIC_ALGORITHM_MAX_POPLUATION_SIZE)
		{
			populationSize = Constants.GENETIC_ALGORITHM_MAX_POPLUATION_SIZE;
		}
		conf.setPopulationSize(populationSize);
		Genotype population = Genotype.randomInitialGenotype(conf);
		
		/*List<IChromosome> chromosomes = population.getPopulation().getChromosomes();
		IChromosome chrom = (IChromosome) chromosomes.get(0);
		Gene gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(-10));
		chrom = (IChromosome) chromosomes.get(1);
		gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(10));
		chrom = (IChromosome) chromosomes.get(2);
		gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(5));
		chrom = (IChromosome) chromosomes.get(3);
		gene = (Gene) chrom.getGene(0);
		gene.setAllele(new Integer(-5));*/



		boolean fixFound = false;
		int fixValue = 0;

		long startTime = System.nanoTime();
		for (int i = 0; i < Constants.GENETIC_ALGORITHM_MAX_EVOLUTIONS && !fixFound && (Util.convertNanosecondsToSeconds(System.nanoTime() - startTime) <= Constants.GENETIC_ALGORITHM_TIMEOUT_IN_MINS * 60); i++)
		{
			log.info("    Evolution " + (i + 1) + ", population size = " + population.getPopulation().size() + ", range = [" + low + ", " + high + "]");
			population.evolve();
			for (IChromosome c : population.getPopulation().getChromosomes())
			{
				log.info("        value (GA) = " + Util.getHexFromDecimal((Integer)c.getGene(0).getAllele()) + ",  FV = " + c.getFitnessValue());
				if (c.getFitnessValue() == Integer.MAX_VALUE)
				{
					fixFound = true;
					fixValue = (Integer) c.getGene(0).getAllele();
				}
			}
		}

		IChromosome bestSolutionSoFar = population.getFittestChromosome();
		if (!fixFound)
		{
			fixValue = (Integer) bestSolutionSoFar.getGene(0).getAllele();
		}

		String isFixFound = (fixFound) ? " => exact root cause found!" : "";
		log.info("        value = " + Util.getHexFromDecimal(fixValue) + isFixFound);
		
		if (fixFound)
		{
			// problem solved
			returnMap.put(Constants.RCA_FIX_FOUND, Util.getHexFromDecimal(fixValue));
			return returnMap;
		}
		return returnMap;
	}

	public String runGAForColorAnalysis(List<String> values) throws InvalidConfigurationException
	{
		analysisType = "COLOR";

		Configuration.reset();
		Configuration conf = new DefaultConfiguration();
		conf.setFitnessFunction(this);
		conf.setPreservFittestIndividual(true);
		conf.setKeepPopulationSizeConstant(false);
		conf.getNaturalSelectors(false).clear();
		BestChromosomesSelector bcs = new BestChromosomesSelector(conf, 1.0d);
		bcs.setDoubletteChromosomesAllowed(false);
		conf.addNaturalSelector(bcs, false);

		Gene[] sampleGenes = new Gene[1];
		sampleGenes[0] = new CustomGene(conf, values);

		Chromosome sampleChromosome = new Chromosome(conf, sampleGenes);
		conf.setSampleChromosome(sampleChromosome);

		int totalPopulationSize = values.size();
		int initialPopulationSize = (int) Math.ceil((double)totalPopulationSize / (double)Constants.GENETIC_ALGORITHM_INITIAL_POPULATION_SIZE_DIVIDING_FACTOR);
		conf.setPopulationSize(initialPopulationSize);
		Genotype population = Genotype.randomInitialGenotype(conf);

		boolean fixFound = false;
		String fixValue = "";
		for (int i = 0; i < totalPopulationSize && !fixFound; i++)
		{
			log.info("    Evolution " + (i + 1) + ", population size = " + initialPopulationSize + ", values size = " + values.size());
			population.evolve();
			for (IChromosome c : population.getPopulation().getChromosomes())
			{
				log.info("        value (GA) = " + c.getGene(0).getAllele() + ", histogram value = " + c.getFitnessValue());
				if (c.getFitnessValue() == 1)
				{
					fixFound = true;
					fixValue = (String) c.getGene(0).getAllele();
				}
			}
		}

		IChromosome bestSolutionSoFar = population.getFittestChromosome();
		if (!fixFound)
		{
			fixValue = (String) bestSolutionSoFar.getGene(0).getAllele();
		}

		String isFixFound = (fixFound) ? " => exact root cause found!" : "";
		log.info("        value = " + fixValue + isFixFound);
		return (String) fixValue;
	}
}