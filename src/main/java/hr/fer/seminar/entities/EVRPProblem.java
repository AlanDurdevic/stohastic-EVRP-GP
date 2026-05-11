package hr.fer.seminar.entities;

import java.util.List;

public class EVRPProblem {

	private final Depot depot;

	private final double dueDate;

	private final double vehicleFuelTankCapacity;

	private final double vehicleLoadCapacity;

	private final double fuelConsumptionRate;

	private final double inverseRefuelingRate;

	private final double averageVelocity;

	private final List<Customer> customers;

	private final List<ChargingStation> chargingStations;

	public EVRPProblem(Depot depot, double dueDate, double vehicleFuelTankCapacity, double vehicleLoadCapacity,
			double fuelConsumptionRate, double inverseRefuelingRate, double averageVelocity, List<Customer> customers,
			List<ChargingStation> chargingStations) {
		this.depot = depot;
		this.dueDate = dueDate;
		this.vehicleFuelTankCapacity = vehicleFuelTankCapacity;
		this.vehicleLoadCapacity = vehicleLoadCapacity;
		this.fuelConsumptionRate = fuelConsumptionRate;
		this.inverseRefuelingRate = inverseRefuelingRate;
		this.averageVelocity = averageVelocity;
		this.customers = customers;
		this.chargingStations = chargingStations;
	}

	public Depot getDepot() {
		return depot;
	}

	public double getDueDate() {
		return dueDate;
	}

	public double getVehicleFuelTankCapacity() {
		return vehicleFuelTankCapacity;
	}

	public double getVehicleLoadCapacity() {
		return vehicleLoadCapacity;
	}

	public double getFuelConsumptionRate() {
		return fuelConsumptionRate;
	}

	public double getInverseRefuelingRate() {
		return inverseRefuelingRate;
	}

	public double getAverageVelocity() {
		return averageVelocity;
	}

	public List<Customer> getCustomers() {
		return customers;
	}

	public List<ChargingStation> getChargingStations() {
		return chargingStations;
	}

	@Override
	public String toString() {
		return "EVRPProblem [depot=" + depot + ", dueDate=" + dueDate + ", vehicleFuelTankCapacity="
				+ vehicleFuelTankCapacity + ", vehicleLoadCapacity=" + vehicleLoadCapacity + ", fuelConsumptionRate="
				+ fuelConsumptionRate + ", inverseRefuelingRate=" + inverseRefuelingRate + ", averageVelocity="
				+ averageVelocity + "]";
	}

}
