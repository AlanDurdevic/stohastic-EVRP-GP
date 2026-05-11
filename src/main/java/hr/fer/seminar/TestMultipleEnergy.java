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
import io.jenetics.prog.ProgramGene;

public class TestMultipleEnergy {

	private static int NUMBER_OF_TESTS = 1;
	
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

				double sum = 0;
				double minimumEnergy = Double.MAX_VALUE;
				for(int i = 0; i < NUMBER_OF_TESTS; i++) {
					System.out.println("Iteration: " + i);
					Genotype<ProgramGene<Double>> programDynamic = gp.calculate();
					double energy = gp.error(programDynamic) - programDynamic.gene().depth();
					sum += energy;
					if(energy < minimumEnergy ) {
						minimumEnergy  = energy;
					}
				}
				
				System.out.println("Minimum energy: " + minimumEnergy);
				System.out.println("Average energy: " + sum/NUMBER_OF_TESTS);
				
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
				
				double sum = 0;
				double minimumEnergy = Double.MAX_VALUE;
				for(int i = 0; i < NUMBER_OF_TESTS; i++) {
					System.out.println("Iteration: " + i);
					Genotype<EnumGene<Integer>> programDynamic = ga.calculate();
					double energy = ga.error(programDynamic);
					sum += energy;
					if(energy < minimumEnergy ) {
						minimumEnergy  = energy;
					}
				}
				
				System.out.println("Minimum energy: " + minimumEnergy);
				System.out.println("Average energy: " + sum/NUMBER_OF_TESTS);
			}
			else {
				System.err.println("Wrong input!");
				System.exit(3);
			}
		}
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
