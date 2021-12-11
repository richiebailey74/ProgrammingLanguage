# ProgrammingLanguage
This project creates a new programming language based off of the Java compiler. It uses Gradle in IntelliJ. It is broken into 5 components (with helper classes) that is designed to take in a typed out program in our langauge and then output code functionality. The project is broken down into a Lexer, Parser, Interpreter, Analyzer, and Generator.

The Lexer lexes a character stream (typed out program in our programming language, following its grammar) and lexes it into Tokens based on the language grammar.
The Parser takes the list of Tokens created by the Parser and then creates meaningful formulations of tokens and inserts it into an abstract syntax tree.
The Analyzer performs data type validation to make sure the data types associated with certain behaviors within our program are valid.
The Interpreter actually evaluates the code and extracts meaning from the code via calculations (executing the code / simplifying it)
The Generator takes the values in the abstract syntax tree and essentially converts from our language to the java language and prints the java language analogous in functionality to this language, this way the Java compiler can actually execute and compile the code.

The project also includes test cases that were written as the project developed to make sure the code was valid and viable and to check to see if an implementation was correct before the entire project was done (essentially process checks).

At any point in the program, if a comment says that a method implementation is according the grammar, then reference the language's grammar below so there is no confusion:  




Programming language grammar (using Regex):

source ::= global* function*

global ::= ( list | mutable | immutable ) ';'
list ::= 'LIST' identifier ':' identifier '=' '[' expression (',' expression)* ']'
mutable ::= 'VAR' identifier ':' identifier ('=' expression)?
immutable ::= 'VAL' identifier ':' identifier '=' expression

function ::= 'FUN' identifier '(' (identifier ':' identifier (',' identifier ':' identifier)* )? ')' (':' identifier)? 'DO' block 'END'

block ::= statement*

statement ::=
    'LET' identifier (':' identifier)? ('=' expression)? ';' |
    'SWITCH' expression ('CASE' expression ':' block)* 'DEFAULT' block 'END' | 
    'IF' expression 'DO' block ('ELSE' block)? 'END' |
    'WHILE' expression 'DO' block 'END' |
    'RETURN' expression ';' |
    expression ('=' expression)? ';'

expression ::= logical_expression

logical_expression ::= comparison_expression (('&&' | '||') comparison_expression)*
comparison_expression ::= additive_expression (('<' | '>' | '==' | '!=') additive_expression)*
additive_expression ::= multiplicative_expression (('+' | '-') multiplicative_expression)*
multiplicative_expression ::= primary_expression (('*' | '/' | '^') primary_expression)*

primary_expression ::=
    'NIL' | 'TRUE' | 'FALSE' |
    integer | decimal | character | string |
    '(' expression ')' |
    identifier ('(' (expression (',' expression)*)? ')')? |
    identifier '[' expression ']'

identifier ::= '@'? [A-Za-z] [A-Za-z0-9_-]*
integer ::= '0' | '-'? [1-9] [0-9]*
decimal ::= '-'? ('0' | [1-9] [0-9]*) '.' [0-9]+
character ::= ['] ([^'\n\r\\] | escape) [']
string ::= '"' ([^"\n\r\\] | escape)* '"'
escape ::= '\' [bnrt'"\\]
operator ::= [!=] '='? | '&&' | '||' | 'any character'

whitespace ::= [ \b\n\r\t]
