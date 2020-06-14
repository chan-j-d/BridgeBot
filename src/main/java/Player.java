public abstract class Player {

    String name;
    PlayerState playerState;

    public Player(String name, PlayerState playerState) {
        this.name = name;
        this.playerState = playerState;
    }

    public String getName() { return this.name; }
    public CardCollection getHand() {
        return this.playerState.getHand();
    }

    abstract Bid getBid();
    abstract Card getPartnerCard();
    abstract Card getFirstCard(boolean trumpBroken, char trumpSuit);
    abstract Card getNextCard(char firstSuit, char trumpSuit);

}
