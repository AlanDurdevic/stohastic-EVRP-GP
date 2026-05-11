package hr.fer.seminar.solution.ga;

import java.util.HashMap;
import java.util.Map;

import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.operators.InterceptorGA;
import hr.fer.seminar.solution.SolutionEVRP;
import io.jenetics.EliteSelector;
import io.jenetics.EnumGene;
import io.jenetics.Genotype;
import io.jenetics.PartiallyMatchedCrossover;
import io.jenetics.PermutationChromosome;
import io.jenetics.SwapMutator;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.Factory;

public abstract class GeneticAlgorithmEVRP extends SolutionEVRP<EnumGene<Integer>> {

	private static int ITERATION_NUMBER = 1000;

	private static int POPULATION_SIZE = 200;

	private static double SWAP_MUTATOR = 0.2;

	private static double PMX = 1;

	private static int ELITISM_NUMBER = 1;
	
	private static final double OFFSPRING_FRACTION = 0.6;
	
	protected Map<Integer, Customer> customerMap;

	public GeneticAlgorithmEVRP(EVRPProblem problem) {
		super(problem);
		customerMap = new HashMap<>();
		int i = 0;
		for(Customer customer : problem.getCustomers()) {
			customerMap.put(i++, customer);
		}
	}

	@Override
	public Genotype<EnumGene<Integer>> calculate() {
		Factory<Genotype<EnumGene<Integer>>> gtf = Genotype
				.of(PermutationChromosome.ofInteger(problem.getCustomers().size()));
		Engine<EnumGene<Integer>, Double> engine = Engine.builder(this::error, gtf).populationSize(POPULATION_SIZE)
				.maximalPhenotypeAge(ITERATION_NUMBER + 1).minimizing()
				.interceptor(new InterceptorGA())
				.offspringFraction(OFFSPRING_FRACTION)
				.alterers(new PartiallyMatchedCrossover<>(PMX), new SwapMutator<>(SWAP_MUTATOR))
				.survivorsSelector(new EliteSelector<EnumGene<Integer>, Double>(ELITISM_NUMBER)).build();
		Genotype<EnumGene<Integer>> result = engine.stream().limit(ITERATION_NUMBER)
				.collect(EvolutionResult.toBestGenotype());
		return result;
	}

}
