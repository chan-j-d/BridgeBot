import java.util.Scanner;

public class TestPlayer extends Player {

    private Scanner s = new Scanner(System.in);

    public TestPlayer(String name, PlayerState playerState) {
        super(name, playerState);
    }

    Bid getBid() {
        String bid = s.next();
        return Bid.createBid(bid);
    }

    Card getPartnerCard() {
        Deck newDeck = Deck.init();
        Card partnerCard = newDeck.draw();
        while (this.getHand().contains(partnerCard)){
            partnerCard = newDeck.draw();
        }
        return partnerCard;
    }

    Card getFirstCard(boolean trumpBroken, char trumpSuit) {
        if (!trumpBroken) {
            for (Card card : this.getHand()) {
                if (card.getSuit() != trumpSuit) {
                    return card;
                }
            }
        }
        return this.getHand().get(0);
    }

    Card getNextCard(char firstSuit, char trumpSuit) {
        for (Card card : this.getHand()) {
            if (card.getSuit() == firstSuit) {
                return card;
            }
        }
        return this.getHand().get(0);
    }

    void registerResponse(String response) {
        return;
    }

}
