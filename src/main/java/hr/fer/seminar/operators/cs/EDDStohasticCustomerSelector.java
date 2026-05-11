package hr.fer.seminar.operators.cs;

import java.util.List;

import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Vehicle;

public class EDDStohasticCustomerSelector implements CustomerSelector{
	

	@Override
	public Customer selectCustomer(Vehicle v, List<Customer> UC, EVRPProblem problem, Vehicle[] vehicles) {
	
		if (UC.isEmpty()) {
			return null;
		}
		
		double minDD = Double.MAX_VALUE;
		Customer bestCustomer = UC.getFirst();
		for(Customer c : UC) {
			
			double DD = c.getDueDate();
			if(DD < minDD) {
				minDD = DD;
				bestCustomer = c;
			}
			
		}
		
		return bestCustomer;
	}

}
