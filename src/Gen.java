
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
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
    private static class Generator implements Supplier<String> {
        private String name;
        private Supplier<String> generator;
        Generator(String name, Supplier<String> generator){
            this.name = name;
            this.generator = generator;
        }
        String getName(){
            return name;
        }
        @Override public String get(){
            return generator.get();
        }
    }
    private static int level = 0; // indentation, multiples of 2.
    private static boolean genNegatives = false; // generate negative values?
    private static boolean strictMode = false;
    private static boolean disableOutput = false;
    private static boolean justGeneratedRepeat = false; // just generated a repeat tasks's start?
    private static PrintStream out = System.out;

    private static final Random random = new Random(); // rng
    private static final int MAX_COMMANDS = 150; // max number of commands a process can have.

    public static void main(String... args) {
        int cores = 4;
        int nproc = 1;

        int max = 0; // maximum key in generators
        TreeMap<Integer, Generator> generators = new TreeMap<>();
        HashMap<String, Integer> weights = new HashMap<>();
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
            options.addOption("s", "strict", false, "Enable strict mode (don't generate repeats as the first task).");
            options.addOption("d", "disable-output", false, "Disable printing to standard out (only applies if an ouput file has been set).");
            options.addOption(Option.builder("W").hasArgs().valueSeparator('=').build());
            CommandLine cmd = new DefaultParser().parse(options, args);
            if(cmd.hasOption("h")) {
                HelpFormatter f = new HelpFormatter();
                String usage = "java -cp ../lib/commons-cli-1.3.1/commons-cli-1.3.1.jar;./ Gen OR java -jar Gen.jar";
                f.printHelp(usage, "Generates random input for MTU CS1131 Fall 2015 HW9.", options, "", true);
                System.exit(0);
            }
            if(cmd.hasOption("W")){
                String[] opts = cmd.getOptionValues("W");
                for(int i = 0; i < opts.length; i += 2){
                    weights.put(opts[i].toLowerCase(), Integer.parseInt(opts[i + 1]));
                }
            }
            strictMode = cmd.hasOption("s");
            genNegatives = cmd.hasOption("n");
            disableOutput = cmd.hasOption("d");
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
        generators.put(0, new Generator("wait", () -> String.format("%" + level + "swait %d", "", random.nextInt(100) + 1))); 
        max += weights.get("wait");

        // == REPEAT ==
        generators.put(max, new Generator("repeat", () -> {
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
        }));
        max += weights.get("repeat");

        // == USE DISK ==
        generators.put(max, new Generator("usedisk", () -> String.format("%" + level + "suse Disk %d", "", (random.nextBoolean() && genNegatives)?-1:random.nextInt(1024))));
        max += weights.get("usedisk");

        // == USE CPU ==
        generators.put(max, new Generator("usecpu", () -> String.format("%" + level + "suse CPU %d", "", (random.nextDouble() < 0.1 && genNegatives?-1:1) * random.nextInt(random.nextDouble() < 0.2?100:1500))));
        max += weights.get("usecpu");

        writeln(cores);
        for(int cproc = 1; cproc <= nproc; cproc++) {
            writef("process%d%n", cproc);
            level = 2;
            int numCommands = getNumCommands();
            if(strictMode){
                numCommands--;
                Generator g = generators.floorEntry(random.nextInt(max)).getValue();
                while(g.getName().equals("repeat"))
                    g = generators.floorEntry(random.nextInt(max)).getValue();
                writeln(g.get());
            }
            for(; numCommands > 0; numCommands--) {
                writeln(generators.floorEntry(random.nextInt(max)).getValue().get());
            }
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
    
    private static void writef(String formatString, Object... arguments){
        if(out != System.out){
            out.printf(formatString, arguments);
            if(!disableOutput){
                System.out.printf(formatString, arguments);
            }
        } else if(!disableOutput){
            System.out.printf(formatString, arguments);
        }
    }
    
    private static void writeln(Object string){
        if(out != System.out){
            out.println(string);
            if(!disableOutput){
                System.out.println(string);
            }
        } else if(!disableOutput){
            System.out.println(string);
        }
    }
}
