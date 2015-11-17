# HW9 Gen
by Christian LaCourt

Generates sample input for MTU CS1131 Fall 2015 HW9.

## Usage:

If running from class files:

- Open a terminal in the project directory.
- `java -cp ../lib/commons-cli-1.3.1/commons-cli-1.3.1.jar;./bin Gen`

If running from a jar:

- Open a terminal in the same directory as the .jar.
- `java -jar gen.jar`

### Options:

- `-h, --help`: Shows the help text.
- `-v, --version`: Shows the current version of the program.
- `-c, --cores <arg>`: The number of CPU cores to be used in the simulator. Defaults to 4.
- `-p, --num-processes <arg>`: The number of processes to be generated. Defaults to 1.
- `-m, --max-commands <arg>`: The maximum number of commands to generate per process. Defaults to 150.
- `-n, --allow-negatives`: Turn on generating negative arguments for CPU and Disk operations. Defaults to off.
- `-o, --output-file <arg>`: Set the file to output to. If this is not provided, the output is printed to the standard output.
- `-s, --strict`: Enable strict mode (don't generate repeats as the first task). Defaults to off.
- `-d, --disable-output`: Disable printing to standard out (only applies if an ouput file has been set). Defaults to off.
- `-W<name>=<weight>`: Set custom weighting for each generator
  - wait: defaults to 2
  - repeat: defaults to 12
  - usedisk: defaults to 9
  - usecpu: defaults to 14

### Examples:
- `java -jar gen.jar -s | demo.hw9`
  Pipes some input to the demo program (sorry interwebs, that program's not for you)
- `java -jar gen.jar -n -p 64 -c 16 -m 250 -Wusedisk=30 -Wwait=0 -o sample.input | java -cp ./bin simulator.Main`
  Creates 64 processes no longer than 250 commands to run on a 16-core CPU with the extra credit check enable, with an emphasis on disk operations and waits disabled. Outputs the input to `sample.input` and standard output, which is then piped to your code.