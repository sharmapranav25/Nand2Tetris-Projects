
// File: SymbolTable.java
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, SymbolInfo> classScope;
    private Map<String, SymbolInfo> subroutineScope;
    private int staticCount, fieldCount, argCount, varCount;

    public SymbolTable() {
        classScope = new HashMap<>();
        subroutineScope = new HashMap<>();
        staticCount = fieldCount = argCount = varCount = 0;
    }

    public void startSubroutine() {
        subroutineScope.clear();
        argCount = varCount = 0;
    }

    public void define(String name, String type, String kind) {
        int index;
        switch (kind) {
            case "static":
                index = staticCount++;
                classScope.put(name, new SymbolInfo(type, kind, index));
                break;
            case "field":
                index = fieldCount++;
                classScope.put(name, new SymbolInfo(type, kind, index));
                break;
            case "arg":
                index = argCount++;
                subroutineScope.put(name, new SymbolInfo(type, kind, index));
                break;
            case "var":
                index = varCount++;
                subroutineScope.put(name, new SymbolInfo(type, kind, index));
                break;
        }
    }

    public int varCount(String kind) {
        switch (kind) {
            case "static": return staticCount;
            case "field": return fieldCount;
            case "arg": return argCount;
            case "var": return varCount;
            default: return 0;
        }
    }

    public String kindOf(String name) {
        if (subroutineScope.containsKey(name)) {
            return subroutineScope.get(name).getKind();
        } else if (classScope.containsKey(name)) {
            return classScope.get(name).getKind();
        } else {
            return "none";
        }
    }

    public String typeOf(String name) {
        if (subroutineScope.containsKey(name)) {
            return subroutineScope.get(name).getType();
        } else if (classScope.containsKey(name)) {
            return classScope.get(name).getType();
        } else {
            return null;
        }
    }

    public int indexOf(String name) {
        if (subroutineScope.containsKey(name)) {
            return subroutineScope.get(name).getIndex();
        } else if (classScope.containsKey(name)) {
            return classScope.get(name).getIndex();
        } else {
            return -1;
        }
    }
}
