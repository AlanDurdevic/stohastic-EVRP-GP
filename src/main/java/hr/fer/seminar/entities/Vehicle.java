package hr.fer.seminar.entities;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Vehicle {

	public class State {

		private final double currentTime;

		private final double currentFuelCapacity;

		private final double currentLoadCapacity;

		private State(double currentTime, double currentFuelCapacity, double currentLoadCapacity) {
			this.currentTime = currentTime;
			this.currentFuelCapacity = currentFuelCapacity;
			this.currentLoadCapacity = currentLoadCapacity;
		}

		public double getCurrentTime() {
			return currentTime;
		}

		public double getCurrentFuelCapacity() {
			return currentFuelCapacity;
		}

		public double getCurrentLoadCapacity() {
			return currentLoadCapacity;
		}

		@Override
		public String toString() {
			return "CT=" + currentTime + ", CFC=" + currentFuelCapacity + ", CLC=" + currentLoadCapacity;
		}

	}

	private final int id;

	private double fuelCapacityLeft;

	private double loadCapacityLeft;

	private double currentTime;

	private List<Location> route = new LinkedList<>();

	private List<State> state = new LinkedList<>();

	public Vehicle(int id, double fuelCapacityLeft, double loadCapacityLeft, Depot depot) {
		this.id = id;
		this.fuelCapacityLeft = fuelCapacityLeft;
		this.loadCapacityLeft = loadCapacityLeft;
		currentTime = 0;
		addLocation(depot);
	}

	public int getId() {
		return id;
	}

	public double getFuelCapacityLeft() {
		return fuelCapacityLeft;
	}

	public void setFuelCapacity(double fuelCapacity) {
		this.fuelCapacityLeft = fuelCapacity;
	}

	public void subtractFuelCapacity(double fuelCapacity) {
		this.fuelCapacityLeft -= fuelCapacity;
	}

	public double getLoadCapacityLeft() {
		return loadCapacityLeft;
	}

	public void subtractLoadCapacity(double loadCapacity) {
		this.loadCapacityLeft -= loadCapacity;
	}

	public double getCurrentTime() {
		return currentTime;
	}

	public void addTime(double time) {
		currentTime += time;
	}

	public Location getCurrentLocation() {
		return route.getLast();
	}

	public void addLocation(Location location) {
//		if (currentTime > location.getDueDate()) {
//			System.out.println("Greška CT " + location.getId() + " current time: " + currentTime + " due time: " + location.getDueDate());
//		}
		if (fuelCapacityLeft < 0) {
			System.out.println("Greška FC");
			throw new RuntimeException();
		}
		if (loadCapacityLeft < location.getDemand()) {
			System.out.println("Greška LC");
			throw new RuntimeException();
		}
		route.add(location);
		state.add(new State(currentTime, fuelCapacityLeft, loadCapacityLeft));
	}

	public List<Location> getRoute() {
		return route;
	}

	public List<State> getState() {
		return state;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vehicle other = (Vehicle) obj;
		return id == other.id;
	}

}
