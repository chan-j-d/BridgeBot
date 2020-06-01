public abstract class Player {

    abstract Bid getBid();
    abstract Card getPartnerCard();
    abstract Card getNextCard();

    protected String name;
    protected CardCollection hand;

    public Player(String name) {
        this.name = name;
        this.cardsShown = new boolean[52];
    }

    public void setHand(CardCollection hand) {
        this.hand = hand;
    }

    public CardCollection getHand() {
        return this.hand;
    }

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
    }

    public String toString() {
        return this.name;
    }

}
