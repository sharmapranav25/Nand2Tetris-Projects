import java.nio.file.*;
import java.io.*;
import java.util.*;

public class JackTokenizer {
    private final String inputPath;
    private String src;
    private int pos = 0;

    private static final Set<Character> SYMBOLS = Set.of(
        '{','}','(',')','[',']','.',',',';','+','-','*','/','&','|','<','>','=','~'
    );

    private static final Set<String> KEYWORDS = Set.of(
        "class","constructor","function","method","field","static","var",
        "int","char","boolean","void","true","false","null","this",
        "let","do","if","else","while","return"
    );

    public JackTokenizer(String inputPath) {
        this.inputPath = inputPath;
    }

    public List<Token> getTokens() throws IOException {
        src = Files.readString(Path.of(inputPath));
        src = src.replaceAll("/\\*\\*?[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");  // block comments
        src = src.replaceAll("//.*", "");  // single line comments

        List<Token> tokens = new ArrayList<>();
        pos = 0;
        while (pos < src.length()) {
            skipWhitespace();
            if (pos >= src.length()) break;

            char c = src.charAt(pos);

            if (SYMBOLS.contains(c)) {
                tokens.add(new Token("symbol", String.valueOf(c)));
                pos++;
            } else if (c == '"') {
                pos++;
                int start = pos;
                while (pos < src.length() && src.charAt(pos) != '"') pos++;
                String strVal = src.substring(start, pos);
                tokens.add(new Token("stringConstant", strVal));
                pos++; // skip closing "
            } else if (Character.isDigit(c)) {
                int start = pos;
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
                String num = src.substring(start, pos);
                tokens.add(new Token("integerConstant", num));
            } else {
                int start = pos;
                while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) pos++;
                String word = src.substring(start, pos);
                if (KEYWORDS.contains(word)) {
                    tokens.add(new Token("keyword", word));
                } else {
                    tokens.add(new Token("identifier", word));
                }
            }
        }
        return tokens;
    }

    public void writeTokens(String outputPath) throws IOException {
        List<Token> tokens = getTokens();
        try (BufferedWriter w = Files.newBufferedWriter(Path.of(outputPath))) {
            w.write("<tokens>\n");
            for (Token tk : tokens) {
                w.write("  <" + tk.type + "> " + escape(tk.text) + " </" + tk.type + ">\n");
            }
            w.write("</tokens>\n");
        }
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static class Token {
        public final String type;
        public final String text;
        public Token(String type, String text) {
            this.type = type;
            this.text = text;
        }
    }
}
