public interface ClientEngineMediator {

    public void addGameIds(GameChatIds gameChatIds);
    public boolean containsUserId(long id);
    public void setIOInterface(IOInterface ioInterface);
    public void addEngine(GameEngine engine);
    public void broadcastUpdateFromEngine(GameEngine engine, GameUpdate update);
    public void updateEngine(long chatId, String message);
    public String queryEngine(long chatId, String query);
    public void registerResponse(long chatId, String response);


}
