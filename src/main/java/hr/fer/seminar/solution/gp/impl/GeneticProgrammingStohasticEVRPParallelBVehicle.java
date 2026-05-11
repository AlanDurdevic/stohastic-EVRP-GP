package hr.fer.seminar.solution.gp.impl;

import java.util.List;

import hr.fer.seminar.entities.StohasticEVRPProblem;
import hr.fer.seminar.entities.Vehicle;
import hr.fer.seminar.operators.cs.CustomerSelector;
import hr.fer.seminar.operators.cs.GPCustomerSelectorStohastic;
import hr.fer.seminar.operators.vs.ParallelBVehicleSupplier;
import hr.fer.seminar.operators.vs.VehicleSupplier;
import hr.fer.seminar.solution.gp.GeneticProgrammingStohasticEVRP;
import io.jenetics.Genotype;
import io.jenetics.prog.ProgramGene;

public class GeneticProgrammingStohasticEVRPParallelBVehicle extends GeneticProgrammingStohasticEVRP{

	public GeneticProgrammingStohasticEVRPParallelBVehicle(StohasticEVRPProblem problem) {
		super(problem);
	}
	
	@Override
	public List<Vehicle> getUsedVehicles(Genotype<ProgramGene<Double>> programGenotype){
		ProgramGene<Double> program = programGenotype.gene();
		List<Vehicle> vehicles = initializeVehicles(getLUNumberOfVehicles());
		CustomerSelector customerSelector = new GPCustomerSelectorStohastic(program);
		VehicleSupplier vehicleSupplier = new ParallelBVehicleSupplier(vehicles, problem);
		return getUsedVehicles(customerSelector, vehicleSupplier);
	}


}
