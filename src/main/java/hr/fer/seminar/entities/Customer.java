package hr.fer.seminar.entities;

import java.util.NoSuchElementException;

public class Customer extends Location {
	
	private ChargingStation nearestChargingStation;

	public Customer(String id, double x, double y, double demand, double readyTime, double dueDate,
			double serviceTime) {
		super(id, x, y, demand, readyTime, dueDate, serviceTime);
	}
	
	public Customer(Customer customer, double demand, double serviceTime) {
		super(customer.getId(), customer.getX(), customer.getY(), demand, customer.getReadyTime(), customer.getDueDate(), serviceTime);
		nearestChargingStation = customer.nearestChargingStation;
	}
	
	@Override
	public ChargingStation getNearestChargingStation() {
		if(nearestChargingStation == null) {
			throw new NoSuchElementException();
		}
		return nearestChargingStation;
	}
	
	public void setNearestChargingStation(ChargingStation nearestChargingStation) {
		if(this.nearestChargingStation != null) {
			throw new IllegalAccessError();
		}
		this.nearestChargingStation = nearestChargingStation;
	}

}
