import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

    private String updateString;
    private String errorMessage;

    private boolean requesting;

    private HashSet<UpdateType> commands;

    private static final String RESEND_EDIT = "(Deleted: Refer to the newest message below for details)";
    private static final String NULL_STRING = "-";

    public BridgeUserInterface(long playerId, IOInterface ioInterface) {
        this.playerId = playerId;
        this.ioInterface = ioInterface;
        this.updateId = -1;
        commands = new HashSet<>();
    }

    public String toString() {
        return "*Hands*: ";
    }

    public void processUpdate(IndexUpdate update) {
        UpdateType updateType = update.getUpdateType();
        commands.add(updateType);
        String message = update.getMessage();
        switch (updateType) {
            case SEND_HAND:
            case EDIT_HAND:
                this.hand = processHand(message);
                break;
            case SEND_UPDATE:
                updateString = message;
                break;
            case SEND_REQUEST:
            case SEND_BID_REQUEST:
                request = message;
                break;
            case ERROR:
                errorMessage = message;
                break;
        }

    }

    public void run() {
        if (requesting) {
            this.requesting = false;
            deleteMessage(requestId);
        }
        if (errorShown) {
            this.errorShown = false;
            deleteMessage(errorId);
        }

        if (commands.contains(UpdateType.SEND_HAND)) {
            messageId = ioInterface.sendMessageWithButtons(playerId, toString(), this.hand);
        } else if (commands.contains(UpdateType.EDIT_HAND)) {
            ioInterface.editMessageButtons(playerId, messageId, this.hand);
        }

        if (commands.contains(UpdateType.SEND_UPDATE)) {
            if (updateId != -1) {
                deleteMessage(updateId);
            }
            updateId = sendMessage(updateString);
        }

        if (commands.contains(UpdateType.ERROR)) {
            errorShown = true;
            errorId = sendMessage("*Error*: " + errorMessage);
            requesting = true;
            requestId = sendMessage("*Request*: " + request);
        }

        if (commands.contains(UpdateType.SEND_REQUEST)) {
            requesting = true;
            requestId = sendMessage("*Request*: " + request);
        }

        if (commands.contains(UpdateType.SEND_BID_REQUEST)) {
            requesting = true;
            requestId = ioInterface.sendMessageWithButtons(playerId,
                    "*Request*: " + request,
                    createBidOffers(request));
        }

        commands = new HashSet<>();

    }

    private boolean commandsContainsOr(UpdateType... updateTypes) {
        for (UpdateType updateType : updateTypes) {
            if (commands.contains(updateType)) return true;
        }
        return false;
    }

    //Creates a 3 x 5 grid (or 2 x 5 / 1 x 5 f for fewer cards) of the cards arranged in grid order
    private static String[][] processHand2(String hand) {
        if (hand.equals("")) {
            return null;
        }

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

    //creates a vertical hand for each suit. empty suits will be given a blank column.
    private static String[][] processHand(String hand) {
        System.out.println(hand);
        if (hand.equals("")) return null;

        String[] cards = hand.split(",[ ?]");
        List<List<String>> listOfSuitCards = new ArrayList<>();
        List<String> currentSuitCards = new ArrayList<>();

        int index = 0;
        int longestSuit = 0;

        for (char suit : new char[] {'C', 'D', 'H', 'S'}) {
            while (index < cards.length) {
                Card currentCard = Card.createCard(cards[index]);
                if (currentCard.getSuit() != suit) {
                    break;
                } else {
                    if (currentSuitCards == null) currentSuitCards = new ArrayList<>();
                    currentSuitCards.add(cards[index++]);
                }
            }
            if (currentSuitCards != null) {
                listOfSuitCards.add(currentSuitCards);
                if (currentSuitCards.size() > longestSuit) longestSuit = currentSuitCards.size();
                currentSuitCards = null;
            }
        }

        System.out.println(listOfSuitCards);
        String[][] finalArray = new String[longestSuit][];
        for (int i = 0; i < longestSuit; i++) {
            for (int j = 0; j < listOfSuitCards.size(); j++) {
                if (j == 0) finalArray[i] = new String[listOfSuitCards.size()];
                currentSuitCards = listOfSuitCards.get(j);
                int size = currentSuitCards.size();
                if (i < size) finalArray[i][j] = currentSuitCards.get(size - 1 - i);
            }
        }

        return finalArray;

    }

    private String[][] createBidOffers(String string) {
        Bid prevHighestBid = getPrevHighestBidFromUpdate(string);

        String[] array = new String[NUM_HIGHER_BIDS];
        for (int i = 0; i < NUM_HIGHER_BIDS; i++) {
            try {
                prevHighestBid = Bid.nextHigherBid(prevHighestBid);
            } catch (IllegalStateException e) {
                if (i == 0) {
                    return new String[][] {new String[] {"pass"}};
                }
                String[] newArray = new String[i];
                for (int j = 0; j < i; j++) {
                    newArray[j] = array[j];
                }
                return new String[][] {newArray, new String[] {"pass"}};
            }
            array[i] = prevHighestBid.toString();
        }

        return new String[][] {array, new String[] {"pass"}};

    }

    private Bid getPrevHighestBidFromUpdate(String string) {
        if (!string.contains("Previous")) {
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
        deleteMessage(messageId);
        messageId = ioInterface.sendMessageWithButtons(playerId, toString(), this.hand);
        if (requesting) {
            deleteMessage(requestId);
            requestId = sendMessage(request);
        }
    }

    private int sendMessage(String message) {
        return ioInterface.sendMessageToId(playerId, message, "md");
    }

    private void editMessage(int messageId, String message) {
        ioInterface.editMessage(playerId, messageId, message);
    }

    private void deleteMessage(int messageId) {
        ioInterface.deleteMessage(playerId, messageId);
    }


    public static void main(String[] args) {
        for (int i = 0; i < 1000; i++) {
            for (CardCollection hand : Deck.distributeHands(8)) {
                hand.sort(new BridgeStandardComparator());
                System.out.println(hand);
                Arrays.stream(processHand(hand.toString()))
                        .forEach(a -> System.out.println(Arrays.toString(a)));
            }
        }
        processHand("");
    }


}
