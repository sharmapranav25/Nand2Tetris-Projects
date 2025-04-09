import java.io.File;
import java.io.IOException;

public class VMTranslator {
    public static void main(String[] args) {
        // Check if user provided an input file or directory.
        if (args.length != 1) {
            System.out.println("Usage: java VMTranslator <inputfile.vm | inputdirectory>");
            return;
        }
        
        String inputPath = args[0];
        File inFile = new File(inputPath);
        
        // Determine the output file name.
        // If a directory is provided, output one file whose name is the directory name + ".asm".
        // Otherwise, change the .vm extension to .asm.
        String outputPath;
        if (inFile.isDirectory()) {
            outputPath = inputPath + "/" + inFile.getName() + ".asm";
        } else {
            outputPath = inputPath.substring(0, inputPath.lastIndexOf('.')) + ".asm";
        }
        
        // Create an instance of CodeWriter with the output file.
        CodeWriter codeWriter = null;
        try {
            codeWriter = new CodeWriter(outputPath);
        } catch (IOException e) {
            System.err.println("Error opening output file: " + e.getMessage());
            return;
        }
        
        // Write bootstrap code (sets SP=256 and calls Sys.init).
        codeWriter.writeInit();
        
        // Process all .vm files.
        if (inFile.isDirectory()) {
            File[] files = inFile.listFiles((dir, name) -> name.endsWith(".vm"));
            if (files != null) {
                for (File file : files) {
                    // Set the file name (needed for static variables).
                    codeWriter.setFileName(file.getName().replace(".vm", ""));
                    processFile(file, codeWriter);
                }
            }
        } else {
            codeWriter.setFileName(inFile.getName().replace(".vm", ""));
            processFile(inFile, codeWriter);
        }
        
        // Close the CodeWriter.
        codeWriter.close();
        System.out.println("Translation complete! Output saved to " + outputPath);
    }

    private static void processFile(File file, CodeWriter codeWriter) {
        try {
            Parser parser = new Parser(file.getAbsolutePath());
            while (parser.hasMoreCommands()) {
                parser.advance();
                int type = parser.commandType();
                switch (type) {
                    case Parser.C_ARITHMETIC:
                        codeWriter.writeArithmetic(parser.arg1());
                        break;
                    case Parser.C_PUSH:
                    case Parser.C_POP:
                        codeWriter.writePushPop(type, parser.arg1(), parser.arg2());
                        break;
                    case Parser.C_LABEL:
                        codeWriter.writeLabel(parser.arg1());
                        break;
                    case Parser.C_GOTO:
                        codeWriter.writeGoto(parser.arg1());
                        break;
                    case Parser.C_IF:
                        codeWriter.writeIf(parser.arg1());
                        break;
                    case Parser.C_FUNCTION:
                        codeWriter.writeFunction(parser.arg1(), parser.arg2());
                        break;
                    case Parser.C_CALL:
                        codeWriter.writeCall(parser.arg1(), parser.arg2());
                        break;
                    case Parser.C_RETURN:
                        codeWriter.writeReturn();
                        break;
                    default:
                        // Should not happen.
                        System.err.println("Unrecognized command: " + parser.arg1());
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
        }
    }
}

