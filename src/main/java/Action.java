import java.util.NoSuchElementException;

public class Action {

    protected CardCollection cardFrom;
    protected CardCollection cardTo;
    protected Card card;
    private boolean used;

    private Action(CardCollection cardFrom, CardCollection cardTo, Card card) {
        this.cardFrom = cardFrom;
        this.cardTo = cardTo;
        this.card = card;
        this.used = false;
    }

    public static Action makeAction(CardCollection cardFrom, CardCollection cardTo, Card card) {
        if (cardFrom.contains(card)) {
            return new Action(cardFrom, cardTo, card);
        } else {
            throw new NoSuchElementException("Does not contain " + card);
        }
    }

    public void run() {
        if (!used) {
            cardFrom.remove(card);
            cardTo.add(card);
            this.used = true;
        } else {
            throw new IllegalStateException("Action has already been used");
        }
    }



}
