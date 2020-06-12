import java.util.Arrays;

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

    public String getName(int index) {
        return names[index - 1];
    }

    public boolean checkFull() {
        return index == numPlayers + 1;
    }

    public String toString() {
        return Arrays.toString(chatIds);
    }


}
