public class TelegramPlayer extends Player {

    private static int WAIT_TIME = 300000;

    private boolean awaitingBidResponse;
    private boolean awaitingCardResponse;
    private boolean awaitingStringResponse;
    private String response;
    private boolean updated;

    public TelegramPlayer(String name, PlayerState state) {
        super(name, state);
    }

    public synchronized Bid getBid() {
        awaitingBidResponse = true;
        updated = false;
        try {
            wait(WAIT_TIME);
        } catch (InterruptedException e) {
        }
        if (updated) {
            return Bid.createBid(response);
        } else {
            return null;
        }

    }

    public synchronized String getStringResponse() {
        awaitingStringResponse = true;
        updated = false;
        try {
            wait(WAIT_TIME);
        } catch (InterruptedException e) {
        }
        if (updated) {
            return response;
        } else {
            return null;
        }
    }

    public synchronized Card getPartnerCard() {
        awaitingCardResponse = true;
        updated = false;
        try {
            wait(WAIT_TIME);
        } catch (InterruptedException e) {
        }
        if (updated) {
            return Card.createCard(response);
        } else {
            return null;
        }
    }

    public synchronized Card getFirstCard(boolean trumpBroken, char trumpSuit) {
        awaitingCardResponse = true;
        updated = false;
        try {
            wait(WAIT_TIME);
        } catch (InterruptedException e) {
        }
        if (updated) {
            return Card.createCard(response);
        } else {
            return null;
        }
    }

    public synchronized Card getNextCard(char firstSuit, char trumpSuit) {
        awaitingCardResponse = true;
        updated = false;
        try {
            wait(WAIT_TIME);
        } catch (InterruptedException e) {
        }
        if (updated) {
            return Card.createCard(response);
        } else {
            return null;
        }
    }

    public synchronized void registerResponse(String response) {
        if (awaitingBidResponse && Bid.isValidBidString(response)) {
            this.response = response;
            awaitingBidResponse = false;
            updated = true;
            notifyAll();
        } else if (awaitingCardResponse && Card.isValidCardString(response)) {
            this.response = response;
            awaitingCardResponse = false;
            updated = true;
            notifyAll();
        } else if (awaitingStringResponse) {
            this.response = response;
            awaitingStringResponse = false;
            updated = true;
            notifyAll();
        }
    }

}
