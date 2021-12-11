package plc.project;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

//The parser takes the sequence of tokens emitted by the lexer and turns that into a structured representation of the program, called the Abstract Syntax Tree (AST).

//The parser has a similar architecture to the lexer


//** at any point, calling errorCases(...) will throw an error and the parameters will depend on the type of error message is desired to be thrown for certain situations

public final class Parser {

    //functionally in these methods, if need to match something at the end of the grammar-
    //-then use !match(...) so it will enter to errorCases() and otherwise exits the if statement

    private final TokenStream tokens;

    //class constructor that takes in a list of tokens to parse according to the language of the grammar
    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    //parses the source field according to the grammar, defined as a list of globals followed by a list of functions
    //throws error if there is anything after the last function because that violated the grammar
    public Ast.Source parseSource() throws ParseException {

        List<Ast.Global> globalsList = new ArrayList<Ast.Global>();
        List<Ast.Function> functionsList = new ArrayList<Ast.Function>();

        //kleene closure global
        while(peek("VAL") || peek("VAR") || peek("LIST")) {
            globalsList.add(parseGlobal());
        }

        //kleene closure functions
        while(peek("FUN")) {
            functionsList.add(parseFunction());
        }

        if(tokens.has(0)) {
            errorCases();
        }

        return new Ast.Source(globalsList, functionsList);
    }

    //parses all globals in the global field according to the grammar, defined as either a mutable, immutable, or list
    public Ast.Global parseGlobal() throws ParseException {

        Ast.Global returnGlobal = null;

        if(peek("VAL")) {
            returnGlobal = parseImmutable();
        } else if(peek("VAR")) {
            returnGlobal = parseMutable();
        } else if(peek("LIST")) {
            returnGlobal = parseList();
        } else {
            errorCases();
        }

        if(!match(";")) {
            errorCases(";");
        }

        return returnGlobal; //not always null, compiler can't look into errorCases() functionality
    }

    //parses type of list according to grammar
    public Ast.Global parseList() throws ParseException { //fix multiple expressions at end
        match("LIST");
        String variableCall = "";
        String variableType = "";
        Ast.Global returnList = null;
        Optional<Ast.Expression> expressionOpt;

        if(peek(Token.Type.IDENTIFIER)) {
            variableCall = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        } else {
            errorCases();
        }

        if(!match(":")) {
            errorCases(":");
        }

        if(peek(Token.Type.IDENTIFIER)) {
            variableType = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        } else {
            errorCases();
        }

        if(!match("=")) {
            errorCases("=");
        }
        if(!match("[")) {
            errorCases("[");
        }

        List<Ast.Expression> exprs = new ArrayList<Ast.Expression>();

        //explicitly grabs first expression, which must be present
        Ast.Expression first = parseExpression();
        exprs.add(first);

        while(peek(",")) {

            match(",");
            exprs.add(parseExpression());

        }

        if(!match("]")) {
            errorCases("]");
        }

        Ast.Expression listOfExpr = new Ast.Expression.PlcList(exprs);

        expressionOpt = Optional.of(listOfExpr);

        return new Ast.Global(variableCall, variableType, true, expressionOpt);
    }

    //parses mutables according to grammar
    public Ast.Global parseMutable() throws ParseException {
        match("VAR");
        String variableCall = "";
        String variableType = "";
        Optional<Ast.Expression> expressionOpt = Optional.empty();

        if(peek(Token.Type.IDENTIFIER)) {
            variableCall = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        } else {
            errorCases();
        }

        if(!match(":")) {
            errorCases(":");
        }

        if(peek(Token.Type.IDENTIFIER)) {
            variableType = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        } else {
            errorCases();
        }

        if(!peek("=")) {
            return new Ast.Global(variableCall, variableType, true, expressionOpt);
        }
        match("=");

        expressionOpt = Optional.of(parseExpression());

        return new Ast.Global(variableCall, variableType, true, expressionOpt);
    }

    //parses immutables according to grammar
    public Ast.Global parseImmutable() throws ParseException {
        match("VAL");
        String variableCall = "";
        String variableType = "";
        Optional<Ast.Expression> expressionOpt;

        if(peek(Token.Type.IDENTIFIER)) {
            variableCall = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        } else {
            errorCases();
        }

        if(!match(":")) {
            errorCases(":");
        }

        if(peek(Token.Type.IDENTIFIER)) {
            variableType = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        } else {
            errorCases();
        }

        if(!match("=")) {
            errorCases("=");
        }

        expressionOpt = Optional.of(parseExpression());

        return new Ast.Global(variableCall, variableType, false, expressionOpt);
    }

    //parses functions according to grammar
    public Ast.Function parseFunction() throws ParseException {
        match("FUN");
        String functionCall = "";
        String returnType = "";
        List<String> params = new ArrayList<String>();
        List<String> paramTypes = new ArrayList<String>();
        List<Ast.Statement> functionCode = new ArrayList<Ast.Statement>();
        Optional<String> optionalReturn = Optional.empty();

        if(peek(Token.Type.IDENTIFIER)) {
            functionCall = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);

        } else {
            errorCases();
        }

        if(match("(")) {

            //for if there are no parameters
            if(match(")")) {

                if(peek(":")) {
                    match(":");
                    if(peek(Token.Type.IDENTIFIER)) {
                        returnType = tokens.get(0).getLiteral();
                        match(Token.Type.IDENTIFIER);
                        optionalReturn = Optional.of(returnType);
                    } else {
                        errorCases();
                    }
                }

                //must match a DO or error
                if(!match("DO")) {
                    errorCases("DO");
                }

                functionCode = parseBlock();

                if(!match("END")) {
                    errorCases("END");
                }

                return new Ast.Statement.Function(functionCall, params, paramTypes, optionalReturn, functionCode);


            } else { //for if there are parameters

                if(peek(Token.Type.IDENTIFIER)) {
                    params.add(tokens.get(0).getLiteral());
                    match(Token.Type.IDENTIFIER);
                } else {
                    errorCases();
                }
                if(!match(":")) {
                    errorCases(":");
                }
                if(peek(Token.Type.IDENTIFIER)) {
                    paramTypes.add(tokens.get(0).getLiteral());
                    match(Token.Type.IDENTIFIER);
                } else {
                    errorCases();
                }

                while(match(",")) {
                    if(!peek(Token.Type.IDENTIFIER)) {
                        errorCases(); //can't have a hanging comma
                    } else if(peek(Token.Type.IDENTIFIER)) {
                        params.add(tokens.get(0).getLiteral());
                        match(Token.Type.IDENTIFIER);
                    }
                    if(!match(":")) {
                        errorCases(":");
                    }
                    if(peek(Token.Type.IDENTIFIER)) {
                        paramTypes.add(tokens.get(0).getLiteral());
                        match(Token.Type.IDENTIFIER);
                    } else {
                        errorCases();
                    }
                }

                //must match a ) or error
                if(!match(")")) {
                    errorCases(")");
                }

                if(peek(":")) {
                    match(":");
                    if(peek(Token.Type.IDENTIFIER)) {
                        returnType = tokens.get(0).getLiteral();
                        match(Token.Type.IDENTIFIER);
                        optionalReturn = Optional.of(returnType);
                    } else {
                        errorCases();
                    }
                }

                //must match a DO or error
                if(!match("DO")) {
                    errorCases("DO");
                }

                functionCode = parseBlock();

                if(!match("END")) {
                    errorCases("END");
                }

                return new Ast.Statement.Function(functionCall, params, paramTypes, optionalReturn, functionCode);
            }


        } else {
            //must have an open brace for parameters even if there are none
            errorCases("(");
        }
        return null;
    }

    //parses code blocks and does so until the end of blocks are identified (peeks in the while loop parameters) according to grammar
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> returnBlock = new ArrayList<Ast.Statement>();
        while(!peek("END") && !peek("DEFAULT") && !peek("ELSE") && !peek("CASE")) {
            returnBlock.add(parseStatement());
        }

        return returnBlock;
    }

    //parses different statements and knows which subsequent methods to call depending on what it peeks
    public Ast.Statement parseStatement() throws ParseException {
        //will include all of the other Ast.Statement methods below

        //don't match in these blocks because the called functions will match
        if(peek("LET")) {
            return parseDeclarationStatement();

        } else if(peek("SWITCH")) {
            return parseSwitchStatement();

        } else if(peek("IF")) {
            return parseIfStatement();

        } else if(peek("WHILE")) {
            return parseWhileStatement();

        } else if(peek("RETURN")) {
            return parseReturnStatement();
        }

        //just the last part of the parseStatement function
        Ast.Expression firstExpression = parseExpression();

        if(peek("=")) {
            match("=");
            Ast.Expression secondExpression = parseExpression();

            if(peek(";")) {
                match(";");
                return new Ast.Statement.Assignment(firstExpression, secondExpression);
            } else {
                errorCases(";");
            }

        } else {
            if(peek(";")) {
                match(";");
                return new Ast.Statement.Expression(firstExpression);
            } else {
                errorCases(";");
            }
        }
        errorCases();
        return null;
    }

    //parses declaration statements according to grammar
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {

        match("LET");

        if(!peek(Token.Type.IDENTIFIER)) {
            errorCases();
        }
        String identVar = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);


        String typeVar;
        Optional<String> type = Optional.empty();
        if(match(":")) {

            if(peek(Token.Type.IDENTIFIER)) {
                typeVar = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                type = Optional.of(typeVar);
            } else {
                errorCases();
            }
        }


        Optional<Ast.Expression> valueOpt = Optional.empty();

        if(match("=")) {
            valueOpt = Optional.of(parseExpression());
        }
        if(!match(";")) {
            errorCases(";");
        }

        return new Ast.Statement.Declaration(identVar, type, valueOpt);

    }

    //parses if statements according to grammar
    public Ast.Statement.If parseIfStatement() throws ParseException {
        match("IF");

        //need these data types bc parseBlock() returns this type
        List<Ast.Statement> thenStatements = new ArrayList<Ast.Statement>();
        List<Ast.Statement> elseStatements = new ArrayList<Ast.Statement>();

        Ast.Expression condition = parseExpression();

        //errorCases needed because DO is required
        if(match("DO")) {
            thenStatements = parseBlock();
        } else {
            errorCases("DO");
        }

        //no errorCases needed because optional (
        if(match("ELSE")) {
            elseStatements = parseBlock();
        }

        if(!match("END")) {
            errorCases("END");
        }

        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }

    //parses switch statements according to grammar
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {//look back at this
        match("SWITCH");
        List<Ast.Statement.Case> cases = new ArrayList<Ast.Statement.Case>();
        List<Ast.Statement> default_list; //default needs a block value but in the switch statement return we will not have a value (no member variable nor constructor valuable for it)

        Ast.Expression condition = parseExpression();

        while(peek("CASE")){
            cases.add(parseCaseStatement());
        }

        if(!match("DEFAULT")){
            errorCases("DEFAULT");
        }

        //add default case
        default_list = parseBlock();
        Optional<Ast.Expression> tempOptional = Optional.empty();
        Ast.Statement.Case def = new Ast.Statement.Case(tempOptional, default_list);

        cases.add(def);

        if(!match("END")){
            errorCases("END");
        }

        return new Ast.Statement.Switch(condition, cases);
    }

    //will only be used inside switch, so essentially works together, all according to grammar
    public Ast.Statement.Case parseCaseStatement() throws ParseException {//look back at this
        Optional<Ast.Expression> valueOpt;
        match("CASE");
        valueOpt = Optional.of(parseExpression());

        if(!match(":")){
            errorCases(":");
        }
        List<Ast.Statement> statements = parseBlock();

        return new Ast.Statement.Case(valueOpt, statements);
    }

    //parses while statement according to grammar
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");

        Ast.Expression condition = parseExpression();

        List<Ast.Statement> loopBlock = new ArrayList<Ast.Statement>();

        if(match("DO")) {
            loopBlock = parseBlock();
        } else {
            errorCases("DO");
        }

        if(!match("END")) {
            errorCases("END");
        }

        return new Ast.Statement.While(condition, loopBlock);
    }

    //parses return statements according to grammar
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        match("RETURN");

        Ast.Expression returnExpression = parseExpression();

        if(!match(";")) {
            errorCases(";");
        }
        return new Ast.Statement.Return(returnExpression);
    }

    //**** has recursive decent tree of parse expression type calls where they build on each other
    //the bottom of the tree can call on the highest tree method if not fully simplified and continuously moves down tree until fully simplified


    //top function call from the recursive decent tree
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    //parses logical expressions that utilize logical operators
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression firstTerm = parseComparisonExpression();

        while(peek("&&") || peek("||")) {
            String logicalOperator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);

            Ast.Expression secondTerm = parseComparisonExpression();

            if(!peek("&&") && !peek("||")) {
                return new Ast.Expression.Binary(logicalOperator, firstTerm, secondTerm);
            } else {
                firstTerm = new Ast.Expression.Binary(logicalOperator, firstTerm, secondTerm);
            }
        }
        return firstTerm;
    }

    //parses comparison expressions that utilize expressions with boolean comparative operators
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression firstTerm = parseAdditiveExpression();

        while(peek("<") || peek(">") || peek("==") || peek("!=")) {
            String comparisonOperator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);

            Ast.Expression secondTerm = parseAdditiveExpression();

            if(!peek("<") && !peek(">") && !peek("==") && !peek("!=")) {
                return new Ast.Expression.Binary(comparisonOperator, firstTerm, secondTerm);
            } else {
                firstTerm = new Ast.Expression.Binary(comparisonOperator, firstTerm, secondTerm);
            }
        }
        return firstTerm;
    }

    //parses additive expressions that utilize expressions that have addition and subtraction
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression firstTerm = parseMultiplicativeExpression();

        while(peek("+") || peek("-")) {
            String additiveOperator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);

            Ast.Expression secondTerm = parseMultiplicativeExpression();

            if(!peek("+") && !peek("-")) {
                return new Ast.Expression.Binary(additiveOperator, firstTerm, secondTerm);
            } else {
                firstTerm = new Ast.Expression.Binary(additiveOperator, firstTerm, secondTerm);
            }
        }
        return firstTerm;
    }

    //parses multiplicative expressions that utilize multiplication, division, and exponents
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {

        //Binary is a dope way to recursively connect more multiplicative expressions and all within the recursive stack of expressions

        Ast.Expression firstTerm = parsePrimaryExpression();

        while(peek("*") || peek("/") || peek("^")) {
            String multiplicativeOperator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR);

            Ast.Expression secondTerm = parsePrimaryExpression();

            if(!peek("*") && !peek("/") && !peek("^")) {
                return new Ast.Expression.Binary(multiplicativeOperator, firstTerm, secondTerm);
            } else {
                firstTerm = new Ast.Expression.Binary(multiplicativeOperator, firstTerm, secondTerm);
            }
        }
        return firstTerm;
    }


    //helper function to throw errors:
    //need two cases because first is if it is the end of the file
    //second is for if it isn't matching the character it should be matching
    public void errorCases(String matching) throws ParseException {

        if(!tokens.has(0)) {
            throw new ParseException("If no token exists, then must get index of previous token plus its length for proper index",
                    tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        } else if(!match(matching)) {
            throw new ParseException("Must match " + matching, tokens.get(0).getIndex());
        }

    }

    public void errorCases(Ast.Expression matching) throws ParseException {

        if(!tokens.has(0)) {
            throw new ParseException("If no token exists, then must get index of previous token plus its length for proper index",
                    tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        } else if(!match(matching)) {
            throw new ParseException("Doesn't match properly", tokens.get(0).getIndex());
        }

    }

    //error cases if error needs to be thrown and there is no matching of strings or Ast.Expression needed
    public void errorCases() throws ParseException {

        if(!tokens.has(0)) {
            throw new ParseException("If no token exists, then must get index of previous token plus its length for proper index",
                    tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        } else {
            throw new ParseException("Doesn't match properly", tokens.get(0).getIndex());
        }

    }

    //parses primary expressions that includes the types of the tokens or NIL or TRUE or FALSE
    //for characters and strings, if escape sequences exist then it must be simplified through the regex and some backslashes need removal
    //if ( peeked then a group call or function call might be needed, and if [ peeked then will need an access call
    public Ast.Expression parsePrimaryExpression() throws ParseException {

        if(peek("NIL")) {
            match("NIL");
            return new Ast.Expression.Literal(null);

        } else if(peek("TRUE")) {
            match("TRUE");
            return new Ast.Expression.Literal(true);

        } else if(peek("FALSE")) {
            match("FALSE");
            return new Ast.Expression.Literal(false);

        } else if (peek(Token.Type.INTEGER)) {
            BigInteger tokenInt = new BigInteger(tokens.get(0).getLiteral());
            match(Token.Type.INTEGER);
            return new Ast.Expression.Literal(tokenInt);

        } else if(peek(Token.Type.DECIMAL)) {
            BigDecimal tokenDec = new BigDecimal(tokens.get(0).getLiteral());
            match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal(tokenDec);

        } else if(peek(Token.Type.CHARACTER)) {
            String tokenStringChar = tokens.get(0).getLiteral();

            tokenStringChar = tokenStringChar.replace("\\t", "\t");
            tokenStringChar = tokenStringChar.replace("\\r", "\r");
            tokenStringChar = tokenStringChar.replace("\\b", "\b");
            tokenStringChar = tokenStringChar.replace("\\n", "\n");
            tokenStringChar = tokenStringChar.replace("\\\\", "\\");
            tokenStringChar = tokenStringChar.replace("\\\"", "\"");
            tokenStringChar = tokenStringChar.replace("\\\'", "\'");

            tokenStringChar = tokenStringChar.substring(1, tokenStringChar.length() - 1);

            Character tokenChar = tokenStringChar.charAt(0);
            match(Token.Type.CHARACTER);
            return new Ast.Expression.Literal(tokenChar);

        } else if(peek(Token.Type.STRING)) {
            String tokenString = tokens.get(0).getLiteral();

            //use String.replace(...,...) to replace all instances of certain char streams with another char stream (escape sequences)
            tokenString = tokenString.replace("\\t","\t");
            tokenString = tokenString.replace("\\r", "\r");
            tokenString = tokenString.replace("\\b", "\b");
            tokenString = tokenString.replace("\\n","\n");
            tokenString = tokenString.replace("\\\\", "\\");
            tokenString = tokenString.replace("\\\"", "\"");
            tokenString = tokenString.replace("\\\'", "\'");

            match(Token.Type.STRING);
            return new Ast.Expression.Literal(tokenString.substring(1,tokenString.length() - 1));

        } else if(peek("(")) {
            match("(");
            //the parseExpression() passed into the group constructor is to account for everything inside the parenthesis
            Ast.Expression expressionVar = parseExpression();
            if(peek(")")) {
                match(")");
                return new Ast.Expression.Group(expressionVar);
            } else {
                //throw different types of errors
//                throw new ParseException("Must close parentheses", tokens.index);
                errorCases(")");

            }

        } else if(peek(Token.Type.IDENTIFIER)) {

            String identVar = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if(peek("(")) {
                match("(");

                ArrayList<Ast.Expression> expressionList = new ArrayList();

                while(!peek(")")) {
                    expressionList.add(parseExpression());
                    if(peek(",")) {
                        match(",");
                        if(peek(")")) {
                            match(")");
                            errorCases();
                        }
                    }
                }
                //technically already a peek since the while loop breaks

                //it will always match but must see if it is the end of the file
                errorCases(")");

                Ast.Expression.Function functionVar = new Ast.Expression.Function(identVar, expressionList);
                return functionVar;

            } else if(peek("[")) {
                //not sure if I implemented the optional correctly
                match("[");
                Optional<Ast.Expression> maybeExpression = Optional.of(parseExpression());

                if(peek("]")) {
                    match("]");
                } else {
                    errorCases("]");
                }

                Ast.Expression.Access accessVar = new Ast.Expression.Access(maybeExpression, identVar);
                return accessVar;

            } else {
                //returning just the literal on its own (nothing that follow cuz in grammar there is a ? present)
                return new Ast.Expression.Access(Optional.empty(), identVar);
            }
        }

        errorCases();
        return null;
    }

    //peeks ahead to return a boolean of if the input peek is equal to the tracker class of valid types from Token.Type or String, otherwise throw error for invalid type
    //does not increment the tracker (tracker is for list of tokens created from the lexer)
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if(!tokens.has(i)) {
                return false;
            } else if(patterns[i] instanceof Token.Type) {
                if(patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if(patterns[i] instanceof String) {
                if(!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    //has the same functionality as peek except it does increment the tracker if there is a match
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if(peek) {
            for(int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }


    //allows the parser function to keep track of the place within the list of tokens
    //similar functionality to the charStream class in the lexer except for different data types
    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }


        //Returns true if there is a token at index + offset.
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        //Gets the token at index + offset.
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        //Advances to the next token, incrementing the index.
        public void advance() {
            index++;
        }

    }

}
