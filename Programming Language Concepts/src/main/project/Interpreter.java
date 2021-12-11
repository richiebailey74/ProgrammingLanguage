package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


//*** the interpreter essentially takes the functionality that is demanded by the syntax of the program and executes operations
//-- it essentially "interprets" the code and does what it wants and does so through a visitor patter
//Java does not support double dispatch, meaning that the methods invoked by objects come from the runtime type, however,
//-when objects are passed as parameters, the java compiler defaults to invoking the compile time type, which is a problem when a program
//-is highly sensitive and dependent on the runtime type since there is such a large inheritance hierarchy
//SO, the visit pattern is implemented so that object types passed are of runtime types, which allows for an implementation of double dispatch

//we are going to need this visitor pattern and double dispatch pattern for the interpreter, analyzer, and generator of this programming language


//the visitor class is an abstract class that is implemented by the Interpreter class to allow for the implementation of the visitor pattern (double dispatch)
public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    //this scope variable is essentially a tracker that essentially allows for different variable definitions across scopes and allows for scope functionality
    private Scope scope = new Scope(null);

    //interpreter function that takes the outer most scope as the parameter and defines some automatically built in functions - print and logarithm
    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });

        scope.defineFunction("logarithm", 1, args -> {

            if(!(args.get(0).getValue() instanceof BigDecimal)) {
                throw new RuntimeException("Expected type of BigDecimal received" +
                        args.get(0).getValue().getClass().getName() + ".");
            }

            BigDecimal bd1 = (BigDecimal) args.get(0).getValue();
            BigDecimal bd2 = requireType(
                    BigDecimal.class,
                    Environment.create(args.get(0).getValue())
            );

            BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));
            return Environment.create(result);

        });

    }

    //returns the scope object
    public Scope getScope() {
        return scope;
    }

    //visits the source and evaluates/interprets everything according the grammar by visiting subsequent types
    //looks up the main function to make sure it is there otherwise it violates the grammar and will throw an error
    @Override
    public Environment.PlcObject visit(Ast.Source ast) {


        for(Ast.Global globs : ast.getGlobals()) {
            visit(globs);
        }

        for(Ast.Function funcs : ast.getFunctions()) {
            visit(funcs);
        }

        return scope.lookupFunction("main", 0).invoke(new ArrayList<>());

    }

    //visits a global and evaluates/interprets everything according the grammar by visiting subsequent types
    //must define each global in the outer most / current scope
    @Override
    public Environment.PlcObject visit(Ast.Global ast) {

        boolean bool = ast.getValue().isPresent();

        if(bool) {

            scope.defineVariable(ast.getName(), ast.getMutable(), visit(ast.getValue().get()));

        } else {

            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);

        }

        return Environment.NIL;

    }

    //visits a function and evaluates/interprets everything according the grammar by visiting subsequent types
    //defines functions scope and defines the function within the scope
    //must define all parameters in the scope and when done get the parent scope (outer most scope typically)
    @Override
    public Environment.PlcObject visit(Ast.Function ast) {

        Scope newScope = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {

            Scope scopeCalled = scope;
            scope = new Scope(newScope);

            for(int i = 0; i < args.size(); i++) {
                scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
            }
                try {
                    for(Ast.Statement currentState : ast.getStatements()) {
                        visit(currentState);
                    }

                } catch (Return e) {
                    return e.value;

                } finally {
                    scope = scopeCalled;
                }
            return Environment.NIL;
        });

        return Environment.NIL;

    }

    //visits a statement expression and evaluates/interprets the contained expression according to the grammar by visiting subsequent types
    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());

        return Environment.NIL;

    }

    //visits a statement declaration and evaluates/interprets everything according the grammar by visiting subsequent types
    //since it is actually declaring, must define variable for declaration within scope
    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Optional optional = ast.getValue();
        boolean present = optional.isPresent();

        if(present) {
            String Name = ast.getName();
            Ast.Expression expr = (Ast.Expression) optional.get();

            scope.defineVariable(Name, true, visit(expr));

        } else { //needs to be finished
            String Name = ast.getName();
            scope.defineVariable(Name, true, Environment.NIL);

        }
        return Environment.NIL;

    }

    //visits a statement assignment and evaluates/interprets everything according the grammar by visiting subsequent types
    //checks in scope to make sure that the receiver is not immutable, if it is immutable then it cannot be modified
    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {//method could be incorrect
        if(!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Receiver is not assignable, not of type Ast.Expression.Access");
        }

        boolean offsetPresent = ((Ast.Expression.Access)ast.getReceiver()).getOffset().isPresent();
        if (offsetPresent) {
            //get an Environment.Variable, getValue() then gets a PlcObject, then getValue() gets an Object
            //Object can be type casted to a List<Object> since List<Object> extends Object
            List<Object> toAssignTo = (List<Object>) scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).getValue().getValue();
            int offsetInt = ((BigInteger) visit(((Ast.Expression.Access)ast.getReceiver()).getOffset().get()).getValue()).intValue();

            if(offsetInt < 0 || toAssignTo.size() - 1 < offsetInt) {
                throw new RuntimeException("Offset out of bounds");
            }
            if(!scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).getMutable()) {
                throw new RuntimeException("Receiver is immutable, cannot assign");
            }

            toAssignTo.set(offsetInt, visit(ast.getValue()).getValue());
            Environment.Variable recevierNameVar = scope.lookupVariable(((Ast.Expression.Access)ast.getReceiver()).getName());
            recevierNameVar.setValue(Environment.create(toAssignTo));

        } else {
            if(!scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).getMutable()) {
                throw new RuntimeException("Receiver is immutable, cannot assign");
            }

            Environment.Variable recevierNameVar = scope.lookupVariable(((Ast.Expression.Access)ast.getReceiver()).getName());
            recevierNameVar.setValue(visit(ast.getValue()));
        }

        return Environment.NIL;
    }

    //visits an if statement and evaluates/interprets everything according the grammar by visiting subsequent types
    //needs new scopes for all statements within then statements and else statements because they contain their own/separate functionality or at least can
    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {

        //will blow up if non boolean
        boolean checkStatements = requireType(Boolean.class, visit(ast.getCondition()));

        if(checkStatements) {
            try {
                scope = new Scope(scope);
                for(Ast.Statement thenStates : ast.getThenStatements()) {
                    visit(thenStates);
                }

            } finally {
                scope = scope.getParent();
            }

        } else { //if the if statement evaluates to false (blows up if non-boolean)
            try {
                scope = new Scope(scope);
                for(Ast.Statement elseStates : ast.getElseStatements()) {
                    visit(elseStates);
                }

            } finally {
                scope = scope.getParent();
            }

        }

        return Environment.NIL;

    }

    //visits a switch statement and evaluates/interprets everything according the grammar by visiting subsequent types
    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        Ast.Statement.Case defaultCase = ast.getCases().get(ast.getCases().size() - 1);

        for(Ast.Statement.Case cases : ast.getCases()) {

            boolean isCaseVal = cases.getValue().isPresent();

            if(isCaseVal) {
                Object caseVal = visit(cases.getValue().get()).getValue();
                Object theCondition = visit(ast.getCondition()).getValue();

                if(caseVal.equals(theCondition)) {

                    defaultCase = cases;
                    break;
                }

            }

        }

        //need a new scope for each case call since it contains its own functionality
        try {
            scope = new Scope(scope);
            visit(defaultCase);

        } finally {
            scope = scope.getParent();
        }

        return Environment.NIL;

    }

    //visits a case statement and evaluates/interprets everything according the grammar by visiting subsequent types
    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {

        for(Ast.Statement stmnts : ast.getStatements()) {

            visit(stmnts);

        }

        return Environment.NIL;
    }

    @Override
    //visits a while loop statement and evaluates/interprets everything according the grammar by visiting subsequent types
    //must create a new scope for each visited statement within the while loop that interprets the while loop
    public Environment.PlcObject visit(Ast.Statement.While ast) {

        boolean checkStatements = requireType(Boolean.class, visit(ast.getCondition()));

        while(checkStatements) {

            try {
                scope = new Scope(scope);

                for( Ast.Statement stmt : ast.getStatements() ) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();

            }
            checkStatements = requireType(Boolean.class, visit(ast.getCondition()));
        }

        return Environment.NIL;
    }

    //visits return statement and throws a Return with the value to be returned in the parameter (functionality extends that of runtime exception at end of file)
    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {

        throw new Return(visit(ast.getValue()));

    }

    //visits an expression literal and evaluates/interprets everything according the grammar
    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {

        if(ast.getLiteral() == null) {
            return Environment.NIL;
        } else {
            return Environment.create(ast.getLiteral());
        }

    }

    //visits a group expression and evaluates/interprets everything according the grammar by visiting subsequent types
    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {

        return visit(ast.getExpression());

    }

    //visits a binary expression and evaluates/interprets everything according the grammar by visiting subsequent types
    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {

        if(ast.getOperator().equals("&&")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Boolean isLeft = requireType(Boolean.class, left);

            if(!isLeft) {
                return Environment.create(false);
            }

            Environment.PlcObject right = visit(ast.getRight());
            Boolean isRight = requireType(Boolean.class, right);

            return Environment.create(isRight); //isLeft is always true

        } else if(ast.getOperator().equals("||")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Boolean isLeft = requireType(Boolean.class, left);

            if(isLeft) {
                return Environment.create(true);
            }

            Environment.PlcObject right = visit(ast.getRight());
            Boolean isRight = requireType(Boolean.class, right);

            return Environment.create(isRight); //isLeft is always false

        } else if(ast.getOperator().equals(">")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            //if it explodes it explodes

            if(requireType(Comparable.class, left).compareTo(requireType(left.getValue().getClass(), right)) > 0) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }

        } else if(ast.getOperator().equals("<")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            //if it explodes it explodes
            if(requireType(Comparable.class, left).compareTo(requireType(left.getValue().getClass(), right)) < 0) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }

        } else if(ast.getOperator().equals("==")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            //if it explodes it explodes
            if(left.getValue().equals(right.getValue())) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }

        } else if(ast.getOperator().equals("!=")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            //if it explodes it explodes
            if(!(left.getValue().equals(right.getValue()))) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }

        } else if(ast.getOperator().equals("+")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            if(left.getValue() instanceof String || right.getValue() instanceof String) {
                String returnVal;
                try {
                    returnVal = left.getValue().toString() + right.getValue().toString(); //don't need multiple cases because toString() is a method of all java objects
                } catch(Exception e) {
                    throw new RuntimeException("Incompatible object to add to string, no toString() method for non-string object");
                }

                return Environment.create(returnVal);

            } else if(left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                BigDecimal returnVal = ((BigDecimal) left.getValue()).add((BigDecimal) right.getValue());
                return Environment.create(returnVal);

            } else if(left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                BigInteger returnVal = ((BigInteger) left.getValue()).add((BigInteger) right.getValue());
                return Environment.create(returnVal);

            } else { //different data types therefore incompatible
                throw new RuntimeException("Incompatible data types in addition");
            }

        } else if(ast.getOperator().equals("-")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            if(left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                BigDecimal returnVal = ((BigDecimal) left.getValue()).subtract((BigDecimal) right.getValue());
                return Environment.create(returnVal);

            } else if(left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                BigInteger returnVal = ((BigInteger) left.getValue()).subtract((BigInteger) right.getValue());
                return Environment.create(returnVal);

            } else { //different data types therefore incompatible
                throw new RuntimeException("Incompatible data types in subtraction");
            }

        } else if(ast.getOperator().equals("*")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            if(left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                BigDecimal returnVal = ((BigDecimal) left.getValue()).multiply((BigDecimal) right.getValue());
                return Environment.create(returnVal);

            } else if(left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                BigInteger returnVal = ((BigInteger) left.getValue()).multiply((BigInteger) right.getValue());
                return Environment.create(returnVal);

            } else { //different data types therefore incompatible
                throw new RuntimeException("Incompatible data types in multiplication");
            }

        } else if(ast.getOperator().equals("/")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            if(left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                BigDecimal zero = new BigDecimal("0.0"); //can pass double as parameter, but kept consistent with BigInteger
                if(((BigDecimal) right.getValue()).equals((zero))) {
                    throw new RuntimeException("Cannot divide by zero in BigDecimal");
                }
                BigDecimal returnVal = ((BigDecimal) left.getValue()).divide(((BigDecimal) right.getValue()), RoundingMode.HALF_EVEN);
                return Environment.create(returnVal);

            } else if(left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                BigInteger zero = new BigInteger("0"); //cannot pass an integer as a parameter for constructor
                if((right.getValue()).equals((zero))) {
                    throw new RuntimeException("Cannot divide by zero in BigInteger");
                }
                BigInteger returnVal = ((BigInteger) left.getValue()).divide((BigInteger) right.getValue());
                return Environment.create(returnVal);

            } else { //different data types therefore incompatible
                throw new RuntimeException("Incompatible data types in division");
            }

        } else if(ast.getOperator().equals("^")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Environment.PlcObject right = visit(ast.getRight());

            BigInteger Z = new BigInteger("0");

            if(!(right.getValue() instanceof BigInteger)) {
                throw new RuntimeException("Exponent must be a BigInteger");
            }

            //values of resultant BigInteger and BigDecimal might be bigger than ranges for int and double so must be done manually with for loop
            if(left.getValue() instanceof BigDecimal) {

                //power is zero, return 1
                if(((BigInteger)right.getValue()).equals(Z)) {
                    return Environment.create(BigDecimal.ONE);
                }

                BigDecimal base = (BigDecimal) left.getValue();
                BigDecimal returnVal = BigDecimal.ONE;

                //power is negative case
                if(((BigInteger)right.getValue()).compareTo(Z) < 0) {
                    BigInteger newPower = ((BigInteger)right.getValue()).multiply(new BigInteger("-1")); //change power to positive for loop
                    for(BigInteger i = new BigInteger("0"); i.compareTo(newPower) < 0; i = i.add(new BigInteger("1"))) {
                        returnVal = returnVal.divide(base, RoundingMode.HALF_EVEN); //divide instead of multiply bc of negative power
                    }
                    return Environment.create(returnVal);
                } else { //power is positive case (power zero is taken care of already)
                    for(BigInteger i = new BigInteger("0"); i.compareTo((BigInteger)right.getValue()) < 0; i = i.add(new BigInteger("1"))) {
                        returnVal = returnVal.multiply(base);
                    }
                    return Environment.create(returnVal);
                }

            } else if(left.getValue() instanceof BigInteger) {

                //power is zero, return 1
                if(((BigInteger)right.getValue()).equals(Z)) {
                    return Environment.create(new BigInteger("1"));
                }

                BigInteger base = (BigInteger) left.getValue();
                BigInteger returnVal = new BigInteger("1");

                //power is negative case
                if(((BigInteger)right.getValue()).compareTo(Z) < 0) {
                    BigInteger newPower = ((BigInteger)right.getValue()).multiply(new BigInteger("-1")); //change power to positive for loop
                    for(BigInteger i = new BigInteger("0"); i.compareTo(newPower) < 0; i = i.add(new BigInteger("1"))) {
                        returnVal = returnVal.divide(base); //divide instead of multiply bc of negative power (since int, should end up being zero)
                    }
                    return Environment.create(returnVal);
                } else { //power is positive case (power zero is taken care of already)
                    for(BigInteger i = new BigInteger("0"); i.compareTo((BigInteger)right.getValue()) < 0; i = i.add(new BigInteger("1"))) {
                        returnVal = returnVal.multiply(base);
                    }
                    return Environment.create(returnVal);
                }

            } else { //different data types therefore incompatible
                throw new RuntimeException("Incompatible data types in multiplication");
            }
        } else {
            throw new RuntimeException("No Valid Operator");
        }
    }

    //visits an access expression and evaluates/interprets everything according the grammar by visiting subsequent types
    //must lookup variable in the current scope to see if it accessable
    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {

        boolean offsetPresent = ast.getOffset().isPresent();

        if(offsetPresent) {

            int offsetInt = ((BigInteger) visit(ast.getOffset().get()).getValue()).intValue();

            //if it blows up it blows up
            List<Object> potentialAccessed = (List<Object>) scope.lookupVariable(ast.getName()).getValue().getValue();

            if(offsetInt < 0 || potentialAccessed.size() - 1 < offsetInt) {
                throw new RuntimeException("Offset out of bounds");
            }

            return Environment.create(potentialAccessed.get(offsetInt));

        } else { //offsetPresent is false, thus there is no offset

            return scope.lookupVariable(ast.getName()).getValue();

        }

    }

    //visits a function call and evaluates/interprets everything according the grammar by visiting subsequent types
    //must lookup function in scope to see if the function has been defined and can even be called
    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {

        List<Environment.PlcObject> arguments = new ArrayList<Environment.PlcObject>();

        for(Ast.Expression exp : ast.getArguments()) {
            arguments.add(visit(exp));

        }

        return scope.lookupFunction(ast.getName(), arguments.size()).invoke(arguments);

    }

    //visits a list type called PlcList and evaluates/interprets everything according the grammar by visiting subsequent types
    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {

        List<Ast.Expression> listz = ast.getValues();

        List<Object> returnList = new ArrayList<Object>();

        for(Ast.Expression iter: listz) {
            returnList.add(visit(iter).getValue());

        }

        return Environment.create(returnList);

    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */

    //requireType makes sure that the first object class is the same as the object stored in the PlcObject
    //throws error otherwise because types would be incompatible
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */

    //the return class must have similar functionality to RuntimeException because it must potentially go from within many function calls and scopes to the
        //-outer most where the return is supposed to return to (just like exception)
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
