package hr.fer.seminar.solution.gp;

import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.operators.InterceptorGP;
import hr.fer.seminar.operators.MyMathOp;
import hr.fer.seminar.solution.SolutionEVRP;
import io.jenetics.EliteSelector;
import io.jenetics.Genotype;
import io.jenetics.MonteCarloSelector;
import io.jenetics.Mutator;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.ext.SingleNodeCrossover;
import io.jenetics.prog.ProgramChromosome;
import io.jenetics.prog.ProgramGene;
import io.jenetics.prog.op.EphemeralConst;
import io.jenetics.prog.op.MathOp;
import io.jenetics.prog.op.Op;
import io.jenetics.prog.op.Var;
import io.jenetics.util.ISeq;

import static io.jenetics.util.RandomRegistry.random;

public abstract class GeneticProgrammingEVRP extends SolutionEVRP<ProgramGene<Double>> {

	private final static int MAXIMUM_DEPTH = 255;

	private final static int STARTING_DEPTH = 10;

	private static final int POPULATION_SIZE = 200;

	private static final int ITERATION_NUMBER = 1000;

	private static final int ELITISM_NUMBER = 1;

	private static final double OFFSPRING_FRACTION = 0.6;

	private static final ISeq<Op<Double>> OPERATIONS = ISeq.of(MathOp.SUB, MathOp.ADD, MyMathOp.DIV, MathOp.MAX,
			MathOp.MIN, MathOp.NEG, MyMathOp.POW2, MathOp.SQR, MathOp.EXP, MyMathOp.LOG, MyMathOp.MAX0, MyMathOp.MIN0);

	private static final ISeq<Op<Double>> TERMINALS = ISeq.of(Var.of("Eni", 0), Var.of("Dni", 1), Var.of("DDni", 2),
			Var.of("STni", 3), Var.of("RTni", 4), Var.of("Evk", 5), Var.of("Cvk", 6), Var.of("Tvk", 7),
			Var.of("ECni", 8), Var.of("ERPni", 9), Var.of("EDepni", 10), Var.of("ERPpvk", 11), Var.of("EDeppvk", 12),
			EphemeralConst.of(() -> ((double) random().nextInt(11)) / 10));

	public GeneticProgrammingEVRP(EVRPProblem problem) {
		super(problem);
	}

	@Override
	public double error(Genotype<ProgramGene<Double>> gt) {
		return super.error(gt) + gt.gene().depth();
	}

	@Override
	public Genotype<ProgramGene<Double>> calculate() {
		ProgramChromosome<Double> pgf = ProgramChromosome.of(STARTING_DEPTH, ch -> ch.root().size() <= MAXIMUM_DEPTH,
				OPERATIONS, TERMINALS);
		final Engine<ProgramGene<Double>, Double> engine = Engine.builder(this::error, pgf).minimizing()
				.populationSize(POPULATION_SIZE).maximalPhenotypeAge(ITERATION_NUMBER + 1)
				.survivorsSelector(new EliteSelector<>(ELITISM_NUMBER)).offspringSelector(new MonteCarloSelector<>())
				.offspringFraction(OFFSPRING_FRACTION).alterers(new SingleNodeCrossover<>(1), new Mutator<>(0.2))
				.interceptor(new InterceptorGP()).build();

		final EvolutionResult<ProgramGene<Double>, Double> result = engine.stream().limit(ITERATION_NUMBER)
				.collect(EvolutionResult.toBestEvolutionResult());

		return result.bestPhenotype().genotype();
	}

}
