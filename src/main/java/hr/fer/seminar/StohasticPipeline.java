package hr.fer.seminar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import hr.fer.seminar.entities.ChargingStation;
import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.Depot;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.StohasticEVRPProblem;
import hr.fer.seminar.operators.cs.CustomerSelector;
import hr.fer.seminar.operators.cs.EDDStohasticCustomerSelector;
import hr.fer.seminar.operators.cs.MSStohasticCustomerSelector;
import hr.fer.seminar.operators.cs.MTEStohasticCustomerSelector;
import hr.fer.seminar.operators.cs.NNStohasticCustomerSelector;
import hr.fer.seminar.operators.vs.VehicleStrategy;
import hr.fer.seminar.solution.gp.GeneticProgrammingStohasticEVRP;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingStohasticEVRPMultiple;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingStohasticEVRPHeuristicVehicle;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingStohasticEVRPParallelBVehicle;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingStohasticEVRPParallelVehicle;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingStohasticEVRPSemiParallelVehicle;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingStohasticEVRPSerialVehicle;
import hr.fer.seminar.util.stohastic.distribution.Distribution;
import hr.fer.seminar.util.stohastic.distribution.impl.LognormalDistribution;
import hr.fer.seminar.util.stohastic.distribution.impl.NoDistribution;
import hr.fer.seminar.util.stohastic.distribution.impl.UniformDistribution;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.ext.util.TreeNode;
import io.jenetics.prog.ProgramGene;
import io.jenetics.prog.op.Op;
import io.jenetics.util.ISeq;

public class StohasticPipeline {

	private static final int numberOfExperiments = 10;

	private static final String resultsFile = "./energy-MS";
	
	private final static VehicleStrategy vs = VehicleStrategy.Serial;
	
	private final static CustomerSelector cs = new MSStohasticCustomerSelector();

	private static final String trainFolder = "./data/stohastic/train";

	private static final String testFolder = "./data/stohastic/test";

	private static final int numberOfClonesTrain = 2;

	private static final int numberOfClonesTest = 6;
	
	private static long baseSeed = 12345L;
	
	private static double trainCV = 0.2;
	
	public static PrintWriter writerIterations;

	static {
	    try {
	        writerIterations = new PrintWriter(
	            new FileWriter(resultsFile + "-iterations")
	        );
	    } catch (IOException e) {
	        throw new RuntimeException("Failed to initialize writerIterations", e);
	    }
	}

	public static void main(String[] args) {

		File folder = new File(trainFolder);
		File[] listFiles = folder.listFiles();
		List<String> trainFileNames = new LinkedList<>();
		for (File file : listFiles) {
			for (int i = 0; i < numberOfClonesTrain; i++) {
				trainFileNames.add(trainFolder + "/" + file.getName());
			}
		}

		folder = new File(testFolder);
		listFiles = folder.listFiles();
		List<String> testFileNames = new LinkedList<>();
		for (File file : listFiles) {
			for (int i = 0; i < numberOfClonesTest; i++) {
				testFileNames.add(testFolder + "/" + file.getName());
			}
		}

		try (PrintWriter writer = new PrintWriter(new FileWriter(resultsFile))) {

			for (int i = 1; i <= numberOfExperiments; i++) {
				writer.println("#EXPERIMENT:" + i);
				writerIterations.println("#EXPERIMENT:" + i);
				
				//training
				List<GeneticProgrammingStohasticEVRP> trainProblems = new LinkedList<>();
				String currentFilename = "";
				int currentRepIdx = 0;
				for (String filename : trainFileNames) {
					if(filename.equals(currentFilename)) {
						currentRepIdx++;
					}
					else {
						currentRepIdx = 0;
						currentFilename = filename;
					}
					trainProblems.add(new GeneticProgrammingStohasticEVRPHeuristicVehicle(generateProblemTrain(filename, currentRepIdx), cs, vs));
				}
				
				GeneticProgrammingStohasticEVRPMultiple gp = new GeneticProgrammingStohasticEVRPMultiple(trainProblems);
				ISeq<Phenotype<ProgramGene<Double>, Double>> populationSeq = gp.calculate();
				List<Phenotype<ProgramGene<Double>, Double>> population = populationSeq.stream()
						.sorted((a, b) -> Double.compare(a.fitness(), b.fitness())).collect(Collectors.toList());
				
				Map<String, Integer> counter = new HashMap<>();
				for (int p = 0; p < 30; p++) {
					final Genotype<ProgramGene<Double>> programDynamic = population.get(p).genotype();
					final TreeNode<Op<Double>> treeDynamic = programDynamic.gene().toTreeNode();
					if (p == 0) {
						writer.println("BestProgram:" + treeDynamic);
						writer.println("BestTreeDepth:" + programDynamic.gene().depth());
						writer.println("BestTreeSize:" + programDynamic.gene().size());
						writer.println("ErrorTrain:" + (population.get(p).fitness() - treeDynamic.size()) / 1000);
					}
					String treeString = treeDynamic.toString();
					String[] splittedString = treeString.split("[,()]");
					for (String s : splittedString) {
						if(s.isBlank()) {
							continue;
						}
						if (!counter.containsKey(s)) {
							counter.put(s, 0);
						}
						counter.put(s, counter.get(s) + 1);
					}
				}		
				counter.forEach((k, v) -> writer.println(k + ":" + v));
				
				//testing
				final Genotype<ProgramGene<Double>> bestProgram = population.get(0).genotype();
				
				//deterministic
				writer.println("##TEST-DETERMINISTIC-0,0,0");
				test(writer, testFileNames, bestProgram, new NoDistribution(), new NoDistribution(), new NoDistribution());

				
				//demand only
				writer.println("##TEST-LOGNORMAL-0.1,0,0");
				test(writer, testFileNames, bestProgram, new LognormalDistribution(0.1), new NoDistribution(), new NoDistribution());
				
				writer.println("##TEST-LOGNORMAL-0.2,0,0");
				test(writer, testFileNames, bestProgram, new LognormalDistribution(0.2), new NoDistribution(), new NoDistribution());
				
				writer.println("##TEST-LOGNORMAL-0.3,0,0");
				test(writer, testFileNames, bestProgram, new LognormalDistribution(0.3), new NoDistribution(), new NoDistribution());

				
				//service only
				writer.println("##TEST-LOGNORMAL-0,0.1,0");
				test(writer, testFileNames, bestProgram, new NoDistribution(), new LognormalDistribution(0.1), new NoDistribution());
			
				writer.println("##TEST-LOGNORMAL-0,0.2,0");
				test(writer, testFileNames, bestProgram, new NoDistribution(), new LognormalDistribution(0.2), new NoDistribution());
				
				writer.println("##TEST-LOGNORMAL-0,0.3,0");
				test(writer, testFileNames, bestProgram, new NoDistribution(), new LognormalDistribution(0.3), new NoDistribution());
				
				
				//travel only
				writer.println("##TEST-LOGNORMAL-0,0,0.1");
				test(writer, testFileNames, bestProgram, new NoDistribution(), new NoDistribution(), new LognormalDistribution(0.1));
				
				writer.println("##TEST-LOGNORMAL-0,0,0.2");
				test(writer, testFileNames, bestProgram, new NoDistribution(), new NoDistribution(), new LognormalDistribution(0.2));
				
				writer.println("##TEST-LOGNORMAL-0,0,0.3");
				test(writer, testFileNames, bestProgram, new NoDistribution(), new NoDistribution(), new LognormalDistribution(0.3));
				
				//demand+service
				writer.println("##TEST-LOGNORMAL-0.2,0.2,0");
				test(writer, testFileNames, bestProgram, new LognormalDistribution(0.2), new LognormalDistribution(0.2), new NoDistribution());
				
				//demand+travel
				writer.println("##TEST-LOGNORMAL-0.2,0,0.2");
				test(writer, testFileNames, bestProgram, new LognormalDistribution(0.2), new NoDistribution(), new LognormalDistribution(0.2));
				
				//service+travel
				writer.println("##TEST-LOGNORMAL-0,0.2,0.2");
				test(writer, testFileNames, bestProgram, new NoDistribution(), new LognormalDistribution(0.2), new LognormalDistribution(0.2));
				
				//all-three
				writer.println("##TEST-LOGNORMAL-0.2,0.2,0.2");
				test(writer, testFileNames, bestProgram, new LognormalDistribution(0.2), new LognormalDistribution(0.2), new LognormalDistribution(0.2));
				
				writer.println("##TEST-LOGNORMAL-0.3,0.3,0.3");
				test(writer, testFileNames, bestProgram, new LognormalDistribution(0.3), new LognormalDistribution(0.3), new LognormalDistribution(0.3));
				
				
				//all-three
				writer.println("##TEST-UNIFORM-0.2,0.2,0.2");
				test(writer, testFileNames, bestProgram, new UniformDistribution(0.2), new UniformDistribution(0.2), new UniformDistribution(0.2));
				
				writer.println("##TEST-UNIFORM-0.3,0.3,0.3");
				test(writer, testFileNames, bestProgram, new UniformDistribution(0.3), new UniformDistribution(0.3), new UniformDistribution(0.3));		

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		writerIterations.flush();
		System.out.println("END");
		System.exit(0);
	}
	
	private static void test(PrintWriter writer, List<String> testFileNames, Genotype<ProgramGene<Double>> bestProgram, Distribution demandDistribution, Distribution serviceDistribution, Distribution velocityDistribution) {
		List<GeneticProgrammingStohasticEVRP> testProblems = new LinkedList<>();
		String currentFilename = "";
		int currentRepIdx = 0;
		for (String filename : testFileNames) {
			if(filename.equals(currentFilename)) {
				currentRepIdx++;
			}
			else {
				currentRepIdx = 0;
				currentFilename = filename;
			}
			testProblems.add(new GeneticProgrammingStohasticEVRPHeuristicVehicle(
					generateProblemTest(filename, demandDistribution, serviceDistribution, velocityDistribution, currentRepIdx), cs, vs));
		}

		for(int i = 0; i < testProblems.size(); i++) {
			GeneticProgrammingStohasticEVRP problem = testProblems.get(i);
			GeneticProgrammingStohasticEVRPMultiple gp = new GeneticProgrammingStohasticEVRPMultiple(List.of(problem));
			double error = gp.errorWD(bestProgram) / 1000;
			writer.println(new File(testFileNames.get(i)).getName() + ":" + error);
		}
	}

	private static StohasticEVRPProblem generateProblemTest(String filename, Distribution demandDistribution,
			Distribution serviceDistribution, Distribution velocityDistribution, int repIdx) {
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
		
		long seed = mixSeed(baseSeed, filename, repIdx);
		
		demandDistribution = demandDistribution.copy();
		serviceDistribution = serviceDistribution.copy();
		velocityDistribution = velocityDistribution.copy();
		
		demandDistribution.setSeed(seed);
		serviceDistribution.setSeed(seed + 1);
		velocityDistribution.setSeed(seed + 2);

		return new StohasticEVRPProblem(depot, dueDate, vehicleFuelTankCapacity, vehicleLoadCapacity,
				fuelConsumptionRate, inverseRefuelingRate, averageVelocity, customers, chargingStations,
				demandDistribution, serviceDistribution, velocityDistribution);
	}

	private static StohasticEVRPProblem generateProblemTrain(String filename, int repIdx) {

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
		
		long seed = mixSeed(baseSeed, filename, repIdx);
		
		Distribution demandDistribution = new LognormalDistribution(trainCV);
		Distribution serviceTimeDistribution = new LognormalDistribution(trainCV);
		Distribution velocityDistribution = new LognormalDistribution(trainCV);
		
		demandDistribution.setSeed(seed);
		serviceTimeDistribution.setSeed(seed + 1);
		velocityDistribution.setSeed(seed + 2);

		return new StohasticEVRPProblem(depot, dueDate, vehicleFuelTankCapacity, vehicleLoadCapacity,
				fuelConsumptionRate, inverseRefuelingRate, averageVelocity, customers, chargingStations,
				demandDistribution, serviceTimeDistribution, velocityDistribution);
	}
	
	private static long mixSeed(long baseSeed, String fileName, int repIdx) {
		long x = baseSeed;
		
		long instanceKey = (long) fileName.hashCode();
		
		x ^= 0x9E3779B97F4A7C15L * (instanceKey + 1L);
		x ^= 0xC2B2AE3D27D4EB4FL * ((long) repIdx + 1L);
		
		x ^= (x >>> 33);
		x *= 0xff51afd7ed558ccdL;
		x ^= (x >>> 33);
		x *= 0xc4ceb9fe1a85ec53L;
		x ^= (x >>> 33);
		
		return x;
	}

}
