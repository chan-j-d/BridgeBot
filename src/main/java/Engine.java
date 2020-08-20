public interface Engine {

    public long getChatId();
    public GameStatus getGameStatus();
    public boolean gameInProgress();
    public boolean biddingInProgress();
    public GameUpdate processPlay(Bid bid);
    public GameUpdate processPlay(Card card);
    public String queryEngine(String query);

}
