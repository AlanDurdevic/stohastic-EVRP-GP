package hr.fer.seminar.entities;

import java.util.List;

import hr.fer.seminar.util.stohastic.distribution.Distribution;

public class StohasticEVRPProblem extends EVRPProblem{
	
	private final Distribution demandDistribution;
	
	private final Distribution serviceTimeDistribution;
	
	private final Distribution velocityDistribution;

	public StohasticEVRPProblem(Depot depot, double dueDate, double vehicleFuelTankCapacity, double vehicleLoadCapacity,
			double fuelConsumptionRate, double inverseRefuelingRate, double averageVelocity, List<Customer> customers,
			List<ChargingStation> chargingStations, Distribution demandDistribution, Distribution serviceTimeDistribution, Distribution velocityDistribution) {
		super(depot, dueDate, vehicleFuelTankCapacity, vehicleLoadCapacity, fuelConsumptionRate, inverseRefuelingRate,
				averageVelocity, customers, chargingStations);
		this.demandDistribution = demandDistribution;
		this.serviceTimeDistribution = serviceTimeDistribution;
		this.velocityDistribution = velocityDistribution;
	}

	public Distribution getDemandDistribution() {
		return demandDistribution;
	}

	public Distribution getServiceTimeDistribution() {
		return serviceTimeDistribution;
	}

	public Distribution getVelocityDistribution() {
		return velocityDistribution;
	}

}
