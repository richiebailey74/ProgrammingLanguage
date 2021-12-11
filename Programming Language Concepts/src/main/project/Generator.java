package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

//the generator generates java code from the values stored in the ast values
//since this project does not implement its own compiler, this is why the java compiler must be used and thus,
//-the code lexed, parsed, analyzed, and interpreted by this project must take the info stored to generate java code to run

//generator also implements the abstract visitor class to allow for the visitor pattern allowing for double dispatch to be used to be utilized-
//-so that java code is generated efficiently
public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0; //used to track the current indent of the java code

    //constructor takes in a PrintWriter object
    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    //takes in a sequence and prints them, and does so either visiting the correlated visit function or the string directly itself
    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    //automatically pushed the program printing to the next line where the parameter is how many indents it should have on that particular line
    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    //prints out the source by hardcoding the main's and then printing all the globals and functions with appropriate newline spacing, all with appropriate indenting
    @Override
    public Void visit(Ast.Source ast) {

        print("public class Main {");
        newline(0);
        newline(++indent);

        for(int i = 0; i < ast.getGlobals().size(); i++) {
            print(ast.getGlobals().get(i));
            if(i != ast.getGlobals().size() - 1) {
                newline(indent);
            }
        }
        if(ast.getGlobals().size() != 0) {
            newline(0);
            newline(indent);
        }

        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");

        for(int i = 0; i < ast.getFunctions().size(); i++) {

            newline(0);
            newline(indent);
            print(ast.getFunctions().get(i));

        }

        newline(0);
        newline(--indent);
        print("}");

        return null;
    }

    //prints out differently depending on whether the ast value is of type list, mutable, or immutable
    //if of type list, then visit the PlcList in order to properly print all the values in the list
    @Override
    public Void visit(Ast.Global ast) {



        if(ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList) {

            print(ast.getVariable().getType().getJvmName());
            print("[]");
            print(" ");
            print(ast.getName()); //is there a reason why this isn't ast.getVariable().getjvmname(); ?
            print(" ");
            print("=");
            print(" ");
            visit(ast.getValue().get()); //prints PLC list
            print(";");

        } else if(ast.getMutable()) {

            print(ast.getVariable().getType().getJvmName());
            print(" ");
            print(ast.getVariable().getJvmName());
            if(ast.getValue().isPresent()) {//why does this say always true?
                print(" ");
                print("=");
                print(" ");
                print(ast.getValue().get());
            }
            print(";");

        } else {

            print("final");
            print(" ");
            print(ast.getVariable().getType().getJvmName());
            print(" ");
            print(ast.getVariable().getJvmName());
            if(ast.getValue().isPresent()) {//why does this say always true
                print(" ");
                print("=");
                print(" ");
                print(ast.getValue().get());
            }
            print(";");

        }

        return null;
    }

    //prints out function return type and function name, and then the parameters and then the statements within it, all with appropriate indenting
    @Override
    public Void visit(Ast.Function ast) {

        print(ast.getFunction().getReturnType().getJvmName());
        print(" ");
        print(ast.getFunction().getJvmName());

        print("(");
        if(ast.getParameterTypeNames().size() != 0) {
            if(ast.getParameterTypeNames().size() == 1) {
                print(Environment.getType(ast.getParameterTypeNames().get(0)).getJvmName()); //converts our type to java type
                print(" ");
                print(ast.getParameters().get(0));
            } else {
                for(int i = 0; i < ast.getParameterTypeNames().size(); i++) {
                    print(Environment.getType(ast.getParameterTypeNames().get(i)).getJvmName()); //converts our type to java type
                    print(" ");
                    print(ast.getParameters().get(i));
                    if(i != ast.getParameterTypeNames().size() - 1) {
                        print(",");
                        print(" ");
                    }
                }
            }
        }
        print(")");

        print(" ");
        print("{");
        if(ast.getStatements().size() == 0) {
            print("}");
            return null;
        }

        newline(++indent);

        for(int i = 0; i < ast.getStatements().size(); i++) {

            print(ast.getStatements().get(i));

            if(i != ast.getStatements().size() - 1) {
                newline(indent);
            } else {
                newline(--indent);
            }
        }
        print("}");


        return null;
    }

    //prints expression stored in ast
    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression());
        print(";");

        return null;
    }

    //prints out declaration statement and only the assigned value if it is present
    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        print(ast.getVariable().getType().getJvmName());
        print(" ");
        print(ast.getVariable().getJvmName());

        if(ast.getValue().isPresent()) {
            print(" ");
            print("=");
            print(" ");
            print(ast.getValue().get());
        }
        print(";");

        return null;
    }

    //prints out an assignment of a value to a receiver
    @Override
    public Void visit(Ast.Statement.Assignment ast) {

        print(ast.getReceiver());
        print(" ");
        print("=");
        print(" ");
        print(ast.getValue());
        print(";");

        return null;
    }

    //prints the if statement's condition and then the then statements and then the else statements, all with appropriate indenting
    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if");
        print(" ");

        print("(");
        print(ast.getCondition());
        print(")");

        print(" ");
        print("{");

        newline(++indent);

        for(int i = 0; i < ast.getThenStatements().size(); i++) {
            print(ast.getThenStatements().get(i));
            if(i != ast.getThenStatements().size() - 1) {
                newline(indent);
            }
        }
        newline(--indent);
        print("}");

        if(ast.getElseStatements().size() == 0) {
            return null;
        }

        print(" ");
        print("else");
        print(" ");
        print("{");
        newline(++indent);

        for(int i = 0; i < ast.getElseStatements().size(); i++) {
            print(ast.getElseStatements().get(i));
            if(i != ast.getElseStatements().size() - 1) {
                newline(indent);
            }
        }
        newline(--indent);
        print("}");


        return null;
    }

    //prints out switch statement and its subsequent statements by visiting all of the cases, all with appropriate indenting
    @Override
    public Void visit(Ast.Statement.Switch ast) {

        print("switch");
        print(" ");
        print("(");
        print(ast.getCondition());
        print(")");
        print(" ");
        print("{");

        newline(++indent);

        for(int i = 0; i < ast.getCases().size(); i++) {

            if(i != ast.getCases().size() - 1) {
                visit(ast.getCases().get(i));
            } else {
                visit(ast.getCases().get(i));
                //takes care of the un-indenting for the bracket in default case by un-indenting two within the one newline
                print("}");
            }

        }

        return null;
    }

    //prints the value along with the statements within the case, all with appropriate indenting
    //has a double un-indent at end to account for the printing in switch
    @Override
    public Void visit(Ast.Statement.Case ast) {

        if(ast.getValue().isPresent()) {
            print("case");
            print(" ");
            print(ast.getValue().get());
            print(":");
            newline(++indent);
            for(int i = 0; i < ast.getStatements().size(); i++) {
                print(ast.getStatements().get(i));
                if(i != ast.getStatements().size() - 1) {
                    newline(indent);
                }
            }
            newline(--indent);

        } else {
            print("default");
            print(":");
            newline(++indent);
            for(int i = 0; i < ast.getStatements().size(); i++) {
                print(ast.getStatements().get(i));
                if(i != ast.getStatements().size() - 1) {
                    newline(indent);
                }
            }
            indent -= 2; //to take care of bracket indenting level in switch
            newline(indent);

        }

        return null;
    }

    //prints out the condition and all statements for the while statement, all with appropriate indenting
    @Override
    public Void visit(Ast.Statement.While ast) {

        print("while");
        print(" ");
        print("(");
        print(ast.getCondition());
        print(")");
        print(" ");
        print("{");

        if(ast.getStatements().size() == 0) {
            print("}");
            return null;
        }

        newline(++indent);

        for(int i = 0; i < ast.getStatements().size(); i++) {
            print(ast.getStatements().get(i));

            if(i != ast.getStatements().size() - 1) {
                newline(indent);
            }
        }

        newline(--indent);
        print("}");


        return null;
    }

    //print out return along with the value stored in the ast
    @Override
    public Void visit(Ast.Statement.Return ast) {

        print("return");
        print(" ");

        print(ast.getValue());

        print(";");

        return null;
    }

    //prints the literal depending on the type bc it must account for small details associated with each data types
    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Environment.Type litType = ast.getType();
        Object lit = ast.getLiteral();

        if(litType.equals(Environment.Type.NIL))  {
            print("null");
        } else if(litType.equals(Environment.Type.STRING)) {
            print("\"");
            print(lit);
            print("\"");
        } else if(litType.equals(Environment.Type.CHARACTER)) {
            print("'");
            print(lit);
            print("'");
        } else if(litType.equals(Environment.Type.INTEGER)) {
            BigInteger intVal = (BigInteger) lit;
            print(intVal.intValue());
        } else if(litType.equals(Environment.Type.DECIMAL)) {
            BigDecimal decVal = new BigDecimal(lit.toString());
            print(decVal.doubleValue());
        } else if(litType.equals(Environment.Type.BOOLEAN)) {
            print(ast.getLiteral());
        } else {
            throw new RuntimeException("Invalid type to be generated!");
        }

        return null;

    }

    //prints out the binary expression within parenthesis
    @Override
    public Void visit(Ast.Expression.Group ast) {

        print("(");
        print(ast.getExpression());
        print(")");

        return null;
    }

    //prints out the left then the operator then the right for a binary operation
    //unless it is exponents, then must call Math.pow() instead of a (left operator right) format
    @Override
    public Void visit(Ast.Expression.Binary ast) {

        if(ast.getOperator().equals("^")) {
            print("Math.pow(");
            print(ast.getLeft());
            print(", ");
            print(ast.getRight());
            print(")");

        } else {
            print(ast.getLeft());
            print(" ");

            print(ast.getOperator());

            print(" ");
            print(ast.getRight());

        }

        return null;

    }

    //prints out the variable name then brackets in order to access elements from the list
    @Override
    public Void visit(Ast.Expression.Access ast) {

        print(ast.getVariable().getJvmName());

        if(ast.getOffset().isPresent()) {
            print("[");
            print(ast.getOffset().get());
            print("]");
        }

        return null;
    }

    //prints out the jvm (java virtual machine) name of the function and then the arguments within parenthesis
    @Override
    public Void visit(Ast.Expression.Function ast) {

        print(ast.getFunction().getJvmName());

        print("(");
        for(int i = 0; i < ast.getArguments().size(); i++) {
            print(ast.getArguments().get(i));

            if(i != ast.getArguments().size() - 1) {
                print(", ");
            }
        }
        print(")");

        return null;
    }

    //separate cases for one and zero elements because no commas, otherwise commas to be printed for values in the PlcList (print all values)
    //called in List for globals only
    @Override
    public Void visit(Ast.Expression.PlcList ast) {

        if(ast.getValues().size() <= 1) {
            print("{");
            if(ast.getValues().size() == 1) {
                print(ast.getValues().get(0));
            }
            print("}");
            return null;
        }

        print("{");
        for(int i = 0; i < ast.getValues().size(); i++) {

            print(ast.getValues().get(i));

            if(i != ast.getValues().size() - 1) {
                print(",");
                print(" ");
            }
        }
        print("}");

        return null;
    }

}
