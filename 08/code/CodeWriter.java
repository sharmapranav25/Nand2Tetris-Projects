import java.io.*;

public class CodeWriter {
    private PrintWriter out;
    // Used to generate unique labels (for eq, gt, lt and function calls).
    private int labelCounter = 0;
    private int returnLabelCounter = 0;
    // Holds the current file name for static variable handling.
    private String fileName;
    // Holds the current function name (for namespacing labels).
    private String currentFunction = "";

    public CodeWriter(String outputFile) throws IOException {
        out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
    }
    
    // Sets the current file name (for static variables).
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    // Sets the current function (used when processing a function declaration).
    private void setCurrentFunction(String functionName) {
        currentFunction = functionName;
    }
    
    // --------------------- Existing Methods from Project 7 ---------------------
    // Translates arithmetic commands into Hack assembly.
    public void writeArithmetic(String command) {
        if(command.startsWith("push") || command.startsWith("pop")) {
            // This method is not used for push/pop commands.
            return;
        }
        if (command.equals("add")) {
            out.println("@SP");
            out.println("AM=M-1"); // SP--, A=SP, M=y
            out.println("D=M");    // D=y
            out.println("A=A-1");  // A points to x
            out.println("M=M+D");  // x = x + y
        } else if (command.equals("sub")) {
            out.println("@SP");
            out.println("AM=M-1");
            out.println("D=M");
            out.println("A=A-1");
            out.println("M=M-D");
        } else if (command.equals("neg")) {
            out.println("@SP");
            out.println("A=M-1");
            out.println("M=-M");
        } else if (command.equals("eq") || command.equals("gt") || command.equals("lt")) {
            String jumpCondition = command.equals("eq") ? "JEQ" : (command.equals("gt") ? "JGT" : "JLT");
            String trueLabel = "TRUE_LABEL" + labelCounter;
            String endLabel = "END_LABEL" + labelCounter;
            labelCounter++;
            
            out.println("@SP");
            out.println("AM=M-1");    // pop y
            out.println("D=M");
            out.println("A=A-1");     // now point to x
            out.println("D=M-D");     // D = x - y
            out.println("@" + trueLabel);
            out.println("D;" + jumpCondition);
            out.println("@SP");
            out.println("A=M-1");
            out.println("M=0");       // set false = 0
            out.println("@" + endLabel);
            out.println("0;JMP");
            out.println("(" + trueLabel + ")");
            out.println("@SP");
            out.println("A=M-1");
            out.println("M=-1");      // set true = -1
            out.println("(" + endLabel + ")");
        } else if (command.equals("and")) {
            out.println("@SP");
            out.println("AM=M-1");
            out.println("D=M");
            out.println("A=A-1");
            out.println("M=M&D");
        } else if (command.equals("or")) {
            out.println("@SP");
            out.println("AM=M-1");
            out.println("D=M");
            out.println("A=A-1");
            out.println("M=M|D");
        } else if (command.equals("not")) {
            out.println("@SP");
            out.println("A=M-1");
            out.println("M=!M");
        }
    }
    
    // Translates push and pop commands.
    public void writePushPop(int commandType, String segment, int index) {
        if (commandType == Parser.C_PUSH) {
            if (segment.equals("constant")) {
                out.println("@" + index);
                out.println("D=A");
            } else if (segment.equals("local")) {
                out.println("@LCL");
                out.println("D=M");
                out.println("@" + index);
                out.println("A=D+A");
                out.println("D=M");
            } else if (segment.equals("argument")) {
                out.println("@ARG");
                out.println("D=M");
                out.println("@" + index);
                out.println("A=D+A");
                out.println("D=M");
            } else if (segment.equals("this")) {
                out.println("@THIS");
                out.println("D=M");
                out.println("@" + index);
                out.println("A=D+A");
                out.println("D=M");
            } else if (segment.equals("that")) {
                out.println("@THAT");
                out.println("D=M");
                out.println("@" + index);
                out.println("A=D+A");
                out.println("D=M");
            } else if (segment.equals("pointer")) {
                int address = (index == 0) ? 3 : 4;
                out.println("@" + address);
                out.println("D=M");
            } else if (segment.equals("temp")) {
                out.println("@" + (5 + index));
                out.println("D=M");
            } else if (segment.equals("static")) {
                out.println("@" + fileName + "." + index);
                out.println("D=M");
            }
            // Push D onto the stack.
            out.println("@SP");
            out.println("A=M");
            out.println("M=D");
            out.println("@SP");
            out.println("M=M+1");
        } else if (commandType == Parser.C_POP) {
            if (segment.equals("local") || segment.equals("argument") ||
                segment.equals("this") || segment.equals("that")) {
                String base = segment.equals("local")   ? "LCL" :
                              segment.equals("argument") ? "ARG" :
                              segment.equals("this")     ? "THIS" : "THAT";
                out.println("@" + base);
                out.println("D=M");
                out.println("@" + index);
                out.println("D=D+A");
                out.println("@R13");
                out.println("M=D");
            } else if (segment.equals("pointer")) {
                int address = (index == 0) ? 3 : 4;
                out.println("@" + address);
                out.println("D=A");
                out.println("@R13");
                out.println("M=D");
            } else if (segment.equals("temp")) {
                out.println("@" + (5 + index));
                out.println("D=A");
                out.println("@R13");
                out.println("M=D");
            } else if (segment.equals("static")) {
                out.println("@" + fileName + "." + index);
                out.println("D=A");
                out.println("@R13");
                out.println("M=D");
            }
            // Pop the top-of-stack into D.
            out.println("@SP");
            out.println("AM=M-1");
            out.println("D=M");
            out.println("@R13");
            out.println("A=M");
            out.println("M=D");
        }
    }
    // --------------------- End of Project 7 Methods ---------------------
    
    // --------------------- New Methods for Project 8 ---------------------
    
    // Writes the bootstrap code.
    public void writeInit() {
        // Set SP = 256.
        out.println("@256");
        out.println("D=A");
        out.println("@SP");
        out.println("M=D");
        // Call Sys.init with 0 arguments.
        writeCall("Sys.init", 0);
    }
    
    // Writes a label command. The label is namespaced by the current function.
    public void writeLabel(String label) {
        String fullLabel = currentFunction.isEmpty() ? label : currentFunction + "$" + label;
        out.println("(" + fullLabel + ")");
    }
    
    // Writes an unconditional goto command.
    public void writeGoto(String label) {
        String fullLabel = currentFunction.isEmpty() ? label : currentFunction + "$" + label;
        out.println("@" + fullLabel);
        out.println("0;JMP");
    }
    
    // Writes a conditional if-goto command.
    public void writeIf(String label) {
        String fullLabel = currentFunction.isEmpty() ? label : currentFunction + "$" + label;
        out.println("@SP");
        out.println("AM=M-1");
        out.println("D=M");
        out.println("@" + fullLabel);
        out.println("D;JNE");
    }
    
    // Writes a function declaration command.
    public void writeFunction(String functionName, int nLocals) {
        setCurrentFunction(functionName);  // update the current function context
        out.println("(" + functionName + ")");
        // Initialize nLocal local variables by pushing 0 repeatedly.
        for (int i = 0; i < nLocals; i++) {
            out.println("@0");
            out.println("D=A");
            out.println("@SP");
            out.println("A=M");
            out.println("M=D");
            out.println("@SP");
            out.println("M=M+1");
        }
    }
    
    // Writes a call command.
    public void writeCall(String functionName, int nArgs) {
        String returnLabel = "RETURN_LABEL" + returnLabelCounter++;
        // Push return address.
        out.println("@" + returnLabel);
        out.println("D=A");
        pushD();
        // Push caller's LCL, ARG, THIS, THAT.
        out.println("@LCL"); out.println("D=M"); pushD();
        out.println("@ARG"); out.println("D=M"); pushD();
        out.println("@THIS"); out.println("D=M"); pushD();
        out.println("@THAT"); out.println("D=M"); pushD();
        // Reposition ARG = SP - nArgs - 5.
        out.println("@SP");
        out.println("D=M");
        out.println("@" + (nArgs + 5));
        out.println("D=D-A");
        out.println("@ARG");
        out.println("M=D");
        // Set LCL = SP.
        out.println("@SP");
        out.println("D=M");
        out.println("@LCL");
        out.println("M=D");
        // Transfer control to the called function.
        out.println("@" + functionName);
        out.println("0;JMP");
        // Declare the return label.
        out.println("(" + returnLabel + ")");
    }
    
    // Writes a return command.
    public void writeReturn() {
        // FRAME = LCL; store FRAME in R13.
        out.println("@LCL");
        out.println("D=M");
        out.println("@R13");
        out.println("M=D");
        // RET = *(FRAME - 5); store return address in R14.
        out.println("@5");
        out.println("A=D-A");
        out.println("D=M");
        out.println("@R14");
        out.println("M=D");
        // *ARG = pop(); reposition return value for caller.
        out.println("@SP");
        out.println("AM=M-1");
        out.println("D=M");
        out.println("@ARG");
        out.println("A=M");
        out.println("M=D");
        // SP = ARG + 1.
        out.println("@ARG");
        out.println("D=M+1");
        out.println("@SP");
        out.println("M=D");
        // Restore THAT, THIS, ARG, and LCL (in that order).
        out.println("@R13");
        out.println("AM=M-1");
        out.println("D=M");
        out.println("@THAT");
        out.println("M=D");
        
        out.println("@R13");
        out.println("AM=M-1");
        out.println("D=M");
        out.println("@THIS");
        out.println("M=D");
        
        out.println("@R13");
        out.println("AM=M-1");
        out.println("D=M");
        out.println("@ARG");
        out.println("M=D");
        
        out.println("@R13");
        out.println("AM=M-1");
        out.println("D=M");
        out.println("@LCL");
        out.println("M=D");
        // Go to return address.
        out.println("@R14");
        out.println("A=M");
        out.println("0;JMP");
    }
    
    // Helper method: pushes the value in D onto the stack.
    private void pushD() {
        out.println("@SP");
        out.println("A=M");
        out.println("M=D");
        out.println("@SP");
        out.println("M=M+1");
    }
    
    // Closes the output writer.
    public void close() {
        out.close();
    }
}

