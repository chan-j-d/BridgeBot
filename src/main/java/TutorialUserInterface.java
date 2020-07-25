public class TutorialUserInterface extends BridgeUserInterface {

    private int tutorialId;
    private String tutorialString;

    public TutorialUserInterface(long chatId, IOInterface ioInterface) {
        super(chatId, ioInterface);
        tutorialId = -1;
    }

    @Override
    public void processUpdate(IndexUpdate update) {
        super.processUpdate(update);
        UpdateType updateType = update.getUpdateType();
        if (updateType == UpdateType.TUTORIAL) {
            tutorialString = update.getMessage();
        }
    }

    @Override
    public void run() {
        if (commands.contains(UpdateType.TUTORIAL)) {
            if (tutorialId == -1) {
                tutorialId = ioInterface.sendMessageToId(chatId, tutorialString, "md");
            } else {
                ioInterface.editMessage(chatId, tutorialId, tutorialString);
            }
        }
        super.run();
    }

    @Override
    public void resend() {
        super.resend();
        if (tutorialId != -1) {
            ioInterface.deleteMessage(chatId, tutorialId);
            tutorialId = ioInterface.sendMessageToId(chatId, tutorialString, "md");
        }

    }

}
