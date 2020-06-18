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

    public BridgeGroupInterface(long chatId, IOInterface ioInterface) {
        this.chatId = chatId;
        this.ioInterface = ioInterface;
        this.bidConcluded = false;
        this.updateId = -1;
    }

    public String toString() {
        if (bidConcluded) {
            return String.format("%s\n*Trick Counts*: %s\n*Trick* %s",
                    bidString,
                    gameState == null ? NULL_STRING : gameState,
                    currentTrick == null ? NULL_STRING : currentTrick);
        } else {
            return String.format("*Bid*: %s", bidString == null ? NULL_STRING : bidString);
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
                this.currentTrick = message;
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

    public String processGameStateMessage(String message) {
        return message;
    }

    public long getViewerId() {
        return this.chatId;
    }

    public void resend() {
        ioInterface.editMessage(chatId, messageId, RESEND_EDIT);
        messageId = ioInterface.sendMessageToId(chatId, this.toString());
    }







}
