package hr.fer.seminar.solution.ga.impl;

import java.util.List;

import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Vehicle;
import hr.fer.seminar.operators.cs.CustomerSelector;
import hr.fer.seminar.operators.cs.GACustomerSelector;
import hr.fer.seminar.operators.vs.SemiParallelVehicleSupplier;
import hr.fer.seminar.operators.vs.VehicleSupplier;
import hr.fer.seminar.solution.ga.GeneticAlgorithmEVRP;
import io.jenetics.EnumGene;
import io.jenetics.Genotype;
import io.jenetics.PermutationChromosome;

public class GeneticAlgorithmEVRPSemiParallelVehicle extends GeneticAlgorithmEVRP{

	public GeneticAlgorithmEVRPSemiParallelVehicle(EVRPProblem problem) {
		super(problem);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Vehicle> getUsedVehicles(Genotype<EnumGene<Integer>> gt) {
		PermutationChromosome<Integer> c = gt.chromosome().as(PermutationChromosome.class);
		List<Vehicle> vehicles = initializeVehicles(getLUNumberOfVehicles());
		CustomerSelector customerSelector = new GACustomerSelector(c, customerMap);
		VehicleSupplier vehicleSupplier = new SemiParallelVehicleSupplier(vehicles);
		return getUsedVehicles(customerSelector, vehicleSupplier);
	}

}
