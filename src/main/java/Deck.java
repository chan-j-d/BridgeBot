import java.util.Arrays;

/*
Encapsulates a deck of cards. Use init() to create a shuffled deck of cards. Repeatedly call draw() for the next
card at the top of the deck. Will throw an IllegalStateException if the deck is already empty and draw() is called.
 */
class Deck {

    protected Card[] deck;
    protected int index;

    private Deck() {
        this.deck = new Card[52];
        char[] suits = new char[] {'S', 'H', 'C', 'D'};
        int index = 0;
        for (char c : suits) {
            for (int i = 1; i <= 13; i++) {
                this.deck[index++] = Card.createCard(c + "" + i);
            }
        }
        this.index = 0;
    }

    //Knuth shuffle
    private Deck shuffle() {
        int newIndex;
        Card tempCard;
        for (int i = 1; i < 52; i++) {
            newIndex = randomBelow(i);
            tempCard = this.deck[newIndex];
            this.deck[newIndex] = this.deck[i];
            this.deck[i] = tempCard;
        }
        return this;
    }

    private int randomBelow(int n) {
        return (int) (Math.random() * (n + 1));
    }

    public static Deck init() {
        return new Deck().shuffle();
    }

    public static Deck initWithoutShuffling() {
        return new Deck();
    }

    public void printInOrder() {
        for (Card c : this.deck) {
            System.out.println(c);
        }
    }

    public String toString() {
        return Arrays.toString(this.deck);
    }

    public Card draw() {
        if (index <= 51) {
            return this.deck[this.index++];
        } else {
            throw new IllegalStateException("Deck is empty!");
        }
    }

    public boolean isEmpty() {
        return index >= 52;
    }

    public static void main(String[] args) {
        //Deck.init().printInOrder();
        Deck deck = Deck.init();
        for (int i = 0; i < 53; i++) {
            System.out.println(i + ": " + deck.draw());
        }
    }

}
