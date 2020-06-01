import java.util.Comparator;

/*
Standard comparator for cards. Used primarily for sorting cards in the hand.
Not used for standard gameplay. See BridgeTrumpComparator instead.
 */
public class BridgeStandardComparator implements Comparator<Card> {

    public int compare(Card c1, Card c2) {
        if (c1.symbol > c2.symbol) {
            return 1;
        } else if (c1.symbol < c2.symbol) {
            return -1;
        } else if (c1.number == c2.number) {
            return 0;
        } else if (c1.number == 1 || c2.number == 1) {
            return c1.number == 1 ? 1 : -1;
        } else {
            return c1.number - c2.number;
        }
    }

}
