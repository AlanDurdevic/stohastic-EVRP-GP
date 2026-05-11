package hr.fer.seminar.operators.cs;

import java.util.List;

import hr.fer.seminar.entities.ChargingStation;
import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.StohasticEVRPProblem;
import hr.fer.seminar.entities.Vehicle;

public class MSStohasticCustomerSelector implements CustomerSelector {

	@Override
	public Customer selectCustomer(Vehicle v, List<Customer> UC, EVRPProblem problem, Vehicle[] vehicles) {

		if (UC.isEmpty()) {
			return null;
		}

		double minSlack = Double.MAX_VALUE;
		Customer bestCustomer = UC.getFirst();
		for (Customer c : UC) {

			double slack = c.getDueDate() - getArrivalTime(v, c, (StohasticEVRPProblem) problem);
			if (slack < minSlack) {
				minSlack = slack;
				bestCustomer = c;
			}

		}

		return bestCustomer;
	}
	
	private double getArrivalTime(Vehicle vehicle, Customer customer, StohasticEVRPProblem problem) {
		double fuelConsumptionRate = problem.getFuelConsumptionRate();
		Location currentLocation = vehicle.getCurrentLocation();
		Location depot = problem.getDepot();
		Location destination = customer;
		double time = 0;
		
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
				double distance = Location.distance(currentLocation, chargingStation);
				double velocity = problem.getVelocityDistribution().generate(problem.getAverageVelocity());
				time += distance / velocity;
				currentLocation = chargingStation;
				
				double fuelCapacityLeft = vehicle.getFuelCapacityLeft() - distance * fuelConsumptionRate;
				double fuelToCharge = problem.getVehicleFuelTankCapacity() - fuelCapacityLeft;
				time += fuelToCharge * problem.getInverseRefuelingRate();
			}

		}
		
		double distance = Location.distance(currentLocation, destination);
		double velocity = problem.getVelocityDistribution().generate(problem.getAverageVelocity());
		time += distance / velocity;
		
		return time;
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
