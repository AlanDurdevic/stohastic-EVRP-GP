package hr.fer.seminar.operators.vs;

import java.util.List;
import java.util.Stack;

import hr.fer.seminar.entities.Vehicle;

public class SerialVehicleSupplier implements VehicleSupplier{
	
	private final Stack<Vehicle> vehicles = new Stack<>();
	
	public SerialVehicleSupplier(List<Vehicle> vehicles) {
		this.vehicles.addAll(vehicles.reversed());
	}

	@Override
	public Vehicle getVehicle() {
		return vehicles.pop();
	}
	
	@Override
	public void addVehicle(Vehicle vehicle) {
		vehicles.push(vehicle);
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
	public Vehicle[] getVehicles() {
		return vehicles.toArray(new Vehicle[vehicles.size()]);
	}


}
