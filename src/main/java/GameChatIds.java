import java.util.Arrays;
import java.util.List;

/*
Encapsulates a simple collection of chatIds for a single game of Bridge.
Index 0: group chat id
1: player 1
2: player 2
etc.
 */
public class GameChatIds {

    private long[] chatIds;
    private String[] names;
    private int numPlayers;
    private int index;
    private int gameType;

    public GameChatIds(int numPlayers) {
        chatIds = new long[numPlayers + 1];
        names = new String[numPlayers];
        index = 0;
        this.numPlayers = numPlayers;
    }

    public void addChatId(long id) {
        if (index == 0) {
            chatIds[index++] = id;
        } else {
            throw new IllegalStateException("Only one group chatId can be added!");
        }
    }

    public void addUserIdAndName(long id, String name) {
        if (index == numPlayers + 1) {
            throw new IllegalStateException("Game is full!");
        }
        names[index - 1] = name;
        chatIds[index++] = id;
    }

    public long getChatId(int index) {
        if (index < 0 || index > numPlayers) {
            throw new IllegalArgumentException("Invalid index! Please choose an index between 0 and 4");
        }
        return chatIds[index];
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getGameType() {
        return this.gameType;
    }

    public String getName(int index) {
        return names[index - 1];
    }

    public void removePlayerId(long gameChatId) {
        long[] newChatIds = new long[numPlayers + 1];
        String[] newNames = new String[numPlayers];
        boolean firstPass = true;
        newChatIds[0] = chatIds[0];
        int newIndex = 1;
        for (int i = 1; i < numPlayers + 1; i++) {
            if (chatIds[i] == gameChatId && firstPass) {
                firstPass = false;
            } else {
                newChatIds[newIndex] = chatIds[i];
                newNames[newIndex++ - 1] = names[i - 1];
            }
        }

        chatIds = newChatIds;
        names = newNames;
        index = index == 1 ? 1 : index - 1;


    }

    public boolean checkFull() {
        return index == numPlayers + 1;
    }

    public boolean checkContainsId(long id) {
        for (long containedId : chatIds) {
            if (containedId == id) return true;
        }
        return false;
    }

    public String toString() {
        return Arrays.toString(chatIds);
    }


}
