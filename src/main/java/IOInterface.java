public interface IOInterface {

    public int sendMessageToId(long chatId, String message);
    public void registerResponse(long chatId, String response);

}
