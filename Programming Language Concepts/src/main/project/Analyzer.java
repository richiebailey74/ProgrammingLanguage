package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.*;

    //*** the analyzer also utilizes a visitor pattern that implements a double dispatch functionality
    //analyzer essentially validates the data types present and makes sure they work with all of the operations for different aspects of the language

    //requireAssignable is a validation function that makes sure that a value to be assigned to a receiver both have compatible data types
    //-and if it does not, then it will throw an error and break the program

public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    //returns the current scope
    public Scope getScope() {
        return scope;
    }

    //visits source and validates the return type of main
    @Override
    public Void visit(Ast.Source ast) {

        for(Ast.Global glob : ast.getGlobals()) {
            visit(glob);
        }

        for(Ast.Function func : ast.getFunctions()) {
            visit(func);
        }

        Environment.Function mainFunc = scope.lookupFunction("main", 0);
        requireAssignable(Environment.Type.INTEGER, mainFunc.getReturnType());

        return null;
    }

    //visits the value inside of global's optional if it is there
    //defines the variable in the current scope and then sets the variables within the ast variable
    @Override
    public Void visit(Ast.Global ast) {

        if(ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
        }
        scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), ast.getMutable(),  Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;
    }

    //validates the expected return type to the type actually being returned (works with the visit return method)
    //defines function in the scope and sets it within the ast variable
    //defines the parameters of the function in the functions scope and then visit all statements within the function
    @Override
    public Void visit(Ast.Function ast) {

        String name = ast.getName();
        String jvmName = name;

        List<Environment.Type> parameterTypes = new ArrayList<Environment.Type>();
        for(int i = 0; i < ast.getParameterTypeNames().size(); i++) {
            parameterTypes.add(Environment.getType(ast.getParameterTypeNames().get(i)));
        }

        Environment.Type returnType;
        if(ast.getReturnTypeName().isPresent()) {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        } else {
            returnType = Environment.Type.NIL;
        }

        java.util.function.Function<List<Environment.PlcObject>, Environment.PlcObject> function = args -> Environment.NIL;

        scope.defineFunction(name, jvmName, parameterTypes, returnType, function);
        ast.setFunction(scope.lookupFunction(name, ast.getParameters().size()));

        scope = new Scope(scope);

        //functionality depends on if the returnTypeName is there or not (since it is optional)
        if(ast.getReturnTypeName().isPresent()) {
            scope.defineVariable("returnVar", "returnVar", Environment.getType(ast.getReturnTypeName().get()), true, Environment.NIL);
        } else {
            scope.defineVariable("returnVar", "returnVar", Environment.Type.NIL, true, Environment.NIL);

        }

        //Present to define all parameters as variables for this scope !!!!!!
        for(int i = 0; i < ast.getParameters().size(); i++) {
            scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), parameterTypes.get(i), true, Environment.NIL);
        }

        for(Ast.Statement state : ast.getStatements()) {
            visit(state);
        }

        scope = scope.getParent();

        return null;
    }

    //visits the expression in the ast variable, and it must be of class Ast.Expression.Function or else it is invalid and should throw an error
    @Override
    public Void visit(Ast.Statement.Expression ast) {

        visit(ast.getExpression());

        if(!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Expression not of type Ast.Expression.Function!");
        }

        return null;
    }

    //if the type is present then the default type is set to that type
    //if the value is not present, then the default type is set to the value's type if it's type is null (otherwise the type was present)
    //validates that the default type (could be value type or receiver type) is compatible to the value type (assigned type)
    //visits value if there
    //defines variable in scope and then sets variable in the ast object
    @Override
    public Void visit(Ast.Statement.Declaration ast) {//is mutable always true???????????????????????????

        Environment.Type defaultType = null;

        if(!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("Needs a type or a value for a declaration!");
        }

        if(ast.getTypeName().isPresent()) {
            defaultType = Environment.getType(ast.getTypeName().get());
        }
        if(ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if(defaultType == null) {
                defaultType = ast.getValue().get().getType();
            }
            //this is for when defaultType is not null, so that the typeName matches value type name
            requireAssignable(defaultType, ast.getValue().get().getType());
        }

        scope.defineVariable(ast.getName(), ast.getName(), defaultType, true, Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;

    }

    //if the receiver is not of the type Ast.Expression.Access then it is invalid since the receiver must be of an access object
    //visits the receiver and value, and validates their types with requireAssignable
    @Override
    public Void visit(Ast.Statement.Assignment ast) {

        if(!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Receiver is not of expression access class type!");
        }

        visit(ast.getReceiver());
        visit(ast.getValue());

        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }

    //visits the condition and makes sure it is a boolean
    //must have at least one then statement or else error since this language does not allow no statements within an if statement
    //visits all then statements within a new scope and all else statements in a new scope
    @Override
    public Void visit(Ast.Statement.If ast) {

        visit(ast.getCondition());

        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        if(ast.getThenStatements().size() == 0) {
            throw new RuntimeException("ThenStatements list is empty!");
        }

        scope = new Scope(scope);
        for(Ast.Statement statement : ast.getThenStatements()) {
            visit(statement);
        }
        scope = scope.getParent();

        scope = new Scope(scope);
        for(Ast.Statement statement : ast.getElseStatements()) {
            visit(statement);
        }
        scope = scope.getParent();

        return null;

    }

    //must have cases besides just the default or else throw an error
    //visits the condition then loops through all cases and validates that the case value must be of the same type of the condition or else it is invalid
    //visits all of the cases inside of this loop
    @Override
    public Void visit(Ast.Statement.Switch ast) {

        if(ast.getCases().get(ast.getCases().size() - 1).getValue().isPresent()) {
            throw new RuntimeException("Default case has a value, it shouldn't!");
        }

        visit(ast.getCondition());

        for(Ast.Statement.Case cases : ast.getCases()) {
            visit(cases);
            if(cases.getValue().isPresent()) {
                if(!(cases.getValue().get().getType().equals(ast.getCondition().getType()))) {
                    throw new RuntimeException("Condition doesn't match all case types!");

                }
            }
        }


        return null;
    }

    //within a new scope, loop through all statements within the case and visit them, and if the value is present then visit it (within the loop)
    @Override
    public Void visit(Ast.Statement.Case ast) {

        scope = new Scope(scope);
        for(Ast.Statement state : ast.getStatements()) {
            visit(state);
            if(ast.getValue().isPresent()) {
                visit(ast.getValue().get());
            }
        }
        scope = scope.getParent();

        return null;
    }

    //visits the condition and makes sure it is of type boolean
    //within a new scope, loop through all statements within the loop and visit them
    @Override
    public Void visit(Ast.Statement.While ast) {

        visit(ast.getCondition());

        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        scope = new Scope(scope);
        for(Ast.Statement statement : ast.getStatements()) {

            visit(statement);

        }
        scope = scope.getParent();

        return null;

    }

    //works together with Ast.Function and loops up the return variable defined in function and validates the return type
    @Override
    public Void visit(Ast.Statement.Return ast) { //isn't done and can't test until statement function is completed

        visit(ast.getValue());

        Environment.Variable returnVar = scope.lookupVariable("returnVar"); //this variable name must be defined in statement function
        requireAssignable(returnVar.getType(), ast.getValue().getType());

        return null;

    }

    //sets the type of the ast variable to be the type of the literal it contains
    //makes sure the integer and decimal are in range
    @Override
    public Void visit(Ast.Expression.Literal ast) {

        Object lit = ast.getLiteral();

        if(lit == null) {
            ast.setType(Environment.Type.NIL);

        } else if(lit instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);

        } else if(lit instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);

        } else if(lit instanceof String) {
            ast.setType(Environment.Type.STRING);

        } else if(lit instanceof BigInteger) {

            Integer tempIntMax = Integer.MAX_VALUE;
            String maxString = tempIntMax.toString();
            BigInteger maxInt = new BigInteger(maxString);

            Integer tempIntMin = Integer.MIN_VALUE;
            String minString = tempIntMin.toString();
            BigInteger minInt = new BigInteger(minString);

            if(((BigInteger) lit).compareTo(maxInt) > 0 || ((BigInteger) lit).compareTo(minInt) < 0) {
                throw new RuntimeException("BigInteger out of range for type int!");
            }

            ast.setType(Environment.Type.INTEGER);

        } else if(lit instanceof BigDecimal) {

            BigDecimal maxDouble = new BigDecimal(Double.MAX_VALUE);
            BigDecimal minDouble = new BigDecimal(Double.MIN_VALUE);

            if(((BigDecimal) lit).compareTo(maxDouble) > 0 || ((BigDecimal) lit).compareTo(minDouble) < 0) {
                throw new RuntimeException("BigDecimal out of range for type double!");
            }

            ast.setType(Environment.Type.DECIMAL);

        } else {
            throw new RuntimeException("Literal type is invalid!");
        }

        return null;
    }

    //makes sure the expression inside of the group is of type binary, or else it should throw an error
    //visit the expression and then set the type of the ast variable to be that type
    @Override
    public Void visit(Ast.Expression.Group ast) {

        if(!(ast.getExpression().getClass().equals(Ast.Expression.Binary.class))) {
            throw new RuntimeException("Contained expression must be of type Binary!");

        }

        visit(ast.getExpression());

        ast.setType(ast.getExpression().getType());

        return null;

    }

    //visit both the left and right side of the binary (will recursive decent until fully simplified) and get the operator
    //depending on the operator, will validate the variable types and then set the ast variable to that type, according to the grammar
    @Override
    public Void visit(Ast.Expression.Binary ast) {


        String operator = ast.getOperator();
        visit(ast.getLeft());
        visit(ast.getRight());


        if(operator.equals("&&") || operator.equals("||")) {
            requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
            requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        } else if(operator.equals(">") || operator.equals("<") || operator.equals("==") || operator.equals("!=")) {

            requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
            requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());

            if(!(ast.getLeft().getType().equals(ast.getRight().getType()))) {
                throw new RuntimeException("Mismatching operand types!");
            }

            ast.setType(Environment.Type.BOOLEAN);

        } else if(operator.equals("+")) {
            if(ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)) {
                ast.setType(Environment.Type.STRING);
            } else if(ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                if(!(ast.getLeft().getType().equals(ast.getRight().getType()))) {
                    throw new RuntimeException("Types of left and right operands are not the same!");
                }
                ast.setType(ast.getLeft().getType());
            } else {
                throw new RuntimeException("Not compatible types to add");
            }
        } else if(operator.equals("-") || operator.equals("*") || operator.equals("/")) {
            if(ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {

                if(ast.getLeft().getType().equals(ast.getRight().getType())) {

                    ast.setType(ast.getLeft().getType());

                } else {
                    throw new RuntimeException("Left and right operands must have matching types!");
                }

            } else {
                throw new RuntimeException("Type must be of integer or decimal!");
            }
        } else if(operator.equals("^")) {
            if(!(ast.getRight().getType().equals(Environment.Type.INTEGER))) {
                throw new RuntimeException("Right operand must be of type integer!");
            }

            if(ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                ast.setType(ast.getLeft().getType());
            } else {
                throw new RuntimeException("Left operand must be an integer or a decimal!");
            }

        } else {
            throw new RuntimeException("Invalid operator!");
        }

        return null;

    }

    //if the offset is present then visit it and validate that the offset is of type integer
    //then set the ast variable to the value of the access receiver (look up variable in scope)
    @Override
    public Void visit(Ast.Expression.Access ast) {

        if(ast.getOffset().isPresent()) {
            visit(ast.getOffset().get());
            if(!(ast.getOffset().get().getType().equals(Environment.Type.INTEGER))) {
                throw new RuntimeException("Offset is not of type Integer (from Environment.Type)");

            }
        }

        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;
    }

    //sets the function in the scope attached to the ast as well (double nested functionality)
    //validate that the parameter arity of the parameter lists are the same size
    @Override
    public Void visit(Ast.Expression.Function ast) {


        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));

        if(ast.getFunction().getParameterTypes().size() != ast.getArguments().size()) {
            throw new RuntimeException("Parameter sizes are not consistent!");
        }

        for(int i = 0; i < ast.getArguments().size(); i++) {

            visit(ast.getArguments().get(i));
            requireAssignable(ast.getFunction().getParameterTypes().get(i), ast.getArguments().get(i).getType());

        }

        return null;

    }

    //visits all values in the list and require assignable's them with all of them with the first type to make sure they are all compatible with each other
    @Override
    public Void visit(Ast.Expression.PlcList ast) {


        for(int i = 0; i < ast.getValues().size(); i++) {
            visit(ast.getValues().get(i));
            requireAssignable(ast.getValues().get(0).getType(), ast.getValues().get(i).getType());
        }

        ast.setType(ast.getValues().get(0).getType());

        return null;
    }

    //validates that variable types are compatible
    //essentially if they are not the same type, and target not of type ANY, and if target is COMPARABLE and the type is not integer, decimal, character, or string, then-
    //-an error should be thrown because by definition from the language's grammar, that means the types are incompatible
    public static void requireAssignable(Environment.Type target, Environment.Type type) {

        if(!target.equals(type)) {

            if (target.equals(Environment.Type.ANY)) {
                return;
            }

            if (target.equals(Environment.Type.COMPARABLE)) {

                if (type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL) || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING)) {
                    return;
                }

            }
            throw new RuntimeException("Target type does not match assignment type!");
        }
    }
}
