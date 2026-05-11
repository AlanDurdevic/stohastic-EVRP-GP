package hr.fer.seminar.operators;

import java.util.Comparator;

import hr.fer.seminar.entities.Vehicle;

public class VehicleComparator implements Comparator<Vehicle>{

	@Override
	public int compare(Vehicle vehicle1, Vehicle vehicle2) {
		double vehicle1Time = vehicle1.getCurrentTime();
		double vehicle2Time = vehicle2.getCurrentTime();
		if(vehicle1Time == vehicle2Time) {
			return Integer.compare(vehicle1.getId(), vehicle2.getId());
		}
		return Double.compare(vehicle1Time, vehicle2Time);
	}

}
