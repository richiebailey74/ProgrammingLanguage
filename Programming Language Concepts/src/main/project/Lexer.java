package plc.project;

import java.util.ArrayList;
import java.util.List;

//Lexer takes the entire typed program as a string and lexes it into tokens that are dictated by grammar rules and are to be used by the parser

public final class Lexer {

    private final CharStream chars;

    //constructor that takes in char stream being the string that is the entire program which is to be lexed by the lex function
    //does so through the CharStream class defined at end of file
    public Lexer(String input) {
        chars = new CharStream(input);
    }

    //function that lexes the program string and takes care of white space and repeats until the end of the program string is reached
    //otherwise calls the lex() function to take care of non-white space tokens
    public List<Token> lex() {

        List<Token> tokenList = new ArrayList<Token>();
        while(chars.has(0)) {

            if(peek("[ \n\r\t\b]")) {
                chars.advance();
                chars.skip();
            } else {
                tokenList.add(lexToken());
            }
        }
        return tokenList;
    }

    //calls non-white space token lexing by peeking ahead to what the particular token should start with
    public Token lexToken() {
        if(peek("[A-Za-z@]")) {
            return lexIdentifier();
        } else if(peek("[-]", "[1-9]") || peek("[0]") || peek("[1-9]")) {
            return lexNumber();
        } else if(peek("\'")) {
            return lexCharacter();
        } else if(peek("\"")) {
            return lexString();
        } else if(peek("\\\\")) {
            lexEscape();
        } else {
            return lexOperator();
        }
        return null;
    }

    //lexes identifiers by following rules of this language for identifier tokens and throws an error if the token will fir sure be an identifier and has invalid syntax
    public Token lexIdentifier() {
        //'@'? [A-Za-z] [A-Za-z0-9_-]*
        if(peek("@")) {
            match("@");
            if(peek("[A-Za-z]")) {
                match("[A-Za-z]");
            } else {
                throw new ParseException("Must follow @ with an alphabetical character", chars.index);
            }
            while (peek("[A-Za-z0-9_-]")) {
                match("[A-Za-z0-9_-]");
            }
        } else if (peek("[A-Za-z]")) {
            match("[A-Za-z]");
            while (peek("[A-Za-z0-9_-]")) {
                match("[A-Za-z0-9_-]");
            }
        } else {
            throw new ParseException("Cannot lead with a non-alphabet or non-@ character", chars.index);
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }


    //lexes numbers according to the syntax rules of the languages grammar
    //complex grammar because must account for cases for both integers and decimals along with each of their subsequent edge cases
    public Token lexNumber() {
        if(peek("-")) {
            match("-");
            if(peek("0")) {
                match("0");
                if(peek("\\.", "[0-9]")) {
                    match("\\.");
                    if(peek("[0-9]")) {
                        while(peek("[0-9]")) {
                            match("[0-9]");
                        }
                        return chars.emit(Token.Type.DECIMAL);
                    }
                } else if (peek("[0-9]")) {
                    throw new ParseException("Cannot have leading zeros", chars.index);
                } else {
                    return chars.emit(Token.Type.INTEGER);
                }
            } else if(peek("\\.")) {
                throw new ParseException("Cannot have a negative decimal without a number between the decimal and negative", chars.index);
            } else if(peek("[1-9]")) {
                match("[1-9]");
                while(peek("[0-9]")) {
                    match("[0-9]");
                }
                if(peek("\\.", "[0-9]")) {
                    match("\\.");
                    if(peek("[0-9]")) {
                        while(peek("[0-9]")) {
                            match("[0-9]");
                        }
                        return chars.emit(Token.Type.DECIMAL);
                    } else {
                        return chars.emit(Token.Type.INTEGER);
                    }
                } else {
                    return chars.emit(Token.Type.INTEGER);
                }
            }
        } else if(peek("0")) {
            match("0");
            if(peek("\\.", "[0-9]")) {
                match("\\.");
                if(peek("[0-9]")) {
                    while(peek("[0-9]")) {
                        match("[0-9]");
                    }
                    return chars.emit(Token.Type.DECIMAL);
                }
            } else if (peek("[0-9]")) {
                throw new ParseException("Cannot have leading zeros", chars.index);
            } else {
                return chars.emit(Token.Type.INTEGER);
            }
        } else if(peek("\\.")) {
            throw new ParseException("Cannot have a decimal without a number before the decimal", chars.index);
        } else if(peek("[1-9]")) {
            match("[1-9]");
            while(peek("[0-9]")) {
                match("[0-9]");
            }
            if(peek("\\.", "[0-9]")) {
                match("\\.");
                if(peek("[0-9]")) {
                    while(peek("[0-9]")) {
                        match("[0-9]");
                    }
                    return chars.emit(Token.Type.DECIMAL);
                }
            } else {
                return chars.emit(Token.Type.INTEGER);
            }
        }
        return null;
    }

    //lexes a character amd must follow the grammar of a character (starts and ends with ' and otherwise throws an error) and can't be empty and only one character
    public Token lexCharacter() {
        //['] ([^'\n\r\\] | escape) [']
        if(peek("'")) {
            match("'");
            if(peek("[^'\n\r]")) {

                if(peek("\\\\")) {
                    lexEscape();
                }
                match("[^'\n\r]");
                if (peek("'")) {
                    match("'");
                } else {
                    throw new ParseException("Character must end with a '", chars.index);
                }
            } else {
                throw new ParseException("Character can't be empty", chars.index);
            }
        } else {
            throw new ParseException("Can't start a character without a ' ", chars.index);
        }
        return chars.emit(Token.Type.CHARACTER);
    }

    //lexes strings according to the language's grammar (must start and end with " other wise throw an error)
    public Token lexString() {
        //'"' ([^"\n\r\\] | escape)* '"'
        if(peek("\"")) {
            match("\"");

            while(peek("[^\"\n\r]")) {
                if (peek("\\\\")) {
                    lexEscape();
                } else {
                    match("[^\"\n\r]");
                }
            }
            if (peek("\"")) {
                match("\"");
            } else {
                throw new ParseException("String must end with a \"", chars.index);
            }

        } else {
            throw new ParseException("Can't start a String without a \" ", chars.index);
        }
        return chars.emit(Token.Type.STRING);
    }

    //lexes all escapes that exist within the program (needs to peek //// because need two to account for the java compiler and the others are for the regex)
    public void lexEscape() {
        //'\' [bnrt'"\\]
        if(peek("\\\\")) {
            match("\\\\");
            if(peek("[bnrt'\"\\\\]")) {
                match("[bnrt'\"\\\\]");
            } else {
                throw new ParseException("Invalid escape", chars.index);
            }
        }

    }

    //lexes all operators in the program that are valid within the language's program
    public Token lexOperator() {
        //[!=] '='? | '&&' | '||' | 'any character'
        if(peek("!")) {
            match("!");
            if(peek("=")) {
                match("=");
            }
        } else if(peek("=")) {
            match("=");
            if(peek("=")) {
                match("=");
            }
        } else if (peek("&")) {
            match("&");
            if(peek("&")) {
                match("&");
            }
        } else if(peek("|")) {
            match("|");
            if(peek("|")) {
                match("|");
            }
        } else {
            match(".");
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    //returns a boolean whether or not the next string sequence inside of the input matches the next input from the current tracker (only changes if manually changed or in the match function)
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if(!chars.has(i) ||
            !String.valueOf(chars.get(i)).matches(patterns[i]) ) {

            return false;
            }
        }
        return true;
    }

    //same functionality as peek but if it returns true then it increments the tracker
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if(peek) {

            for(int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    //char stream class is used to track the length and the tracker that match uses to keep track where the lexer is at in the program amidst its lexing
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
