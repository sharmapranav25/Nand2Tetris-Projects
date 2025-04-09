import java.io.*;
import java.util.*;

public class Parser {
    // Define command types as constants.
    public static final int C_ARITHMETIC = 0;
    public static final int C_PUSH = 1;
    public static final int C_POP = 2;
    
    private List<String> commands;
    private int currentIndex = 0;
    private String currentCommand;

    public Parser(String filePath) throws IOException {
        commands = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            // Remove comments and trim whitespace.
            if (line.contains("//")) {
                line = line.substring(0, line.indexOf("//"));
            }
            line = line.trim();
            if (!line.isEmpty()) {
                commands.add(line);
            }
        }
        reader.close();
    }

    public boolean hasMoreCommands() {
        return currentIndex < commands.size();
    }

    public void advance() {
        currentCommand = commands.get(currentIndex);
        currentIndex++;
    }
    
    // Determine the command type.
    public int commandType() {
        if (currentCommand.startsWith("push")) {
            return C_PUSH;
        } else if (currentCommand.startsWith("pop")) {
            return C_POP;
        } else {
            // For Project 7, remaining commands are arithmetic.
            return C_ARITHMETIC;
        }
    }

    // For an arithmetic command, return the command itself (like "add", "sub", etc.)
    // For push/pop, arg1() will return the segment (like "local", "argument").
    public String arg1() {
        if (commandType() == C_ARITHMETIC) {
            return currentCommand;
        } else {
            String[] parts = currentCommand.split("\\s+");
            return parts[1];
        }
    }

    // For push/pop commands, return the index (as an integer)
    public int arg2() {
        if (commandType() == C_PUSH || commandType() == C_POP) {
            String[] parts = currentCommand.split("\\s+");
            return Integer.parseInt(parts[2]);
        }
        return -1; // Should not be reached for arithmetic commands.
    }
}

