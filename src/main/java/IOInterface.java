public interface IOInterface {

    public int sendMessageToId(long chatId, String message);
    public void editMessage(long chatId, int messageId, String message);
    public int sendMessageWithButtons(long chatId, String message, String[][] buttons);
    public void editMessageButtons(long chatId, int messageId, String[][] buttons);
    public void deleteMessage(long chatId, int messageId);

}
