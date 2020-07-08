import java.util.Arrays;

public class BridgeGroupInterface implements ViewerInterface {

    private IOInterface ioInterface;
    private long chatId;

    private int messageId;
    private int updateId;

    private String gameStartString;

    private String bidString;
    private String currentTrick;

    private String gameState;

    private boolean bidConcluded;

    private static final String RESEND_EDIT = "(Deleted: Refer to the newest message below for details)";
    private static final String NULL_STRING = "-";

    private static final String HEADER = "*GAME FEED*";
    private static final String SHORT_LINE_BREAK = "========================";

    public BridgeGroupInterface(long chatId, IOInterface ioInterface) {
        this.chatId = chatId;
        this.ioInterface = ioInterface;
        this.bidConcluded = false;
        this.updateId = -1;
        this.gameStartString = "";
    }

    public String toString() {
        if (bidConcluded) {
            return HEADER + '\n' +
                    SHORT_LINE_BREAK + '\n' +
                    (gameStartString.equals("") ? "" : gameStartString + '\n' + SHORT_LINE_BREAK + '\n') +
                    "*Bids*: \n" +
                    bidString + '\n' +
                    SHORT_LINE_BREAK + '\n' +
                    "*Trick Counts* " + (gameState == null ? NULL_STRING : gameState) + '\n' +
                    SHORT_LINE_BREAK + '\n' +
                    "*Current Trick*: \n" +
                    (currentTrick == null ? NULL_STRING : currentTrick);

        } else {
            return HEADER + '\n' +
                    SHORT_LINE_BREAK + '\n' +
                    (gameStartString.equals("") ? "" : gameStartString + '\n' + SHORT_LINE_BREAK + '\n') +
                    "*Bids*: \n" +
                    (bidString == null ? NULL_STRING : bidString);
        }
    }

    public void processUpdate(IndexUpdate update) {
        UpdateType updateType = update.getUpdateType();
        String message = update.getMessage();
        switch (updateType) {
            case GAME_START:
                this.gameStartString = message;
                break;
            case SEND_UPDATE:
                if (updateId != -1) {
                    deleteMessage(updateId);
                }
                updateId = sendMessage(message);
                break;
            case SEND_BID:
                this.bidString = message;
                messageId = sendMessage(this.toString());
                break;
            case EDIT_BID:
                this.bidString = processBidStateMessage(message);
                editMessage(messageId, this.toString());
                break;
            case BID_END:
                this.bidString = this.bidString + '\n' + message;
                this.bidConcluded = true;
                editMessage(messageId, this.toString());
                break;
            case EDIT_HAND:
                this.currentTrick = processCurrentTrickMessage(message);
                editMessage(messageId, this.toString());
                break;
            case EDIT_STATE:
                this.gameState = processGameStateMessage(message);
                editMessage(messageId, this.toString());
                break;
            case PARTNER_CARD:
                this.bidString = this.bidString + '\n' + message;
                editMessage(messageId, this.toString());
                break;
            case GAME_END:
                sendMessage(message);
                break;

        }

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

    public long getViewerId() {
        return this.chatId;
    }

    public void resend() {
        ioInterface.editMessage(chatId, messageId, RESEND_EDIT);
        messageId = ioInterface.sendMessageToId(chatId, this.toString(), "md");
    }


    private static String processCurrentTrickMessage(String message) {
        String[] temp = message.split(": ");

        if (temp.length == 1) {
            return temp[0] + ": {  }";
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

    private int sendMessage(String message) {
        return ioInterface.sendMessageToId(chatId, message, "md");
    }

    private void deleteMessage(int messageId) {
        ioInterface.deleteMessage(chatId, messageId);
    }

    private void editMessage(int messageId, String newMessage) {
        ioInterface.editMessage(chatId, messageId, newMessage);
    }





}
