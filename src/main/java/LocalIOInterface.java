import java.util.Arrays;

public class LocalIOInterface implements IOInterface {

    private int count;

    public LocalIOInterface() {
        count = 0;
    }

    public int sendMessageToId(long chatId, String message) {
        System.out.println(chatId + ": "  + message);
        return count++;
    }

    public void editMessage(long chatId, int messageId, String message) {
        System.out.println(chatId + "(M: " + messageId + "): " + message);
    }

    public int sendMessageWithButtons(long chatId, String message, String[][] buttons) {
        System.out.println(chatId + ": " + message);
        for (String[] array : buttons) {
            System.out.println(Arrays.toString(array));
        }
        return count++;
    }

    public void editMessageButtons(long chatId, int messageId, String[][] buttons) {
        System.out.println(chatId + "(M: " + messageId + "): ");
        for (String[] array : buttons) {
            System.out.println(Arrays.toString(array));
        }
    }

    public void deleteMessage(long chatId, int messageId) {
        System.out.println(chatId + "(M: " + messageId + "): (DELETING)");
    }



}
