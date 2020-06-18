public class BridgeUserInterface implements ViewerInterface {

    private IOInterface ioInterface;
    private long playerId;
    private int messageId;
    private int requestId;
    private int updateId;
    private int errorId;

    private String hand;

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
        return String.format("*Hand*: %s", this.hand);
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
                this.hand = message;
                messageId = ioInterface.sendMessageToId(playerId, this.toString());
                break;
            case EDIT_HAND:
                this.hand = message;
                ioInterface.editMessage(playerId, messageId, this.toString());
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
            case ERROR:
                errorShown = true;
                errorId = ioInterface.sendMessageToId(playerId, "*Error*: " + message);
                requesting = true;
                requestId = ioInterface.sendMessageToId(playerId, "*Request*: " + request);
                break;
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
