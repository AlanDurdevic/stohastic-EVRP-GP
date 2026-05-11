package hr.fer.seminar.solution.gp.impl;


import java.util.List;

import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Vehicle;
import io.jenetics.Genotype;
import io.jenetics.prog.ProgramGene;
import hr.fer.seminar.operators.cs.CustomerSelector;
import hr.fer.seminar.operators.cs.GPCustomerSelector;
import hr.fer.seminar.operators.vs.SerialVehicleSupplier;
import hr.fer.seminar.operators.vs.VehicleSupplier;
import hr.fer.seminar.solution.gp.GeneticProgrammingEVRP;

public class GeneticProgrammingEVRPSerialVehicle extends GeneticProgrammingEVRP {
	
	public GeneticProgrammingEVRPSerialVehicle(EVRPProblem problem) {
		super(problem);
	}
	
	@Override
	public List<Vehicle> getUsedVehicles(Genotype<ProgramGene<Double>> programGenotype){
		ProgramGene<Double> program = programGenotype.gene();
		List<Vehicle> vehicles = initializeVehicles(getLUNumberOfVehicles());
		CustomerSelector customerSelector = new GPCustomerSelector(program);
		VehicleSupplier vehicleSupplier = new SerialVehicleSupplier(vehicles);
		return getUsedVehicles(customerSelector, vehicleSupplier);
	}

}
