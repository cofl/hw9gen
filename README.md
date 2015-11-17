# HW9 Gen by Christian LaCourt

Generates sample input for MTU CS1131 Fall 2015 HW9.

## Usage:

If running from class files:

- Open a terminal in the project directory.
- `java -cp ../lib/commons-cli-1.3.1/commons-cli-1.3.1.jar;./bin Gen`

If running from a jar:

- Open a terminal in the same directory as the .jar.
- `java -jar gen.jar`

### Options:

- `-c, --cores <arg>`: The number of CPU cores to be used in the simulator. Defaults to 4.
- `-h, --help`: Shows the help text.
- `-n, --allow-negatives`: Turn on generating negative arguments for CPU and Disk operations.
- `-o, --output-file <arg>`: Set the file to output to. If this is not provided, the output is printed to the standard output.
- `-p, --num-processes <arg>`: The number of processes to be generated. Defaults to 1.

