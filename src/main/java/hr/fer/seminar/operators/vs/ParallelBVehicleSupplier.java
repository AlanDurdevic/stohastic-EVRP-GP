package hr.fer.seminar.operators.vs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Vehicle;
import hr.fer.seminar.operators.VehicleComparator;

public class ParallelBVehicleSupplier implements VehicleSupplier{

	public PriorityQueue<Vehicle> vehicles = new PriorityQueue<>(new VehicleComparator());
	
	private final EVRPProblem problem;
	
	private int nextVehicleNumber;
	
	private static int K = 3;

	public ParallelBVehicleSupplier(List<Vehicle> vehicles, EVRPProblem problem) {
		this.vehicles.addAll(vehicles);
		this.problem = problem;
		nextVehicleNumber = vehicles.size();
	}

	@Override
	public Vehicle getVehicle() {
		if (vehicles.isEmpty()) {
	        return null;
	    }

	    List<Vehicle> candidates = new ArrayList<>();

	    for (int i = 0; i < K && !vehicles.isEmpty(); i++) {
	        candidates.add(vehicles.poll());
	    }

	    candidates.sort(
	        Comparator.comparingDouble(Vehicle::getLoadCapacityLeft).reversed()
	    );

	    Vehicle best = candidates.get(0);

	    for (int i = 1; i < candidates.size(); i++) {
	        vehicles.add(candidates.get(i));
	    }

	    return best;
	}

	@Override
	public void addVehicle(Vehicle vehicle) {
		vehicles.add(vehicle);
	}

	@Override
	public boolean hasMoreVehicles() {
		return !vehicles.isEmpty();
	}

	@Override
	public int getNumberOfVehiclesLeft() {
		return vehicles.size();
	}
	
	@Override
	public void vehicleFinished(Vehicle vehicle, List<Customer> UC) {
		if(UC.isEmpty()) {
			return;
		}
		vehicles.add(new Vehicle(nextVehicleNumber++, problem.getVehicleFuelTankCapacity(), problem.getVehicleLoadCapacity(), problem.getDepot()));
	}
	
	@Override
	public Vehicle[] getVehicles() {
		return vehicles.toArray(new Vehicle[vehicles.size()]);
	}

}
