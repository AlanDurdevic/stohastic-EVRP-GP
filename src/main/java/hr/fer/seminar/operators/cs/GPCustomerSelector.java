package hr.fer.seminar.operators.cs;

import java.util.List;

import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.Vehicle;
import io.jenetics.prog.ProgramGene;

public class GPCustomerSelector implements CustomerSelector{
	
	private final ProgramGene<Double> program;
	
	public GPCustomerSelector(ProgramGene<Double> program) {
		this.program = program;
	}

	@Override
	public Customer selectCustomer(Vehicle vehicle, List<Customer> UC, EVRPProblem problem, Vehicle[] vehicles) {
		if (UC.isEmpty()) {
			return null;
		}

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

		Customer bestCustomer = UC.getFirst();
		double bestPriority = Double.MIN_VALUE;
		for (Customer customer : UC) {
			double distance = Location.distance(currentLocation, customer);
			double Eni = fuelConsumptionRate * distance;
			double Dni = customer.getDemand();
			double DDni = customer.getDueDate();
			double STni = customer.getServiceTime();
			double RTni = customer.getReadyTime();
			double ECni = fuelConsumptionRate
					* Math.sqrt(Math.pow(centroidX - customer.getX(), 2) + Math.pow(centroidY - customer.getY(), 2));
			double ERPni = fuelConsumptionRate * Location.distance(customer, customer.getNearestChargingStation());
			double EDepni = fuelConsumptionRate * Location.distance(customer, problem.getDepot());

			Double[] arguments = { Eni, Dni, DDni, STni, RTni, Evk, Cvk, Tvk, ECni, ERPni, EDepni, ERPpvk, EDeppvk };
			double customerPriority = program.apply(arguments);
			if (customerPriority > bestPriority) {
				bestPriority = customerPriority;
				bestCustomer = customer;
			}
		}
		System.out.println(bestPriority);
		return bestCustomer;
	}

}