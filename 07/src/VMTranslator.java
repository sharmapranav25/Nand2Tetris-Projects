import java.io.File;
import java.io.IOException;

public class VMTranslator {
    public static void main(String[] args) {
        // Check if user provided an input file or directory
        if (args.length != 1) {
            System.out.println("Usage: java VMTranslator <inputfile.vm | inputdirectory>");
            return;
        }
        
        String inputPath = args[0];
        File inFile = new File(inputPath);
        
        // Determine output file name. If a directory is provided,
        // output a file with the directory's name; otherwise, change .vm to .asm.
        String outputPath;
        if (inFile.isDirectory()) {
            outputPath = inputPath + "/" + inFile.getName() + ".asm";
        } else {
            outputPath = inputPath.substring(0, inputPath.lastIndexOf('.')) + ".asm";
        }
        
        // Create an instance of CodeWriter with output file.
        CodeWriter codeWriter = null;
        try {
            codeWriter = new CodeWriter(outputPath);
        } catch (IOException e) {
            System.err.println("Error opening output file: " + e.getMessage());
            return;
        }
        
        // If the input is a directory, process every .vm file inside.
        if (inFile.isDirectory()) {
            File[] files = inFile.listFiles((dir, name) -> name.endsWith(".vm"));
            if (files != null) {
                for (File file : files) {
                    codeWriter.setFileName(file.getName().replace(".vm", ""));
                    processFile(file, codeWriter);
                }
            }
        } else {
            codeWriter.setFileName(inFile.getName().replace(".vm", ""));
            processFile(inFile, codeWriter);
        }
        
        // Finish writing, then close the CodeWriter.
        codeWriter.close();
        System.out.println("Translation complete! Output saved to " + outputPath);
    }

    private static void processFile(File file, CodeWriter codeWriter) {
        try {
            Parser parser = new Parser(file.getAbsolutePath());
            while (parser.hasMoreCommands()) {
                parser.advance();
                int type = parser.commandType();
                if (type == Parser.C_ARITHMETIC) {
                    codeWriter.writeArithmetic(parser.arg1());
                } else if (type == Parser.C_PUSH || type == Parser.C_POP) {
                    codeWriter.writePushPop(type, parser.arg1(), parser.arg2());
                }
                // For Project 7, these two types are sufficient.
            }
        } catch (IOException e) {
            System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
        }
    }
}

