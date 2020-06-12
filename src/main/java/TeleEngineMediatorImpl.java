import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class TeleEngineMediatorImpl implements ClientEngineMediator {

    private HashMap<Long, GameChatIds> gameIds;
    private HashMap<Long, GameEngine> gameEngines;
    private HashMap<Long, Long> playerToGroupIds;

    /*important to note here. In the ArrayList for the group:
        index 0: current trick
        index 1: player update
    In the ArrayList for players,
        index 0: player hand
        index 1: player individual update
        index 2: invalid submission
    */
    private HashMap<Long, ArrayList<Integer>> chatToMessageIds;

    private IOInterface ioInterface;
    private HashMap<Long, TelegramPlayer> chatIdToPlayerMap;

    public TeleEngineMediatorImpl() {
        this.gameIds = new HashMap<>();
        this.gameEngines = new HashMap<>();
        this.playerToGroupIds = new HashMap<>();
        this.chatIdToPlayerMap = new HashMap<>();
    }

    @Override
    public void addGameIds(GameChatIds chatIds) {
        gameIds.put(chatIds.getChatId(0), chatIds);
        long groupId = chatIds.getChatId(0);
        GameEngine gameEngine = GameEngine.init(groupId);
        for (int i = 1; i < 5; i++) {
            long playerId = chatIds.getChatId(i);
            playerToGroupIds.put(playerId, groupId);
            chatIdToPlayerMap.put(playerId, new TelegramPlayer(chatIds.getName(i), gameEngine.getPlayerState(i)));
        }
        gameEngines.put(groupId, gameEngine);
        GameUpdate firstUpdate = gameEngine.startBid();
        broadcastUpdateFromEngine(gameEngine, firstUpdate);
        CompletableFuture.runAsync(() -> runGame(groupId, firstUpdate));
    }

    private void runGame(long chatId, GameUpdate firstUpdate) {
        GameEngine gameEngine = gameEngines.get(chatId);

        GameUpdate currentUpdate = firstUpdate;

        int currentPlayer = currentUpdate.get(0)
                .getIndex();

        boolean partnerCard = true;

        while (gameEngine.gameInProgress()) {
            if (gameEngine.biddingInProgress()) {
                currentUpdate = gameEngine.processPlay(getPlayer(chatId, currentPlayer).getBid());
            } else if (partnerCard) {
                currentUpdate = gameEngine.processPlay(getPlayer(chatId, currentPlayer).getPartnerCard());
                partnerCard = false;
            } else if (gameEngine.firstCardOfTrick()) {
                currentUpdate = gameEngine.processPlay(getPlayer(chatId, currentPlayer).getFirstCard(
                        gameEngine.getTrumpBroken(),
                        gameEngine.getTrumpSuit()));
            } else {
                currentUpdate = gameEngine.processPlay(getPlayer(chatId, currentPlayer).getNextCard(
                        gameEngine.getFirstCardSuit(),
                        gameEngine.getTrumpSuit()));
            }
            currentPlayer = currentUpdate.get(0).getIndex();
            broadcastUpdateFromEngine(gameEngine, currentUpdate);
        }
    }

    public boolean containsUserId(long id) {
        return playerToGroupIds.containsKey(id);
    }

    @Override
    public void addEngine(GameEngine engine) {
        gameEngines.put(engine.getChatId(), engine);
    }

    @Override
    public void setIOInterface(IOInterface ioInterface) {
        this.ioInterface = ioInterface;
    }

    @Override
    public void broadcastUpdateFromEngine(GameEngine engine, GameUpdate update) {
        long chatId = engine.getChatId();
        for (IndexUpdate indexUpdate : update) {
            ioInterface.sendMessageToId(
                    getUserId(chatId, indexUpdate.getIndex()),
                    indexUpdate.getMessage());
        }
    }

    private long getUserId(long chatId, int playerIndex) {
        return gameIds.get(chatId).getChatId(playerIndex);
    }

    @Override
    public void updateEngine(long chatId, String message) {
        Engine engine = gameEngines.get(chatId);
    }

    @Override
    public String queryEngine(long chatId, String query) {
        return gameEngines.get(chatId).queryEngine(query);
    }

    public void registerResponse(long chatId, String response) {
        chatIdToPlayerMap.get(chatId).registerResponse(response);
    }

    private Player getPlayer(long chatId, int playerIndex) {
        return chatIdToPlayerMap.get(gameIds.get(chatId).getChatId(playerIndex));
    }


}
