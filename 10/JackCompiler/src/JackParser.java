// File: JackParser.java
// Driver for Stage 2A: parse ExpressionlessSquare (no full-expression support)
import java.io.IOException;
import java.util.List;

public class JackParser {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java JackParser <input.jack> <output.xml>");
            System.exit(1);
        }
        String input = args[0];
        String output = args[1];
        try {
            // Tokenize
            JackTokenizer tokenizer = new JackTokenizer(input);
            List<JackTokenizer.Token> tokens = tokenizer.getTokens();
            // Parse
            CompilationEngine engine = new CompilationEngine(tokens, output);
            engine.compileClass();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}