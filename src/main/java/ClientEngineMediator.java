public interface ClientEngineMediator {

    /*
    Game-related commands
     */
    public void addGameIds(GameChatIds gameChatIds);
    public boolean containsUserId(long id);
    public boolean cancelGame(long chatId);
    public void resend(long chatId);
    public void registerResponse(long chatId, String response);
    public boolean queryInProgress(long chatId);

    /*
    Game/Interface-management commands
     */
    public void setIOInterface(IOInterface ioInterface);
    public void broadcastUpdateFromEngine(GameEngine engine, GameUpdate update);
    public GameChatIds getRecentGameChatIds(long groupId);


}
