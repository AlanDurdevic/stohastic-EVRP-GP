package hr.fer.seminar;

import static io.jenetics.util.RandomRegistry.random;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hr.fer.seminar.entities.ChargingStation;
import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.Depot;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.StohasticEVRPProblem;
import hr.fer.seminar.operators.MyMathOp;
import hr.fer.seminar.operators.cs.GPCustomerSelectorStohastic;
import hr.fer.seminar.operators.vs.VehicleStrategy;
import hr.fer.seminar.solution.gp.GeneticProgrammingStohasticEVRP;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingStohasticEVRPMultiple;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingStohasticEVRPHeuristicVehicle;
import hr.fer.seminar.util.stohastic.distribution.Distribution;
import hr.fer.seminar.util.stohastic.distribution.impl.LognormalDistribution;
import hr.fer.seminar.util.stohastic.distribution.impl.NoDistribution;
import io.jenetics.Genotype;
import io.jenetics.ext.util.TreeNode;
import io.jenetics.prog.ProgramChromosome;
import io.jenetics.prog.ProgramGene;
import io.jenetics.prog.op.EphemeralConst;
import io.jenetics.prog.op.MathOp;
import io.jenetics.prog.op.Op;
import io.jenetics.prog.op.Var;
import io.jenetics.util.ISeq;

public class StohasticPipelineFixedSingle {

    /*
     * OPTION 1:
     * Set function and test file directly here.
     */
    private static final String FUNCTION_STRING =
        "add(Eni,Eni)";

    private static final String TEST_FILE =
        "./data/stohastic/test/rc207_21.txt";

    private static final String resultsFile = "./single-test-result";

    private static final VehicleStrategy vs = VehicleStrategy.Serial;

    private static final long baseSeed = 12345L;

    /*
     * One test case.
     *
     * For deterministic:
     * demand = NoDistribution
     * service = NoDistribution
     * velocity = NoDistribution
     *
     * For stochastic example:
     * new LognormalDistribution(0.2)
     */
    private static final Distribution DEMAND_DISTRIBUTION = new NoDistribution();
    private static final Distribution SERVICE_DISTRIBUTION = new NoDistribution();
    private static final Distribution VELOCITY_DISTRIBUTION = new NoDistribution();

    /*
     * If you want stochastic test case instead, use this:
     *
     * private static final Distribution DEMAND_DISTRIBUTION = new LognormalDistribution(0.2);
     * private static final Distribution SERVICE_DISTRIBUTION = new LognormalDistribution(0.2);
     * private static final Distribution VELOCITY_DISTRIBUTION = new LognormalDistribution(0.2);
     */

    private static final ISeq<Op<Double>> OPERATIONS = ISeq.of(
        MathOp.SUB,
        MathOp.ADD,
        MyMathOp.DIV,
        MathOp.MUL,
        MathOp.MAX,
        MathOp.MIN,
        MathOp.NEG,
        MyMathOp.POW2,
        MyMathOp.SQR,
        MathOp.EXP,
        MyMathOp.LOG,
        MyMathOp.MAX0,
        MyMathOp.MIN0
    );

    private static final ISeq<Op<Double>> TERMINALS = ISeq.of(
        Var.of("Eni", 0),
        Var.of("Dni", 1),
        Var.of("DDni", 2),
        Var.of("STni", 3),
        Var.of("RTni", 4),
        Var.of("Evk", 5),
        Var.of("Cvk", 6),
        Var.of("Tvk", 7),
        Var.of("ECni", 8),
        Var.of("ERPni", 9),
        Var.of("EDepni", 10),
        Var.of("ERPpvk", 11),
        Var.of("EDeppvk", 12),
        Var.of("Var_Dni", 13),
        Var.of("Var_Sni", 14),
        Var.of("Var_Tij", 15),
        Var.of("Slack_TW", 16),
        Var.of("UC", 17),
        Var.of("DsumUC", 18),
        Var.of("CsumV", 19),
        Var.of("BestOtherETA_i", 20),
        Var.of("Slack_Self", 21),
        EphemeralConst.of(() -> (random().nextInt(11) / 10.0))
    );

    private static final java.util.Locale LOCALE = java.util.Locale.US;

    private static String fmt(double value) {
        return String.format(LOCALE, "%.10f", value);
    }

    public static void main(String[] args) {
        /*
         * OPTION 2:
         * You can also provide function and file path from command line:
         *
         * args[0] = function string
         * args[1] = test file path
         *
         * Example:
         * java hr.fer.seminar.StohasticPipelineFixedSingle "add(Eni,Eni)" "./data/stohastic/test/file.txt"
         */
        String functionString = args.length >= 1 ? args[0] : FUNCTION_STRING;
        String testFile = args.length >= 2 ? args[1] : TEST_FILE;

        Genotype<ProgramGene<Double>> fixedProgram = parseProgram(functionString);

        try (PrintWriter writer = new PrintWriter(new FileWriter(resultsFile))) {
            writer.println("FunctionString:" + functionString);
            writer.println("ParsedProgram:" + fixedProgram.gene().toTreeNode());
            writer.println("TreeDepth:" + fixedProgram.gene().depth());
            writer.println("TreeSize:" + fixedProgram.gene().size());
            writer.println("TestFile:" + testFile);

            StohasticEVRPProblem problemData = generateProblemTest(
                testFile,
                DEMAND_DISTRIBUTION,
                SERVICE_DISTRIBUTION,
                VELOCITY_DISTRIBUTION,
                0
            );

            GeneticProgrammingStohasticEVRP problem =
                new GeneticProgrammingStohasticEVRPHeuristicVehicle(
                    problemData,
                    new GPCustomerSelectorStohastic(fixedProgram.gene()),
                    vs
                );

            GeneticProgrammingStohasticEVRPMultiple gp =
                new GeneticProgrammingStohasticEVRPMultiple(List.of(problem));

            long startNs = System.nanoTime();

            double error = gp.errorWD(fixedProgram) / 1000.0;

            long endNs = System.nanoTime();

            double elapsedMs = (endNs - startNs) / 1_000_000.0;

            writer.println("ERROR:" + fmt(error));
            writer.println("TIME_MS:" + fmt(elapsedMs));

            System.out.println("ERROR:" + fmt(error));
            System.out.println("TIME_MS:" + fmt(elapsedMs));

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("END");
        System.exit(0);
    }

    private static Genotype<ProgramGene<Double>> parseProgram(String expression) {
        Map<String, Op<Double>> opByName = createOpMap();

        List<Op<Double>> terminals = new ArrayList<>();
        TERMINALS.forEach(terminals::add);

        TreeNode<Op<Double>> tree = TreeNode.parse(expression, token -> {
            Op<Double> op = opByName.get(token);

            if (op != null) {
                return op;
            }

            try {
                double value = Double.parseDouble(token);
                Op<Double> constant = Op.of(token, 0, args -> value);

                opByName.put(token, constant);
                terminals.add(constant);

                return constant;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Unknown operator or terminal in GP string: '" + token + "'.",
                    e
                );
            }
        });

        checkArity(tree);

        ProgramChromosome<Double> chromosome = ProgramChromosome.of(
            tree,
            OPERATIONS,
            ISeq.of(terminals)
        );

        return Genotype.of(chromosome);
    }

    private static Map<String, Op<Double>> createOpMap() {
        Map<String, Op<Double>> map = new HashMap<>();

        for (Op<Double> op : OPERATIONS) {
            map.put(op.name(), op);
        }

        for (Op<Double> terminal : TERMINALS) {
            map.put(terminal.name(), terminal);
        }

        map.put("+", MathOp.ADD);
        map.put("-", MathOp.SUB);
        map.put("*", MathOp.MUL);

        map.put("add", MathOp.ADD);
        map.put("sub", MathOp.SUB);
        map.put("mul", MathOp.MUL);
        map.put("max", MathOp.MAX);
        map.put("min", MathOp.MIN);
        map.put("neg", MathOp.NEG);
        map.put("exp", MathOp.EXP);

        map.put("div", MyMathOp.DIV);
        map.put("pow2", MyMathOp.POW2);
        map.put("sqr", MyMathOp.SQR);
        map.put("log", MyMathOp.LOG);
        map.put("max0", MyMathOp.MAX0);
        map.put("min0", MyMathOp.MIN0);

        return map;
    }

    private static void checkArity(TreeNode<Op<Double>> node) {
        Op<Double> op = node.value();

        if (op.arity() != node.childCount()) {
            throw new IllegalArgumentException(
                "Operator '" + op.name() + "' expects " + op.arity() +
                " arguments, but got " + node.childCount() +
                ". Node: " + node
            );
        }

        for (int i = 0; i < node.childCount(); i++) {
            checkArity(node.childAt(i));
        }
    }

    private static StohasticEVRPProblem generateProblemTest(
        String filename,
        Distribution demandDistribution,
        Distribution serviceDistribution,
        Distribution velocityDistribution,
        int repIdx
    ) {
        Depot depot = null;

        double dueDate = 0;
        double vehicleFuelTankCapacity = 0;
        double vehicleLoadCapacity = 0;
        double fuelConsumptionRate = 0;
        double inverseRefuelingRate = 0;
        double averageVelocity = 0;

        List<Customer> customers = new ArrayList<>();
        List<ChargingStation> chargingStations = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            br.readLine();

            while (true) {
                String line = br.readLine();

                if (line == null || line.isBlank()) {
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

                        ChargingStation newChargingStation =
                            new ChargingStation(id, x, y, dueDate);

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

                        Customer newCustomer = new Customer(
                            id,
                            x,
                            y,
                            demand,
                            readyTime,
                            dueDateCustomer,
                            serviceTime
                        );

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

        return new StohasticEVRPProblem(
            depot,
            dueDate,
            vehicleFuelTankCapacity,
            vehicleLoadCapacity,
            fuelConsumptionRate,
            inverseRefuelingRate,
            averageVelocity,
            customers,
            chargingStations,
            demandDistribution,
            serviceDistribution,
            velocityDistribution
        );
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