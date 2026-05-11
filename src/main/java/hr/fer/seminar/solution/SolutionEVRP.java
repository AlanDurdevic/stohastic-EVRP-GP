package hr.fer.seminar.solution;

import java.util.ArrayList;
import java.util.List;

import hr.fer.seminar.entities.ChargingStation;
import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.Depot;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.Vehicle;
import hr.fer.seminar.entities.Vehicle.State;
import hr.fer.seminar.operators.cs.CustomerSelector;
import hr.fer.seminar.operators.vs.VehicleSupplier;
import io.jenetics.Gene;
import io.jenetics.Genotype;

public abstract class SolutionEVRP<T extends Gene<?, T>> {

	private static final double VEHICLE_PENALTY_CONSTANT = 100;

	private static final double ENERGY_PENALTY_CONSTANT = 0;
	
	private static final double LATENCY_PENALTY_CONSTANT = 1;

	protected final EVRPProblem problem;

	public abstract Genotype<T> calculate();

	public abstract List<Vehicle> getUsedVehicles(Genotype<T> gt);

	public SolutionEVRP(EVRPProblem problem) {
		this.problem = problem;
	}
	
	public EVRPProblem getProblem() {
		return problem;
	}

	public double error(Genotype<T> gt) {
		List<Vehicle> usedVehicles = getUsedVehicles(gt);
		double distance = 0;
		double latency = 0;
		for (Vehicle vehicle : usedVehicles) {
			List<Location> route = vehicle.getRoute();
			List<State> states = vehicle.getState();
			for (int i = 0; i < route.size() - 1; i++) {
				distance += Location.distance(route.get(i), route.get(i + 1));
				double diff = states.get(i + 1).getCurrentTime() - route.get(i + 1).getDueDate();
				if(diff > 0) {
					latency += diff;
				}
			}
		}
		return ENERGY_PENALTY_CONSTANT * distance * problem.getFuelConsumptionRate()
				+ VEHICLE_PENALTY_CONSTANT * usedVehicles.size() + LATENCY_PENALTY_CONSTANT * latency;
	}

	protected List<Vehicle> getUsedVehicles(CustomerSelector cs, VehicleSupplier vehicleSupplier) {
		int numberOfVehicles = vehicleSupplier.getNumberOfVehiclesLeft();
		List<Vehicle> usedVehicles = new ArrayList<>();
		Depot depot = problem.getDepot();
		double averageVelocity = problem.getAverageVelocity();
		double fuelConsumptionRate = problem.getFuelConsumptionRate();
		List<Customer> UC = initializeUC();
		while (!UC.isEmpty() || vehicleSupplier.hasMoreVehicles()) {
			Location destination = null;
			Vehicle v = vehicleSupplier.getVehicle();
			Customer c = cs.selectCustomer(v, UC, problem, vehicleSupplier.getVehicles());
			if (c == null) {
				destination = depot;
			} else {
				if (v.getLoadCapacityLeft() >= c.getDemand()) {
					double timeNeeded = Location.distance(v.getCurrentLocation(), c) / averageVelocity;
					if (v.getCurrentTime() + timeNeeded <= c.getDueDate()) {
						destination = c;
					} else {
						destination = depot;
					}
				} else {
					destination = depot;
				}
			}

			if (destination instanceof Customer && !checkIfVehicleCanReturnToDepot(v, c)) {
				if(checkIfCanVisitChargingStationBeforeLastCustomer(v, c)) {
					// choose charging station
					charge(v, chooseChargingStationBeforeDepot(v, c));
					// go to customer
					double distance = Location.distance(v.getCurrentLocation(), destination);
					double time = distance / averageVelocity;
					v.addTime(time);
					double fuel = fuelConsumptionRate * distance;
					v.subtractFuelCapacity(fuel);
					v.addLocation(destination);
					// wait if arrive early
					if (v.getCurrentTime() < destination.getReadyTime()) {
						v.addTime(destination.getReadyTime() - v.getCurrentTime());
					}
					// serving customer
					v.addTime(destination.getServiceTime());
					v.subtractLoadCapacity(destination.getDemand());
					UC.remove(c);
				}
				destination = depot;
			}

			double fuelNeeded = fuelConsumptionRate * (Location.distance(v.getCurrentLocation(), destination));
			if (destination instanceof Customer) {
				fuelNeeded += fuelConsumptionRate
						* Location.distance(destination, ((Customer) destination).getNearestChargingStation());
			}
			if (v.getFuelCapacityLeft() < fuelNeeded) {
				ChargingStation chargingStation = chooseChargingStation(v, destination);
				if (chargingStation == null) {
					double fuel = fuelConsumptionRate * Location.distance(v.getCurrentLocation(), depot);
					if (fuel > v.getFuelCapacityLeft()) {
						chargingStation = ((Customer) v.getCurrentLocation()).getNearestChargingStation();
					}
					destination = depot;
				}
				if (chargingStation != null) {
					charge(v, chargingStation);
				}

			}
			// travel to destination
			double distance = Location.distance(v.getCurrentLocation(), destination);
			double time = distance / averageVelocity;
			v.addTime(time);
			double fuel = fuelConsumptionRate * distance;
			v.subtractFuelCapacity(fuel);
			v.addLocation(destination);
			
			if (destination instanceof Customer) {
				// arrives before
				if (v.getCurrentTime() < destination.getReadyTime()) {
					v.addTime(destination.getReadyTime() - v.getCurrentTime());
				}
				// serving customer
				v.addTime(destination.getServiceTime());
				v.subtractLoadCapacity(destination.getDemand());
				UC.remove(c);
				vehicleSupplier.addVehicle(v);
			} else {
				// returning vehicle to depot
				usedVehicles.add(v);
				vehicleSupplier.vehicleFinished(v, UC);

				if (!vehicleSupplier.hasMoreVehicles() && !UC.isEmpty()) {
					vehicleSupplier.addVehicle(new Vehicle(numberOfVehicles++, problem.getVehicleFuelTankCapacity(),
							problem.getVehicleLoadCapacity(), depot));
				}
			}

		}

		return usedVehicles;
	}

	// check if vehicle can visit charging station before last customer
	protected boolean checkIfCanVisitChargingStationBeforeLastCustomer(Vehicle v, Customer customer) {
		Depot depot = problem.getDepot();
		double fuelConsumptionRate = problem.getFuelConsumptionRate();
		double averageVelocity = problem.getAverageVelocity();
		double vehicleFuelTankCapacity = problem.getVehicleFuelTankCapacity();
		double inverseRefuelingRate = problem.getInverseRefuelingRate();
		
		double currentTime = v.getCurrentTime();
		Location currentLocation = v.getCurrentLocation();
		double fuelLeft = v.getFuelCapacityLeft();
		
		// choose charging station
		ChargingStation cs = chooseChargingStationBeforeDepot(v, customer);
		
		if(cs == null) {
			return false;
		}
		
		// go to charging station
		double distanceCS = Location.distance(currentLocation, cs);
		fuelLeft -= fuelConsumptionRate*distanceCS;
		currentTime += distanceCS / averageVelocity;
		currentLocation = cs;
		
		// charge
		currentTime += (vehicleFuelTankCapacity - fuelLeft) * inverseRefuelingRate;
		fuelLeft = vehicleFuelTankCapacity;
		
		// go to customer
		double distanceC = Location.distance(currentLocation, customer);
		currentTime += distanceC / averageVelocity;
		fuelLeft -= distanceC * fuelConsumptionRate;
		currentLocation = customer;
		// wait if arrive early
		if(currentTime < customer.getReadyTime()) {
			currentTime = customer.getReadyTime();
		}
		// serve customer
		currentTime += customer.getServiceTime();
		
		// return to depot
		double distanceD = Location.distance(customer, depot);
		currentTime += distanceD / averageVelocity;
		fuelLeft -= distanceD * fuelConsumptionRate;
		
		if(fuelLeft < 0) {
			return false;
		}
		
		if(currentTime > depot.getDueDate()) {
			return false;
		}
		return true;
	}

	// choose charging station before return depot
	// case when charging before visiting last customer
	protected ChargingStation chooseChargingStationBeforeDepot(Vehicle v, Customer destination) {
		Location currentLocation = v.getCurrentLocation();
		Depot depot = problem.getDepot();
		double vehicleFullTankCapacity = problem.getVehicleFuelTankCapacity();
		double inverseRefuelingRate = problem.getInverseRefuelingRate();
		double averageVelocity = problem.getAverageVelocity();
		double fuelConsumptionRate = problem.getFuelConsumptionRate();
		ChargingStation bestChargingStation = null;
		double bestEnergy = Double.MAX_VALUE;
		for (ChargingStation cs : problem.getChargingStations()) {
			double fuelCapacityLeft = v.getFuelCapacityLeft();
			double distanceLCS = Location.distance(currentLocation, cs);
			double fuelLCS = fuelConsumptionRate * distanceLCS;
			fuelCapacityLeft -= fuelLCS;
			// check if vehicle can reach charging station
			if (fuelCapacityLeft > 0) {
				// get to charging station
				double time = distanceLCS / averageVelocity;
				// charge vehicle
				time += (vehicleFullTankCapacity - fuelCapacityLeft) * inverseRefuelingRate;
				// get to destination from charging station
				double distanceCSD = Location.distance(cs, destination);
				time += distanceCSD / averageVelocity;
				// check if vehicle can reach destination and depot
				double fuelNeeded = fuelConsumptionRate
						* (distanceCSD + Location.distance(destination, depot));
				if (fuelNeeded <= vehicleFullTankCapacity) {
					// check time
					if (time + v.getCurrentTime() <= destination.getDueDate()) {
						
						// go to customer
						time += v.getCurrentTime();
						// wait if arrive early
						if(time < destination.getReadyTime()) {
							time = destination.getReadyTime();
						}
						// serve customer
						time += destination.getServiceTime();
						// return to depot
						double distanceD = Location.distance(destination, depot);
						time += distanceD / averageVelocity;
						if(time < depot.getDueDate()) {
							double energy = fuelConsumptionRate * (distanceLCS + distanceCSD);
							if (energy < bestEnergy) {
								bestChargingStation = cs;
								bestEnergy = energy;
							}
						}
					}
				}
			}
		}
		return bestChargingStation;
	}

	// check if vehicle can return to depot in time from every customer
	protected boolean checkIfVehicleCanReturnToDepot(Vehicle v, Customer customer) {
		Depot depot = problem.getDepot();
		double fuelConsumptionRate = problem.getFuelConsumptionRate();
		double averageVelocity = problem.getAverageVelocity();
		double vehicleFuelTankCapacity = problem.getVehicleFuelTankCapacity();
		double inverseRefuelingRate = problem.getInverseRefuelingRate();
		
		double currentTime = v.getCurrentTime();
		Location currentLocation = v.getCurrentLocation();
		double fuelLeft = v.getFuelCapacityLeft();

		// has enough fuel to visit customer and nearest charging station
		double fuelNeeded = fuelConsumptionRate * (Location.distance(currentLocation, customer));
		fuelNeeded += fuelConsumptionRate
				* Location.distance(customer, customer.getNearestChargingStation());
		if (fuelLeft < fuelNeeded) {
			// choose charging station
			ChargingStation cs = chooseChargingStation(v, customer);
			if(cs == null) {
				return false;
			}
			// go to charging station
			double distanceCS = Location.distance(currentLocation, cs);
			currentTime += distanceCS / averageVelocity;
			fuelLeft -= distanceCS * fuelConsumptionRate;
			currentLocation = cs;
			
			// charge
			currentTime += (vehicleFuelTankCapacity - fuelLeft) * inverseRefuelingRate;
			fuelLeft = vehicleFuelTankCapacity;
		}

		// go to customer
		double distanceC = Location.distance(currentLocation, customer);
		currentTime += distanceC / averageVelocity;
		fuelLeft -= distanceC * fuelConsumptionRate;
		currentLocation = customer;
		// wait if arrive early
		if (currentTime < currentLocation.getReadyTime()) {
			currentTime = currentLocation.getReadyTime();
		}
		// serve customer
		currentTime += currentLocation.getServiceTime();

		// check if can reach depot (enough fuel)
		double distanceD = Location.distance(currentLocation, depot);
		fuelNeeded = distanceD * fuelConsumptionRate;
		if (fuelNeeded > fuelLeft) {
			// choose charging station
			ChargingStation cs = ((Customer)currentLocation).getNearestChargingStation();
			// go to charging station
			double distanceCS = Location.distance(currentLocation, cs);
			currentTime += distanceCS / averageVelocity;
			fuelLeft -= distanceCS * fuelConsumptionRate;
			currentLocation = cs;
			
			// charge
			currentTime += (vehicleFuelTankCapacity - fuelLeft) * inverseRefuelingRate;
			fuelLeft = vehicleFuelTankCapacity;
		}

		// return to depot
		distanceD = Location.distance(currentLocation, depot);
		currentTime += distanceD / averageVelocity;
		fuelLeft -= distanceD * fuelConsumptionRate;

		if (currentTime > depot.getDueDate()) {
			return false;
		}
		return true;
	}

	protected List<Vehicle> initializeVehicles(int LB) {
		List<Vehicle> V = new ArrayList<>();
		Depot startingLocation = problem.getDepot();
		double fuelCapacity = problem.getVehicleFuelTankCapacity();
		double loadCapacity = problem.getVehicleLoadCapacity();
		for (int i = 0; i < LB; i++) {
			Vehicle newVehicle = new Vehicle(i, fuelCapacity, loadCapacity, startingLocation);
			V.add(newVehicle);
		}
		return V;
	}

	protected int getLUNumberOfVehicles() {
		double sum = 0;
		for (Customer customer : problem.getCustomers()) {
			sum += customer.getDemand();
		}
		return (int) Math.ceil(sum / problem.getVehicleLoadCapacity());
	}

	protected void charge(Vehicle v, ChargingStation chargingStation) {
		// travel to charging station
		double distance = Location.distance(v.getCurrentLocation(), chargingStation);
		double time = distance / problem.getAverageVelocity();
		v.addTime(time);
		double fuel = problem.getFuelConsumptionRate() * distance;
		v.subtractFuelCapacity(fuel);
		v.addLocation(chargingStation);
		// charging vehicle
		double capacityToCharge = problem.getVehicleFuelTankCapacity() - v.getFuelCapacityLeft();
		time = capacityToCharge * problem.getInverseRefuelingRate();
		v.addTime(time);
		v.setFuelCapacity(problem.getVehicleFuelTankCapacity());
	}

	// choose station to preserve time window
	protected ChargingStation chooseChargingStation(Vehicle v, Location location) {
		if (location instanceof Depot) {
			return null;
		}
		Location currentLocation = v.getCurrentLocation();
		Customer destination = (Customer) location;
		double vehicleFullTankCapacity = problem.getVehicleFuelTankCapacity();
		double inverseRefuelingRate = problem.getInverseRefuelingRate();
		double averageVelocity = problem.getAverageVelocity();
		double fuelConsumptionRate = problem.getFuelConsumptionRate();
		ChargingStation bestChargingStation = null;
		double bestEnergy = Double.MAX_VALUE;
		for (ChargingStation cs : problem.getChargingStations()) {
			double fuelCapacityLeft = v.getFuelCapacityLeft();
			double distanceLCS = Location.distance(currentLocation, cs);
			double fuelLCS = fuelConsumptionRate * distanceLCS;
			fuelCapacityLeft -= fuelLCS;
			// check if vehicle can reach charging station
			if (fuelCapacityLeft > 0) {
				// get to charging station
				double time = distanceLCS / averageVelocity;
				// charge vehicle
				time += (vehicleFullTankCapacity - fuelCapacityLeft) * inverseRefuelingRate;
				// get to destination from charging station
				double distanceCSD = Location.distance(cs, destination);
				time += distanceCSD / averageVelocity;
				// check if vehicle can reach destination and nearest charging station
				double fuelNeeded = fuelConsumptionRate
						* (distanceCSD + Location.distance(destination, destination.getNearestChargingStation()));
				if (fuelNeeded <= vehicleFullTankCapacity) {
					// check time
					if (time + v.getCurrentTime() <= destination.getDueDate()) {
						double energy = fuelConsumptionRate * (distanceLCS + distanceCSD);
						if (energy < bestEnergy) {
							bestChargingStation = cs;
							bestEnergy = energy;
						}
					}
				}
			}
		}
		return bestChargingStation;
	}

	protected List<Customer> initializeUC() {
		List<Customer> UC = new ArrayList<>();
		UC.addAll(problem.getCustomers());
		return UC;
	}

}
