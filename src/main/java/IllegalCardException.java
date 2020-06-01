public class IllegalCardException extends IllegalArgumentException {

    private String cardString;

    public IllegalCardException(String cardString) {
        this.cardString = cardString;
    }

    public String toString() {
        return "\"" + cardString + "\" is an invalid card!";
    }

}
