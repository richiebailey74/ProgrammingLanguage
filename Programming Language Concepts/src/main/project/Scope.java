package plc.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

//scope functionality allows for hierarchical access of variables and functions depending on location of declaration
public final class Scope {

    private final Scope parent;
    private final Map<String, Environment.Variable> variables = new HashMap<>();
    private final Map<String, Environment.Function> functions = new HashMap<>();

    //scope constructor sets scope
    public Scope(Scope parent) {
        this.parent = parent;
    }

    //returns the direct parent Scope of the invoking Scope object
    public Scope getParent() {
        return parent;
    }

    //defines a variables within the invoking scope (all extending/lower scopes will have access)
    public void defineVariable(String name, boolean mutable, Environment.PlcObject value) {
        defineVariable(name, name, Environment.Type.ANY, mutable, value);
    }

    //defines a variables within the invoking scope (all parent/higher scopes will have access)
    public Environment.Variable defineVariable(String name, String jvmName, Environment.Type type, boolean mutable, Environment.PlcObject value) {
        if (variables.containsKey(name)) {
            throw new RuntimeException("The variable " + name + " is already defined in this scope.");
        } else {
            Environment.Variable variable = new Environment.Variable(name, jvmName, type, mutable, value);
            variables.put(variable.getName(), variable);
            return variables.get(name);
        }
    }

    //looks up to see if a variable exists within the current scope or parent scopes
    public Environment.Variable lookupVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        } else if (parent != null) {
            return parent.lookupVariable(name);
        } else {
            throw new RuntimeException("The variable " + name + " is not defined in this scope.");
        }
    }

    //defines a function within the invoking scope (all parent/higher scopes will have access)
    public void defineFunction(String name, int arity, Function<List<Environment.PlcObject>, Environment.PlcObject> function) {
        List<Environment.Type> parameterTypes = new ArrayList<>();
        for (int i = 0; i < arity; i++) {
            parameterTypes.add(Environment.Type.ANY);
        }
        defineFunction(name, name, parameterTypes, Environment.Type.ANY, function);
    }

    //defines a function within the invoking scope (all parent/higher scopes will have access)
    public Environment.Function defineFunction(String name, String jvmName, List<Environment.Type> parameterTypes, Environment.Type returnType, java.util.function.Function<List<Environment.PlcObject>, Environment.PlcObject> function) {
        if (functions.containsKey(name + "/" + parameterTypes.size())) {
            throw new RuntimeException("The function " + name + "/" + parameterTypes.size() + " is already defined in this scope.");
        } else {
            Environment.Function func = new Environment.Function(name, jvmName, parameterTypes, returnType, function);
            functions.put(func.getName() + "/" + func.getParameterTypes().size(), func);
            return func;
        }
    }

    //looks up a function to see if it exists within the current scope or parent scopes
    public Environment.Function lookupFunction(String name, int arity) {
        if (functions.containsKey(name + "/" + arity)) {
            return functions.get(name + "/" + arity);
        } else if (parent != null) {
            return parent.lookupFunction(name, arity);
        } else {
            throw new RuntimeException("The function " + name + "/" + arity + " is not defined in this scope.");
        }
    }

    @Override
    public String toString() {
        return "Scope{" +
                "parent=" + parent +
                ", variables=" + variables.keySet() +
                ", functions=" + functions.keySet() +
                '}';
    }

}
