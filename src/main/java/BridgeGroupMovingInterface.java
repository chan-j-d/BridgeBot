import java.util.Arrays;
import java.util.HashSet;

public class BridgeGroupMovingInterface extends BridgeGroupInterface {

    public BridgeGroupMovingInterface(long chatId, IOInterface ioInterface) {
        super(chatId, ioInterface);
    }

    @Override
    public void run() {
        if (containsCommandsOr(UpdateType.GAME_START, UpdateType.SEND_BID)) {
            System.out.println("case 1");
            messageId = sendMessage(this.toString());
        } else if (containsCommandsOr(UpdateType.EDIT_BID, UpdateType.BID_END, UpdateType.PARTNER_CARD,
                UpdateType.EDIT_STATE, UpdateType.EDIT_HAND)) {
            if (containsCommandsOr(UpdateType.SEND_UPDATE) && updateId != -1) {
                System.out.println("case 2");
                deleteMessage(messageId);
                editMessage(updateId, this.toString());
                messageId = updateId;
            } else {
                System.out.println("case 3");
                editMessage(messageId, this.toString());
            }
        }

        if (containsCommandsOr(UpdateType.SEND_UPDATE)) {
            updateId = sendMessage(updateMessage);
        }

        if (containsCommandsOr(UpdateType.GAME_END)) {
            sendMessage(endMessage);
        }

        commands = new HashSet<>();
    }

}

