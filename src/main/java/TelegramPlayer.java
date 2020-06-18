public class TelegramPlayer extends Player {

    private static int WAIT_TIME = 50000;

    private boolean awaitingBidResponse;
    private boolean awaitingCardResponse;
    private String response;

    public TelegramPlayer(String name, PlayerState state) {
        super(name, state);
    }

    public synchronized Bid getBid() {
        awaitingBidResponse = true;
        try {
            wait(WAIT_TIME);
        } catch (InterruptedException e) {
            throw new IllegalStateException("System has timed out!");
        }
        return Bid.createBid(response);
    }

    public synchronized Card getPartnerCard() {
        awaitingCardResponse = true;
        try {
            wait(WAIT_TIME);
        } catch (InterruptedException e) {
            throw new IllegalStateException("System has timed out!");
        }
        return Card.createCard(response);
    }

    public synchronized Card getFirstCard(boolean trumpBroken, char trumpSuit) {
        awaitingCardResponse = true;
        try {
            wait(WAIT_TIME);
        } catch (InterruptedException e) {
            throw new IllegalStateException("System has timed out!");
        }
        return Card.createCard(response);
    }

    public synchronized Card getNextCard(char firstSuit, char trumpSuit) {
        awaitingCardResponse = true;
        try {
            wait(WAIT_TIME);
        } catch (InterruptedException e) {
            throw new IllegalStateException("System has timed out!");
        }
        return Card.createCard(response);
    }

    public synchronized void registerResponse(String response) {
        if (awaitingBidResponse && Bid.isValidBidString(response)) {
            this.response = response;
            awaitingBidResponse = false;
            notifyAll();
        } else if (awaitingCardResponse && Card.isValidCardString(response)) {
            this.response = response;
            awaitingCardResponse = false;
            notifyAll();
        }
    }

}
