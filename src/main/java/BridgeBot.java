import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BridgeBot extends TelegramLongPollingBot implements IOInterface {

    private ClientEngineMediator mediator;

    public BridgeBot(ClientEngineMediator mediator) {
        this.mediator = mediator;
    }

    private HashMap<Long, GameChatIds> groupChatIds = new HashMap<>();
    private HashMap<Long, Pair<Integer, String>> startGameMessageId = new HashMap<>();

    private static List<String> validCommands = Arrays.asList(
            new String[] {"startgame", "joingame"}
    );

    public void onUpdateReceived(Update update) {
        boolean groupChat = update.getMessage().getChat().isGroupChat();
        if (groupChat) {
            String text = update.getMessage().getText();
            String command = text.split("@")[0].substring(1);
            if (isValidCommand(command)) {
                processUpdate(command, update);
            } else {
                long chatId = update.getMessage().getChatId();
                sendMessageToId(chatId, "Invalid bot command!");
            }
        } else {
            long userId = update.getMessage().getChatId();
            if (!mediator.containsUserId(userId)) {
                sendMessageToId(userId, "No game running!");
            } else {
                mediator.registerResponse(userId, update.getMessage().getText());
            }
        }
    }

    public void registerResponse(long chatId, String response) {
        mediator.registerResponse(chatId, response);
    }

    public boolean isValidCommand(String command) {
        return validCommands.contains(command);
    }

    public void processUpdate(String command, Update update) {
        switch (command) {
            case "startgame":
                startGameRequest(update);
                break;
            case "joingame":
                joinGameRequest(update);
                break;
        }
    }

    public void startGameRequest(Update update) {
        long chatId = update.getMessage().getChatId();
        if (groupChatIds.containsKey(chatId)) {
            sendMessageToId(chatId, "Game already started!");
        } else {
            GameChatIds gameChatIds = new GameChatIds(4);
            gameChatIds.addChatId(chatId);
            groupChatIds.put(chatId, gameChatIds);
            String stringToSend = "Game initialised! Type /joingame@BridgeBot to join the game.\n" +
                    "Players joined: ";
            SendMessage sendMessage = new SendMessage()
                    .setChatId(chatId)
                    .setText(stringToSend);
            try {
                Message message = execute(sendMessage);
                startGameMessageId.put(chatId, new Pair<>(
                        message.getMessageId(),
                        stringToSend));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public void joinGameRequest(Update update) {
        long chatId = update.getMessage().getChatId();
        long userId = (long) update.getMessage().getFrom().getId();
        if (!groupChatIds.containsKey(chatId)) {
            sendMessageToId(chatId, "No game running!");
        } else {
            GameChatIds gameChatIds = groupChatIds.get(chatId);
            if (gameChatIds.checkFull()) {
                sendMessageToId(chatId, "Game is full!");
            } else {
                String firstName = update.getMessage().getFrom().getFirstName();
                gameChatIds.addUserIdAndName(userId, firstName);
                sendMessageToId(chatId, firstName + " has joined the game!");
                sendMessageToId(userId, "You have joined the game!");
                int messageIdToUpdate = startGameMessageId.get(chatId).first;
                String stringToEdit  = startGameMessageId.get(chatId).second + "\n" + firstName;
                editMessage(chatId, messageIdToUpdate, stringToEdit);
                startGameMessageId.get(chatId).second = stringToEdit;
                if (gameChatIds.checkFull()) {
                    sendMessageToId(chatId, "Game starting!");
                    mediator.addGameIds(gameChatIds);
                    startGameMessageId.remove(chatId);
                }
            }

        }
    }

    public int sendMessageToId(long chatId, String text) {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text);
        try {
            return execute(message).getMessageId();
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
        return -1;
    }

    public void editMessage(long chatId, int messageId, String newText) {
        System.out.println("BridgeBot editMessage: " + chatId + " " + messageId + " " + newText);
        EditMessageText edit = new EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText(newText);
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
    }

    public String getBotUsername() {
        return "";
    }

    public String getBotToken() {
        return "1293154216:AAHjpKZgt_jtVVnD_cN2j1xl0Zre2pLr6Eo";
    }


}
