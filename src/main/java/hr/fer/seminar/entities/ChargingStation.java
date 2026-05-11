package hr.fer.seminar.entities;

public class ChargingStation extends Location {

	public ChargingStation(String id, double x, double y, double dueDate) {
		super(id, x, y, 0, 0, dueDate, 0);
	}

	@Override
	public ChargingStation getNearestChargingStation() {
		return this;
	}

}
