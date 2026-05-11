package hr.fer.seminar.solution.gp.impl;

import java.util.List;

import hr.fer.seminar.entities.StohasticEVRPProblem;
import hr.fer.seminar.entities.Vehicle;
import hr.fer.seminar.operators.cs.CustomerSelector;
import hr.fer.seminar.operators.vs.ParallelBVehicleSupplier;
import hr.fer.seminar.operators.vs.ParallelVehicleSupplier;
import hr.fer.seminar.operators.vs.SemiParallelBVehicleSupplier;
import hr.fer.seminar.operators.vs.SemiParallelVehicleSupplier;
import hr.fer.seminar.operators.vs.SerialVehicleSupplier;
import hr.fer.seminar.operators.vs.VehicleStrategy;
import hr.fer.seminar.operators.vs.VehicleSupplier;
import hr.fer.seminar.solution.gp.GeneticProgrammingStohasticEVRP;
import io.jenetics.Genotype;
import io.jenetics.prog.ProgramGene;

public class GeneticProgrammingStohasticEVRPHeuristicVehicle extends GeneticProgrammingStohasticEVRP{
	
	private final VehicleStrategy vs;
	
	private final CustomerSelector cs;

	public GeneticProgrammingStohasticEVRPHeuristicVehicle(StohasticEVRPProblem problem, CustomerSelector cs, VehicleStrategy vs) {
		super(problem);
		this.vs = vs;
		this.cs = cs;
	}

	@Override
	public List<Vehicle> getUsedVehicles(Genotype<ProgramGene<Double>> programGenotype) {
		ProgramGene<Double> program = programGenotype.gene();
		List<Vehicle> vehicles = initializeVehicles(getLUNumberOfVehicles());
		CustomerSelector customerSelector = cs;
		VehicleSupplier vehicleSupplier = null;
		switch(vs) {
			case VehicleStrategy.Serial -> vehicleSupplier = new SerialVehicleSupplier(vehicles);
			case VehicleStrategy.SemiParallel -> vehicleSupplier = new SemiParallelVehicleSupplier(vehicles);
			case VehicleStrategy.SemiParallelB -> vehicleSupplier = new SemiParallelBVehicleSupplier(vehicles);
			case VehicleStrategy.Parallel -> vehicleSupplier = new ParallelVehicleSupplier(vehicles, problem);
			case VehicleStrategy.ParallelB -> vehicleSupplier = new ParallelBVehicleSupplier(vehicles, problem);
			default -> throw new RuntimeException();
		}
		return getUsedVehicles(customerSelector, vehicleSupplier);
	}

}
