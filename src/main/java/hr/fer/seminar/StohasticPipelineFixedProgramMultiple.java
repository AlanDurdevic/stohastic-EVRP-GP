package hr.fer.seminar;

import static io.jenetics.util.RandomRegistry.random;

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
import java.util.function.Supplier;

import hr.fer.seminar.entities.ChargingStation;
import hr.fer.seminar.entities.Customer;
import hr.fer.seminar.entities.Depot;
import hr.fer.seminar.entities.Location;
import hr.fer.seminar.entities.StohasticEVRPProblem;
import hr.fer.seminar.operators.MyMathOp;
import hr.fer.seminar.operators.cs.CustomerSelector;
import hr.fer.seminar.operators.cs.GPCustomerSelectorStohastic;
import hr.fer.seminar.operators.cs.MSStohasticCustomerSelector;
import hr.fer.seminar.operators.vs.VehicleStrategy;
import hr.fer.seminar.solution.gp.GeneticProgrammingStohasticEVRP;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingStohasticEVRPMultiple;
import hr.fer.seminar.solution.gp.impl.GeneticProgrammingStohasticEVRPHeuristicVehicle;
import hr.fer.seminar.util.stohastic.distribution.Distribution;
import hr.fer.seminar.util.stohastic.distribution.impl.LognormalDistribution;
import hr.fer.seminar.util.stohastic.distribution.impl.NoDistribution;
import hr.fer.seminar.util.stohastic.distribution.impl.UniformDistribution;
import io.jenetics.Genotype;
import io.jenetics.ext.util.TreeNode;
import io.jenetics.prog.ProgramChromosome;
import io.jenetics.prog.ProgramGene;
import io.jenetics.prog.op.EphemeralConst;
import io.jenetics.prog.op.MathOp;
import io.jenetics.prog.op.Op;
import io.jenetics.prog.op.Var;
import io.jenetics.util.ISeq;

public class StohasticPipelineFixedProgramMultiple {

    private static final int numberOfExperiments = 1;

    private static final String resultsFile = "./time";

    private static final VehicleStrategy vs = VehicleStrategy.Serial;

    private static final String testFolder = "./data/stohastic/test";

    private static final int numberOfClonesTest = 6;

    private static final long initialBaseSeed = 12345L;

    private static long baseSeed = initialBaseSeed;

    private static final List<String> FUNCTIONS = List.of(
        "add(Eni,Eni)",
        "sub(div(min0(Cvk),neg(ECni)),max(max0(max(neg(ECni),ERPni)),log(div(exp(CsumV),min(ERPpvk,Tvk)))))",
        "neg(0.7)",
        "max(log(neg(min(Eni,Eni))),min(neg(min(neg(Eni),Eni)),max0(sqr(Eni))))",
        "sqr(min(log(RTni),min0(add(RTni,Evk))))",
        "min0(Var_Dni)",
        "div(Eni,max0(log(Eni)))",
        "min0(Evk)",
        "exp(Eni)",
        "add(max(div(exp(sqr(exp(sqr(Eni)))),exp(mul(EDepni,div(exp(sqr(Eni)),exp(mul(EDepni,ECni)))))),min(0.1,sub(DsumUC,mul(EDepni,BestOtherETA_i)))),EDepni)"
    );

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

    private static final class Scenario {
        final String name;
        final Supplier<Distribution> demand;
        final Supplier<Distribution> service;
        final Supplier<Distribution> velocity;

        Scenario(
            String name,
            Supplier<Distribution> demand,
            Supplier<Distribution> service,
            Supplier<Distribution> velocity
        ) {
            this.name = name;
            this.demand = demand;
            this.service = service;
            this.velocity = velocity;
        }
    }

    private static final class TestSummary {
        final List<Double> errors;
        final List<Double> timesMs;

        TestSummary(List<Double> errors, List<Double> timesMs) {
            this.errors = errors;
            this.timesMs = timesMs;
        }
    }

    private static final List<Scenario> SCENARIOS = List.of(
        new Scenario(
            "TEST-DETERMINISTIC-0,0,0",
            NoDistribution::new,
            NoDistribution::new,
            NoDistribution::new
        ),

        new Scenario(
            "TEST-LOGNORMAL-0.1,0,0",
            () -> new LognormalDistribution(0.1),
            NoDistribution::new,
            NoDistribution::new
        ),

        new Scenario(
            "TEST-LOGNORMAL-0.2,0,0",
            () -> new LognormalDistribution(0.2),
            NoDistribution::new,
            NoDistribution::new
        ),

        new Scenario(
            "TEST-LOGNORMAL-0.3,0,0",
            () -> new LognormalDistribution(0.3),
            NoDistribution::new,
            NoDistribution::new
        ),

        new Scenario(
            "TEST-LOGNORMAL-0,0.1,0",
            NoDistribution::new,
            () -> new LognormalDistribution(0.1),
            NoDistribution::new
        ),

        new Scenario(
            "TEST-LOGNORMAL-0,0.2,0",
            NoDistribution::new,
            () -> new LognormalDistribution(0.2),
            NoDistribution::new
        ),

        new Scenario(
            "TEST-LOGNORMAL-0,0.3,0",
            NoDistribution::new,
            () -> new LognormalDistribution(0.3),
            NoDistribution::new
        ),

        new Scenario(
            "TEST-LOGNORMAL-0,0,0.1",
            NoDistribution::new,
            NoDistribution::new,
            () -> new LognormalDistribution(0.1)
        ),

        new Scenario(
            "TEST-LOGNORMAL-0,0,0.2",
            NoDistribution::new,
            NoDistribution::new,
            () -> new LognormalDistribution(0.2)
        ),

        new Scenario(
            "TEST-LOGNORMAL-0,0,0.3",
            NoDistribution::new,
            NoDistribution::new,
            () -> new LognormalDistribution(0.3)
        ),

        new Scenario(
            "TEST-LOGNORMAL-0.2,0.2,0",
            () -> new LognormalDistribution(0.2),
            () -> new LognormalDistribution(0.2),
            NoDistribution::new
        ),

        new Scenario(
            "TEST-LOGNORMAL-0.2,0,0.2",
            () -> new LognormalDistribution(0.2),
            NoDistribution::new,
            () -> new LognormalDistribution(0.2)
        ),

        new Scenario(
            "TEST-LOGNORMAL-0,0.2,0.2",
            NoDistribution::new,
            () -> new LognormalDistribution(0.2),
            () -> new LognormalDistribution(0.2)
        ),

        new Scenario(
            "TEST-LOGNORMAL-0.2,0.2,0.2",
            () -> new LognormalDistribution(0.2),
            () -> new LognormalDistribution(0.2),
            () -> new LognormalDistribution(0.2)
        ),

        new Scenario(
            "TEST-LOGNORMAL-0.3,0.3,0.3",
            () -> new LognormalDistribution(0.3),
            () -> new LognormalDistribution(0.3),
            () -> new LognormalDistribution(0.3)
        ),

        new Scenario(
            "TEST-UNIFORM-0.2,0.2,0.2",
            () -> new UniformDistribution(0.2),
            () -> new UniformDistribution(0.2),
            () -> new UniformDistribution(0.2)
        ),

        new Scenario(
            "TEST-UNIFORM-0.3,0.3,0.3",
            () -> new UniformDistribution(0.3),
            () -> new UniformDistribution(0.3),
            () -> new UniformDistribution(0.3)
        )
    );

    public static void main(String[] args) {
        List<String> testFileNames = loadTestFiles();

        List<Double> globalErrors = new ArrayList<>();
        List<Double> globalTimesMs = new ArrayList<>();

        try (PrintWriter writer = new PrintWriter(new FileWriter(resultsFile))) {

            for (int functionIndex = 0; functionIndex < FUNCTIONS.size(); functionIndex++) {
                String functionString = FUNCTIONS.get(functionIndex);

                writer.println("#FUNCTION:" + (functionIndex + 1));
                writer.println("FunctionString:" + functionString);

                Genotype<ProgramGene<Double>> fixedProgram = parseProgram(functionString);

                writer.println("ParsedProgram:" + fixedProgram.gene().toTreeNode());
                writer.println("TreeDepth:" + fixedProgram.gene().depth());
                writer.println("TreeSize:" + fixedProgram.gene().size());

                for (int experiment = 1; experiment <= numberOfExperiments; experiment++) {
                    writer.println("#EXPERIMENT:" + experiment);

                    baseSeed = initialBaseSeed + experiment;

                    for (Scenario scenario : SCENARIOS) {
                        writer.println("##" + scenario.name);

                        TestSummary summary = test(
                            writer,
                            testFileNames,
                            fixedProgram,
                            scenario.demand.get(),
                            scenario.service.get(),
                            scenario.velocity.get()
                        );

                        globalErrors.addAll(summary.errors);
                        globalTimesMs.addAll(summary.timesMs);
                    }
                }

                writer.println("#END_FUNCTION:" + (functionIndex + 1));
                writer.println();
                writer.flush();
            }

            writer.println("#GLOBAL_SUMMARY_ALL_FUNCTIONS_ALL_EXPERIMENTS_ALL_SCENARIOS_ALL_INSTANCES");
            writer.println("GLOBAL_COUNT:" + globalErrors.size());

            writer.println("GLOBAL_AVG_ERROR:" + fmt(avg(globalErrors)));
            writer.println("GLOBAL_STD_ERROR:" + fmt(stddev(globalErrors)));

            writer.println("GLOBAL_AVG_TIME_MS:" + fmt(avg(globalTimesMs)));
            writer.println("GLOBAL_STD_TIME_MS:" + fmt(stddev(globalTimesMs)));

            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("END");
        System.exit(0);
    }
    
    private static final java.util.Locale LOCALE = java.util.Locale.US;

    private static String fmt(double value) {
        return String.format(LOCALE, "%.10f", value);
    }

    private static List<String> loadTestFiles() {
        File folder = new File(testFolder);
        File[] listFiles = folder.listFiles();

        if (listFiles == null) {
            throw new IllegalStateException("Test folder ne postoji ili je prazan: " + testFolder);
        }

        List<String> testFileNames = new LinkedList<>();

        for (File file : listFiles) {
            for (int i = 0; i < numberOfClonesTest; i++) {
                testFileNames.add(testFolder + "/" + file.getName());
            }
        }

        return testFileNames;
    }

    private static TestSummary test(
        PrintWriter writer,
        List<String> testFileNames,
        Genotype<ProgramGene<Double>> bestProgram,
        Distribution demandDistribution,
        Distribution serviceDistribution,
        Distribution velocityDistribution
    ) {
        List<GeneticProgrammingStohasticEVRP> testProblems = new LinkedList<>();

        String currentFilename = "";
        int currentRepIdx = 0;

        for (String filename : testFileNames) {
            if (filename.equals(currentFilename)) {
                currentRepIdx++;
            } else {
                currentRepIdx = 0;
                currentFilename = filename;
            }

            testProblems.add(new GeneticProgrammingStohasticEVRPHeuristicVehicle(
                generateProblemTest(
                    filename,
                    demandDistribution,
                    serviceDistribution,
                    velocityDistribution,
                    currentRepIdx
                ),
                new GPCustomerSelectorStohastic(bestProgram.gene()),
                vs
            ));
        }

        List<Double> errors = new ArrayList<>();
        List<Double> timesMs = new ArrayList<>();

        for (int i = 0; i < testProblems.size(); i++) {
            GeneticProgrammingStohasticEVRP problem = testProblems.get(i);

            GeneticProgrammingStohasticEVRPMultiple gp =
                new GeneticProgrammingStohasticEVRPMultiple(List.of(problem));

            long startNs = System.nanoTime();

            double error = gp.errorWD(bestProgram) / 1000.0;

            long endNs = System.nanoTime();

            double elapsedMs = (endNs - startNs) / 1_000_000.0;

            errors.add(error);
            timesMs.add(elapsedMs);

            writer.println(
                new File(testFileNames.get(i)).getName()
                    + ":error=" + error
                    + ":timeMs=" + elapsedMs
            );
        }

        return new TestSummary(errors, timesMs);
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
                    "Nepoznat operator ili terminal u GP stringu: '" + token + "'. " +
                    "Provjeri da li postoji u OPERATIONS ili TERMINALS.",
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
                "Operator '" + op.name() + "' očekuje " + op.arity() +
                " argumenata, a dobio je " + node.childCount() +
                ". Node: " + node
            );
        }

        for (int i = 0; i < node.childCount(); i++) {
            checkArity(node.childAt(i));
        }
    }

    private static double avg(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;

        for (double v : values) {
            sum += v;
        }

        return sum / values.size();
    }

    private static double stddev(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }

        double mean = avg(values);
        double sumSquaredDiff = 0.0;

        for (double v : values) {
            double diff = v - mean;
            sumSquaredDiff += diff * diff;
        }

        return Math.sqrt(sumSquaredDiff / (values.size() - 1));
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