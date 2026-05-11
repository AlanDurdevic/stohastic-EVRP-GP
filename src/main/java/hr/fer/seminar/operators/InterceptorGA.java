package hr.fer.seminar.operators;

import io.jenetics.EnumGene;
import io.jenetics.engine.EvolutionInterceptor;
import io.jenetics.engine.EvolutionStart;

public class InterceptorGA implements EvolutionInterceptor<EnumGene<Integer>, Double>{
		
		@Override
		public EvolutionStart<EnumGene<Integer>, Double> before(final EvolutionStart<EnumGene<Integer>, Double> start) {
			System.out.println(start.generation());
			return start;
		}
		
	}