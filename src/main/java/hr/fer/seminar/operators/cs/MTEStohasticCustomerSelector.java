package hr.fer.seminar.operators.cs;

import java.util.List;

import hr.fer.seminar.entities.ChargingStation;
import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.StohasticEVRPProblem;
import hr.fer.seminar.entities.Vehicle;

public class MTEStohasticCustomerSelector implements CustomerSelector {

	@Override
	public Customer selectCustomer(Vehicle v, List<Customer> UC, EVRPProblem problem, Vehicle[] vehicles) {

		if (UC.isEmpty()) {
			return null;
		}

		double minTE = Double.MAX_VALUE;
		Customer bestCustomer = UC.getFirst();
		for (Customer c : UC) {

			double distance = getDistance(v, c, (StohasticEVRPProblem) problem);
			double TE = problem.getFuelConsumptionRate() * distance;
			if (TE < minTE) {
				minTE = TE;
				bestCustomer = c;
			}

		}

		return bestCustomer;
	}

	// get distance from vehicle to customer (check if need to visit charging
	// station)
	private double getDistance(Vehicle vehicle, Customer customer, StohasticEVRPProblem problem) {
		double fuelConsumptionRate = problem.getFuelConsumptionRate();
		Location currentLocation = vehicle.getCurrentLocation();
		Location depot = problem.getDepot();
		Location destination = customer;
		double distance = 0;

		double fuelNeeded = fuelConsumptionRate * (Location.distance(currentLocation, customer));
		fuelNeeded += fuelConsumptionRate * Location.distance(customer, customer.getNearestChargingStation());
		if (vehicle.getFuelCapacityLeft() < fuelNeeded) {
			ChargingStation chargingStation = chooseChargingStation(vehicle, customer, problem);
			if (chargingStation == null) {
				double fuel = fuelConsumptionRate * Location.distance(currentLocation, depot);
				if (fuel > vehicle.getFuelCapacityLeft()) {
					chargingStation = chooseChargingStation(vehicle, depot, problem);
				}
				destination = depot;
			}
			if (chargingStation != null) {
				distance += Location.distance(currentLocation, chargingStation);
				currentLocation = chargingStation;
			}

		}

		distance += Location.distance(currentLocation, destination);

		return distance;
	}

	protected ChargingStation chooseChargingStation(Vehicle v, Location location, StohasticEVRPProblem problem) {
		Location currentLocation = v.getCurrentLocation();
		Location destination = location;
		double vehicleFullTankCapacity = problem.getVehicleFuelTankCapacity();
		double fuelConsumptionRate = problem.getFuelConsumptionRate();

		ChargingStation bestChargingStation = null;
		double bestEnergy = Double.MAX_VALUE;
		for (ChargingStation cs : problem.getChargingStations()) {
			double fuelCapacityLeft = v.getFuelCapacityLeft();
			double distanceLCS = Location.distance(currentLocation, cs);
			double fuelLCS = fuelConsumptionRate * distanceLCS;
			fuelCapacityLeft -= fuelLCS;
			// check if vehicle can reach charging station
			if (fuelCapacityLeft >= 0) {
				// get to destination from charging station
				double distanceCSD = Location.distance(cs, destination);
				// check if vehicle can reach destination and nearest charging station
				double fuelNeeded = fuelConsumptionRate
						* (distanceCSD + Location.distance(destination, destination.getNearestChargingStation()));
				if (fuelNeeded <= vehicleFullTankCapacity) {
					double energy = fuelConsumptionRate * (distanceLCS + distanceCSD);
					if (energy < bestEnergy) {
						bestChargingStation = cs;
						bestEnergy = energy;
					}
				}

			}
		}
		return bestChargingStation;
	}

}
