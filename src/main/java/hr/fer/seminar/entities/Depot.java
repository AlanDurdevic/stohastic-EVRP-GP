package hr.fer.seminar.entities;

public class Depot extends Location {

	public Depot(String id, double x, double y, double dueDate) {
		super(id, x, y, 0, 0, dueDate, 0);
	}

	@Override
	public ChargingStation getNearestChargingStation() {
		return new ChargingStation(getId(), getX(), getY(), getDueDate());
	}

}
