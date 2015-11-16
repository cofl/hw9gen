
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Generate sample input for MTU CS1131 Fall 2015 HW9.
 * 
 * @author Christian LaCourt
 */
public class Gen {
    private static int level = 0; // indentation, multiples of 2.
    private static boolean genNegatives = false; // generate negative values?
    private static boolean justGeneratedRepeat = false; // just generated a
                                                        // repeat tasks's start?

    private static final Random random = new Random(); // rng
    private static final int MAX_COMMANDS = 150; // max number of commands a
                                                 // process can have.

    public static void main(String... args) {
        int cores = 4;
        int nproc = 1;
        PrintStream out = System.out;

        int max = 0; // maximum key in generators
        TreeMap<Integer, Supplier<String>> generators = new TreeMap<>();

        try {
            Options options = new Options();
            options.addOption("c", "cores", true, "The number of CPU cores to be used in the simulator. Defaults to 4.");
            options.addOption("n", "allow-negatives", false, "Generate negative arguments for CPU and Disk operations.");
            options.addOption("p", "num-processes", true, "The number of processes to be generated. Defaults to 1.");
            options.addOption("o", "output-file", true, "A file to write the generated sample input to.");
            options.addOption("h", "help", false, "Show this help text.");
            CommandLine cmd = new DefaultParser().parse(options, args);
            if(cmd.hasOption("h")) {
                HelpFormatter f = new HelpFormatter();
                String usage = "java -cp ../lib/commons-cli-1.3.1/commons-cli-1.3.1.jar;./ Gen OR java -jar Gen.jar";
                f.printHelp(usage, "Generates random input for MTU CS1131 Fall 2015 HW9.", options, "", true);
                System.exit(0);
            }
            genNegatives = cmd.hasOption("n");
            if(cmd.hasOption("c")) {
                cores = Integer.parseInt(cmd.getOptionValue("c"));
            }
            if(cmd.hasOption("p")) {
                nproc = Integer.parseInt(cmd.getOptionValue("p"));
            }
            if(cmd.hasOption("o")) {
                out = new PrintStream(new FileOutputStream(new File(cmd.getOptionValue("o"))));
            }
        } catch(ParseException | NumberFormatException | IOException e) {
            System.err.println("Fatal error while parsing command-line options");
            System.exit(1);
        }

        // == WAIT ==
        generators.put(0, () -> String.format("%" + level + "swait %d", "", random.nextInt(100) + 1));
        max += 2;

        // == REPEAT ==
        generators.put(max, () -> {
            if(random.nextBoolean() && level > 2 && !justGeneratedRepeat) {
                level -= 2;
                justGeneratedRepeat = false;
                return String.format("%" + level + "s;", "");
            }
            if(level > 12 || justGeneratedRepeat) {
                // Don't generate very deep repeats.
                justGeneratedRepeat = false;
                return String.format("%" + level + "swait %d", "", random.nextInt(100) + 1);
            }
            String text = String.format("%" + level + "srepeat %d", "", random.nextInt(level > 6?5:13) + 2);
            level += 2;
            justGeneratedRepeat = true;
            return text;
        });
        max += 20;

        // == USE DISK ==
        generators.put(max, () -> String.format("%" + level + "suse Disk %d", "", (random.nextBoolean() && genNegatives)?-1:random.nextInt(1024)));
        max += 9;

        // == USE CPU ==
        generators.put(max, () -> String.format("%" + level + "suse CPU %d", "", (random.nextDouble() < 0.1 && genNegatives?-1:1) * random.nextInt(random.nextDouble() < 0.2?100:1500)));
        max += 14;

        out.println(cores);
        for(int cproc = 1; cproc <= nproc; cproc++) {
            System.out.printf("process%d%n", cproc);
            level = 2;
            for(int numCommands = getNumCommands(); numCommands > 0; numCommands--) {
                System.out.println(generators.floorEntry(random.nextInt(max)).getValue().get());
            }
            while(level > 0) {
                level -= 2;
                if(level == 0) {
                    System.out.println(";");
                } else {
                    System.out.printf("%" + level + "s%s%n", "", ";");
                }
            }
        }
        out.println("end");
        if(out != System.out) {
            out.close();
            System.out.println("Generation complete.");
        }
    }

    private static int getNumCommands() {
        double range = 1.8;
        double distrib = random.nextGaussian() + range / 2;
        if(distrib < 0)
            distrib = 0;
        if(distrib > range)
            distrib = range;
        return (int) Math.round((MAX_COMMANDS - 4) * distrib / range) + 4;
    }
}
