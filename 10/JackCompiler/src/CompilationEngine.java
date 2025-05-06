// File: CompilationEngine.java

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CompilationEngine {
    private final List<JackTokenizer.Token> tokens;
    private final BufferedWriter w;
    private int index = 0;
    private int indentLevel = 0;

    public CompilationEngine(List<JackTokenizer.Token> tokens, String outputPath) throws IOException {
        this.tokens = tokens;
        this.w = Files.newBufferedWriter(Path.of(outputPath));
    }

    public void compileClass() throws IOException {
        writeStartTag("class");
        writeToken(); advance(); // 'class'
        writeToken(); advance(); // className
        writeToken(); advance(); // '{'
        while (currentIsKeyword("static") || currentIsKeyword("field")) compileClassVarDec();
        while (currentIsKeyword("constructor") || currentIsKeyword("function") || currentIsKeyword("method")) compileSubroutine();
        writeToken(); advance(); // '}' closing class
        writeEndTag("class");
        w.close();
    }

    private void compileClassVarDec() throws IOException {
        writeStartTag("classVarDec");
        writeToken(); advance();  // 'static'|'field'
        writeToken(); advance();  // type
        writeToken(); advance();  // varName
        while (symbol(",")) {
            writeToken(); advance();  // ','
            writeToken(); advance();  // varName
        }
        writeToken(); advance();  // ';'
        writeEndTag("classVarDec");
    }

    private void compileSubroutine() throws IOException {
    writeStartTag("subroutineDec");
    writeToken(); advance();  // 'constructor'|'function'|'method'
    writeToken(); advance();  // 'void'|type
    writeToken(); advance();  // subroutineName
    writeToken(); advance();  // '('
    compileParameterList();
    writeToken(); advance();  // ')'
    compileSubroutineBody();
    writeEndTag("subroutineDec");  // 
} 

    private void compileParameterList() throws IOException {
        writeStartTag("parameterList");
        if (isType()) {
            writeToken(); advance();  // type
            writeToken(); advance();  // varName
            while (symbol(",")) {
                writeToken(); advance();  // ','
                writeToken(); advance();  // type
                writeToken(); advance();  // varName
            }
        }
        writeEndTag("parameterList");
    }

    private void compileSubroutineBody() throws IOException {
        writeStartTag("subroutineBody");
        writeToken(); advance();  // '{'
        while (currentIsKeyword("var")) compileVarDec();
        compileStatements();
        writeToken(); advance();  // '}'
        writeEndTag("subroutineBody");
    }

    private void compileVarDec() throws IOException {
        writeStartTag("varDec");
        writeToken(); advance();  // 'var'
        writeToken(); advance();  // type
        writeToken(); advance();  // varName
        while (symbol(",")) {
            writeToken(); advance();  // ','
            writeToken(); advance();  // varName
        }
        writeToken(); advance();  // ';'
        writeEndTag("varDec");
    }

    private void compileStatements() throws IOException {
        writeStartTag("statements");
        while (true) {
            if      (currentIsKeyword("let"))    compileLet();
            else if (currentIsKeyword("if"))     compileIf();
            else if (currentIsKeyword("while"))  compileWhile();
            else if (currentIsKeyword("do"))     compileDo();
            else if (currentIsKeyword("return")) compileReturn();
            else break;
        }
        writeEndTag("statements");
    }

    private void compileLet() throws IOException {
    advance(); // 'let'
    String varName = current().text; advance(); // varName

    boolean isArray = false;
    if (symbol("[")) {
        isArray = true;
        advance(); // '['
        compileExpression(); // index
        advance(); // ']'

        String kind = symbolTable.kindOf(varName);
        int index = symbolTable.indexOf(varName);
        String segment = switch (kind) {
            case "var" -> "local";
            case "arg" -> "argument";
            case "static" -> "static";
            case "field" -> "this";
            default -> throw new RuntimeException("Unknown kind: " + kind);
        };
        vmWriter.writePush(segment, index);
        vmWriter.writeArithmetic("add");
    }

    advance(); // '='
    compileExpression();
    advance(); // ';'

    if (isArray) {
        vmWriter.writePop("temp", 0);
        vmWriter.writePop("pointer", 1);
        vmWriter.writePush("temp", 0);
        vmWriter.writePop("that", 0);
    } else {
        String kind = symbolTable.kindOf(varName);
        int index = symbolTable.indexOf(varName);
        String segment = switch (kind) {
            case "var" -> "local";
            case "arg" -> "argument";
            case "static" -> "static";
            case "field" -> "this";
            default -> throw new RuntimeException("Unknown kind: " + kind);
        };
        vmWriter.writePop(segment, index);
    }
}


    private void compileIf() throws IOException {
        writeStartTag("ifStatement");
        writeToken(); advance();  // 'if'
        writeToken(); advance();  // '('
        compileExpression();
        writeToken(); advance();  // ')'
        writeToken(); advance();  // '{'
        compileStatements();
        writeToken(); advance();  // '}'
        if (currentIsKeyword("else")) {
            writeToken(); advance();  // 'else'
            writeToken(); advance();  // '{'
            compileStatements();
            writeToken(); advance();  // '}'
        }
        writeEndTag("ifStatement");
    }

    private void compileWhile() throws IOException {
        writeStartTag("whileStatement");
        writeToken(); advance();  // 'while'
        writeToken(); advance();  // '('
        compileExpression();
        writeToken(); advance();  // ')'
        writeToken(); advance();  // '{'
        compileStatements();
        writeToken(); advance();  // '}'
        writeEndTag("whileStatement");
    }

    private void compileDo() throws IOException {
        writeStartTag("doStatement");
        writeToken(); advance();  // 'do'
        writeToken(); advance();  // identifier|className
        if (symbol(".")) {
            writeToken(); advance();  // '.'
            writeToken(); advance();  // subroutineName
        }
        writeToken(); advance();  // '('
        compileExpressionList();
        writeToken(); advance();  // ')'
        writeToken(); advance();  // ';'
        writeEndTag("doStatement");
    }

    private void compileReturn() throws IOException {
        writeStartTag("returnStatement");
        writeToken(); advance();  // 'return'
        if (!symbol(";")) {
            compileExpression();
        }
        writeToken(); advance();  // ';'
        writeEndTag("returnStatement");
    }

    private void compileExpression() throws IOException {
        writeStartTag("expression");
        compileTerm();
        while (currentIsOperator()) {
            writeToken(); advance();  // op
            compileTerm();           // next term
        }
        writeEndTag("expression");
    }

    private void compileTerm() throws IOException {
        writeStartTag("term");
        JackTokenizer.Token t = current();
        if (t.type.equals("integerConstant") || t.type.equals("stringConstant") ||
           (t.type.equals("keyword") && List.of("true","false","null","this").contains(t.text))) {
            writeToken(); advance();
        } else if (symbol("(")) {
            writeToken(); advance();  // '('
            compileExpression();
            writeToken(); advance();  // ')'
        } else if (symbol("-") || symbol("~")) {
            writeToken(); advance();  // unaryOp
            compileTerm();            // term
        } else if (t.type.equals("identifier")) {
            writeToken(); advance();  // identifier
            if (symbol("[")) {
                writeToken(); advance();  // '['
                compileExpression();
                writeToken(); advance();  // ']'
            } else if (symbol("(") || symbol(".")) {
                if (symbol(".")) {
                    writeToken(); advance();  // '.'
                    writeToken(); advance();  // subroutineName
                }
                writeToken(); advance();  // '('
                compileExpressionList();
                writeToken(); advance();  // ')'
            }
        }
        writeEndTag("term");
    }

    private void compileExpressionList() throws IOException {
        writeStartTag("expressionList");
        if (!symbol(")")) {
            compileExpression();
            while (symbol(",")) {
                writeToken(); advance();  // ','
                compileExpression();
            }
        }
        writeEndTag("expressionList");
    }

    /** Helpers **/
    private JackTokenizer.Token current() { return tokens.get(index); }
    private void advance() { index++; }
    private boolean currentIsKeyword(String kw) { JackTokenizer.Token t = current(); return "keyword".equals(t.type) && kw.equals(t.text); }
    private boolean symbol(String s)      { JackTokenizer.Token t = current(); return "symbol".equals(t.type) && s.equals(t.text); }
    private boolean isType()             { JackTokenizer.Token t = current(); return ("keyword".equals(t.type) && List.of("int","char","boolean").contains(t.text)) || "identifier".equals(t.type); }
    private boolean currentIsOperator()  { JackTokenizer.Token t = current(); return "symbol".equals(t.type) && List.of(
            "+","-","*","/","&","|","<",">","=").contains(t.text);
    }

    private void writeStartTag(String tag) throws IOException { indent(); w.write("<" + tag + ">\n"); indentLevel++; }
    private void writeEndTag(String tag)   throws IOException { indentLevel--; indent(); w.write("</" + tag + ">\n"); }
    private void writeToken() throws IOException {
        JackTokenizer.Token t = current();
        indent();
        w.write("<" + t.type + "> " + escape(t.text) + " </" + t.type + ">\n");
    }
    private void indent() throws IOException {
        for (int i = 0; i < indentLevel; i++) w.write("  ");
    }

    // Escape special XML characters in token text
    private static String escape(String s) {
        // Only escape raw single-character symbols, leave multi-character entities untouched
        if (s.length() == 1) {
            switch (s.charAt(0)) {
                case '<': return "&lt;";
                case '>': return "&gt;";
                case '&': return "&amp;";
            }
        }
        return s;
    }
}

