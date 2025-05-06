import java.io.IOException;
import java.util.List;

public class CompilationEngine {
    private final List<JackTokenizer.Token> tokens;
    private final VMWriter vmWriter;
    private final SymbolTable symbolTable = new SymbolTable();
    private int index = 0;
    private int labelCounter = 0;
    private String className;
    private String subroutineType;

    public CompilationEngine(List<JackTokenizer.Token> tokens, VMWriter vmWriter) {
        this.tokens = tokens;
        this.vmWriter = vmWriter;
    }

    public void compileClass() throws IOException {
        advance(); // 'class'
        className = current().text; advance(); // className
        advance(); // '{'
        while (currentIsKeyword("static") || currentIsKeyword("field")) {
            compileClassVarDec();
        }
        while (currentIsKeyword("constructor") || currentIsKeyword("function") || currentIsKeyword("method")) {
            compileSubroutine();
        }
        advance(); // '}'
    }

    private void compileClassVarDec() {
        String kind = current().text; advance(); // 'static' or 'field'
        String type = current().text; advance(); // type
        String name = current().text; advance(); // varName
        symbolTable.define(name, type, kind);

        while (symbol(",")) {
            advance(); // ','
            name = current().text; advance(); // varName
            symbolTable.define(name, type, kind);
        }

        advance(); // ';'
    }

    private void compileSubroutine() throws IOException {
        symbolTable.startSubroutine();
        subroutineType = current().text; advance(); // constructor/function/method
        advance(); // return type
        String subroutineName = current().text; advance(); // name
        advance(); // '('
        compileParameterList();
        advance(); // ')'
        advance(); // '{'
        int localCount = 0;
        while (currentIsKeyword("var")) {
            localCount += defineVarDec();
        }

        vmWriter.writeFunction(className + "." + subroutineName, localCount);

        if (subroutineType.equals("constructor")) {
            int nFields = symbolTable.varCount("field");
            vmWriter.writePush("constant", nFields);
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop("pointer", 0);
        }

        if (subroutineType.equals("method")) {
            vmWriter.writePush("argument", 0);
            vmWriter.writePop("pointer", 0);
        }

        compileStatements();
        advance(); // '}'
    }

    private void skipParameterList() {
        while (!symbol(")")) advance();
    }

    private int defineVarDec() {
        int count = 0;
        advance(); // 'var'
        String type = current().text; advance(); // type
        String name = current().text; advance(); // varName
        symbolTable.define(name, type, "var");
        count++;
        while (symbol(",")) {
            advance(); // ','
            name = current().text; advance(); // varName
            symbolTable.define(name, type, "var");
            count++;
        }
        advance(); // ';'
        return count;
    }
    private void compileParameterList() throws IOException {
    if (current().type.equals("identifier") || currentIsKeyword("int") || currentIsKeyword("char") || currentIsKeyword("boolean")) {
        String type = current().text; advance(); // type
        String name = current().text; advance(); // name
        symbolTable.define(name, type, "arg");

        while (symbol(",")) {
            advance(); // ','
            type = current().text; advance(); // type
            name = current().text; advance(); // name
            symbolTable.define(name, type, "arg");
        }
    }
}

   private void compileStatements() throws IOException {
    while (true) {
        System.out.println(">> STATEMENT: " + current().text);
        if (currentIsKeyword("let")) compileLet();
        else if (currentIsKeyword("do")) compileDo();
        else if (currentIsKeyword("return")) compileReturn();
        else if (currentIsKeyword("if")) compileIf();
        else if (currentIsKeyword("while")) compileWhile();
        else break;
    }
}

    private void compileLet() throws IOException {
    advance(); // 'let'
    String varName = current().text; advance(); // varName

    boolean isArray = false;
    if (symbol("[")) {
        isArray = true;
        advance(); // '['
        compileExpression(); // push index
        advance(); // ']'

        // Push base address of the array
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
        vmWriter.writeArithmetic("add"); // arr + index
    }

    advance(); // '='
    compileExpression(); // value
    advance(); // ';'

    if (isArray) {
        // Save value to temp
        vmWriter.writePop("temp", 0);      // pop value
        vmWriter.writePop("pointer", 1);   // set THAT = arr + index
        vmWriter.writePush("temp", 0);     // restore value
        vmWriter.writePop("that", 0);      // arr[i] = value
    } else {
        String kind = symbolTable.kindOf(varName);
        int index = symbolTable.indexOf(varName);
        if (kind.equals("none") || index == -1) {
            throw new RuntimeException("LET: Unknown variable '" + varName + "'");
        }

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

    private void compileDo() throws IOException {
        advance(); // 'do'
        String name = current().text; advance(); // identifier
        if (symbol(".")) {
            advance(); // '.'
            String subroutineName = current().text; advance(); // subroutine
            name = name + "." + subroutineName;
        } else {
            name = className + "." + name;
        }

        advance(); // '('
        int argCount = compileExpressionList();
        advance(); // ')'
        advance(); // ';'
        vmWriter.writeCall(name, argCount);
        vmWriter.writePop("temp", 0);
    }

    private void compileReturn() throws IOException {
        advance(); // 'return'
        if (!symbol(";")) {
            compileExpression();
        } else {
            vmWriter.writePush("constant", 0); // void return = push 0
        }
        advance(); // ';'
        vmWriter.writeReturn();
    }

    private void compileIf() throws IOException {
        advance(); // 'if'
        String labelElse = generateLabel("IF_ELSE");
        String labelEnd = generateLabel("IF_END");

        advance(); // '('
        compileExpression();
        advance(); // ')'
        vmWriter.writeArithmetic("not");
        vmWriter.writeIf(labelElse);

        advance(); // '{'
        compileStatements();
        advance(); // '}'
        vmWriter.writeGoto(labelEnd);
        vmWriter.writeLabel(labelElse);

        if (currentIsKeyword("else")) {
            advance(); // 'else'
            advance(); // '{'
            compileStatements();
            advance(); // '}'
        }

        vmWriter.writeLabel(labelEnd);
    }

    private void compileWhile() throws IOException {
        advance(); // 'while'
        String labelExp = generateLabel("WHILE_EXP");
        String labelEnd = generateLabel("WHILE_END");

        vmWriter.writeLabel(labelExp);
        advance(); // '('
        compileExpression();
        advance(); // ')'
        vmWriter.writeArithmetic("not");
        vmWriter.writeIf(labelEnd);

        advance(); // '{'
        compileStatements();
        advance(); // '}'

        vmWriter.writeGoto(labelExp);
        vmWriter.writeLabel(labelEnd);
    }

    private int compileExpressionList() throws IOException {
        int count = 0;
        if (!symbol(")")) {
            compileExpression();
            count++;
            while (symbol(",")) {
                advance(); // ','
                compileExpression();
                count++;
            }
        }
        return count;
    }

    private boolean isOperator() {
        return symbol("+") || symbol("-") || symbol("*") || symbol("/") ||
               symbol("&") || symbol("|") || symbol("<") || symbol(">") || symbol("=");
    }

    private void compileExpression() throws IOException {
    compileTerm();
    while (isOperator()) {
        String op = current().text;
        advance();
        compileTerm();

        switch (op) {
            case "+" -> vmWriter.writeArithmetic("add");
            case "-" -> vmWriter.writeArithmetic("sub");
            case "*" -> vmWriter.writeCall("Math.multiply", 2);
            case "/" -> vmWriter.writeCall("Math.divide", 2);
            case "&" -> vmWriter.writeArithmetic("and");
            case "|" -> vmWriter.writeArithmetic("or");
            case "<" -> vmWriter.writeArithmetic("lt");
            case ">" -> vmWriter.writeArithmetic("gt");
            case "=" -> vmWriter.writeArithmetic("eq");
        }
    }
}

private void compileTerm() throws IOException {
    if (current().type.equals("integerConstant")) {
        vmWriter.writePush("constant", Integer.parseInt(current().text));
        advance();

    } else if (current().type.equals("stringConstant")) {
        String str = current().text;
        advance();
        vmWriter.writePush("constant", str.length());
        vmWriter.writeCall("String.new", 1);
        for (int i = 0; i < str.length(); i++) {
            vmWriter.writePush("constant", str.charAt(i));
            vmWriter.writeCall("String.appendChar", 2);
        }

    } else if (symbol("(")) {
        // ( expression )
        advance();               // consume '('
        compileExpression();
        advance();               // consume ')'

    } else if (symbol("-")) {
        // unary minus
        advance();               // consume '-'
        compileTerm();           // compile operand
        vmWriter.writeArithmetic("neg");

    } else if (symbol("~")) {
        // bitwise not
        advance();               // consume '~'
        compileTerm();           // compile operand
        vmWriter.writeArithmetic("not");

    } else if (current().type.equals("identifier")) {
        String name = current().text;
        advance();               // consume identifier

        if (symbol("[")) {
            // array access: name[expr]
            advance();           // '['
            compileExpression();
            advance();           // ']'

            String kind = symbolTable.kindOf(name);
            int index  = symbolTable.indexOf(name);
            String segment = switch (kind) {
                case "var"    -> "local";
                case "arg"    -> "argument";
                case "static" -> "static";
                case "field"  -> "this";
                default       -> throw new RuntimeException("Unknown kind: " + kind);
            };

            vmWriter.writePush(segment, index);
            vmWriter.writeArithmetic("add");
            vmWriter.writePop("pointer", 1);
            vmWriter.writePush("that", 0);

        } else if (symbol("(")) {
            // subroutine call in *this* class: name(exprList)
            advance();           // consume '('
            int argCount = compileExpressionList();
            advance();           // consume ')'
            vmWriter.writeCall(className + "." + name, argCount);

        } else if (symbol(".")) {
            // qualified call: ClassName|varName.subName(exprList)
            advance();           // consume '.'
            String sub = current().text;
            advance();           // consume subroutine name
            advance();           // consume '('
            int argCount = compileExpressionList();
            advance();           // consume ')'
            vmWriter.writeCall(name + "." + sub, argCount);

        } else {
            // simple var access
            String kind = symbolTable.kindOf(name);
            int index  = symbolTable.indexOf(name);
            if (!kind.equals("none")) {
                String segment = switch (kind) {
                    case "var"    -> "local";
                    case "arg"    -> "argument";
                    case "static" -> "static";
                    case "field"  -> "this";
                    default       -> throw new RuntimeException("Unknown kind: " + kind);
                };
                vmWriter.writePush(segment, index);
            }
        }

    } else {
        // fallback: skip unknown token to avoid infinite loop
        advance();
    }
}



    private String generateLabel(String base) {
        return className + "$" + base + (labelCounter++);
    }

    /** Token Helpers **/
    private JackTokenizer.Token current() { return tokens.get(index); }
    private void advance() { index++; }
    private boolean currentIsKeyword(String kw) {
        JackTokenizer.Token t = current();
        return "keyword".equals(t.type) && kw.equals(t.text);
    }
    private boolean symbol(String s) {
        JackTokenizer.Token t = current();
        return "symbol".equals(t.type) && s.equals(t.text);
    }
}