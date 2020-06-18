import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TeleEngineMediatorImpl implements ClientEngineMediator {

    private HashMap<Long, ArrayList<ViewerInterface>> listViewerInterfaces;
    private HashMap<Long, GameEngine> gameEngines;
    private HashMap<Long, Long> playerToGroupIds;

    private IOInterface ioInterface;
    private HashMap<Long, TelegramPlayer> chatIdToPlayerMap;

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
                viewer = new BridgeGroupInterface(chatId, ioInterface);
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

        boolean partnerCard = true;

        while (gameEngine.gameInProgress()) {
            if (gameEngine.biddingInProgress()) {
                Bid newBid = getPlayer(chatId, currentPlayer).getBid();
                currentUpdate = gameEngine.processPlay(newBid);
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
        IndexUpdate requestUpdate = null;
        for (IndexUpdate indexUpdate : update) {
            if (indexUpdate.getUpdateType() == UpdateType.SEND_REQUEST) {
                requestUpdate = indexUpdate;
                continue;
            }
            int index = indexUpdate.getIndex();
            String message = indexUpdate.getMessage();
            indexUpdate = indexUpdate.editString(stringEditing(chatId, message));
            ViewerInterface viewerInterface = getViewerInterface(chatId, index);
            viewerInterface.processUpdate(indexUpdate);
        }
        if (requestUpdate != null) {
            ViewerInterface viewerInterface = getViewerInterface(chatId, requestUpdate.getIndex());
            viewerInterface.processUpdate(requestUpdate);
        }
    }

    private String stringEditing(long chatId, String string) {
        String currentString = string;
        for (int i = 1; i < 5; i++) {
            String playerName = getPlayer(chatId, i).getName();
            currentString = String.join("*" + playerName + "*", currentString.split("P" + i));
        }
        return currentString;
    }

    private ViewerInterface getViewerInterface(long chatId, int index) {
        return listViewerInterfaces.get(chatId).get(index);
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
        return chatIdToPlayerMap.get(listViewerInterfaces.get(chatId).get(playerIndex).getViewerId());
    }


}
