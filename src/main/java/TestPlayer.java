import java.util.Scanner;

public class TestPlayer extends Player {

    private Scanner s = new Scanner(System.in);

    public TestPlayer(String name) {
        super(name);
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

    Card getNextCard() {
        return this.hand.get(0);
    }

}
