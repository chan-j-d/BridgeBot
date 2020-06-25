import java.util.Iterator;

public interface GameLogger {

    public void addUpdate(GameUpdate update); //Adds a GameUpdate
    public void addBid(int player, Bid bid); //Adds a bid
    public void addCardPlayed(int player, int turn, Card card); //Adds details of the card played
    public void addPartnerCard(Card card);
    public Iterator<GameUpdate> getUpdateHistory();


}
