package hr.fer.seminar.operators.cs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hr.fer.seminar.entities.ChargingStation;
import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.StohasticEVRPProblem;
import hr.fer.seminar.entities.Vehicle;
import io.jenetics.prog.ProgramGene;

public class GPCustomerSelectorStohastic implements CustomerSelector {
	
	private record VehicleETA(Vehicle vehicle, double eta) {}

	private final int NUMBER_OF_ITERATIONS = 5;
	
	private final int K = 3;

	private final ProgramGene<Double> program;

	public GPCustomerSelectorStohastic(ProgramGene<Double> program) {
		this.program = program;
	}

	@Override
	public Customer selectCustomer(Vehicle vehicle, List<Customer> UC, EVRPProblem p, Vehicle[] otherVehicles) {
		if (UC.isEmpty()) {
			return null;
		}
		double UCn = UC.size();

		double DsumUC = 0;
		for (Customer customer : UC) {
			DsumUC += customer.getDemand();
		}
		
		double CsumV = vehicle.getLoadCapacityLeft();
		double CminV = vehicle.getLoadCapacityLeft();
		for (Vehicle v : otherVehicles) {
			CsumV += v.getLoadCapacityLeft();
			if (v.getLoadCapacityLeft() < CminV) {
				CminV = v.getLoadCapacityLeft();
			}
		}
		
		StohasticEVRPProblem problem = (StohasticEVRPProblem) p;

		double fuelConsumptionRate = problem.getFuelConsumptionRate();
		double Evk = vehicle.getFuelCapacityLeft();
		double Cvk = vehicle.getLoadCapacityLeft();
		double Tvk = vehicle.getCurrentTime();
		double ERPpvk = 0;

		Location currentLocation = vehicle.getCurrentLocation();
		if (currentLocation instanceof Customer) {
			ERPpvk = fuelConsumptionRate
					* Location.distance(((Customer) currentLocation).getNearestChargingStation(), currentLocation);
		}
		double EDeppvk = fuelConsumptionRate * Location.distance(currentLocation, problem.getDepot());

		double centroidX = 0;
		double centroidY = 0;
		for (Customer customer : UC) {
			centroidX += customer.getX();
			centroidY += customer.getY();
		}
		centroidX /= UC.size();
		centroidY /= UC.size();

		Map<Customer, Integer> bestCount = new HashMap<>();
		for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
			Customer bestCustomerIteration = UC.getFirst();
			double bestPriorityIteration = -Double.MAX_VALUE;
			for (Customer customer : UC) {
				double distance = getDistance(vehicle, customer, problem);
				double Eni = fuelConsumptionRate * distance;
				double Dni = customer.getDemand();
				double DDni = customer.getDueDate();
				double STni = customer.getServiceTime();
				double RTni = customer.getReadyTime();
				double ECni = fuelConsumptionRate * Math
						.sqrt(Math.pow(centroidX - customer.getX(), 2) + Math.pow(centroidY - customer.getY(), 2));
				double ERPni = fuelConsumptionRate * Location.distance(customer, customer.getNearestChargingStation());
				double EDepni = fuelConsumptionRate * Location.distance(customer, problem.getDepot());
				double Var_Dni = problem.getDemandDistribution().getCV();
				double Var_Sni = problem.getServiceTimeDistribution().getCV();
				double Var_Tij = problem.getVelocityDistribution().getCV();
				double Slack_TW = customer.getDueDate() - vehicle.getCurrentTime();
				double Slack_Self = customer.getDueDate() - getArrivalTime(vehicle, customer, problem);
				
				List<VehicleETA> etas = new ArrayList<>();
				for(Vehicle v : otherVehicles) {
					double newVelocity = problem.getVelocityDistribution().generate(problem.getAverageVelocity());
					double newEta = Location.distance(v.getCurrentLocation(), customer) / newVelocity;
					etas.add(new VehicleETA(v, newEta));
				}
				etas.sort(Comparator.comparingDouble(VehicleETA::eta));
				
				double BestOtherETA_i = Double.MAX_VALUE;
				int k = Integer.min(etas.size(), K);
				for(int j = 0; j < k; j++) {
					Vehicle v = etas.get(j).vehicle;
					double ETA = getArrivalTime(v, customer, problem);
					if(ETA < BestOtherETA_i) {
						BestOtherETA_i = ETA;
					}
				}			
				if(BestOtherETA_i == Double.MAX_VALUE) {
					BestOtherETA_i = getArrivalTime(vehicle, customer, problem);
				}

				Double[] arguments = { Eni, Dni, DDni, STni, RTni, Evk, Cvk, Tvk, ECni, ERPni, EDepni, ERPpvk, EDeppvk,
						Var_Dni, Var_Sni, Var_Tij, Slack_TW, UCn, DsumUC, CsumV, BestOtherETA_i, Slack_Self};
				double customerPriority = program.apply(arguments);
				if(Double.isNaN(customerPriority) || Double.isInfinite(customerPriority)) {
				    customerPriority = 0;
				}
				if (customerPriority > bestPriorityIteration || customer.equals(UC.getFirst())) {
					bestPriorityIteration = customerPriority;
					bestCustomerIteration = customer;
				}
			
			}
			//System.out.println(bestPriorityIteration);
			bestCount.merge(bestCustomerIteration, 1, Integer::sum);
		}
		
		Customer finalBestCustomer = UC.getFirst();
		int maxCount = -1;
		for (Map.Entry<Customer, Integer> entry : bestCount.entrySet()) {
			//System.out.println(entry.getValue());
		    if (entry.getValue() > maxCount) {
		        maxCount = entry.getValue();
		        finalBestCustomer = entry.getKey();
		    }
		}
		//System.out.println("Kraj");
		return finalBestCustomer;
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

	//get distance from vehicle to customer (check if need to visit charging station)
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
