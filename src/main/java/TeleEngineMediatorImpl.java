import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TeleEngineMediatorImpl implements ClientEngineMediator {

    private static final int NORMAL_GAME = 0;
    private static final int OPEN_HAND = 1;
    private static final int CARDS_PLAYED = 2;
    private static final int SUITS_PLAYED = 3;
    private static final int TUTORIAL = 4;

    private HashMap<Long, ArrayList<ViewerInterface>> listViewerInterfaces;
    private HashMap<Long, GameEngine> gameEngines;
    private HashMap<Long, Long> playerToGroupIds;
    private HashMap<Long, Player> chatIdToPlayerMap;

    //Retains memory of old gameChatIds
    private FixedMap<Long, GameChatIds> chatIdsMap;
    private int TEMP_STORE_SIZE = 20;

    private IOInterface ioInterface;

    private LogsManagement logsManager = LogsManagement.init("C:/Users/raido/Desktop/Orbital_Log_Dump/");

    public TeleEngineMediatorImpl() {
        this.listViewerInterfaces = new HashMap<>();
        this.gameEngines = new HashMap<>();
        this.chatIdsMap = new FixedMap<>(TEMP_STORE_SIZE);
        this.playerToGroupIds = new HashMap<>();
        this.chatIdToPlayerMap = new HashMap<>();

    }

    @Override
    public void addGameIds(GameChatIds chatIds) {
        long groupId = chatIds.getChatId(0);
        int gameType = chatIds.getGameType();
        GameEngine gameEngine = gameEngineSelector(groupId, gameType);
        chatIdsMap.put(groupId, chatIds);

        ArrayList<ViewerInterface> list = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            long chatId = chatIds.getChatId(i);

            ViewerInterface viewer;
            if (i == 0) {
                viewer = new BridgeGroupMovingInterface(chatId, ioInterface);
            } else {
                viewer = new TutorialUserInterface(chatId, ioInterface);
            }

            list.add(viewer);

            if (i == 0) continue;

            chatIdToPlayerMap.put(chatId, new TelegramPlayer(
                    chatIds.getName(i),
                    gameEngine.getPlayerState(i)));

            playerToGroupIds.put(chatId, groupId);
        }

        listViewerInterfaces.put(groupId, list);

        gameEngines.put(groupId, gameEngine);

        CompletableFuture<GameEngine> future = CompletableFuture.supplyAsync(() -> runGame(groupId));
        future.thenAccept(engine -> processGameEnd(engine));

    }

    private GameEngine gameEngineSelector(long groupId, int gameType) {
        switch (gameType) {
            case (NORMAL_GAME):
                return new GameEngine(groupId);
            case (OPEN_HAND):
                return new OpenHandEngine(groupId);
            case (CARDS_PLAYED):
                return new CardPlayedEngine(groupId);
            case (SUITS_PLAYED):
                return new SuitPlayedEngine(groupId);
            case (TUTORIAL):
                return new TutorialEngine(groupId);
            default:
                throw new IllegalArgumentException("No such gameType!");
        }
    }

    private GameEngine runGame(long chatId) {
        GameEngine gameEngine = gameEngines.get(chatId);

        GameUpdate currentUpdate = gameEngine.startGame();
        broadcastUpdateFromEngine(gameEngine, currentUpdate);

        int currentPlayer = 0;

        GameStatus currentStatus = gameEngine.getGameStatus();

        while (!currentStatus.equals(GameStatus.END)) {

            Card card;

            switch (currentStatus) {

                case COMMUNICATING:
                    CompletableFuture<Boolean> future1 = CompletableFuture.supplyAsync(
                            () -> queryResponseFromUser(gameEngine, chatId, 1));
                    CompletableFuture<Boolean> future2 = CompletableFuture.supplyAsync(
                            () -> queryResponseFromUser(gameEngine, chatId, 2));
                    CompletableFuture<Boolean> future3 = CompletableFuture.supplyAsync(
                            () -> queryResponseFromUser(gameEngine, chatId, 3));
                    CompletableFuture<Boolean> future4 = CompletableFuture.supplyAsync(
                            () -> queryResponseFromUser(gameEngine, chatId, 4));
                    CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(
                            future1, future2, future3, future4);
                    combinedFuture.join();

                    if (!(future1.join() && future2.join() && future3.join() && future4.join())) {
                        if (this.gameEngines.get(chatId) == gameEngine) {
                            ioInterface.sendMessageToId(chatId, "Game cancelled due to inactivity!");
                            cancelGame(chatId);
                        }
                        return null;
                    }

                    currentUpdate = gameEngine.startBid();
                    broadcastUpdateFromEngine(gameEngine, currentUpdate);
                    currentPlayer = currentUpdate.get(0).getIndex();
                    currentStatus = gameEngine.getGameStatus();
                    continue;

                case BIDDING:
                    Bid newBid = getPlayer(chatId, currentPlayer).getBid();
                    if (newBid == null) {
                        if (this.gameEngines.get(chatId) == gameEngine) {
                            ioInterface.sendMessageToId(chatId, "Game cancelled due to inactivity!");
                            cancelGame(chatId);
                        }
                        return null;
                    }
                    currentUpdate = gameEngine.processPlay(newBid);
                    break;

                case PARTNER:
                    card = getPlayer(chatId, currentPlayer).getPartnerCard();
                    if (card == null) {
                        if (this.gameEngines.get(chatId) == gameEngine) {
                            ioInterface.sendMessageToId(chatId, "Game cancelled due to inactivity!");
                            cancelGame(chatId);
                        }
                        return null;
                    }
                    currentUpdate = gameEngine.processPlay(card);
                    break;

                case FIRST_CARD:
                    card = getPlayer(chatId, currentPlayer).getFirstCard(
                            gameEngine.getTrumpBroken(),
                            gameEngine.getTrumpSuit());
                    if (card == null) {
                        if (this.gameEngines.get(chatId) == gameEngine) {
                            ioInterface.sendMessageToId(chatId, "Game cancelled due to inactivity!");
                            cancelGame(chatId);
                        }
                        return null;
                    }
                    currentUpdate = gameEngine.processPlay(card);
                    break;

                case OTHER_CARDS:
                    card = getPlayer(chatId, currentPlayer).getNextCard(
                            gameEngine.getFirstCardSuit(),
                            gameEngine.getTrumpSuit());
                    if (card == null) {
                        if (this.gameEngines.get(chatId) == gameEngine) {
                            ioInterface.sendMessageToId(chatId, "Game cancelled due to inactivity!");
                            cancelGame(chatId);
                        }
                        return null;
                    }
                    currentUpdate = gameEngine.processPlay(card);
                break;

            }
            currentStatus = gameEngine.getGameStatus();

            currentPlayer = currentUpdate.get(0).getIndex();
            broadcastUpdateFromEngine(gameEngine, currentUpdate);
        }

        return gameEngine;

    }

    private boolean queryResponseFromUser(GameEngine engine, long chatId, int player) {
        while (engine.expectingResponse(player)) {
            String response = getPlayer(chatId, player).getStringResponse();
            if (response == null) {
                return false;
            }
            broadcastUpdateFromEngine(engine, engine.processResponse(player, response));
        }
        return true;
    }

    private void processGameEnd(GameEngine engine) {
        if (engine == null) return;
        logsManager.updateLogs(engine.getGameLogger());
        System.out.println(new GameHashImpl1().hashGame(engine.getGameLogger().getGameReplay()));
        ioInterface.registerGameEnded(engine.getGameLogger());
        removeGame(engine.getChatId());
    }

    public boolean containsUserId(long id) {
        return playerToGroupIds.containsKey(id);
    }

    @Override
    public void setIOInterface(IOInterface ioInterface) {
        this.ioInterface = ioInterface;
    }

    @Override
    public void broadcastUpdateFromEngine(GameEngine engine, GameUpdate update) {
        long chatId = engine.getChatId();
        System.out.println(update);
        for (IndexUpdate indexUpdate : update) {
            int index = indexUpdate.getIndex();
            String message = indexUpdate.getMessage();
            indexUpdate = indexUpdate.editString(stringEditing(chatId, message));
            ViewerInterface viewerInterface = getViewerInterface(chatId, index);
            viewerInterface.processUpdate(indexUpdate);
        }

        for (ViewerInterface viewerInterface : listViewerInterfaces.get(chatId)) {
            viewerInterface.run();
        }

        System.out.println("completed broadcasting");
    }

    private String stringEditing(long chatId, String string) {
        String currentString = string;
        for (int i = 1; i < 5; i++) {
            String playerName = getPlayer(chatId, i).getName();
            currentString = currentString.replace("P" + i, "*" + playerName + "*");
        }
        return currentString;
    }

    private ViewerInterface getViewerInterface(long chatId, int index) {
        return listViewerInterfaces.get(chatId).get(index);
    }

    public void registerResponse(long chatId, String response) {
        chatIdToPlayerMap.get(chatId).registerResponse(response);
    }

    private Player getPlayer(long chatId, int playerIndex) {
        return chatIdToPlayerMap.get(listViewerInterfaces.get(chatId).get(playerIndex).getViewerId());
    }

    public boolean cancelGame(long chatId) {
        if (!listViewerInterfaces.containsKey(chatId)) {
            return false;
        } else {
            List<ViewerInterface> list = listViewerInterfaces.get(chatId);
            for (int i = 1; i < 5; i++) {
                ioInterface.sendMessageToId(list.get(i).getViewerId(), "Game has been cancelled");
            }
            removeGame(chatId);
            return true;
        }
    }

    public void resend(long chatId) {
        if (queryInProgress(chatId)) {
            ViewerInterface viewer = listViewerInterfaces.get(chatId).get(0);
            viewer.resend();
        } else {
            List<ViewerInterface> list = listViewerInterfaces.get(playerToGroupIds.get(chatId));
            for (ViewerInterface viewer : list) {
                if (viewer.getViewerId() == chatId) {
                    viewer.resend();
                    break;
                }
            }
        }
    }

    public boolean queryInProgress(long chatId) {
        return this.listViewerInterfaces.containsKey(chatId);
    }

    private void removeGame(long chatId) {
        ArrayList<ViewerInterface> list = listViewerInterfaces.get(chatId);
        for (int i = 1; i < list.size(); i++) {
            long userId = list.get(i).getViewerId();
            playerToGroupIds.remove(userId);
            chatIdToPlayerMap.remove(userId);
        }

        gameEngines.remove(chatId);
        listViewerInterfaces.remove(chatId);

        //Implement way to get rid of old gameCHatIds;

    }

    @Override
    public GameChatIds getRecentGameChatIds(long groupId) {
        return chatIdsMap.get(groupId);
    }

    private void testGameIds() {
        long groupId = 0L;
        GameEngine gameEngine = new TutorialEngine(groupId);

        ArrayList<ViewerInterface> list = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            long chatId = (long) i;

            ViewerInterface viewer;
            if (i == 0) {
                viewer = new BridgeGroupMovingInterface(chatId, ioInterface);
            } else {
                viewer = new TutorialUserInterface(chatId, ioInterface);
            }

            list.add(viewer);

            if (i == 0) continue;

            chatIdToPlayerMap.put(chatId, new TestPlayer(
                    i + "",
                    gameEngine.getPlayerState(i)));

            playerToGroupIds.put(chatId, groupId);
        }

        listViewerInterfaces.put(groupId, list);

        gameEngines.put(groupId, gameEngine);

        runGame(groupId);
    }

    public static void main(String[] args) {
        TeleEngineMediatorImpl mediator = new TeleEngineMediatorImpl();
        mediator.setIOInterface(new LocalIOInterface());
        mediator.testGameIds();
    }

}
