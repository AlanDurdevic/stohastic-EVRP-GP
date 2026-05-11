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
import hr.fer.seminar.entities.EVRPProblem;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.Vehicle;
import hr.fer.seminar.solution.ga.GeneticAlgorithmEVRP;
import hr.fer.seminar.solution.ga.impl.GeneticAlgorithmEVRPParallelVehicle;
import hr.fer.seminar.solution.ga.impl.GeneticAlgorithmEVRPSemiParallelVehicle;
import hr.fer.seminar.solution.ga.impl.GeneticAlgorithmEVRPSerialVehicle;
import hr.fer.seminar.solution.gp.GeneticProgrammingEVRP;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingEVRPParallelVehicle;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingEVRPSemiParallelVehicle;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingEVRPSerialVehicle;
import io.jenetics.EnumGene;
import io.jenetics.Genotype;
import io.jenetics.ext.util.TreeNode;
import io.jenetics.prog.ProgramGene;
import io.jenetics.prog.op.Op;

public class EVRP {

	public static void main(String[] args) {

		if (args.length != 1) {
			System.err.println("Invalid number of arguments!");
			System.exit(1);
		}

		String filename = args[0];
		EVRPProblem problem = generateProblem(filename);
		
		try (Scanner sc = new Scanner(System.in)) {
			
			System.out.println("1-Genetic programming");
			System.out.println("2-Genetic Algorithm");
			
			int input = sc.nextInt();
			
			if(input == 1) {
				System.out.println("1-Serial");
				System.out.println("2-Semi-Parallel");
				System.out.println("3-Parallel");
				input = sc.nextInt();
				GeneticProgrammingEVRP gp = null;
				switch(input) {
				case 1 -> gp = new GeneticProgrammingEVRPSerialVehicle(problem);
				case 2 -> gp = new GeneticProgrammingEVRPSemiParallelVehicle(problem);
				case 3 -> gp = new GeneticProgrammingEVRPParallelVehicle(problem);
				default ->{
					System.err.println("Wrong input!");
					System.exit(3);
				}
				}
				
				final Genotype<ProgramGene<Double>> programDynamic = gp.calculate();
				final TreeNode<Op<Double>> treeDynamic = programDynamic.gene().toTreeNode();
				System.out.println("Program: " + treeDynamic);
				System.out.println("Tree depth: " + programDynamic.gene().depth());
				System.out.println("Error: " + gp.error(programDynamic));
				List<Vehicle> usedVehicles = gp.getUsedVehicles(programDynamic);
				usedVehicles.sort((v1, v2) -> Integer.compare(v1.getId(), v2.getId()));
				System.out.println("Number of vehicles: " + usedVehicles.size());
				usedVehicles.forEach(v -> printRoute(v));
			}
			else if(input == 2) {
				System.out.println("1-Serial");
				System.out.println("2-Semi-Parallel");
				System.out.println("3-Parallel");
				input = sc.nextInt();
				GeneticAlgorithmEVRP ga = null;
				switch(input) {
				case 1 -> ga = new GeneticAlgorithmEVRPSerialVehicle(problem);
				case 2 -> ga = new GeneticAlgorithmEVRPSemiParallelVehicle(problem);
				case 3 -> ga = new GeneticAlgorithmEVRPParallelVehicle(problem);
				default ->{
					System.err.println("Wrong input!");
					System.exit(3);
				}
				}
				final Genotype<EnumGene<Integer>> programDynamic = ga.calculate();
				System.out.println("Error: " + ga.error(programDynamic));
				List<Vehicle> usedVehicles = ga.getUsedVehicles(programDynamic);
				usedVehicles.sort((v1, v2) -> Integer.compare(v1.getId(), v2.getId()));
				System.out.println("Number of vehicles: " + usedVehicles.size());
				usedVehicles.forEach(v -> printRoute(v));
			}
			else {
				System.err.println("Wrong input!");
				System.exit(3);
			}
		}
	}

	private static void printRoute(Vehicle vehicle) {
		System.out.println();
		System.out.println("ID: " + vehicle.getId());
		int routeLength = vehicle.getRoute().size();
		for(int i = 0; i < routeLength; i++) {
			if(i == routeLength - 1) {
				System.out.println(vehicle.getRoute().getLast().getId() + "(" + vehicle.getState().getLast() + ")");
			}
			else {
				System.out.print(vehicle.getRoute().get(i).getId() + "(" + vehicle.getState().get(i) + ")"+  " -> ");
			}
		}
		
		double energyUsed = 0;
		for(int i = 1; i < routeLength; i++) {
			energyUsed += Location.distance(vehicle.getRoute().get(i-1), vehicle.getRoute().get(i));
		}
		System.out.println("Energy used: " + energyUsed);
		System.out.println();
	}

	private static EVRPProblem generateProblem(String filename) {

		Depot depot = null;
		double dueDate = 0, vehicleFuelTankCapacity = 0, vehicleLoadCapacity = 0, fuelConsumptionRate = 0,
				inverseRefuelingRate = 0, averageVelocity = 0;
		List<Customer> customers = new ArrayList<>();
		List<ChargingStation> chargingStations = new ArrayList<>();

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

		} catch (IOException e) {
			System.err.println("Error while opening file: " + filename);
			System.exit(2);
		}
		
		for(Customer customer : customers) {
			ChargingStation nearestChargingStation = null;
			double nearestDistance = Double.MAX_VALUE;
			for(ChargingStation chargingStation : chargingStations) {
				double newDistance = Location.distance(customer, chargingStation);
				if(newDistance < nearestDistance) {
					nearestChargingStation = chargingStation;
					nearestDistance = newDistance;
				}
			}
			customer.setNearestChargingStation(nearestChargingStation);
		}
		
		return new EVRPProblem(depot, dueDate, vehicleFuelTankCapacity, vehicleLoadCapacity, fuelConsumptionRate,
				inverseRefuelingRate, averageVelocity, customers, chargingStations);
	}

}
