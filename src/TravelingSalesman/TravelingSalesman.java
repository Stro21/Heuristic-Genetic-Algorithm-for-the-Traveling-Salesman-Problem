
package TravelingSalesman;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Scanner;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.event.EventManager;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.StockRandomGenerator;

// --------------------------------------------- //
// OPTIMAL TOURS:
// Length 29: 27603
// Length 48: 10628
// Length 101: 629
// Length 280: 2579
// --------------------------------------------- //

/**
 * @author Aaron Foltz
 */
public class TravelingSalesman implements Serializable {

	// The number of cities to visit
	public static int				CITIES;

	// The array that actually holds the city coordinates
	public static double[][]		CITYARRAY;

	// The weighting of the edges between each of the cities. Decides the
	// distance function
	public static String			EdgeWeightType		= null;

	// The best chromosome over an entire TSP
	private static IChromosome		bestChromosome;

	// Set to true if you want to see more textual output as well as writing
	// data to a file
	private static boolean			debugOutput			= true;

	private static Configuration	m_config;

	// Set up writing data to a file
	private static BufferedWriter	writer				= null;

	// The population for the GA
	static Genotype					population			= null;

	// The culling percentage for the GA, the percentage of the current
	// population that you want to keep for the next generation
	private final double			cullingPercentage	= .75;

	// The number of evolutons for the GA
	private int						m_maxEvolution		= (1 * (int) ((Math.log(1 - Math.pow(
																.99,
																(1.0 / CITIES)))) / (Math
																.log(((float) (CITIES - 3) / (float) (CITIES - 1))))));
	// Population size estimation for the GA
	// from Tommi Rintala located at:
	// http://lipas.uwasa.fi/cs/publications/2NWGA/node11.html#SECTION04120000000000000000
	private int						m_populationSize	= (1 * (int) ((Math.log(1 - Math.pow(
																.99,
																(1.0 / CITIES)))) / (Math
																.log(((float) (CITIES - 3) / (float) (CITIES - 1))))));

	// Offset for the TSP. This allows you to make the cities before the offset
	// stay in place, not being changed by mutation/crossover
	private int						m_startOffset		= 0;

	// Mutation rate = 1/X
	// The entire GA seems to work better with a high mutationRate (about 1 in
	// every 3 are mutated)
	private final int				mutationRate		= 3;


	public static Configuration getConfiguration() {

		return m_config;
	}


	/**
	 * Gather the needed city information, and start the evolution process. We
	 * can run multiple instances of the TSP problem, printing out interim
	 * results and final results at the very end.
	 * 
	 * @author Aaron Foltz
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		// --------------------------------------------- //
		// Gather input from the user

		// Gather the file containing the TSP problem data
		System.out.print("Enter the file: ");
		Scanner scan = new Scanner(System.in);
		String file = scan.nextLine().concat(".tsp");

		// Gather the number of TSP iterations that you want to run on this data
		System.out.print("Enter iterations: ");
		int iterations = scan.nextInt();

		// Gather the optimal solution - this helps to automatically collect
		// percentages to the optimal solution
		System.out.print("Enter optimal for this problem: ");
		int optimalTour = scan.nextInt();
		// --------------------------------------------- //

		// Create the file to output the results of the experiment
		if (debugOutput) {
			// Write data to a file
			writer = new BufferedWriter(new FileWriter("data/"
					+ file.concat(".data")));
		}

		// Get the coordinates of the cities from the file.
		CITYARRAY = Reader.getCoordinates(file);

		// Set the length of the CITIES array to the length of cities
		CITIES = CITYARRAY.length;

		// Get the EdgeWeightType from the problem file. This will determine the
		// type of approach to take when calculating distance
		EdgeWeightType = Reader.getEdgeWeightType(file);

		// Collect average and best information for the TSP instances
		int average = 0;
		double averagePercent = 0, averageRunningTime = 0;
		int bestOverall = Integer.MAX_VALUE;

		// Run this TSP the desired amount of times
		for (int i = 0; i < iterations; i++) {

			// Gather the starting time for the program
			long startTime = System.currentTimeMillis();

			try {

				// Create new Traveling Salesman problem and start evolving
				TravelingSalesman t = new TravelingSalesman();
				IChromosome optimal = t.findOptimalPath(null);

				// Gather the ending time of the program
				long endTime = System.currentTimeMillis();

				// --------------------------------------------- //
				// Print out ending results
				System.out.println("Solution: ");
				System.out.println(bestChromosome);
				System.out.println("Score "
						+ (bestChromosome.getFitnessValue()));

				// Print out the total running time at the end
				System.out.println("RUNNING TIME: " + (endTime - startTime)
						/ 1000F + " seconds");
				// --------------------------------------------- //

				// Save the best overall chromosome - over ALL of the TSP
				// instances
				if (bestOverall > bestChromosome.getFitnessValue()) {
					bestOverall = (int) bestChromosome.getFitnessValue();
				}

				// --------------------------------------------- //
				// Keep an average fitness value, percentage, and running time
				average += bestChromosome.getFitnessValue();
				averagePercent += (100 * ((optimal.getFitnessValue() - optimalTour) / optimal
						.getFitnessValue()));
				averageRunningTime += (endTime - startTime) / 1000F;
				// --------------------------------------------- /

				// Add the fitness value and running time for each iteration
				if (debugOutput) {

					writer.write((int) optimal.getFitnessValue() + "\t"
							+ (endTime - startTime) / 1000F + " seconds" + "\t");
					writer.write((100 * ((optimal.getFitnessValue() - optimalTour) / optimal
							.getFitnessValue()))
							+ "\n\n");
				}

				// Reset the configuration so that we can run another TSP
				// instance on the same data
				getConfiguration().reset();

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		// --------------------------------------------- //
		// At the end of the TSP iterations on this data, print out the results,
		// and write them to a file if debug output is true
		if (debugOutput) {
			// Write out the average and best run
			writer.write("\n--------------------------------------------------\n");
			writer.write("AVERAGE: " + (average / iterations) + "\n");
			writer.write("AVERAGE PERCENT: " + (averagePercent / iterations)
					+ "\n");
			writer.write("AVERAGE RUNNINGTIME: "
					+ (averageRunningTime / iterations) + "\n");
			writer.write("BEST: " + bestOverall + "\n\n");
			writer.write("PERCENTAGE: "
					+ ((float) (100 * (bestOverall - optimalTour) / bestOverall)));
			writer.close();
		}

		System.out
				.println("\n\n----------------------------------------------\nAVERAGE: "
						+ (average / iterations));
		System.out.println("AVERAGE PERCENT: " + (averagePercent / iterations));
		System.out.println("AVERAGE RUNNINGTIME: "
				+ (averageRunningTime / iterations));
		System.out.println("BEST: " + bestOverall);
		System.out.println("PERCENTAGE: "
				+ ((float) (100 * (bestOverall - optimalTour) / bestOverall)));
		// --------------------------------------------- //

	}


	/**
	 * Create a configuration for this TSP instance. Include the culling
	 * percentage (BestChromosomeSelector), RandomGenerator, Minimum Population
	 * Size, Fitness evaluator (a lower fitness is better here), Crossover
	 * Operator, and Mutation Operator. The configuration should not contain
	 * operators for ordinary crossover and mutations, as they can make
	 * chromosomes invalid.
	 * 
	 * @param a_initial_data
	 *            the same object as was passed to findOptimalPath. It can be
	 *            used to specify the task more precisely if the class is used
	 *            for solving multiple tasks
	 * 
	 * @return created configuration
	 * 
	 * @throws InvalidConfigurationException
	 * 
	 * @author Aaron Foltz
	 */
	public Configuration createConfiguration(final Object a_initial_data)
			throws InvalidConfigurationException {

		Configuration config = new Configuration();

		// Object that will select the best individuals to be processed for
		// crossover and mutation. The selected 75% will be duplicated to cover
		// the discarded 25%. This allows us to keep the population size
		// constant
		BestChromosomesSelector bestChromsSelector = new BestChromosomesSelector(
				config, cullingPercentage);
		bestChromsSelector.setDoubletteChromosomesAllowed(true);
		config.addNaturalSelector(bestChromsSelector, true);

		// Creates random numbers used throughout the process
		config.setRandomGenerator(new StockRandomGenerator());

		// We do not want the population size to vary whatsoever, so we want the
		// minimum to be 100% of the original at all times
		config.setMinimumPopSizePercent(100);

		config.setEventManager(new EventManager());

		// Object that declares a fitness value better if it is lower.
		config.setFitnessEvaluator(new OppositeFitnessEvaluator());

		// Used to preserve memory with the chromosome allocations
		config.setChromosomePool(new ChromosomePool());

		// Genetic operator for crossover - Grefenstettes Heuristic (greedy)
		// Crossover
		config.addGeneticOperator(new TravelingSalesmanHeuristicCrossover(
				config, this));

		// Genetic operator for mutation - 2-Opt Segment Mutation Operator
		config.addGeneticOperator(new SegmentSwappingMutation(config,
				mutationRate, this));
		return config;
	}


	/**
	 * Simply returns the fitness function that will be used for this TSP.
	 * 
	 * @param a_initial_data
	 *            the same object as was passed to findOptimalPath. It can be
	 *            used to specify the task more precisely if the class is used
	 *            for solving multiple tasks
	 * @return an applicable fitness function
	 * 
	 * @author Aaron Foltz
	 */
	public FitnessFunction createFitnessFunction(final Object a_initial_data) {

		return new TravelingSalesmanFitnessFunction(this);
	}


	/**
	 * Create a sample array of the given number of integer genes. The first
	 * gene is always 0, this is the city where the salesman starts the journey.
	 * 
	 * @param a_initial_data
	 *            ignored
	 * @return Chromosome
	 * 
	 * @author Aaron Foltz
	 */
	public IChromosome createSampleChromosome(Object a_initial_data) {

		try {
			Gene[] genes = new Gene[CITIES];

			// Create a sample chromosome from consecutive gene numbers
			for (int i = 0; i < genes.length; i++) {
				genes[i] = new IntegerGene(getConfiguration(), 0, CITIES - 1);
				genes[i].setAllele(new Integer(i));
			}
			IChromosome sample = new Chromosome(getConfiguration(), genes);
			return sample;

		} catch (InvalidConfigurationException iex) {
			throw new IllegalStateException(iex.getMessage());
		}
	}


	/**
	 * Return the distance between two genes (cities), given by the Edge weight
	 * type provided by the TSP instance
	 * 
	 * @param a_from
	 *            first gene, representing a city
	 * @param a_to
	 *            second gene, representing a city
	 * @return the distance between two cities represented as genes
	 * 
	 * @author Aaron Foltz
	 */
	public double distance(Gene a_from, Gene a_to) {

		// ATT Distance Function
		if (EdgeWeightType.equals("ATT")) {

			// Get the city value represented by the gene
			IntegerGene geneA = (IntegerGene) a_from;
			IntegerGene geneB = (IntegerGene) a_to;
			int a = geneA.intValue();
			int b = geneB.intValue();

			// If the same city, the distance is 0
			if (a == b) {
				return 0;
			}

			// Calculate the distance

			// Gather the X and Y coordinate for each city from the city array -
			// provided by the TSP data
			double x1 = CITYARRAY[a][0];
			double y1 = CITYARRAY[a][1];
			double x2 = CITYARRAY[b][0];
			double y2 = CITYARRAY[b][1];

			double xd = x1 - x2;
			double yd = y2 - y1;

			double rij = Math.sqrt(((xd * xd) + (yd * yd)) / 10.0);
			double tij = Math.round(rij);

			if (tij < rij) {
				return tij + 1;
			} else {
				return tij;
			}

			// EUC_2D Distance Function
		} else if (EdgeWeightType.equals("EUC_2D")) {

			// Get the city (integer) representation
			IntegerGene geneA = (IntegerGene) a_from;
			IntegerGene geneB = (IntegerGene) a_to;
			int a = geneA.intValue();
			int b = geneB.intValue();

			// If the same city, the distance is 0
			if (a == b) {
				return 0;

				// Calculate and return the basic Euclidean distance
			} else {

				// Gather the X and Y coordinate for each city from the city
				// array -
				// provided by the TSP data
				double x1 = CITYARRAY[a][0];
				double y1 = CITYARRAY[a][1];
				double x2 = CITYARRAY[b][0];
				double y2 = CITYARRAY[b][1];

				double xd = x1 - x2;
				double yd = y1 - y2;

				return Math.round(Math.sqrt((xd * xd) + (yd * yd)));

			}
		}

		return -1;

	}


	/**
	 * Return the distance between two cities, represented as numbers, given by
	 * the Edge weight type provided by the TSP instance
	 * 
	 * @param a_from
	 *            first gene, representing a city
	 * @param a_to
	 *            second gene, representing a city
	 * @return the distance between two cities represented as genes
	 * 
	 * @author Aaron Foltz
	 */
	public double distance(int a_from, int a_to) {

		// ATT Distance Function
		if (EdgeWeightType.equals("ATT")) {

			int a = a_from;
			int b = a_to;

			// If the same city, the distance is 0
			if (a == b) {
				return 0;
			}

			// Calculate/return the distance between the two cities

			// Gather the X and Y coordinate for each city from the city array -
			// provided by the TSP data
			double x1 = CITYARRAY[a][0];
			double y1 = CITYARRAY[a][1];
			double x2 = CITYARRAY[b][0];
			double y2 = CITYARRAY[b][1];

			double xd = x1 - x2;
			double yd = y2 - y1;

			double rij = Math.sqrt(((xd * xd) + (yd * yd)) / 10.0);
			double tij = Math.round(rij);

			if (tij < rij) {
				return tij + 1;
			} else {
				return tij;
			}

			// EUC_2D Distance Function
		} else if (EdgeWeightType.equals("EUC_2D")) {

			int a = a_from;
			int b = a_to;

			// If the same city, the distance is 0
			if (a == b) {
				return 0;

				// Calculate and return the basic Euclidean distance
			} else {

				// Gather the X and Y coordinate for each city from the city
				// array -
				// provided by the TSP data
				double x1 = CITYARRAY[a][0];
				double y1 = CITYARRAY[a][1];
				double x2 = CITYARRAY[b][0];
				double y2 = CITYARRAY[b][1];

				double xd = x1 - x2;
				double yd = y1 - y2;

				return Math.round(Math.sqrt((xd * xd) + (yd * yd)));

			}
		}

		return -1;

	}


	/**
	 * Executes the Genetic Algorithm to calculate the suboptimal tour between
	 * each of the cities.
	 * 
	 * @param a_initial_data
	 *            can be a record with fields, specifying the task more
	 *            precisely if the class is used to solve multiple tasks. It is
	 *            passed to createFitnessFunction, createSampleChromosome and
	 *            createConfiguration
	 * 
	 * @throws Exception
	 * @return chromosome representing the optimal path between cities
	 * 
	 * @author Aaron Foltz
	 */
	public IChromosome findOptimalPath(final Object a_initial_data)
			throws Exception {

		// Get the configuration for this TSP instance
		m_config = createConfiguration(a_initial_data);

		// Gather the fitness function needed for this TSP
		FitnessFunction myFunc = createFitnessFunction(a_initial_data);
		m_config.setFitnessFunction(myFunc);

		// Gather the sample chromosome. This will be used to create our
		// population based on that blueprint
		IChromosome sampleChromosome = createSampleChromosome(a_initial_data);
		m_config.setSampleChromosome(sampleChromosome);

		// Output basic debugging information
		if (debugOutput) {
			System.out.println("\n\nPOPULATION SIZE: " + getPopulationSize());
			System.out.println("MAX EVOLUTIONS: " + m_maxEvolution);
			System.out.println("MUTATION RATE: " + mutationRate);
			System.out.println("CULLING PERCENTAGE: " + cullingPercentage);
		}

		// Set the number of chromosomes/individuals that we want in our
		// population
		m_config.setPopulationSize(getPopulationSize());

		// --------------------------------------------- //
		// Genetic Algorithm Initialization
		// --------------------------------------------- //

		// Create the array representing the population
		IChromosome[] chromosomes = new IChromosome[m_config
				.getPopulationSize()];

		// Get the sample chromosomes - these will be used in initialization
		Gene[] samplegenes = sampleChromosome.getGenes();

		// Create a linked list with each of the city integers. This is used
		// in initialization to satisfy set characteristics (a city can only be
		// visited once except for the start/end city)
		LinkedList<Integer> cityList = new LinkedList<Integer>();
		for (int j = 1; j < samplegenes.length; j++) {
			cityList.add(j);
		}

		// Initialize the population with the Stochastic Method
		for (int i = 0; i < chromosomes.length; i++) {

			// Create the array of genes that will comprise this chromosome
			Gene[] genes = new Gene[samplegenes.length];

			// Shuffle the collection to mix things up
			Collections.shuffle(cityList);

			// Stochastically build up the chromosome. Take the average of the
			// edges left in the "not picked" pile, then choose a random edge
			// for inclusion in the "picked" pile. If the inclusion of that edge
			// is less than the average, then take it.
			genes = StochasticInitialization.operate(genes, samplegenes, this,
					(LinkedList<Integer>) cityList.clone());

			// We now have this individual in the population
			chromosomes[i] = new Chromosome(m_config, genes);
		}
		// --------------------------------------------- //

		// Create the Genotype. We cannot use Genotype.randomInitialGenotype,
		// Because we need unique gene values (representing the indices of the
		// cities of our problem).
		// -------------------------------------------------------------------
		population = new Genotype(m_config, new Population(m_config,
				chromosomes));

		// Keep track of the best chromosome in the population during each
		// evolutionary stage
		IChromosome best = null;

		// Exit after the best fitness hasn't changed for a number of times
		int counter = 0;

		// Track the last best chromosome
		int previousBest = Integer.MAX_VALUE;

		// Evolve the population. Since we don't know what the best answer
		// is going to be, we just evolve the max number of times.
		// ---------------------------------------------------------------
		Evolution: for (int i = 0; i < getMaxEvolution(); i++) {

			// --------------------------------------------- //
			// Debugging Code - used to get best-so-far graphs of the evolution
			// if (debugOutput) {
			// System.out.println("\n----------------------------");
			// System.out.println("STARTING: " + best.getFitnessValue());
			//
			// for (Gene gene : best.getGenes()) {
			// System.out.print(gene.getAllele() + " ");
			// }
			// System.out.println();
			// }
			// --------------------------------------------- //

			population.evolve();
			best = population.getFittestChromosome();

			// --------------------------------------------- //
			// Debugging Code - used to get best-so-far graphs of the evolution

			// Write evolutionary progress to the file
			// if (debugOutput) {
			// writer.write(i + "\t" + best.getFitnessValue() + "\n");
			// }
			// if (debugOutput) {
			// System.out.println("\nAFTER EVOLUTION: " +
			// best.getFitnessValue());
			// for (Gene gene : best.getGenes()) {
			// System.out.print(gene.getAllele() + " ");
			// }
			// System.out.println("\n----------------------------");
			// }
			// Save the best fitness value for comparison in the next iteration
			// previousBest = (int) best.getFitnessValue();
			// --------------------------------------------- //

			// If the current best value is equal to the last best value
			if (best.getFitnessValue() == previousBest) {

				// If it has been stuck here 30% of the total iterations in a
				// row, just exit. This allows us to exit out of a convergence
				// that is not changing
				if (counter++ == (m_maxEvolution * .3)) {
					System.out.println("Exiting Early");
					return best;
				}

				// If it is the best so far, then keep it
			} else if (best.getFitnessValue() < previousBest) {
				previousBest = (int) best.getFitnessValue();
				bestChromosome = best;
				counter = 0;

				// If worse, reset counter only
			} else if (best.getFitnessValue() > previousBest) {

				counter = 0;

			}

			previousBest = (int) best.getFitnessValue();

		}

		// Return the best solution found during evolution.
		return best;
	}


	/**
	 * @return maximal number of iterations for population to evolve
	 * 
	 * @author Aaron Foltz
	 */
	public int getMaxEvolution() {

		return m_maxEvolution;
	}


	/**
	 * @return population size for this solution
	 */
	public int getPopulationSize() {

		return m_populationSize;
	}


	/**
	 * Gets a number of genes at the start of chromosome, that are excluded from
	 * the swapping. In the Salesman task, the first city in the list should
	 * (where the salesman leaves from) probably should not change as it is part
	 * of the list. The default value is 1.
	 * 
	 * @return start offset for chromosome
	 * 
	 */
	public int getStartOffset() {

		return m_startOffset;
	}


	/**
	 * Set the maximal number of iterations for population to evolve (default
	 * 512).
	 * 
	 * @param a_maxEvolution
	 *            sic
	 * 
	 * @author Aaron Foltz
	 */
	public void setMaxEvolution(final int a_maxEvolution) {

		m_maxEvolution = a_maxEvolution;
	}


	/**
	 * Set an population size for this solution (default 512)
	 * 
	 * @param a_populationSize
	 *            sic
	 */
	public void setPopulationSize(final int a_populationSize) {

		m_populationSize = a_populationSize;
	}


	/**
	 * Sets a number of genes at the start of chromosome, that are excluded from
	 * the swapping. In the Salesman task, the first city in the list should
	 * (where the salesman leaves from) probably should not change as it is part
	 * of the list. The default value is 1.
	 * 
	 * @param a_offset
	 *            start offset for chromosome
	 */
	public void setStartOffset(final int a_offset) {

		m_startOffset = a_offset;
	}

}
