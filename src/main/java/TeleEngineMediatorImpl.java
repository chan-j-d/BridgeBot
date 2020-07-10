import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TeleEngineMediatorImpl implements ClientEngineMediator {

    private HashMap<Long, ArrayList<ViewerInterface>> listViewerInterfaces;
    private HashMap<Long, GameEngine> gameEngines;
    private HashMap<Long, Long> playerToGroupIds;
    private HashMap<Long, Player> chatIdToPlayerMap;

    private IOInterface ioInterface;

    private LogsManagement logsManager = LogsManagement.init("C:\\Users\\raido\\Desktop\\Orbital_Log_Dump\\");

    public TeleEngineMediatorImpl() {
        this.listViewerInterfaces = new HashMap<>();
        this.gameEngines = new HashMap<>();
        this.playerToGroupIds = new HashMap<>();
        this.chatIdToPlayerMap = new HashMap<>();

    }

    @Override
    public void addGameIds(GameChatIds chatIds) {
        long groupId = chatIds.getChatId(0);
        GameEngine gameEngine = GameEngine.init(groupId);

        ArrayList<ViewerInterface> list = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            long chatId = chatIds.getChatId(i);

            ViewerInterface viewer;
            if (i == 0) {
                viewer = new BridgeGroupMovingInterface(chatId, ioInterface);
            } else {
                viewer = new BridgeUserInterface(chatId, ioInterface);
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

        GameUpdate firstUpdate = gameEngine.startBid();
        broadcastUpdateFromEngine(gameEngine, firstUpdate);
        CompletableFuture.runAsync(() -> runGame(groupId, firstUpdate));
    }

    private void runGame(long chatId, GameUpdate firstUpdate) {
        GameEngine gameEngine = gameEngines.get(chatId);

        GameUpdate currentUpdate = firstUpdate;

        int currentPlayer = currentUpdate.get(0)
                .getIndex();

        while (gameEngine.gameInProgress()) {
            if (gameEngine.biddingInProgress()) {
                Bid newBid = getPlayer(chatId, currentPlayer).getBid();
                if (newBid == null) {
                    cancelGame(chatId);
                    if (this.gameEngines.containsKey(chatId)) {
                        ioInterface.sendMessageToId(chatId, "Game cancelled due to inactivity!");
                    }
                    return;
                }
                currentUpdate = gameEngine.processPlay(newBid);
            } else {
                Card card;
                if (gameEngine.gettingPartnerCard()) {
                    card = getPlayer(chatId, currentPlayer).getPartnerCard();
                } else if (gameEngine.firstCardOfTrick()) {
                    card = getPlayer(chatId, currentPlayer).getFirstCard(
                            gameEngine.getTrumpBroken(),
                            gameEngine.getTrumpSuit());
                } else {
                    card = getPlayer(chatId, currentPlayer).getNextCard(
                            gameEngine.getFirstCardSuit(),
                            gameEngine.getTrumpSuit());
                }
                if (card == null) {
                    if (this.gameEngines.containsKey(chatId)) {
                        ioInterface.sendMessageToId(chatId, "Game cancelled due to inactivity!");
                    }
                    cancelGame(chatId);
                    return;
                }

                currentUpdate = gameEngine.processPlay(card);
            }
            currentPlayer = currentUpdate.get(0).getIndex();
            broadcastUpdateFromEngine(gameEngine, currentUpdate);
        }

        logsManager.updateLogs(gameEngine.getGameLogger());

        System.out.println(new GameHashImpl1().hashGame(gameEngine.getGameLogger().getGameReplay()));

        ioInterface.registerGameEnded(gameEngine.getGameLogger());

        removeGame(chatId);
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
    }

    private void testGameIds() {
        long groupId = 0L;
        GameEngine gameEngine = GameEngine.init(groupId);

        ArrayList<ViewerInterface> list = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            long chatId = (long) i;

            ViewerInterface viewer;
            if (i == 0) {
                viewer = new BridgeGroupInterface(chatId, ioInterface);
            } else {
                viewer = new BridgeUserInterface(chatId, ioInterface);
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

        GameUpdate firstUpdate = gameEngine.startBid();
        broadcastUpdateFromEngine(gameEngine, firstUpdate);
        runGame(groupId, firstUpdate);
    }

    public static void main(String[] args) {
        TeleEngineMediatorImpl mediator = new TeleEngineMediatorImpl();
        mediator.setIOInterface(new LocalIOInterface());
        mediator.testGameIds();
    }

}
