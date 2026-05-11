package hr.fer.seminar.operators.cs;

import java.util.List;

import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.Vehicle;

public class NNStohasticCustomerSelector implements CustomerSelector{
	

	@Override
	public Customer selectCustomer(Vehicle v, List<Customer> UC, EVRPProblem problem, Vehicle[] vehicles) {
	
		if (UC.isEmpty()) {
			return null;
		}
		
		double minDistance = Double.MAX_VALUE;
		Customer bestCustomer = UC.getFirst();
		for(Customer c : UC) {
			
			double distance = Location.distance(v.getCurrentLocation(), c);
			if(distance < minDistance) {
				minDistance = distance;
				bestCustomer = c;
			}
			
		}
		
		return bestCustomer;
	}

}
