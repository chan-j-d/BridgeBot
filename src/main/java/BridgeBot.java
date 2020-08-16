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
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.*;

public class BridgeBot extends TelegramLongPollingBot implements IOInterface {

    private static final String WEBSITE_LINK = "https://fyshhh.github.io/bridgebot/";
    private static final String REGAME_STRING = "regame";
    private static final int BOT_BLOCKED_ERROR_CODE = 403;

    private ClientEngineMediator mediator;

    //While bot is not daemonized, we will ignore all updates coming before time the bot is initialized
    private int timeOfStart;

    public BridgeBot(ClientEngineMediator mediator) {
        this.mediator = mediator;
        this.timeOfStart = (int) (System.currentTimeMillis() / 1000L);
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

    private static final int NORMAL_GAME = 0;
    private static final int OPEN_HAND = 1;
    private static final int CARDS_PLAYED = 2;
    private static final int SUITS_PLAYED = 3;
    private static final int TUTORIAL = 4;
    private static final String OPEN_HAND_CALLBACK_DATA = "openhandgame";
    private static final String CARDS_PLAYED_CALLBACK_DATA = "cardsplayedgame";
    private static final String SUIT_PLAYED_CALLBACK_DATA = "suitsplayedgame";



    //HashMap for recording cancelGame messageIds for voting
    private HashMap<Long, Pair<Integer, Integer>> cancelGameId = new HashMap<>();

    //Current HashImpl
    private GameHash hasher = new GameHashImpl1();

    //HelpManager to manage help commands separately
    private HelpManager helpManager = HelpManager.init();

    //Valid commands, edit this and create the relevant method in order to include it as a Telegram bot command.
    private static List<String> validCommands = Arrays.asList(
            "joingame",
            "creategame",
            "createpracticegame",
            "cancelgame",
            "help",
            "leavegame",
            "resend",
            "skip",
            "next");

    /*
    Required method by the Telegram package. Only recognise bot commands in group chats and private messages.
    This includes callback queries for buttons.
     */
    public void onUpdateReceived(Update update) {

        int date = -1;
        if (update.hasMessage()) date = update.getMessage().getDate();
        else if (update.hasCallbackQuery()) date = update.getCallbackQuery().getMessage().getDate();
        if (date < timeOfStart) return;

        System.out.println(getUpdateSummary(update));

        if (update.hasCallbackQuery()) {

            boolean groupChat = update.getCallbackQuery().getMessage().isGroupMessage() ||
                    update.getCallbackQuery().getMessage().isSuperGroupMessage();
            CallbackQuery query = update.getCallbackQuery();

            if (groupChat) {
                String command = query.getData().split("@")[0].substring(1);
                processCommand(command, update);

                processCallbackString(update);

            } else {
                int userId = query.getFrom().getId();
                String message = query.getData();
                mediator.registerResponse(userId, message);
            }

        } else if (update.hasMessage()) {
            Message message = update.getMessage();
            boolean groupChat = update.getMessage().getChat().isGroupChat() ||
                    update.getMessage().getChat().isSuperGroupChat();

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
                        processPrivateCommand(command, update);
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

    private void processCallbackString(Update update) {
        String data = update.getCallbackQuery().getData();
        switch (data) {
            case REGAME_STRING:
                startGameRequest(update);
                break;
            case OPEN_HAND_CALLBACK_DATA:
                registerNewJoinGameWindow(update, OPEN_HAND);
                break;
            case CARDS_PLAYED_CALLBACK_DATA:
                registerNewJoinGameWindow(update, CARDS_PLAYED);
                break;
            case SUIT_PLAYED_CALLBACK_DATA:
                registerNewJoinGameWindow(update, SUITS_PLAYED);
                break;
        }

    }

    private void processPrivateCommand(String command, Update update) {
        long chatId = update.getMessage().getChatId();
        if (command.equals("start")) {
            sendMessageToId(chatId, "You have registered with us! Create a game in a group chat with the command " +
                    "/creategame@" + getBotUsername() + "!");
            return;
        }
        if (isValidCommand(command)) {
            switch (command) {
                case "help":
                    helpRequest(update);
                    break;
                case "resend":
                    mediator.resend(chatId);
                    break;
                case "next":
                    mediator.registerResponse(chatId, TutorialEngine.NEXT_STRING);
                    break;
                case "skip":
                    mediator.registerResponse(chatId, TutorialEngine.SKIP_STRING);
                    break;
                default:
                    sendMessageToId(chatId, "Command only valid in a group chat!");
            }
        } else if (helpManager.validHelpCommmand(command)) {
            sendMessageToId(chatId, helpManager.getHelpText(command));
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
            case "joingame":
                joinGameRequest(update);
                break;
            case "creategame":
                createGameRequest(update);
                break;
            case "createpracticegame":
                createPracticeGameRequest(update);
                break;
            case "cancelgame":
                cancelGameRequest(update);
                break;
            case "help":
                helpRequest(update);
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

        //FOR TESTING ONLY
        String testingMessage = update.getMessage().getText();
        String[] tempArray = testingMessage.split(getBotUsername());
        if (tempArray.length == 2 && tempArray[1].equals(" test")) {
            long chatId = update.hasMessage() ? update.getMessage().getChatId() :
                    update.getCallbackQuery().getMessage().getChatId();
            GameChatIds gameChatIds = new GameChatIds(4);
            gameChatIds.setGameType(TUTORIAL);
            gameChatIds.addChatId(chatId);
            for (int i = 0; i < 4; i++) {
                User user = update.getMessage().getFrom();
                gameChatIds.addUserIdAndName((long) user.getId(), user.getFirstName());
            }

            //Adjust test game type here!!
            mediator.addGameIds(gameChatIds);
            sendMessageToId(chatId, "Test game starting!");
            return;
        }
        //FOR TESTING ONLY

        registerNewJoinGameWindow(update, NORMAL_GAME);

    }

    private void registerNewJoinGameWindow(Update update, int gameType) {
        long chatId;
        String groupName;
        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
            groupName = update.getMessage().getChat().getTitle();
        } else {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            groupName = update.getCallbackQuery().getMessage().getChat().getTitle();
        }

        registerNewJoinGameWindow(chatId, groupName, gameType);

    }

    private void registerNewJoinGameWindow(long chatId, String groupName, int gameType) {
        //checks if a similar game has already started
        if (groupChatIds.containsKey(chatId) || checkGameInProgress(chatId)) {
            sendMessageToId(chatId, "Game already started!");
        } else {
            //Assigns the group chat Id with a name
            groupNames.put(chatId, groupName);

            System.out.println("reached here 1");

            //Creates the gameChatId object to store the game Ids
            GameChatIds gameChatIds = new GameChatIds(4);
            gameChatIds.setGameType(gameType);
            gameChatIds.addChatId(chatId);
            groupChatIds.put(chatId, gameChatIds);
            String stringToSend = createStartGameString(gameType);
            System.out.println("reached here 2");

            SendMessage sendMessage = new SendMessage()
                    .setChatId(chatId)
                    .setText(stringToSend)
                    .enableMarkdown(true);

            //Adds a join game button
            InlineKeyboardMarkup buttonMarkup = createJoinButtonMarkup();

            sendMessage.setReplyMarkup(buttonMarkup);
            System.out.println("reached here 3");

            try {
                Message message = execute(sendMessage);
                startGameMessageId.put(chatId, new Pair<>(
                        message.getMessageId(),
                        stringToSend));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            System.out.println("reached here 4");

        }

    }

    private String createStartGameString(int gameType) {
        return "Game initialised! (*" + getGameModeName(gameType) +
                "*)\nClick the button below or use the /joingame command!" +
                "\n==============\n*Players joined*: ";
    }

    private String getGameModeName(int gameType) {
        switch (gameType) {
            case NORMAL_GAME:
                return "Normal";
            case OPEN_HAND:
                return "Open hand";
            case CARDS_PLAYED:
                return "Cards-played";
            case SUITS_PLAYED:
                return "Suits-played";
            case TUTORIAL:
                return "Tutorial";
            default:
                throw new IllegalArgumentException("Invalid integer gametype");
        }
    }

    private InlineKeyboardMarkup createJoinButtonMarkup() {
        InlineKeyboardButton joinButton = new InlineKeyboardButton()
                .setText("Join Game")
                .setCallbackData("/joingame@" + this.getBotUsername());

        return new InlineKeyboardMarkup()
                .setKeyboard(List.of(List.of(joinButton)));
    }

    private InlineKeyboardMarkup createStartButtonMarkup() {
        InlineKeyboardButton startButton = new InlineKeyboardButton()
                .setText("Start Game")
                .setCallbackData("/startgame@" + this.getBotUsername());

        return new InlineKeyboardMarkup()
                .setKeyboard(List.of(List.of(startButton)));
    }

    //Create practice game request
    private void createPracticeGameRequest(Update update) {
        long chatId = update.getMessage().getChatId();
        String groupName = update.getMessage().getChat().getTitle();

        //checks if a similar game has already started
        if (groupChatIds.containsKey(chatId) || checkGameInProgress(chatId)) {
            sendMessageToId(chatId, "Game already started!");
        } else {

            String[] split = update.getMessage().getText().split(getBotUsername());
            if (split.length == 2) {
                int gameType = split[1].charAt(1) - 48;
                System.out.println("reached here:" + gameType);
                registerNewJoinGameWindow(chatId, groupName, gameType);
            } else {

                //String to be sent
                String stringToSend = "Select the game mode below or use the command \"/createpracticegame@" +
                        getBotUsername() + " X\" where X is the number for that mode.";

                InlineKeyboardButton openHandButton = new InlineKeyboardButton()
                        .setText("1. Open hand game")
                        .setCallbackData(OPEN_HAND_CALLBACK_DATA);

                InlineKeyboardButton cardsPlayedButton = new InlineKeyboardButton()
                        .setText("2. Shows all previous cards played")
                        .setCallbackData(CARDS_PLAYED_CALLBACK_DATA);

                InlineKeyboardButton suitsPlayedButton = new InlineKeyboardButton()
                        .setText("3. Shows the number of cards of each suit played")
                        .setCallbackData(SUIT_PLAYED_CALLBACK_DATA);

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup()
                        .setKeyboard(List.of(
                                List.of(openHandButton),
                                List.of(cardsPlayedButton),
                                List.of(suitsPlayedButton)
                        ));

                SendMessage sendMessage = new SendMessage()
                        .setChatId(chatId)
                        .setText(stringToSend);

                sendMessage.setReplyMarkup(markup);

                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }


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
            if (sendMessageToId(userId,
                    String.format("You have joined a bridge game in %s!",
                            groupNames.get(chatId))) == -1) {
                notifyRegister(chatId, firstName);
                return;
            }

            gameChatIds.addUserIdAndName(userId, firstName);

            userIds.add(userId);

            sendMessageToId(chatId, "*" + firstName + "* has joined the game!", "md");

            int messageIdToUpdate = startGameMessageId.get(chatId).first;
            String stringToEdit  = startGameMessageId.get(chatId).second + "\n - " + "*" + firstName + "*";

            EditMessageText editMessage = new EditMessageText()
                    .setChatId(chatId)
                    .setMessageId(messageIdToUpdate)
                    .enableMarkdown(true);

            startGameMessageId.get(chatId).second = stringToEdit;

            if (gameChatIds.checkFull()) {
                editMessage.setReplyMarkup(null);
            } else {
                editMessage.setReplyMarkup(createJoinButtonMarkup());
            }

            editMessage.setText(stringToEdit);

            try {
                execute(editMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            if (gameChatIds.checkFull()) startGameRequest(update);

        }
    }

    private void startGameRequest(Update update) {
        long chatId;
        boolean recreating = false;
        GameChatIds gameChatIds = null;

        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            chatId = query.getMessage().getChatId();
            if (query.getData().equals(REGAME_STRING)) {
                gameChatIds = mediator.getRecentGameChatIds(chatId);
                if (gameChatIds == null) {
                    sendMessageToId(chatId, "Re-game has expired. Please create a new lobby.");
                    return;
                }
                recreating = true;
            }
        } else if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else {
            chatId = -1L;
        }

        if (checkGameInProgress(chatId)) {
            sendMessageToId(chatId, "Game has already started!");
            return;
        }

        if (!recreating) gameChatIds = groupChatIds.get(chatId);

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
            e.printStackTrace();
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
            GameChatIds gameChatIds = groupChatIds.get(chatId);
            if (!gameChatIds.checkContainsId(userId)) {
                sendMessageToId(chatId, firstName + " is not currently in a game!");
                return;
            }

            String stringToEdit = startGameMessageId.get(chatId).second
                    .replaceFirst("\n - \\*" + firstName + "\\*", "");

            gameChatIds.removePlayerId(userId);
            userIds.remove(userId);

            int messageId = startGameMessageId.get(chatId).first;

            EditMessageText edit = new EditMessageText()
                    .setChatId(chatId)
                    .setMessageId(messageId)
                    .setText(stringToEdit)
                    .enableMarkdown(true);

            startGameMessageId.get(chatId).second = stringToEdit;

            edit.setReplyMarkup(createJoinButtonMarkup());

            sendMessageToId(chatId, firstName + " has left the game!");

            try {
                execute(edit);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

        }
    }

    private void cancelGameRequest(Update update) {
        long chatId = update.getMessage().getChatId();

        if (groupChatIds.containsKey(chatId)) {
            GameChatIds gameChatIds = groupChatIds.remove(chatId);
            for (int i = 1; i <= gameChatIds.getNumPlayers(); i++) {
                long playerId = gameChatIds.getChatId(i);
                userIds.remove(playerId);
            }
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
                if (status.equals("creator") || status.equals("adminstrator")) {
                    mediator.cancelGame(chatId);
                    if (cancelGameId.containsKey(chatId)) {
                        deleteMessage(chatId, cancelGameId.get(chatId).first);
                    }
                    sendMessageToId(chatId, "Game cancelled by adminstrator!");
                    adminstratorCancelled = true;
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            if (!adminstratorCancelled) {
                if (!mediator.containsUserId(userId)) {
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

    private void helpRequest(Update update) {
        boolean groupMessage = update.getMessage().isGroupMessage() || update.getMessage().isSuperGroupMessage();
        long sendId;
        if (groupMessage) {
            sendId = (long) update.getMessage().getFrom().getId();
        } else {
            sendId = update.getMessage().getChatId();
        }
        sendMessageToId(sendId, helpManager.getDefaultText());
    }

    private void resendRequest(Update update) {
        long chatId = update.getMessage().getChatId();

        if (!checkGameInProgress(chatId)) {
            sendMessageToId(chatId, "No game running!");
        } else {
            mediator.resend(chatId);
        }
    }

    private void notifyRegister(long chatId, String name) {
        sendMessageToId(chatId, "Hi " + name + ", we have noticed that you have not registered with us!" +
                "\nPlease click on @" + getBotUsername() + " and use the command \"/start\"");
    }

    private boolean checkGameInProgress(long chatId) {
        return mediator.queryInProgress(chatId);
    }

    public int sendMessageToId(long chatId, String text) {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text);
        try {
            return execute(message).getMessageId();
        } catch (TelegramApiRequestException e1) {
            e1.printStackTrace();
            if (e1.getErrorCode() == BOT_BLOCKED_ERROR_CODE) {
                return -1;
            }
        } catch (TelegramApiException e2) {
            e2.printStackTrace();
        }
        return 0;
    }

    public int sendMessageToId(long chatId, String text, String style) {
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText(text);

        if (style.equals("md")) {
            message = message.enableMarkdown(true);
        } else if (style.equals("html")) {
            message = message.enableHtml(true);
        }

        try {
            return execute(message).getMessageId();
        } catch (TelegramApiRequestException e1) {
            e1.printStackTrace();
            if (e1.getErrorCode() == BOT_BLOCKED_ERROR_CODE) {
                return -1;
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return 0;
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
            e.printStackTrace();
        }
    }

    public void deleteMessage(long chatId, int messageId) {
        DeleteMessage delete = new DeleteMessage()
                .setChatId(chatId)
                .setMessageId(messageId);
        try {
            execute(delete);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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

        return 0;

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
            e.printStackTrace();
        }

    }


    @Override
    public void registerGameEnded(GameLogger logs) {
        long chatId = logs.getGameId();

        cancelGameId.remove(chatId);

        String hash = hasher.hashGame(logs.getGameReplay());

        InlineKeyboardButton regameButton = new InlineKeyboardButton()
                .setText("Start another game!")
                .setCallbackData(REGAME_STRING);

        InlineKeyboardButton replayLinkButton = new InlineKeyboardButton()
                .setText("View replay")
                .setUrl(createWebLink(hash));

        InlineKeyboardMarkup newMarkup = new InlineKeyboardMarkup()
                .setKeyboard(List.of(List.of(regameButton), List.of(replayLinkButton)));

        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .setText("Game has ended!\nGame replay code: " + hash)
                .setReplyMarkup(newMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String createWebLink(String hash) {
        return WEBSITE_LINK + "#" + hash;
    }

    private String getUpdateSummary(Update update) {
        String chatName;
        String userName;
        String response;
        long date;
        long chatId;
        long userId;

        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            boolean groupChat = query.getMessage().isGroupMessage() ||
                    query.getMessage().isSuperGroupMessage();
            chatName = groupChat ? query.getMessage().getChat().getTitle() :
                    query.getFrom().getFirstName();
            chatId = query.getMessage().getChatId();
            userName = query.getFrom().getUserName();
            userId = query.getFrom().getId();
            response = query.getData();
            date = query.getMessage().getDate();
        } else {
            Message message = update.getMessage();
            boolean groupChat = message.isGroupMessage() ||
                    message.isSuperGroupMessage();
            chatName = groupChat ? message.getChat().getTitle() :
                    message.getFrom().getFirstName();
            chatId = message.getChatId();
            userName = message.getFrom().getUserName();
            userId = message.getFrom().getId();
            response = message.getText();
            date = message.getDate();
        }

        return String.format("Time: %d, Chat: (%d, %s), User: (%d, %s), Message: %s",
                date, chatId, chatName, userId, userName, response);
    }

    public String getBotUsername() {
        return "O_Bridge_Test_Bot";
    }

    public String getBotToken() {
        return "";
    }



}
