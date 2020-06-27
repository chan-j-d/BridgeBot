import java.util.List;

public class GameReplayImpl implements GameReplay {

    private int firstPlayer;
    private List<Bid> bidList;
    private Card partnerCard;
    private CardCollection[] tricks;
    private int[] firstPlayerOfTrick;
    private CardCollection[] unplayedCards;

    protected GameReplayImpl(int firstPlayer, List<Bid> bids, Card partnerCard,
             CardCollection[] tricks, int[] firstPlayerOfTrick, CardCollection[] unplayedCards) {
        this.firstPlayer = firstPlayer;
        this.bidList = bids;
        this.partnerCard = partnerCard;
        this.tricks = tricks;
        this.firstPlayerOfTrick = firstPlayerOfTrick;
        this.unplayedCards = unplayedCards;
    }


    public int getFirstPlayer() {
        return firstPlayer;
    }

    public int getNumBids() {
        return bidList.size();
    }

    public Bid getBid(int bidNumber) {
        return bidList.get(bidNumber - 1);
    }

    public Card getPartnerCard() {
        return partnerCard;
    }

    public int getNumTricks() {
        return tricks.length;
    }

    public Card getCardPlayed(int trickNumber, int player) {
        return tricks[trickNumber - 1].get(player - 1);
    }

    public int getTrickWinner(int trickNumber) {
        return firstPlayerOfTrick[trickNumber - 1];
    }

    public CardCollection getUnplayedCards(int player) {
        return unplayedCards[player - 1];
    }

}
