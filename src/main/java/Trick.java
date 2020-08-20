import java.util.Arrays;
import java.util.Iterator;

public class Trick {

    private int trickNumber;
    private Card[] trick;
    private int[] played;

    public Trick(int trickNumber) {
        this.trickNumber = trickNumber;
        this.trick = new Card[5];
        played = new int[5];
    }

    public void add(int player, Card card) {
        this.trick[player] = card;
        played[player] = 1;
    }

    public int getTrickNumber() {
        return this.trickNumber;
    }

    public Card getCardPlayedBy(int player) {
        return this.trick[player];
    }

    public String toString() {
        StringBuilder string = new StringBuilder();
        boolean first = true;
        for (int i = 1; i <= 4; i++) {
            if (played[i] == 0) continue;
            if (!first) {
                string.append(", ");
            } else first = false;
            string.append(convertIntToOrientation(i) + ": " + trick[i]);
        }
        return string.toString();
    }

    private char convertIntToOrientation(int player) {
        switch (player) {
            case 1:
                return 'N';
            case 2:
                return 'E';
            case 3:
                return 'S';
            case 4:
                return 'W';
            default:
                throw new IllegalArgumentException("Invalid player number!");

        }
    }

    public boolean checkContains(Card card) {
        for (int i = 1; i <= 4; i++) {
            Card trickCard = trick[i];
            if (trickCard.equals(card)) return true;
        }
        return false;
    }

    private int countCardsPlayed() {
        return Arrays.stream(played).sum();
    }

    public boolean checkFull() {
        return countCardsPlayed() == 4;
    }



}
