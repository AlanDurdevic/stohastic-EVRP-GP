package hr.fer.seminar.operators.vs;

import java.util.List;
import java.util.PriorityQueue;

import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Vehicle;
import hr.fer.seminar.operators.VehicleComparator;

public class ParallelVehicleSupplier implements VehicleSupplier{

	public PriorityQueue<Vehicle> vehicles = new PriorityQueue<>(new VehicleComparator());
	
	private final EVRPProblem problem;
	
	private int nextVehicleNumber;

	public ParallelVehicleSupplier(List<Vehicle> vehicles, EVRPProblem problem) {
		this.vehicles.addAll(vehicles);
		this.problem = problem;
		nextVehicleNumber = vehicles.size();
	}

	@Override
	public Vehicle getVehicle() {
		return vehicles.poll();
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
