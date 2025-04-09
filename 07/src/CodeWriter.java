import java.io.*;

public class CodeWriter {
    private PrintWriter out;
    // Used to generate unique labels for comparison commands.
    private int labelCounter = 0;
    // Holds the current file name (for static variables).
    private String fileName;

    public CodeWriter(String outputFile) throws IOException {
        out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
    }
    
    // Sets the current file name for handling static variables.
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    // Translates an arithmetic command into Hack assembly.
    public void writeArithmetic(String command) {
        if (command.startsWith("push") || command.startsWith("pop")) {
            // Not applicable here.
            return;
        }
        if(command.equals("add")) {
            out.println("@SP");
            out.println("AM=M-1"); // SP--, A=SP, M=y
            out.println("D=M");    // D=y
            out.println("A=A-1");  // A points to x
            out.println("M=M+D");  // x = x + y
        }
        else if(command.equals("sub")) {
            out.println("@SP");
            out.println("AM=M-1");
            out.println("D=M");
            out.println("A=A-1");
            out.println("M=M-D");
        }
        else if(command.equals("neg")) {
            out.println("@SP");
            out.println("A=M-1");
            out.println("M=-M");
        }
        else if(command.equals("eq") || command.equals("gt") || command.equals("lt")) {
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
            out.println("M=0");       // false: 0
            out.println("@" + endLabel);
            out.println("0;JMP");
            out.println("(" + trueLabel + ")");
            out.println("@SP");
            out.println("A=M-1");
            out.println("M=-1");      // true: -1 (all bits 1)
            out.println("(" + endLabel + ")");
        }
        else if(command.equals("and")) {
            out.println("@SP");
            out.println("AM=M-1");
            out.println("D=M");
            out.println("A=A-1");
            out.println("M=M&D");
        }
        else if(command.equals("or")) {
            out.println("@SP");
            out.println("AM=M-1");
            out.println("D=M");
            out.println("A=A-1");
            out.println("M=M|D");
        }
        else if(command.equals("not")) {
            out.println("@SP");
            out.println("A=M-1");
            out.println("M=!M");
        }
    }
    
    // Translates push/pop commands.
    public void writePushPop(int commandType, String segment, int index) {
        // Handle push command:
        if (commandType == Parser.C_PUSH) {
            if(segment.equals("constant")) {
                out.println("@" + index);
                out.println("D=A");
            } else if(segment.equals("local")) {
                out.println("@LCL");
                out.println("D=M");
                out.println("@" + index);
                out.println("A=D+A");
                out.println("D=M");
            }
            else if(segment.equals("argument")) {
                out.println("@ARG");
                out.println("D=M");
                out.println("@" + index);
                out.println("A=D+A");
                out.println("D=M");
            }
            else if(segment.equals("this")) {
                out.println("@THIS");
                out.println("D=M");
                out.println("@" + index);
                out.println("A=D+A");
                out.println("D=M");
            }
            else if(segment.equals("that")) {
                out.println("@THAT");
                out.println("D=M");
                out.println("@" + index);
                out.println("A=D+A");
                out.println("D=M");
            }
            else if(segment.equals("pointer")) {
                int address = (index == 0) ? 3 : 4;
                out.println("@" + address);
                out.println("D=M");
            }
            else if(segment.equals("temp")) {
                out.println("@" + (5 + index));
                out.println("D=M");
            }
            else if(segment.equals("static")) {
                out.println("@" + fileName + "." + index);
                out.println("D=M");
            }
            // Push D onto the stack.
            out.println("@SP");
            out.println("A=M");
            out.println("M=D");
            out.println("@SP");
            out.println("M=M+1");
        }
        // Handle pop command.
        else if (commandType == Parser.C_POP) {
            if(segment.equals("local") || segment.equals("argument") ||
               segment.equals("this") || segment.equals("that")) {
                String base = segment.equals("local") ? "LCL" :
                              segment.equals("argument") ? "ARG" :
                              segment.equals("this") ? "THIS" : "THAT";
                out.println("@" + base);
                out.println("D=M");
                out.println("@" + index);
                out.println("D=D+A");
                out.println("@R13");
                out.println("M=D");
            }
            else if(segment.equals("pointer")) {
                int address = (index == 0) ? 3 : 4;
                out.println("@" + address);
                out.println("D=A");
                out.println("@R13");
                out.println("M=D");
            }
            else if(segment.equals("temp")) {
                out.println("@" + (5 + index));
                out.println("D=A");
                out.println("@R13");
                out.println("M=D");
            }
            else if(segment.equals("static")) {
                out.println("@" + fileName + "." + index);
                out.println("D=A");
                out.println("@R13");
                out.println("M=D");
            }
            // Pop the top of the stack into D.
            out.println("@SP");
            out.println("AM=M-1");
            out.println("D=M");
            out.println("@R13");
            out.println("A=M");
            out.println("M=D");
        }
    }
    
    public void close() {
        out.close();
    }
}

