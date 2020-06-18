public interface IOInterface {

    public int sendMessageToId(long chatId, String message);
    public void editMessage(long chatId, int messageId, String message);
    public void registerResponse(long chatId, String response);
    public void deleteMessage(long chatId, int messageId);

}
