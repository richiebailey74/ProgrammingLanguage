package plc.project;

//ParseException is a class that extends the runtime exception functionality that allows for a special error and message to be thrown when the parser should throw an error
public final class ParseException extends RuntimeException {

    private final int index;

    public ParseException(String message, int index) {
        super(message);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

}
