package hr.fer.seminar;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import hr.fer.seminar.entities.ChargingStation;
import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.Depot;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.StohasticEVRPProblem;
import hr.fer.seminar.entities.Vehicle;
import hr.fer.seminar.operators.cs.CustomerSelector;
import hr.fer.seminar.operators.cs.NNCustomerSelector;
import hr.fer.seminar.operators.vs.ParallelVehicleSupplier;
import hr.fer.seminar.operators.vs.SemiParallelVehicleSupplier;
import hr.fer.seminar.operators.vs.SerialVehicleSupplier;
import hr.fer.seminar.operators.vs.VehicleSupplier;
import hr.fer.seminar.solution.NNStohasticSolutionEVRP;
import hr.fer.seminar.util.stohastic.distribution.Distribution;
import hr.fer.seminar.util.stohastic.distribution.impl.LognormalDistribution;
import hr.fer.seminar.util.stohastic.distribution.impl.NoDistribution;
import hr.fer.seminar.util.stohastic.distribution.impl.UniformDistribution;

public class NNStohasticEVRP {

	public static void main(String[] args) {

		if (args.length != 1) {
			System.err.println("Invalid number of arguments!");
			System.exit(1);
		}

		String filename = args[0];
		StohasticEVRPProblem problem = generateProblem(filename);
		
		try (Scanner sc = new Scanner(System.in)) {
			
			List<Vehicle> vehicles = initializeVehicles(problem);

			System.out.println("1-Serial");
			System.out.println("2-Semi-Parallel");
			System.out.println("3-Parallel");
			int input = sc.nextInt();
			CustomerSelector cs = new NNCustomerSelector();
			VehicleSupplier vs = null;
			switch(input) {
			case 1 -> vs = new SerialVehicleSupplier(vehicles);
			case 2 -> vs = new SemiParallelVehicleSupplier(vehicles);
			case 3 -> vs = new ParallelVehicleSupplier(vehicles, problem);
			default ->{
				System.err.println("Wrong input!");
				System.exit(3);
			}
			}
				
			NNStohasticSolutionEVRP<?> solution = new NNStohasticSolutionEVRP<>(problem, cs, vs);
			List<Vehicle> usedVehicles = solution.getUsedVehicles();
			System.out.println("Number of vehicles: " + usedVehicles.size());
			usedVehicles.forEach(v -> printRoute(v));

		}
	}

	private static void printRoute(Vehicle vehicle) {
		System.out.println();
		System.out.println("ID: " + vehicle.getId());
		int routeLength = vehicle.getRoute().size();
		for (int i = 0; i < routeLength; i++) {
			if (i == routeLength - 1) {
				System.out.println(vehicle.getRoute().getLast().getId() + "(" + vehicle.getState().getLast() + ")");
			} else {
				System.out.print(vehicle.getRoute().get(i).getId() + "(" + vehicle.getState().get(i) + ")" + " -> ");
			}
		}

		double energyUsed = 0;
		for (int i = 1; i < routeLength; i++) {
			energyUsed += Location.distance(vehicle.getRoute().get(i - 1), vehicle.getRoute().get(i));
		}
		System.out.println("Energy used: " + energyUsed);
		System.out.println();
	}

	private static StohasticEVRPProblem generateProblem(String filename) {

		Depot depot = null;
		double dueDate = 0, vehicleFuelTankCapacity = 0, vehicleLoadCapacity = 0, fuelConsumptionRate = 0,
				inverseRefuelingRate = 0, averageVelocity = 0;
		List<Customer> customers = new ArrayList<>();
		List<ChargingStation> chargingStations = new ArrayList<>();
		Distribution demandDistribution = null, serviceTimeDistribution = null, velocityDistribution = null;

		try (BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
			br.readLine();
			while (true) {
				String line = br.readLine();
				if (line.isBlank()) {
					break;
				}

				String[] splittedLine = line.split("\\s+");
				char locationTag = splittedLine[0].charAt(0);
				switch (locationTag) {
				case 'D' -> {
					String id = splittedLine[0];
					double x = Double.parseDouble(splittedLine[2]);
					double y = Double.parseDouble(splittedLine[3]);
					dueDate = Double.parseDouble(splittedLine[6]);
					depot = new Depot(id, x, y, dueDate);
				}
				case 'S' -> {
					String id = splittedLine[0];
					double x = Double.parseDouble(splittedLine[2]);
					double y = Double.parseDouble(splittedLine[3]);
					ChargingStation newChargingStation = new ChargingStation(id, x, y, dueDate);
					chargingStations.add(newChargingStation);
				}
				case 'C' -> {
					String id = splittedLine[0];
					double x = Double.parseDouble(splittedLine[2]);
					double y = Double.parseDouble(splittedLine[3]);
					double demand = Double.parseDouble(splittedLine[4]);
					double readyTime = Double.parseDouble(splittedLine[5]);
					double dueDateCustomer = Double.parseDouble(splittedLine[6]);
					double serviceTime = Double.parseDouble(splittedLine[7]);
					Customer newCustomer = new Customer(id, x, y, demand, readyTime, dueDateCustomer, serviceTime);
					customers.add(newCustomer);
				}
				}
			}

			String line = br.readLine();
			line = line.substring(line.indexOf('/') + 1, line.length() - 1);
			vehicleFuelTankCapacity = Double.parseDouble(line);

			line = br.readLine();
			line = line.substring(line.indexOf('/') + 1, line.length() - 1);
			vehicleLoadCapacity = Double.parseDouble(line);

			line = br.readLine();
			line = line.substring(line.indexOf('/') + 1, line.length() - 1);
			fuelConsumptionRate = Double.parseDouble(line);

			line = br.readLine();
			line = line.substring(line.indexOf('/') + 1, line.length() - 1);
			inverseRefuelingRate = Double.parseDouble(line);

			line = br.readLine();
			line = line.substring(line.indexOf('/') + 1, line.length() - 1);
			averageVelocity = Double.parseDouble(line);
			
			line = br.readLine();
			String[] splittedLine = line.split(" ");
			demandDistribution = new NoDistribution();
			if(splittedLine[1].equals("stohastic")) {
				if(splittedLine[2].equals("Gaussian")) {
					demandDistribution = new LognormalDistribution(Double.parseDouble(splittedLine[3]));
				}
				else if(splittedLine[2].equals("Uniform")) {
					demandDistribution = new UniformDistribution(Double.parseDouble(splittedLine[3]));
				}
			}
			
			line = br.readLine();
			splittedLine = line.split(" ");
			serviceTimeDistribution = new NoDistribution();
			if(splittedLine[1].equals("stohastic")) {
				if(splittedLine[2].equals("Gaussian")) {
					serviceTimeDistribution = new LognormalDistribution(Double.parseDouble(splittedLine[3]));
				}
				else if(splittedLine[2].equals("Uniform")) {
					serviceTimeDistribution = new UniformDistribution(Double.parseDouble(splittedLine[3]));
				}
			}
			
			line = br.readLine();
			splittedLine = line.split(" ");
			velocityDistribution = new NoDistribution();
			if(splittedLine[1].equals("stohastic")) {
				if(splittedLine[2].equals("Gaussian")) {
					velocityDistribution = new LognormalDistribution(Double.parseDouble(splittedLine[3]));
				}
				else if(splittedLine[2].equals("Uniform")) {
					velocityDistribution = new UniformDistribution(Double.parseDouble(splittedLine[3]));
				}
			}

		} catch (IOException e) {
			System.err.println("Error while opening file: " + filename);
			System.exit(2);
		}

		for (Customer customer : customers) {
			ChargingStation nearestChargingStation = null;
			double nearestDistance = Double.MAX_VALUE;
			for (ChargingStation chargingStation : chargingStations) {
				double newDistance = Location.distance(customer, chargingStation);
				if (newDistance < nearestDistance) {
					nearestChargingStation = chargingStation;
					nearestDistance = newDistance;
				}
			}
			customer.setNearestChargingStation(nearestChargingStation);
		}

		return new StohasticEVRPProblem(depot, dueDate, vehicleFuelTankCapacity, vehicleLoadCapacity, fuelConsumptionRate,
				inverseRefuelingRate, averageVelocity, customers, chargingStations, demandDistribution, serviceTimeDistribution, velocityDistribution);
	}
	
	private static int getLUNumberOfVehicles(StohasticEVRPProblem problem) {
		double sum = 0;
		for (Customer customer : problem.getCustomers()) {
			sum += customer.getDemand();
		}
		return (int) Math.ceil(sum / problem.getVehicleLoadCapacity());
	}
	
	private static List<Vehicle> initializeVehicles(StohasticEVRPProblem problem) {
		int LB = getLUNumberOfVehicles(problem);
		List<Vehicle> V = new ArrayList<>();
		Depot startingLocation = problem.getDepot();
		double fuelCapacity = problem.getVehicleFuelTankCapacity();
		double loadCapacity = problem.getVehicleLoadCapacity();
		for (int i = 0; i < LB; i++) {
			Vehicle newVehicle = new Vehicle(i, fuelCapacity, loadCapacity, startingLocation);
			V.add(newVehicle);
		}
		return V;
	}

}
