package hr.fer.seminar.operators.cs;

import java.util.List;

import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Vehicle;

public interface CustomerSelector {
	
	Customer selectCustomer(Vehicle v, List<Customer> UC, EVRPProblem problem, Vehicle[] vehicles);

}
