import java.util.ArrayList;

public class PlayerState {

    protected CardCollection hand;
    protected ArrayList<Trick> tricks;

    public PlayerState() {
        this.tricks = new ArrayList<>();
    }

    public void addTrick(Trick trick) {
        this.tricks.add(trick);
    }

    public int countTricks() {
        return this.tricks.size();
    }

    public void setHand(CardCollection hand) {
        this.hand = hand;
    }

    public Card playCard(Card card) {
        if (!hand.contains(card)) {
            throw new IllegalArgumentException("Your hand does not contain the following card: " + card);
        }
        this.hand.remove(card);
        return card;
    }

    public boolean containsCard(Card card) {
        return this.hand.contains(card);
    }

    public boolean containsSuit(char suit) {
        for (Card c : hand) {
            if (c.getSuit() == suit) {
                return true;
            }
        }
        return false;
    }

    public boolean containsOnlySuit(char suit) {
        for (Card c : hand) {
            if (c.getSuit() != suit) {
                return false;
            }
        }
        return true;
    }

    public CardCollection getHand() {
        return this.hand;
    }
/*
    protected boolean[] cardsShown;

    public void updateInfo(CardCollection update) {
        for (Card c : update) {
            this.cardsShown[cardToIndex(c)] = true;
        }
    }

    private int cardToIndex(Card c) {
        int multiplier = -1;
        switch (c.symbol) {
            case 'S':
                multiplier = 3;
                break;
            case 'H':
                multiplier = 2;
                break;
            case 'D':
                multiplier = 1;
                break;
            case 'C':
                multiplier = 0;
        }
        return multiplier * 13 + c.number - 1;
    }*/

}
