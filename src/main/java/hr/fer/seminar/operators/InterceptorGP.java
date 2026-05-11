package hr.fer.seminar.operators;

import io.jenetics.engine.EvolutionInterceptor;
import io.jenetics.engine.EvolutionStart;
import io.jenetics.prog.ProgramGene;
import hr.fer.seminar.StohasticPipeline;

public class InterceptorGP implements EvolutionInterceptor<ProgramGene<Double>, Double> {

	@Override
	public EvolutionStart<ProgramGene<Double>, Double> before(final EvolutionStart<ProgramGene<Double>, Double> start) {
		System.out.println(start.generation());
		
		if(start.generation() > 1) {
			double bestFitness = Double.MAX_VALUE;
			for(var gene : start.population()) {
				double fitness = (gene.fitness() - gene.genotype().gene().size()) / 1000;
				if(fitness < bestFitness) {
					bestFitness = fitness;
				}
			}
			System.out.println("BestFitness: " + bestFitness);
			StohasticPipeline.writerIterations.println(bestFitness);
		}
		return start;
	}

}