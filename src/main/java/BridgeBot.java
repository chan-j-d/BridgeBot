import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class BridgeBot extends TelegramLongPollingBot implements IOInterface {

    private static final String WEBSITE_LINK = "https://fyshhh.github.io/bridgebot/";
    private static final String REGAME_STRING = "regame";

    private ClientEngineMediator mediator;

    public BridgeBot(ClientEngineMediator mediator) {
        this.mediator = mediator;
    }

    //GameChatId is an object containing 5 telegram long ids used to start a game
    private HashMap<Long, GameChatIds> groupChatIds = new HashMap<>();
    //Keeps track of the messageId of the currently edited message for joining the game
    private HashMap<Long, Pair<Integer, String>> startGameMessageId = new HashMap<>();
    //Keeps track of the group name of games started
    private HashMap<Long, String> groupNames = new HashMap<>();
    //Keeps track of all players that are in the process of joining a game
    private HashSet<Long> userIds = new HashSet<>();
    //All of these are removed the moment a game starts

    //HashMap for recording cancelGame messageIds for voting
    private HashMap<Long, Pair<Integer, Integer>> cancelGameId = new HashMap<>();

    //Current HashImpl
    private GameHash hasher = new GameHashImpl1();

    //Valid commands, edit this and create the relevant method in order to include it as a Telegram bot command.
    private static List<String> validCommands = Arrays.asList(
            "startgame",
            "joingame",
            "creategame",
            "cancelgame",
            "help",
            "leavegame",
            "resend");

    /*
    Required method by the Telegram package. Only recognise bot commands in group chats and private messages.
    This includes callback queries for buttons.
     */
    public void onUpdateReceived(Update update) {

        System.out.println(update);

        if (update.hasCallbackQuery()) {

            boolean groupChat = update.getCallbackQuery().getMessage().isGroupMessage();
            CallbackQuery query = update.getCallbackQuery();

            if (groupChat) {
                String command = query.getData().split("@")[0].substring(1);
                processCommand(command, update);

                if (query.getData().equals(REGAME_STRING)) {
                    createGameRequest(update);
                }

            } else {
                int userId = query.getFrom().getId();
                String message = query.getData();
                mediator.registerResponse(userId, message);
            }

        } else if (update.hasMessage()) {
            Message message = update.getMessage();
            boolean groupChat = update.getMessage().getChat().isGroupChat();

            if (groupChat) {
                List<MessageEntity> entities = message.getEntities();
                for (MessageEntity entity : entities) {
                    String command = entity.getText().split("@")[0].substring(1);
                    if (isValidCommand(command)) {
                        processCommand(command, update);
                    }
                }
            } else {
                long userId = message.getChatId();
                List<MessageEntity> entities = message.getEntities();
                if (entities != null) {
                    for (MessageEntity entity : message.getEntities()) {
                        String command = entity.getText().substring(1);
                        if (isValidCommand(command)) {
                            processPrivateCommand(command, update);
                        }
                    }
                } else {
                    if (!mediator.containsUserId(userId)) {
                        sendMessageToId(userId, "You are not in a game!");
                    } else {
                        mediator.registerResponse(userId, update.getMessage().getText());
                    }
                }
            }
        }

    }

    private void processPrivateCommand(String command, Update update) {
        long chatId = update.getMessage().getChatId();
        if (isValidCommand(command)) {
            switch (command) {
                case "help":
                    break;
                case "resend":
                    mediator.resend(chatId);
                    break;
                default:
                    sendMessageToId(chatId, "Command only valid in a group chat!");
            }
        } else {
            sendMessageToId(chatId, "Not a valid command!");
        }
    }


    private boolean isValidCommand(String command) {
        return validCommands.contains(command);
    }

    //Command processer after parsing out the main command as a String.
    public void processCommand(String command, Update update) {
        switch (command) {
            case "startgame":
                startGameRequest(update);
                break;
            case "joingame":
                joinGameRequest(update);
                break;
            case "creategame":
                createGameRequest(update);
                break;
            case "cancelgame":
                cancelGameRequest(update);
                break;
            case "help":
                break;
            case "resend":
                resendRequest(update);
                break;
            case "leavegame":
                leaveGameRequest(update);
                break;
        }
    }

    //Processes a start game request.
    private void createGameRequest(Update update) {
        long chatId;
        Chat chat;
        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
            chat = update.getMessage().getChat();
        } else {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            chat = update.getCallbackQuery().getMessage().getChat();
        }

        //checks if a similar game has already started
        if (groupChatIds.containsKey(chatId) || checkGameInProgress(chatId)) {
            sendMessageToId(chatId, "Game already started!");
        } else {
            //Assigns the group chat Id with a name
            String groupName = chat.getTitle();
            groupNames.put(chatId, groupName);

            //Creates the gameChatId object to store the game Ids
            GameChatIds gameChatIds = new GameChatIds(4);
            gameChatIds.addChatId(chatId);
            groupChatIds.put(chatId, gameChatIds);
            String stringToSend = "Game initialised!\nType /joingame@" + getBotUsername() + " or press the " +
                    "button below to join the game.\nPlayers joined: ";

            SendMessage sendMessage = new SendMessage()
                    .setChatId(chatId)
                    .setText(stringToSend);

            //Adds a join game button
            InlineKeyboardMarkup buttonMarkup = createJoinButtonMarkup();

            sendMessage.setReplyMarkup(buttonMarkup);

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

    private InlineKeyboardMarkup createJoinButtonMarkup() {
        InlineKeyboardButton joinButton = new InlineKeyboardButton()
                .setText("Join Game")
                .setCallbackData("/joingame@" + this.getBotUsername());

        InlineKeyboardMarkup buttonMarkup = new InlineKeyboardMarkup()
                .setKeyboard(List.of(List.of(joinButton)));

        return buttonMarkup;
    }

    private InlineKeyboardMarkup createStartButtonMarkup() {
        InlineKeyboardButton startButton = new InlineKeyboardButton()
                .setText("Start Game")
                .setCallbackData("/startgame@" + this.getBotUsername());

        InlineKeyboardMarkup buttonMarkup = new InlineKeyboardMarkup()
                .setKeyboard(List.of(List.of(startButton)));

        return buttonMarkup;
    }

    //Processes a joinGame request
    private void joinGameRequest(Update update) {
        long userId; //Used for sending an acknowledgement message to the player
        long chatId;
        String firstName; //Used for adding the name to the list of players that have joined

        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            chatId = query.getMessage().getChatId();
            userId = (long) query.getFrom().getId();
            firstName = query.getFrom().getFirstName();
        } else if (update.hasMessage()) {
            userId = (long) update.getMessage().getFrom().getId();
            chatId = update.getMessage().getChatId();
            firstName = update.getMessage().getFrom().getFirstName();
        } else {
            userId = -1L;
            chatId = -1L;
            firstName = "Invalid";
        }

        GameChatIds gameChatIds;

        if (checkGameInProgress(chatId)) {
            sendMessageToId(chatId, "Game has already started!");
        } else if (!groupChatIds.containsKey(chatId)) {
            sendMessageToId(chatId, "No game running!");

            /* **DISABLED FOR NOW DUE TO THE NEED TO BE ABLE TO JOIN THE SAME GAME MULTIPLE TIMES FOOR TESTING**
        } else if (mediator.containsUserId(userId) || userIds.contains(userId)) {
            sendMessageToId(userId, "You are already in a game!");
            */

        } else if ((gameChatIds = groupChatIds.get(chatId)).checkFull()) {
            sendMessageToId(chatId, "Game is full!");
        } else {

            gameChatIds.addUserIdAndName(userId, firstName);

            userIds.add(userId);

            sendMessageToId(chatId, "*" + firstName + "* has joined the game!");
            sendMessageToId(userId,
                    String.format("You have joined a bridge game in %s!",
                            groupNames.get(chatId)));
            int messageIdToUpdate = startGameMessageId.get(chatId).first;
            String stringToEdit  = startGameMessageId.get(chatId).second + "\n - " + firstName;

            EditMessageText editMessage = new EditMessageText()
                    .setChatId(chatId)
                    .setMessageId(messageIdToUpdate);

            startGameMessageId.get(chatId).second = stringToEdit;

            if (gameChatIds.checkFull()) {
                editMessage.setReplyMarkup(createStartButtonMarkup());
                stringToEdit = stringToEdit + "\nClick the button below or type /startgame@" +
                        getBotUsername() + " to start the game!";
            } else {
                editMessage.setReplyMarkup(createJoinButtonMarkup());
            }

            editMessage.setText(stringToEdit);

            try {
                execute(editMessage);
            } catch (TelegramApiException e) {
                System.err.println(e);
            }

        }
    }

    private void startGameRequest(Update update) {
        long chatId;

        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            chatId = query.getMessage().getChatId();
        } else if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else {
            chatId = -1L;
        }

        GameChatIds gameChatIds;

        if (!groupChatIds.containsKey(chatId)) {
            if (!checkGameInProgress(chatId)) {
                sendMessageToId(chatId, "No game running! Create a game with the command /creategame@" +
                        getBotUsername(), false);
            } else {
                sendMessageToId(chatId, "Game has already started!");
            }
        } else if (!(gameChatIds = groupChatIds.get(chatId)).checkFull()) {
            sendMessageToId(chatId, "4 players are required!");
        } else {
            sendMessageToId(chatId, "Game starting!");
            mediator.addGameIds(gameChatIds);
            startGameMessageId.remove(chatId);
            groupChatIds.remove(chatId);
            groupNames.remove(chatId);

            for (int i = 1; i < 5; i++) {
                userIds.remove(gameChatIds.getChatId(i));
            }

            EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup()
                    .setReplyMarkup(null)
                    .setChatId(chatId)
                    .setMessageId(startGameMessageId.get(chatId).first);

            try {
                execute(editMessageReplyMarkup);
            } catch (TelegramApiException e) {
                System.err.println(e);
            }
        }
    }

    private void leaveGameRequest(Update update) {
        long userId; //Used for sending an acknowledgement message to the player
        long chatId;
        String firstName; //Used for adding the name to the list of players that have joined

        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            chatId = query.getMessage().getChatId();
            userId = (long) query.getFrom().getId();
            firstName = query.getFrom().getFirstName();
        } else if (update.hasMessage()) {
            userId = (long) update.getMessage().getFrom().getId();
            chatId = update.getMessage().getChatId();
            firstName = update.getMessage().getFrom().getFirstName();
        } else {
            userId = -1L;
            chatId = -1L;
            firstName = "Invalid";
        }

        if (checkGameInProgress(chatId)) {
            sendMessageToId(chatId, "Game has already started!");
        } else if (!groupChatIds.containsKey(chatId)) {
            sendMessageToId(chatId, "No game running!");
        } else {
            String stringToEdit = startGameMessageId.get(chatId).second
                    .replaceFirst("\n - " + firstName, "");
            GameChatIds gameChatIds = groupChatIds.get(chatId);

            gameChatIds.removePlayerId(userId);

            int messageId = startGameMessageId.get(chatId).first;

            EditMessageText edit = new EditMessageText()
                    .setChatId(chatId)
                    .setMessageId(messageId)
                    .setText(stringToEdit);

            startGameMessageId.get(chatId).second = stringToEdit;

            edit.setReplyMarkup(createJoinButtonMarkup());

            sendMessageToId(chatId, firstName + " has left the game!");

            try {
                execute(edit);
            } catch (TelegramApiException e) {
                System.err.println(e);
            }

        }
    }

    private void cancelGameRequest(Update update) {
        long chatId = update.getMessage().getChatId();

        if (groupChatIds.containsKey(chatId)) {
            groupChatIds.remove(chatId);
            startGameMessageId.remove(chatId);
            groupNames.remove(chatId);
            sendMessageToId(chatId, "Game cancelled!");
        } else if (checkGameInProgress(chatId)) {
            int userId = update.getMessage().getFrom().getId();
            GetChatMember getChatMember = new GetChatMember()
                    .setChatId(chatId)
                    .setUserId(userId);

            boolean adminstratorCancelled = false;
            try {
                String status = execute(getChatMember).getStatus();
                System.out.println(status);
                if (status.equals("creator") || status.equals("adminstrator")) {
                    mediator.cancelGame(chatId);
                    if (cancelGameId.containsKey(chatId)) {
                        deleteMessage(chatId, cancelGameId.get(chatId).first);
                    }
                    sendMessageToId(chatId, "Game cancelled by adminstrator!");
                    adminstratorCancelled = true;
                }
            } catch (TelegramApiException e) {
                System.err.println(e);
            }

            if (!adminstratorCancelled) {
                if (!mediator.containsUserId((long) userId)) {
                    sendMessageToId(chatId, "Only in-game players or adminstrators can cancel the game!");
                } else if (cancelGameId.containsKey(chatId)) {
                    if (cancelGameId.get(chatId).second != userId) {
                        deleteMessage(chatId, cancelGameId.get(chatId).first);
                        sendMessageToId(chatId, "Game cancelled!");
                        mediator.cancelGame(chatId);
                    } else {
                        sendMessageToId(chatId, "You can only vote to cancel once!");
                    }
                } else {
                    int messageId = sendMessageToId(chatId, "One vote for cancelling the current game!" +
                            "\nTwo votes are required to cancel the game!");
                    cancelGameId.put(chatId, new Pair<>(messageId, userId));
                }
            }

        } else {
            sendMessageToId(chatId, "No game running!");
        }
    }

    private void resendRequest(Update update) {
        long chatId = update.getMessage().getChatId();

        if (!checkGameInProgress(chatId)) {
            sendMessageToId(chatId, "No game running!");
        } else {
            mediator.resend(chatId);
        }
    }

    private boolean checkGameInProgress(long chatId) {
        return mediator.queryInProgress(chatId);
    }

    public int sendMessageToId(long chatId, String text) {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text)
                .enableMarkdown(true);
        try {
            return execute(message).getMessageId();
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
        return -1;
    }

    public int sendMessageToId(long chatId, String text, boolean markdown) {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text)
                .enableMarkdown(markdown);
        try {
            return execute(message).getMessageId();
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
        return -1;
    }

    public void editMessage(long chatId, int messageId, String newText) {
        EditMessageText edit = new EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText(newText)
                .enableMarkdown(true);
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
    }

    public void deleteMessage(long chatId, int messageId) {
        DeleteMessage delete = new DeleteMessage()
                .setChatId(chatId)
                .setMessageId(messageId);
        try {
            execute(delete);
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
    }

    private List<List<InlineKeyboardButton>> processButtons(String[][] buttonStrings) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        for (int i = 0; i < buttonStrings.length; i++) {
            List<InlineKeyboardButton> currentRow = new ArrayList<>();
            for (int j = 0; j < buttonStrings[i].length; j++) {
                String currentButton = buttonStrings[i][j];
                if (currentButton == null) currentButton = " ";
                currentRow.add(new InlineKeyboardButton()
                        .setText(currentButton)
                        .setCallbackData(currentButton));
            }
            buttons.add(currentRow);
        }
        return buttons;
    }



    public int sendMessageWithButtons(long chatId, String text, String[][] buttonStrings) {

        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text)
                .enableMarkdown(true);

        InlineKeyboardMarkup keyboardButtons;

        if (buttonStrings != null) {
            keyboardButtons = new InlineKeyboardMarkup()
                    .setKeyboard(processButtons(buttonStrings));
        } else {
            keyboardButtons = null;
        }

        message.setReplyMarkup(keyboardButtons);

        try {
            return execute(message).getMessageId();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        return -1;

    }

    public void editMessageButtons(long chatId, int messageId, String[][] buttonStrings) {
        InlineKeyboardMarkup buttonKeyboardObject;
        if (buttonStrings != null) {
            buttonKeyboardObject = new InlineKeyboardMarkup()
                    .setKeyboard(processButtons(buttonStrings));
        } else {
            buttonKeyboardObject = null;
        }

        EditMessageReplyMarkup editMessageReply = new EditMessageReplyMarkup()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setReplyMarkup(buttonKeyboardObject);

        try {
            execute(editMessageReply);
        } catch (TelegramApiException e) {
            System.err.println(e);
        }

    }


    @Override
    public void registerGameEnded(GameLogger logs) {
        long chatId = logs.getGameId();

        if (cancelGameId.containsKey(chatId)) {
            cancelGameId.remove(chatId);
        }

        String hash = hasher.hashGame(logs.getGameReplay());

        InlineKeyboardButton regameButton = new InlineKeyboardButton()
                .setText("Start a new game!")
                .setCallbackData(REGAME_STRING);

        InlineKeyboardButton replayLinkButton = new InlineKeyboardButton()
                .setText("View replay")
                .setUrl(createWebLink(hash));

        InlineKeyboardMarkup newMarkup = new InlineKeyboardMarkup()
                .setKeyboard(List.of(List.of(regameButton), List.of(replayLinkButton)));

        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText("Game has ended!")
                .setReplyMarkup(newMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println(e);
        }
    }

    private String createWebLink(String hash) {
        return WEBSITE_LINK + "#" + hash;
    }


    public String getBotUsername() {
        return "O_Bridge_Bot";
    }

    public String getBotToken() {
        return "";
    }

}
