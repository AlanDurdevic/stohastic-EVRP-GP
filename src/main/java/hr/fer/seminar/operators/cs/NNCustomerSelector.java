package hr.fer.seminar.operators.cs;

import java.util.List;

import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.Vehicle;

public class NNCustomerSelector implements CustomerSelector{

	@Override
	public Customer selectCustomer(Vehicle v, List<Customer> UC, EVRPProblem problem, Vehicle[] vehicles) {
		if(UC.isEmpty())
			return null;
		Location currentLocation = v.getCurrentLocation();
		Customer nearestCustomer = UC.getFirst();
		double closestDistance = Location.distance(currentLocation, nearestCustomer);
		for(Customer customer : UC) {
			double newDistance = Location.distance(currentLocation, customer);
			if(newDistance < closestDistance) {
				closestDistance = newDistance;
				nearestCustomer = customer;
			}		
		}
		return nearestCustomer;
	}

}
