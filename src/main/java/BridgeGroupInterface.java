import java.util.Arrays;
import java.util.HashSet;

public class BridgeGroupInterface extends IDedIOUserInterface {

    protected int messageId;
    protected int updateId;

    protected String gameStartString;

    protected String bidString;
    protected String currentTrick;

    protected String gameState;

    //TEMPORARY MESSAGES
    protected String updateMessage;
    protected String endMessage;

    protected boolean bidConcluded;

    protected static final String RESEND_EDIT = "(Deleted: Refer to the newest message below for details)";
    protected static final String NULL_STRING = "-";

    protected static final String HEADER = "*GAME FEED*";

    public BridgeGroupInterface(long chatId, IOInterface ioInterface) {
        super(chatId, ioInterface);
        this.bidConcluded = false;
        this.updateId = -1;
        this.gameStartString = "";
    }

    public String toString() {
        if (bidConcluded) {
            return HEADER +
                    SHORT_LINE_BREAK +
                    (gameStartString.equals("") ? "" : gameStartString + SHORT_LINE_BREAK) +
                    "*Bids*: \n" +
                    bidString +
                    SHORT_LINE_BREAK +
                    "*Trick Counts* " + (gameState == null ? NULL_STRING : gameState) +
                    SHORT_LINE_BREAK  +
                    "*Current Trick* " +
                    (currentTrick == null ? NULL_STRING : currentTrick);

        } else {
            return HEADER +
                    SHORT_LINE_BREAK +
                    (gameStartString.equals("") ? "" : gameStartString + SHORT_LINE_BREAK ) +
                    "*Bids*: \n" +
                    (bidString == null ? NULL_STRING : bidString);
        }
    }

    public void processUpdate(IndexUpdate update) {
        super.processUpdate(update);
        UpdateType updateType = update.getUpdateType();
        String message = update.getMessage();
        switch (updateType) {
            case GAME_START:
                this.gameStartString = message;
                break;
            case SEND_UPDATE:
                updateMessage = message;
                break;
            case SEND_BID:
                this.bidString = message;
                break;
            case EDIT_BID:
                this.bidString = processBidStateMessage(message);
                break;
            case BID_END:
                this.bidString = this.bidString + '\n' + message;
                this.bidConcluded = true;
                break;
            case EDIT_HAND:
                this.currentTrick = processCurrentTrickMessage(message);
                break;
            case EDIT_STATE:
                this.gameState = processGameStateMessage(message);
                break;
            case PARTNER_CARD:
                this.bidString = this.bidString + '\n' + message;
                break;
            case GAME_END:
                endMessage = message;
                break;

        }

    }

    public void run() {

        if (containsCommandsOr(UpdateType.GAME_START, UpdateType.SEND_BID)) {
            messageId = sendMessage(this.toString());
        } else if (containsCommandsOr(UpdateType.EDIT_BID, UpdateType.BID_END, UpdateType.PARTNER_CARD,
                UpdateType.EDIT_STATE, UpdateType.EDIT_HAND)) {
            editMessage(messageId, this.toString());
        }

        if (containsCommandsOr(UpdateType.SEND_UPDATE)) {
            if (updateId != -1) {
                deleteMessage(updateId);
            }
            updateId = sendMessage(updateMessage);
        }

        if (containsCommandsOr(UpdateType.GAME_END)) {
            sendMessage(endMessage);
        }

        super.run();
    }


    private static String processGameStateMessage(String message) {
        String[] trickCounts = message.split(", ");
        return "\n - " + String.join("\n - ", trickCounts);

    }

    private static String processBidStateMessage(String message) {
        if (message.equals(IndexUpdateGenerator.createNoBidEdit().getMessage())) return message;
        StringBuilder finalString = new StringBuilder();
        finalString.append("```");
        finalString.append("  N  |  E  |  S  |  W  ");
        int count = 0;
        for (String bid : message.split(", ")) {
            if (count++ % 4 == 0) {
                finalString.append('\n');
            } else {
                finalString.append("|");
            }

            if (bid.equals("null")) {
                finalString.append("  -  ");
            } else if (bid.equals(Bid.createPassBid().toString())) {
                finalString.append("  P  ");
            } else {
                finalString.append(" " + bid + " ");
            }
        }

        for (; count % 4 != 0; count++) {
            finalString.append("|     ");
        }
        finalString.append("```");

        return finalString.toString();

    }

    public void resend() {
        editMessage(messageId, RESEND_EDIT);
        messageId = sendMessage(this.toString());
    }


    private static String processCurrentTrickMessage(String message) {
        String[] temp = message.split(": ");

        if (temp.length == 1) {
            return temp[0] + ":\n{  }";
        }

        String turn = temp[0];
        String cards = temp[1];

        String[] cardsArray = cards.split(", ");

        StringBuilder finalString = new StringBuilder();
        finalString.append("*" + turn + "*: \n");
        finalString.append("{  ");
        boolean first = true;
        for (String card : cardsArray) {
            if (first) {
                first = false;
            } else {
                finalString.append(", ");
            }
            finalString.append("\\[ " + card + "  ]");
        }
        finalString.append("  }");
        return finalString.toString();
    }



    public static void main(String[] args) {
        String testString = "1: SA, HA";
        System.out.println(processCurrentTrickMessage(testString));
    }


}
