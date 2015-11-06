package rca;

import java.util.List;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.DefaultConfiguration;

import config.Constants;

public class CustomGeneFitnessFunction extends FitnessFunction
{
	public CustomGeneFitnessFunction()
	{
	}

	public double evaluate( IChromosome a_subject )
	{
		double fitnessScore = -1;
		
		return fitnessScore;
	}
	
	public void runCustomGeneForPredefinedValues(List<String> values) throws InvalidConfigurationException
	{
		Configuration conf = new DefaultConfiguration();
		conf.setFitnessFunction(new CustomGeneFitnessFunction());
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

		conf.setPopulationSize(values.size()/Constants.GENETIC_ALGORITHM_INITIAL_POPULATION_SIZE_DIVIDING_FACTOR);
		Genotype population = Genotype.randomInitialGenotype(conf);
		
		for( int i = 0; i < values.size(); i++ )
		{
			System.out.println("EVOLUTION = " + i);
		    population.evolve();
		    for(IChromosome c : population.getPopulation().getChromosomes())
		    {
		    	System.out.println("fitness value = " + c.getGene(0).getAllele() + ", " + c.getGene(1).getAllele() + ": " + c.getFitnessValue());
		    }
		}
		IChromosome bestSolutionSoFar = population.getFittestChromosome();
		
		System.out.println("The best solution contained the following: ");
		System.out.println("Best solution fitness value: " + bestSolutionSoFar.getFitnessValue());
		System.out.println("Best solution visual property value: " + bestSolutionSoFar.getGene(0).getAllele());
	}
}