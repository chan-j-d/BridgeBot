import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashSet;

public abstract class IDedIOUserInterface implements ViewerInterface {

    protected IOInterface ioInterface;
    protected long chatId;

    protected HashSet<UpdateType> commands;

    protected static final String SHORT_LINE_BREAK = "\n========================\n";

    public IDedIOUserInterface(long chatId, IOInterface ioInterface) {
        this.ioInterface = ioInterface;
        this.chatId = chatId;
        commands = new HashSet<>();
    }

    protected int sendMessage(String message) {
        return ioInterface.sendMessageToId(chatId, message, "md");
    }

    protected void deleteMessage(int messageId) {
        ioInterface.deleteMessage(chatId, messageId);
    }

    protected void editMessage(int messageId, String newMessage) {
        ioInterface.editMessage(chatId, messageId, newMessage);
    }

    public long getViewerId() {
        return this.chatId;
    }

    /*
    Override this to perform the necessary updates to local variables with each update.
    Default method only adds the update's type to the list of command types before a run() operation is performed
    */
    public void processUpdate(IndexUpdate update) {
        commands.add(update.getUpdateType());
    }

    /*
    Override this to send/edit/delete the necessary messages.
    Default only resets the HashSet.
     */
    public void run() {
        this.commands = new HashSet<>();
    }

    protected boolean containsCommandsOr(UpdateType... updateTypes) {
        for (UpdateType updateType : updateTypes) {
            if (commands.contains(updateType)) return true;
        }
        return false;
    }

    public abstract void resend();

}
