public interface GameReplay {

    public int getFirstPlayer();

    public int getNumBids();
    public Bid getBid(int bidNumber);;

    public Card getPartnerCard();

    public int getNumTricks();
    public Card getCardPlayed(int trickNumber, int player);
    public int getTrickWinner(int trickNumber);
    public CardCollection getUnplayedCards(int player);


}
