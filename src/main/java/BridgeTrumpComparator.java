import java.util.Comparator;

/*
Comparator for determining who wins each set of play. Requires input of the trumpSuit and firstSuit.
If bid is NT, input trumpSuit as firstSuit.
 */
public class BridgeTrumpComparator implements Comparator<Card> {

    char trumpSuit;
    char firstSuit;

    public BridgeTrumpComparator(char trumpSuit, char firstSuit) {
        this.trumpSuit = trumpSuit;
        this.firstSuit = firstSuit;
    }

    public int compare(Card c1, Card c2) {
        if (c1.symbol == c2.symbol && c1.symbol == this.trumpSuit) {
            return compareSameSuit(c1, c2);
        } else if (c1.symbol == this.trumpSuit || c2.symbol == this.trumpSuit) {
            return c1.symbol == this.trumpSuit ? 1 : -1;
        } else if (c1.symbol == this.firstSuit && c2.symbol == this.firstSuit) {
            return compareSameSuit(c1, c2);
        } else if (c1.symbol == this.firstSuit || c2.symbol == this.firstSuit) {
            return c1.symbol == this.firstSuit ? 1 : -1;
        } else {
            return 0;
        }
    }

    private int compareSameSuit(Card c1, Card c2) {
        if (c1.number == 1) {
            return 1;
        } else if (c2.number == 1) {
            return -1;
        } else {
            return c1.number - c2.number;
        }
    }

}
