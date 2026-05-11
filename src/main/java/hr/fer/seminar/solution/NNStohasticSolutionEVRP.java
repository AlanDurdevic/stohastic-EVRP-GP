package hr.fer.seminar.solution;

import java.util.List;

import hr.fer.seminar.entities.StohasticEVRPProblem;
import hr.fer.seminar.entities.Vehicle;
import hr.fer.seminar.operators.cs.CustomerSelector;
import hr.fer.seminar.operators.vs.VehicleSupplier;
import io.jenetics.Gene;
import io.jenetics.Genotype;

public class NNStohasticSolutionEVRP<T extends Gene<?, T>> extends StohasticSolutionEVRP<T>{
	
	private final CustomerSelector cs;
	
	private final VehicleSupplier vs;

	public NNStohasticSolutionEVRP(StohasticEVRPProblem problem, CustomerSelector cs, VehicleSupplier vs) {
		super(problem);
		this.cs = cs;
		this.vs = vs;
	}

	@Override
	public Genotype<T> calculate() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<Vehicle> getUsedVehicles(Genotype<T> gt) {
		throw new UnsupportedOperationException();
	}

	public List<Vehicle> getUsedVehicles() {
		return getUsedVehicles(cs, vs);
	}
	
	

}
