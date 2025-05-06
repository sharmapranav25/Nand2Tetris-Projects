// File: Tokenize.java
import java.io.IOException;

public class Tokenize {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Tokenize <input.jack> <output.xml>");
            System.exit(1);
        }
        String input = args[0];
        String output = args[1];
        try {
            JackTokenizer tokenizer = new JackTokenizer(input);
            tokenizer.writeTokens(output);
        } catch (IOException e) {
            System.err.println("Error during tokenization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
