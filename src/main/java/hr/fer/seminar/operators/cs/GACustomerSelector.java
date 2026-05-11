package hr.fer.seminar.operators.cs;

import java.util.List;
import java.util.Map;

import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Vehicle;
import io.jenetics.PermutationChromosome;

public class GACustomerSelector implements CustomerSelector{
	
	private final PermutationChromosome<Integer> c;
	
	private final Map<Integer, Customer> customerMap;
	
	public GACustomerSelector(PermutationChromosome<Integer> c, Map<Integer, Customer> customerMap) {
		this.c = c;
		this.customerMap = customerMap;
	}

	@Override
	public Customer selectCustomer(Vehicle v, List<Customer> UC, EVRPProblem problem, Vehicle[] vehicles) {
		for (int i = 0; i < c.validAlleles().size(); i++) {
			Integer currentNode = c.get(i).allele();
			Customer currentCustomer = customerMap.get(currentNode);
			if(UC.contains(currentCustomer)) {
				return currentCustomer;
			}
		}
		return null;
	}

}
