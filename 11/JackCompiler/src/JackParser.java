import java.io.IOException;
import java.util.List;

public class JackParser {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java JackParser <input.jack> <output.vm>");
            System.exit(1);
        }

        String input = args[0];
        String output = args[1];

        try {
            // Tokenize
            JackTokenizer tokenizer = new JackTokenizer(input);
            List<JackTokenizer.Token> tokens = tokenizer.getTokens();

            // Set up VMWriter
            VMWriter vmWriter = new VMWriter(output);

            // Parse and generate VM code
            CompilationEngine engine = new CompilationEngine(tokens, vmWriter);
            engine.compileClass();

            // Done
            vmWriter.close();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
