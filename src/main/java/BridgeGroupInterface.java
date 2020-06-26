import java.util.Arrays;

public class BridgeGroupInterface implements ViewerInterface {

    private IOInterface ioInterface;
    private long chatId;

    private int messageId;
    private int updateId;

    private String bidString;
    private String currentTrick;

    private String gameState;

    private boolean bidConcluded;

    private static final String RESEND_EDIT = "(Deleted: Refer to the newest message below for details)";
    private static final String NULL_STRING = "-";

    private static final String HEADER = "*GAME FEED*";
    private static final String SHORT_LINE_BREAK = "--------------------------------------------";
    private static final String LONG_LINE_BREAK = SHORT_LINE_BREAK + SHORT_LINE_BREAK;

    public BridgeGroupInterface(long chatId, IOInterface ioInterface) {
        this.chatId = chatId;
        this.ioInterface = ioInterface;
        this.bidConcluded = false;
        this.updateId = -1;
    }

    public String toString() {
        if (bidConcluded) {
            return String.format("%s\n%s\n%s\n*Trick Counts*: %s\n\n*Trick* %s",
                    HEADER + "\n" + LONG_LINE_BREAK,
                    bidString,
                    LONG_LINE_BREAK,
                    gameState == null ? NULL_STRING : gameState,
                    currentTrick == null ? NULL_STRING : currentTrick);
        } else {
            return String.format(HEADER + "\n" + SHORT_LINE_BREAK +
                    "\n*Bid*: %s", bidString == null ? NULL_STRING : bidString);
        }
    }

    public void processUpdate(IndexUpdate update) {
        UpdateType updateType = update.getUpdateType();
        String message = update.getMessage();
        switch (updateType) {
            case SEND_UPDATE:
                if (updateId != -1) {
                    ioInterface.deleteMessage(chatId, updateId);
                }
                updateId = ioInterface.sendMessageToId(chatId, message);
                break;
            case SEND_BID:
                this.bidString = message;
                messageId = ioInterface.sendMessageToId(chatId, this.toString());
            case EDIT_BID:
                this.bidString = message;
                ioInterface.editMessage(chatId, messageId, this.toString());
                break;
            case BID_END:
                this.bidString = message;
                this.bidConcluded = true;
                ioInterface.editMessage(chatId, messageId, this.toString());
                break;
            case EDIT_HAND:
                this.currentTrick = processCurrentTrickMessage(message);
                ioInterface.editMessage(chatId, messageId, this.toString());
                break;
            case EDIT_STATE:
                this.gameState = processGameStateMessage(message);
                ioInterface.editMessage(chatId, messageId, this.toString());
                break;
            case GAME_END:
                ioInterface.sendMessageToId(chatId, message);
                break;

        }

    }

    private static String processGameStateMessage(String message) {

        String[] trickCounts = message.split(", ");
        return "\n - " + String.join("\n - ", trickCounts);

    }

    public long getViewerId() {
        return this.chatId;
    }

    public void resend() {
        ioInterface.editMessage(chatId, messageId, RESEND_EDIT);
        messageId = ioInterface.sendMessageToId(chatId, this.toString());
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
        finalString.append("*" + turn + "*: ");
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
