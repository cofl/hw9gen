
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Generate sample input for MTU CS1131 Fall 2015 HW9.
 * 
 * @author Christian LaCourt
 */
public class Gen {

    /**
     * A command generator.
     */
    private static class Generator implements Supplier<String> {
        private String name;
        private Supplier<String> generator;

        Generator(String name, Supplier<String> generator) {
            this.name = name;
            this.generator = generator;
        }

        String getName() {
            return name;
        }

        @Override
        public String get() {
            return generator.get();
        }
    }

    private static int level = 0; // indentation, multiples of 2.
    private static boolean genNegatives = false; // generate negative values?
    private static boolean disableOutput = false; // disable standard output when outputting to a file
    private static boolean justGeneratedRepeat; // just generated a repeat tasks's start?
    private static PrintStream out = System.out;

    private static final Random random = new Random(); // rng
    private static int MAX_COMMANDS = 150; // max number of commands a process can have. (> 5)

    private static final TreeMap<Integer, Generator> generators = new TreeMap<>();
    private static final HashMap<String, Integer> weights = new HashMap<>();
    private static int MAX_WEIGHT;

    public static void main(String... args) {
        int cores = 4;
        int nproc = 1;

        weights.put("wait", 2);
        weights.put("repeat", 12);
        weights.put("usedisk", 9);
        weights.put("usecpu", 14);

        try {
            Options options = new Options();
            options.addOption("c", "cores", true, "The number of CPU cores to be used in the simulator. Defaults to 4.");
            options.addOption("n", "allow-negatives", false, "Generate negative arguments for CPU and Disk operations.");
            options.addOption("p", "num-processes", true, "The number of processes to be generated. Defaults to 1.");
            options.addOption("o", "output-file", true, "A file to write the generated sample input to.");
            options.addOption("h", "help", false, "Show this help text.");
            options.addOption("d", "disable-output", false, "Disable printing to standard out (only applies if an ouput file has been set).");
            options.addOption("m", "max-commands", true, "Set the maximum number of commands to generate (Must not be less than 5).");
            options.addOption("v", "version", false, "Shows the current version of the program.");
            options.addOption(Option.builder("W").hasArgs().valueSeparator('=').build());
            CommandLine cmd = new DefaultParser().parse(options, args);
            if(cmd.hasOption("h")) {
                HelpFormatter f = new HelpFormatter();
                System.out.println("hw9gen v0.3.1 Cordial Cobalt");
                String usage = "java -cp ../lib/commons-cli-1.3.1/commons-cli-1.3.1.jar;./ Gen OR java -jar Gen.jar";
                f.printHelp(usage, "Generates random input for MTU CS1131 Fall 2015 HW9.", options, "", true);
                System.exit(0);
            }
            if(cmd.hasOption("v")) {
                System.out.println("hw9gen v0.3.0 Cordial Cobalt");
                System.exit(0);
            }
            if(cmd.hasOption("W")) {
                Properties opts = cmd.getOptionProperties("W");
                for(final String name: opts.stringPropertyNames())
                    weights.put(name, Math.max(0, Integer.parseInt(opts.getProperty(name))));
            }
            genNegatives = cmd.hasOption("n");
            disableOutput = cmd.hasOption("d");
            if(cmd.hasOption("c"))
                cores = Integer.parseInt(cmd.getOptionValue("c"));
            if(cmd.hasOption("p"))
                nproc = Integer.parseInt(cmd.getOptionValue("p"));
            if(cmd.hasOption("o"))
                out = new PrintStream(new FileOutputStream(new File(cmd.getOptionValue("o"))));
            if(cmd.hasOption("m"))
                MAX_COMMANDS = Math.max(5, Integer.parseInt(cmd.getOptionValue("m")));
        } catch(ParseException | NumberFormatException | IOException e) {
            System.err.println("Fatal error while parsing command-line options");
            System.exit(1);
        }

        // == WAIT ==
        addGenerator(new Generator("wait", () -> String.format("%" + level + "swait %d", "", random.nextInt(100) + 1)));

        // == REPEAT ==
        addGenerator(new Generator("repeat", () -> {
            if(random.nextBoolean() && level > 2) {
                level -= 2;
                return String.format("%" + level + "s;", "");
            }
            if(level > 12) {
                // Don't generate very deep repeats.
                return String.format("%" + level + "swait %d", "", random.nextInt(100) + 1);
            }
            String text = String.format("%" + level + "srepeat %d", "", random.nextInt(level > 6 ? 5 : 13) + 2);
            level += 2;
            justGeneratedRepeat = true;
            return text;
        }));

        // == USE DISK ==
        addGenerator(new Generator("usedisk", () -> String.format("%" + level + "suse Disk %d", "", (random.nextBoolean() && genNegatives) ? -1 : random.nextInt(1024))));

        // == USE CPU ==
        addGenerator(new Generator("usecpu", () -> String.format("%" + level + "suse CPU %d", "", (random.nextDouble() < 0.1 && genNegatives ? -1 : 1) * random.nextInt(random.nextDouble() < 0.2 ? 100 : 1500))));

        writeln(cores);
        for(int cproc = 1; cproc <= nproc; cproc++) {
            writef("process%d%n", cproc);
            level = 2;
            justGeneratedRepeat = true; // not really, but tricks the generator into not generating a repeat at the start of a process.
            for(int numCommands = getNumCommands(); numCommands > 1; numCommands--)
                generate();
            // next, force the last command to not be a repeat.
            justGeneratedRepeat = true; // not really, but tricks the generator into not generating an empty repeat at the end of a process.
            generate();
            while(level > 0) {
                level -= 2;
                if(level == 0) {
                    writeln(";");
                } else {
                    writef("%" + level + "s%s%n", "", ";");
                }
            }
        }
        writeln("end");
    }

    /**
     * Adds a generator to the list.
     */
    private static void addGenerator(Generator generator) {
        int weight = weights.getOrDefault(generator.getName(), 1);
        if(weight == 0)
            return;
        generators.put(MAX_WEIGHT += weight, generator);
    }

    /**
     * Generates a command.
     */
    private static void generate() {
        Generator g;
        do
            g = generators.ceilingEntry(random.nextInt(MAX_WEIGHT)).getValue();
        while(justGeneratedRepeat && g.getName().equals("repeat"));
        if(justGeneratedRepeat && !g.getName().equals("repeat"))
            justGeneratedRepeat = false;
        writeln(g.get());
    }

    /**
     * Gets the number of commands to generate by Gaussian distribution from 4 to MAX_COMMANDS.
     */
    private static int getNumCommands() {
        double range = 1.8;
        double distrib = random.nextGaussian() + range / 2;
        if(distrib < 0)
            distrib = 0;
        else if(distrib > range)
            distrib = range;
        return (int) Math.round((MAX_COMMANDS - 4) * distrib / range) + 4;
    }

    /**
     * Write to all outputs.
     */
    private static void writef(String formatString, Object... arguments) {
        if(out != System.out) {
            out.printf(formatString, arguments);
            if(!disableOutput) {
                System.out.printf(formatString, arguments);
            }
        } else {
            System.out.printf(formatString, arguments);
        }
    }

    /**
     * Write to all outputs.
     */
    private static void writeln(Object string) {
        if(out != System.out) {
            out.println(string);
            if(!disableOutput) {
                System.out.println(string);
            }
        } else {
            System.out.println(string);
        }
    }
}
