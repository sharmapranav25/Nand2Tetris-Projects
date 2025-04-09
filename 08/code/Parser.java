import java.io.*;
import java.util.*;

public class Parser {
    // Command type constants.
    public static final int C_ARITHMETIC = 0;
    public static final int C_PUSH       = 1;
    public static final int C_POP        = 2;
    public static final int C_LABEL      = 3;
    public static final int C_GOTO       = 4;
    public static final int C_IF         = 5;
    public static final int C_FUNCTION   = 6;
    public static final int C_CALL       = 7;
    public static final int C_RETURN     = 8;
    
    private List<String> commands;
    private int currentIndex = 0;
    private String currentCommand;

    public Parser(String filePath) throws IOException {
        commands = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            // Remove inline comments and trim whitespace.
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

    // Are there more commands in the input?
    public boolean hasMoreCommands() {
        return currentIndex < commands.size();
    }

    // Reads the next command from the input.
    public void advance() {
        currentCommand = commands.get(currentIndex);
        currentIndex++;
    }
    
    // Determines the command type of the current command.
    public int commandType() {
        if (currentCommand.startsWith("push"))
            return C_PUSH;
        else if (currentCommand.startsWith("pop"))
            return C_POP;
        else if (currentCommand.startsWith("if-goto"))
            return C_IF;
        else if (currentCommand.startsWith("goto"))
            return C_GOTO;
        else if (currentCommand.startsWith("label"))
            return C_LABEL;
        else if (currentCommand.startsWith("function"))
            return C_FUNCTION;
        else if (currentCommand.startsWith("call"))
            return C_CALL;
        else if (currentCommand.startsWith("return"))
            return C_RETURN;
        else
            return C_ARITHMETIC;
    }
    
    // For arithmetic commands, arg1() returns the entire command (e.g., "add").
    // For push/pop, label, goto, if-goto, function, and call, arg1() returns the first argument.
    // For return, which has no arguments, it returns an empty string.
    public String arg1() {
        int type = commandType();
        if (type == C_ARITHMETIC) {
            return currentCommand;
        } else if (type == C_RETURN) {
            return "";
        } else {
            String[] parts = currentCommand.split("\\s+");
            return parts[1];
        }
    }
    
    // For push, pop, function, and call commands, arg2() returns the second argument as an integer.
    // For other commands, no second argument is used.
    public int arg2() {
        int type = commandType();
        if (type == C_PUSH || type == C_POP || type == C_FUNCTION || type == C_CALL) {
            String[] parts = currentCommand.split("\\s+");
            return Integer.parseInt(parts[2]);
        }
        return -1;
    }
}

