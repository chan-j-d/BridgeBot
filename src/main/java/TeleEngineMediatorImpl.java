import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    */
    private HashMap<Long, List<Integer>> chatToMessageIds;
    private static int HAND = 0;
    private static int UPDATE = 1;

    private IOInterface ioInterface;
    private HashMap<Long, TelegramPlayer> chatIdToPlayerMap;

    public TeleEngineMediatorImpl() {
        this.gameIds = new HashMap<>();
        this.gameEngines = new HashMap<>();
        this.playerToGroupIds = new HashMap<>();
        this.chatIdToPlayerMap = new HashMap<>();
        this.chatToMessageIds = new HashMap<>();
    }

    @Override
    public void addGameIds(GameChatIds chatIds) {
        gameIds.put(chatIds.getChatId(0), chatIds);
        long groupId = chatIds.getChatId(0);
        GameEngine gameEngine = GameEngine.init(groupId);
        for (int i = 0; i < 5; i++) {
            long chatId = chatIds.getChatId(i);
            chatToMessageIds.put(chatId, Arrays.asList(new Integer[] {0, 0}));
            if (i == 0) {
                continue;
            }
            playerToGroupIds.put(chatId, groupId);
            chatIdToPlayerMap.put(chatId, new TelegramPlayer(chatIds.getName(i), gameEngine.getPlayerState(i)));
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
        System.out.println(update);
        System.out.println(chatToMessageIds);
        for (IndexUpdate indexUpdate : update) {
            String text = stringEditing(chatId, indexUpdate.getMessage());
            UpdateType updateType = indexUpdate.getUpdateType();
            long userId = getUserId(chatId, indexUpdate.getIndex());
            System.out.println(userId);
            switch (updateType) {
                case SEND:
                    ioInterface.sendMessageToId(userId, text);
                    break;
                case EDIT_HAND:
                    ioInterface.editMessage(userId,
                            chatToMessageIds.get(userId).get(HAND),
                            text);
                    break;
                case EDIT_UPDATE:
                    ioInterface.editMessage(userId,
                            chatToMessageIds.get(userId).get(UPDATE),
                            text);
                    break;
                case SEND_HAND:
                case SEND_UPDATE:
                    int messageId = ioInterface.sendMessageToId(userId, text);
                    updateMessageIds(userId, messageId, updateType);
                    break;

            }
        }
    }

    private String stringEditing(long chatId, String string) {
        String currentString = string;
        for (int i = 1; i < 5; i++) {
            String playerName = chatIdToPlayerMap.get(getUserId(chatId, i)).getName();
            currentString = String.join(playerName, currentString.split("P" + i));
        }
        return currentString;
    }

    private void updateMessageIds(long chatId, int messageId, UpdateType updateType) {
        System.out.println("message id: " + messageId);
        if (updateType == UpdateType.SEND_HAND) {
            System.out.println("Before update: " + chatToMessageIds);
            chatToMessageIds.get(chatId).set(0, messageId);
            System.out.println("After update: " + chatToMessageIds);
        } else if (updateType == UpdateType.SEND_UPDATE) {
            chatToMessageIds.get(chatId).set(1, messageId);
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
