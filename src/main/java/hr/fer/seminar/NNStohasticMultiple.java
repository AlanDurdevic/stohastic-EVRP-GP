package hr.fer.seminar;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import hr.fer.seminar.entities.*;
import hr.fer.seminar.operators.cs.CustomerSelector;
import hr.fer.seminar.operators.cs.NNCustomerSelector;
import hr.fer.seminar.operators.vs.SerialVehicleSupplier;
import hr.fer.seminar.operators.vs.VehicleSupplier;
import hr.fer.seminar.solution.NNStohasticSolutionEVRP;
import hr.fer.seminar.util.stohastic.distribution.Distribution;
import hr.fer.seminar.util.stohastic.distribution.impl.*;

public class NNStohasticMultiple {

    private static final String resultsFile = "./nn-vehicle-serial";

    private static final String trainFolder = "./data/stohastic/train";
    private static final String testFolder  = "./data/stohastic/test";

    private static final int numberOfClonesTrain = 2;
    private static final int numberOfClonesTest  = 6;

    private static final long baseSeed = 12345L;
    private static final double trainCV = 0.2;

    public static void main(String[] args) {

        List<String> trainFiles = collect(trainFolder, numberOfClonesTrain);
        List<String> testFiles  = collect(testFolder, numberOfClonesTest);

        CustomerSelector cs = new NNCustomerSelector();

        try (PrintWriter writer = new PrintWriter(new FileWriter(resultsFile))) {

            /* ================= TRAIN ================= */

            writer.println("#TRAIN");
            int vehiclesTrain = 0;

            for (int i = 0; i < trainFiles.size(); i++) {
                String f = trainFiles.get(i);
                int rep = repetitionIndex(trainFiles, i);
                vehiclesTrain += solve(generateTrainProblem(f, rep), cs);
            }

            writer.println("TotalVehiclesTrain:" + vehiclesTrain);

            /* ================= TESTS (IDENTICAL TO GP) ================= */

            writer.println("##TEST-DETERMINISTIC-0,0,0");
            test(writer, testFiles, cs,
                    new NoDistribution(),
                    new NoDistribution(),
                    new NoDistribution());

            writer.println("##TEST-LOGNORMAL-0.1,0,0");
            test(writer, testFiles, cs,
                    new LognormalDistribution(0.1),
                    new NoDistribution(),
                    new NoDistribution());

            writer.println("##TEST-LOGNORMAL-0.2,0,0");
            test(writer, testFiles, cs,
                    new LognormalDistribution(0.2),
                    new NoDistribution(),
                    new NoDistribution());

            writer.println("##TEST-LOGNORMAL-0.3,0,0");
            test(writer, testFiles, cs,
                    new LognormalDistribution(0.3),
                    new NoDistribution(),
                    new NoDistribution());

            writer.println("##TEST-LOGNORMAL-0,0.1,0");
            test(writer, testFiles, cs,
                    new NoDistribution(),
                    new LognormalDistribution(0.1),
                    new NoDistribution());

            writer.println("##TEST-LOGNORMAL-0,0.2,0");
            test(writer, testFiles, cs,
                    new NoDistribution(),
                    new LognormalDistribution(0.2),
                    new NoDistribution());

            writer.println("##TEST-LOGNORMAL-0,0.3,0");
            test(writer, testFiles, cs,
                    new NoDistribution(),
                    new LognormalDistribution(0.3),
                    new NoDistribution());

            writer.println("##TEST-LOGNORMAL-0,0,0.1");
            test(writer, testFiles, cs,
                    new NoDistribution(),
                    new NoDistribution(),
                    new LognormalDistribution(0.1));

            writer.println("##TEST-LOGNORMAL-0,0,0.2");
            test(writer, testFiles, cs,
                    new NoDistribution(),
                    new NoDistribution(),
                    new LognormalDistribution(0.2));

            writer.println("##TEST-LOGNORMAL-0,0,0.3");
            test(writer, testFiles, cs,
                    new NoDistribution(),
                    new NoDistribution(),
                    new LognormalDistribution(0.3));

            writer.println("##TEST-LOGNORMAL-0.2,0.2,0");
            test(writer, testFiles, cs,
                    new LognormalDistribution(0.2),
                    new LognormalDistribution(0.2),
                    new NoDistribution());

            writer.println("##TEST-LOGNORMAL-0.2,0,0.2");
            test(writer, testFiles, cs,
                    new LognormalDistribution(0.2),
                    new NoDistribution(),
                    new LognormalDistribution(0.2));

            writer.println("##TEST-LOGNORMAL-0,0.2,0.2");
            test(writer, testFiles, cs,
                    new NoDistribution(),
                    new LognormalDistribution(0.2),
                    new LognormalDistribution(0.2));

            writer.println("##TEST-LOGNORMAL-0.2,0.2,0.2");
            test(writer, testFiles, cs,
                    new LognormalDistribution(0.2),
                    new LognormalDistribution(0.2),
                    new LognormalDistribution(0.2));

            writer.println("##TEST-LOGNORMAL-0.3,0.3,0.3");
            test(writer, testFiles, cs,
                    new LognormalDistribution(0.3),
                    new LognormalDistribution(0.3),
                    new LognormalDistribution(0.3));

            writer.println("##TEST-UNIFORM-0.2,0.2,0.2");
            test(writer, testFiles, cs,
                    new UniformDistribution(0.2),
                    new UniformDistribution(0.2),
                    new UniformDistribution(0.2));

            writer.println("##TEST-UNIFORM-0.3,0.3,0.3");
            test(writer, testFiles, cs,
                    new UniformDistribution(0.3),
                    new UniformDistribution(0.3),
                    new UniformDistribution(0.3));

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("END");
        System.exit(0);
    }

    /* ================= TEST EXECUTION ================= */

    private static void test(PrintWriter writer, List<String> files,
                             CustomerSelector cs,
                             Distribution d, Distribution s, Distribution v) {

        for (int i = 0; i < files.size(); i++) {
            String f = files.get(i);
            int rep = repetitionIndex(files, i);
            StohasticEVRPProblem p = generateTestProblem(f, d, s, v, rep);
            int vehicles = solve(p, cs);
            writer.println(new File(f).getName() + ":" + vehicles);
        }
    }

    private static int solve(StohasticEVRPProblem problem, CustomerSelector cs) {
        List<Vehicle> vehicles = initializeVehicles(problem);
        VehicleSupplier vs = new SerialVehicleSupplier(vehicles);
        return new NNStohasticSolutionEVRP<>(problem, cs, vs)
                .getUsedVehicles().size();
    }

    /* ================= FILE HANDLING ================= */

    private static List<String> collect(String folder, int clones) {
        List<String> result = new LinkedList<>();
        for (File f : new File(folder).listFiles()) {
            for (int i = 0; i < clones; i++) {
                result.add(folder + "/" + f.getName());
            }
        }
        return result;
    }

    private static int repetitionIndex(List<String> list, int idx) {
        int r = 0;
        for (int i = idx - 1; i >= 0 && list.get(i).equals(list.get(idx)); i--) {
            r++;
        }
        return r;
    }

    private static long mixSeed(long baseSeed, String filename, int repIdx) {
        long x = baseSeed;
        long instanceKey = filename.hashCode();
        x ^= 0x9E3779B97F4A7C15L * (instanceKey + 1L);
        x ^= 0xC2B2AE3D27D4EB4FL * ((long) repIdx + 1L);
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);
        return x;
    }

    /* ================= PROBLEM GENERATION ================= */

    private static StohasticEVRPProblem generateTrainProblem(String filename, int repIdx) {
        Distribution d = new LognormalDistribution(trainCV);
        Distribution s = new LognormalDistribution(trainCV);
        Distribution v = new LognormalDistribution(trainCV);

        long seed = mixSeed(baseSeed, filename, repIdx);
        d.setSeed(seed);
        s.setSeed(seed + 1);
        v.setSeed(seed + 2);

        return parseProblem(filename, d, s, v);
    }

    private static StohasticEVRPProblem generateTestProblem(
            String filename,
            Distribution d, Distribution s, Distribution v,
            int repIdx) {

        d = d.copy();
        s = s.copy();
        v = v.copy();

        long seed = mixSeed(baseSeed, filename, repIdx);
        d.setSeed(seed);
        s.setSeed(seed + 1);
        v.setSeed(seed + 2);

        return parseProblem(filename, d, s, v);
    }

    private static StohasticEVRPProblem parseProblem(
            String filename,
            Distribution demand,
            Distribution service,
            Distribution velocity) {

        Depot depot = null;
        double dueDate = 0, tank = 0, load = 0, cons = 0, refuel = 0, avgVel = 0;
        List<Customer> customers = new ArrayList<>();
        List<ChargingStation> stations = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            br.readLine();
            while (true) {
                String line = br.readLine();
                if (line.isBlank()) break;

                String[] s = line.split("\\s+");
                switch (s[0].charAt(0)) {
                    case 'D' -> {
                        depot = new Depot(
                                s[0],
                                Double.parseDouble(s[2]),
                                Double.parseDouble(s[3]),
                                Double.parseDouble(s[6]));
                        dueDate = depot.getDueDate();
                    }
                    case 'S' -> stations.add(new ChargingStation(
                            s[0],
                            Double.parseDouble(s[2]),
                            Double.parseDouble(s[3]),
                            dueDate));
                    case 'C' -> customers.add(new Customer(
                            s[0],
                            Double.parseDouble(s[2]),
                            Double.parseDouble(s[3]),
                            Double.parseDouble(s[4]),
                            Double.parseDouble(s[5]),
                            Double.parseDouble(s[6]),
                            Double.parseDouble(s[7])));
                }
            }

            tank   = Double.parseDouble(br.readLine().split("/")[1].replace(";", ""));
            load   = Double.parseDouble(br.readLine().split("/")[1].replace(";", ""));
            cons   = Double.parseDouble(br.readLine().split("/")[1].replace(";", ""));
            refuel = Double.parseDouble(br.readLine().split("/")[1].replace(";", ""));
            avgVel = Double.parseDouble(br.readLine().split("/")[1].replace(";", ""));

        } catch (IOException e) {
            throw new RuntimeException("Error reading: " + filename, e);
        }

        for (Customer c : customers) {
            ChargingStation best = null;
            double bestD = Double.MAX_VALUE;
            for (ChargingStation s : stations) {
                double d = Location.distance(c, s);
                if (d < bestD) {
                    bestD = d;
                    best = s;
                }
            }
            c.setNearestChargingStation(best);
        }

        return new StohasticEVRPProblem(
                depot, dueDate, tank, load, cons, refuel, avgVel,
                customers, stations,
                demand, service, velocity);
    }

    private static List<Vehicle> initializeVehicles(StohasticEVRPProblem p) {
        double sum = 0;
        for (Customer c : p.getCustomers()) {
            sum += c.getDemand();
        }

        int LB = (int) (sum / p.getVehicleLoadCapacity()) + 1;
        List<Vehicle> vehicles = new ArrayList<>();

        for (int i = 0; i < LB; i++) {
            vehicles.add(new Vehicle(
                    i,
                    p.getVehicleFuelTankCapacity(),
                    p.getVehicleLoadCapacity(),
                    p.getDepot()));
        }
        return vehicles;
    }
}
