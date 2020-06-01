import org.apache.commons.codec.language.MatchRatingApproachEncoder;

import java.util.Arrays;

/*
Encapsulates the functionality of a card represented by a symbol and a number. 'S' for Spades, 'H' for Hearts,
'D' for Diamonds, 'C' for Clubs. Program accepts lower and uppercase letters 'A', 'K', 'Q' and 'J' and their number
equivalents 1, 11, 12, 13.
 */
class Card {

    protected char symbol;
    protected int number;

    private Card(char symbol, int number) {
        this.symbol = symbol;
        this.number = number;
    }

    public String toString() {
        switch (this.number) {
            case 1:
                return symbol + ": A";
            case 11:
                return symbol + ": J";
            case 12:
                return symbol + ": Q";
            case 13:
                return symbol + ": K";
            default:
                return symbol + ": " + this.number;
        }
    }

    public char getSuit() {
        return this.symbol;
    }

    public int getNumber() {
        return this.number;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Card) {
            Card c = (Card) o;
            return c.symbol == this.symbol && c.number == this.number;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Arrays.hashCode(new int[] {(int) this.symbol, this.number});
    }

    public static Card createCard(String cardString) {
        char symbol = Character.toUpperCase(cardString.charAt(0));
        char secondSymbol = Character.toUpperCase(cardString.charAt(1));
        String remainderString = cardString.substring(1);
        int number;
        if (cardString.length() == 0 || cardString.length() > 3){
            throw new IllegalCardException(cardString);
        } else if (symbol != 'S' && symbol != 'C' && symbol != 'D' && symbol != 'H') {
            throw new IllegalCardException(cardString);
        } else if (cardString.length() == 2 && (secondSymbol == 'A' || secondSymbol == 'J' ||
                secondSymbol == 'Q' || secondSymbol == 'K')) {
            switch (secondSymbol) {
                case 'A':
                    secondSymbol = 1;
                    break;
                case 'K':
                    secondSymbol = 13;
                    break;
                case 'Q':
                    secondSymbol = 12;
                    break;
                case 'J':
                    secondSymbol = 11;
                    break;
            }
            return new Card(symbol, secondSymbol);
        } else if (!(Character.isDigit(secondSymbol) &&
                (cardString.length() != 3 || Character.isDigit(cardString.charAt(2))))) {
            throw new IllegalCardException(cardString);
        } else if ((number = Integer.parseInt(remainderString)) > 13 || number < 1) {
            throw new IllegalCardException(cardString);
        } else {
            return new Card(symbol, number);
        }
    }

    public static void main(String[] args) {
        Card card;
        //length 2 test
        for (int i = 0; i < 128; i++) {
            for (int j = 0; j < 128; j++) {
                try {
                    card = Card.createCard(((char) i) + "" + ((char) j));
                    System.out.println(card);
                } catch (IllegalCardException e) {
                    System.err.println(e);
                }
            }
        }
        //length 3 test
        for (int i = 0; i < 128; i++) {
            for (int j = 0; j < 128; j++) {
                for (int k = 0; k < 128; k++) {
                    try {
                        card = Card.createCard(((char) i) + "" + ((char) j) + ((char) k));
                        System.out.println(card);
                    } catch (IllegalCardException e) {
                        System.err.println(e);
                    }
                }
            }
        }
    }

}
