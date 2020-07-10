import java.util.Iterator;

public interface GameLogger {

    public void addUpdate(GameUpdate update); //Adds a GameUpdate
    public void addHands(CardCollection[] hands);
    public void addBid(int player, Bid bid); //Adds a bid
    public void addCardPlayed(int player, int turn, Card card); //Adds details of the card played
    public void addPartnerCard(Card card);
    public void setLastTrickWinner(int player);
    public void addCardsNotPlayed(int player, CardCollection cards);
    public Iterator<GameUpdate> getUpdateHistory();

    public GameReplay getGameReplay();
    public long getGameId();


}
