import java.util.Arrays;
import java.util.List;

public class BridgeUserInterface implements ViewerInterface {

    private IOInterface ioInterface;
    private long playerId;
    private int messageId;
    private int requestId;
    private int updateId;
    private int errorId;

    private static final int NUM_HIGHER_BIDS = 5;

    private String[][] hand;

    private boolean errorShown;
    private String request;

    private boolean requesting;

    private static final String RESEND_EDIT = "(Deleted: Refer to the newest message below for details)";
    private static final String NULL_STRING = "-";

    public BridgeUserInterface(long playerId, IOInterface ioInterface) {
        this.playerId = playerId;
        this.ioInterface = ioInterface;
        this.updateId = -1;
    }

    public String toString() {
        return "*Hands*: ";
    }

    public void processUpdate(IndexUpdate update) {
        UpdateType updateType = update.getUpdateType();
        String message = update.getMessage();
        if (requesting) {
            this.requesting = false;
            ioInterface.deleteMessage(playerId, requestId);
        }
        if (errorShown) {
            this.errorShown = false;
            ioInterface.deleteMessage(playerId, errorId);
        }
        switch (updateType) {
            case SEND_HAND:
                this.hand = processHand(message);
                messageId = ioInterface.sendMessageWithButtons(playerId, toString(), this.hand);
                break;
            case EDIT_HAND:
                this.hand = processHand(message);
                ioInterface.editMessageButtons(playerId, messageId, this.hand);
                break;
            case SEND_UPDATE:
                if (updateId != -1) {
                    ioInterface.deleteMessage(playerId, updateId);
                }
                updateId = ioInterface.sendMessageToId(playerId, message);
                break;
            case SEND_REQUEST:
                requesting = true;
                request = message;
                requestId = ioInterface.sendMessageToId(playerId, "*Request*: " + message);
                break;
            case SEND_BID_REQUEST:
                requesting = true;
                request = message;
                requestId = ioInterface.sendMessageWithButtons(playerId,
                        "*Request*: " + message,
                        createBidOffers(update));
                break;
            case ERROR:
                errorShown = true;
                errorId = ioInterface.sendMessageToId(playerId, "*Error*: " + message);
                requesting = true;
                requestId = ioInterface.sendMessageToId(playerId, "*Request*: " + request);
                break;
        }


    }

    public String[][] processHand(String hand) {
        String[] cards = hand.split(",[ ?]");
        String[][] cardArray;
        if (cards.length > 10) {
            cardArray = new String[3][];
        } else if (cards.length > 5) {
            cardArray = new String[2][];
        } else {
            cardArray = new String[1][];
        }

        int row = 0;
        int column = 0;
        String[] cardRow = new String[5];
        for (String card : cards) {
            if (column == 5) {
                column = 0;
                cardArray[row++] = cardRow;
                cardRow = new String[5];
            }
            cardRow[column++] = card;
        }

        cardArray[row] = cardRow;

        return cardArray;

    }

    private String[][] createBidOffers(IndexUpdate update) {
        Bid prevHighestBid = getPrevHighestBidFromUpdate(update);

        String[] array = new String[NUM_HIGHER_BIDS];
        for (int i = 0; i < NUM_HIGHER_BIDS; i++) {
            prevHighestBid = Bid.nextHigherBid(prevHighestBid);
            array[i] = prevHighestBid.toString();
        }

        return new String[][] {array, new String[] {"pass"}};

    }

    private Bid getPrevHighestBidFromUpdate(IndexUpdate update) {
        String string = update.getMessage();
        if (!string.contains("Previous highest")) {
            return Bid.createPassBid();
        } else {
            int stringLength = string.length();
            String bid = string.substring(stringLength - 2);
            if (bid.toUpperCase().charAt(0) == 'N') {
                bid = string.substring(stringLength - 3);
            }
            return Bid.createBid(bid);
        }
    }

    public long getViewerId() {
        return this.playerId;
    }

    public void resend() {
        ioInterface.editMessage(playerId, messageId, RESEND_EDIT);
        messageId = ioInterface.sendMessageToId(playerId, this.toString());
    }







}
