package hr.fer.seminar.solution.gp.impl;

import static io.jenetics.util.RandomRegistry.random;

import java.util.List;
import java.util.concurrent.Executors;

import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.Vehicle;
import hr.fer.seminar.entities.Vehicle.State;
import hr.fer.seminar.operators.InterceptorGP;
import hr.fer.seminar.operators.MyMathOp;
import hr.fer.seminar.solution.gp.GeneticProgrammingStohasticEVRP;
import io.jenetics.EliteSelector;
import io.jenetics.Genotype;
import io.jenetics.Mutator;
import io.jenetics.Phenotype;
import io.jenetics.TournamentSelector;
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

public class GeneticProgrammingStohasticEVRPMultiple{
	
	private static final double VEHICLE_PENALTY_CONSTANT = 0;

	private static final double ENERGY_PENALTY_CONSTANT = 1000;
	
	private static final double LATENCY_PENALTY_CONSTANT = 0;
	
	private final static int MAXIMUM_DEPTH = 255;

	private final static int STARTING_DEPTH = 5;

	private static final int POPULATION_SIZE = 200;

	private static final int ITERATION_NUMBER = 1000;

	private static final int ELITISM_NUMBER = 1;

	private static final double OFFSPRING_FRACTION = 0.05;

	private static final ISeq<Op<Double>> OPERATIONS = ISeq.of(MathOp.SUB, MathOp.ADD, MyMathOp.DIV, MathOp.MUL, MathOp.MAX,
			MathOp.MIN, MathOp.NEG, MyMathOp.POW2, MyMathOp.SQR, MathOp.EXP, MyMathOp.LOG, MyMathOp.MAX0, MyMathOp.MIN0);

	private static final ISeq<Op<Double>> TERMINALS = ISeq.of(Var.of("Eni", 0), Var.of("Dni", 1), Var.of("DDni", 2),
			Var.of("STni", 3), Var.of("RTni", 4), Var.of("Evk", 5), Var.of("Cvk", 6), Var.of("Tvk", 7),
			Var.of("ECni", 8), Var.of("ERPni", 9), Var.of("EDepni", 10), Var.of("ERPpvk", 11), Var.of("EDeppvk", 12),
			Var.of("Var_Dni", 13),Var.of("Var_Sni", 14),Var.of("Var_Tij", 15),Var.of("Slack_TW", 16), Var.of("UC", 17),
			Var.of("DsumUC", 18), Var.of("CsumV", 19), Var.of("BestOtherETA_i", 20), Var.of("Slack_Self", 21),
			EphemeralConst.of(() -> (random().nextInt(11) / 10.)));

	private final List<GeneticProgrammingStohasticEVRP> problems;
	
	public GeneticProgrammingStohasticEVRPMultiple(List<GeneticProgrammingStohasticEVRP> problems) {
		this.problems = problems;
	}
	
	public double error(Genotype<ProgramGene<Double>> gt) {
		double fuel = 0;
		double latency = 0;
		int vehiclesNumber = 0;
		for(GeneticProgrammingStohasticEVRP problem : problems) {
			List<Vehicle> usedVehicles = problem.getUsedVehicles(gt);
			vehiclesNumber += usedVehicles.size();
			for (Vehicle vehicle : usedVehicles) {
				List<Location> route = vehicle.getRoute();
				List<State> states = vehicle.getState();
				for (int i = 0; i < route.size() - 1; i++) {
					fuel += Location.distance(route.get(i), route.get(i + 1)) * problem.getProblem().getFuelConsumptionRate();
					double diff = states.get(i + 1).getCurrentTime() - route.get(i + 1).getDueDate();
					if(diff > 0) {
						latency += diff;
					}
				}
			}
		}
		return ENERGY_PENALTY_CONSTANT * fuel
				+ VEHICLE_PENALTY_CONSTANT * vehiclesNumber + LATENCY_PENALTY_CONSTANT * latency + gt.gene().size();
		
	}
	
	public double errorWD(Genotype<ProgramGene<Double>> gt) {
		double fuel = 0;
		double latency = 0;
		int vehiclesNumber = 0;
		for(GeneticProgrammingStohasticEVRP problem : problems) {
			List<Vehicle> usedVehicles = problem.getUsedVehicles(gt);
			vehiclesNumber += usedVehicles.size();
			for (Vehicle vehicle : usedVehicles) {
				List<Location> route = vehicle.getRoute();
				List<State> states = vehicle.getState();
				for (int i = 0; i < route.size() - 1; i++) {
					fuel += Location.distance(route.get(i), route.get(i + 1)) * problem.getProblem().getFuelConsumptionRate();
					double diff = states.get(i + 1).getCurrentTime() - route.get(i + 1).getDueDate();
					if(diff > 0) {
						latency += diff;
					}
				}
			}
		}
		return ENERGY_PENALTY_CONSTANT * fuel
				+ VEHICLE_PENALTY_CONSTANT * vehiclesNumber + LATENCY_PENALTY_CONSTANT * latency;
		
	}

	public ISeq<Phenotype<ProgramGene<Double>,Double>> calculate() {
		ProgramChromosome<Double> pgf = ProgramChromosome.of(STARTING_DEPTH, ch -> ch.root().depth() <= MAXIMUM_DEPTH,
				OPERATIONS, TERMINALS);
		final Engine<ProgramGene<Double>, Double> engine = Engine.builder(this::error, pgf).minimizing()
				.executor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()))
				.populationSize(POPULATION_SIZE).maximalPhenotypeAge(ITERATION_NUMBER + 1)
				.survivorsSelector(new EliteSelector<>(ELITISM_NUMBER)).offspringSelector(new TournamentSelector<>(3))
				.offspringFraction(OFFSPRING_FRACTION).alterers(new SingleNodeCrossover<>(1), new Mutator<>(0.2))
				.interceptor(new InterceptorGP())
				.build();

		return engine.stream().limit(ITERATION_NUMBER).collect(EvolutionResult.toBestEvolutionResult()).population();

	}
	
	


}
