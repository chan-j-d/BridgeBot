# BridgeBot

![poster](/images/poster.jpg)

### Contents
[1. Introduction](https://github.com/chan-j-d/BridgeBot#1-introduction) <br/>
[2. Using the bot](https://github.com/chan-j-d/BridgeBot#2-using-the-bot) <br/>
[3. Program structure](https://github.com/chan-j-d/BridgeBot#3-program-structure) <br/>
[4. Program flow](https://github.com/chan-j-d/BridgeBot#4-program-flow-and-how-they-work) <br/>

## 1. Introduction
BridgeBot is a Telegram bot used to facilitate games of _floating bridge_ on Telegram. 
Besides the main game, we also aim to help newer plays learn the game by including recommendation features
and a replay system.

Link to replay system repository: https://github.com/fyshhh/BridgeBot

## 2. Using the bot
#### 2.1. Starting a game
 1. Add the bot @O\_Bridge\_Bot to your Telegram group of choice
 2. All users who are interested to play will have to enable the bot by going to @O\_Bridge\_Bot and sending the bot the message "/start"
 3. Create a game by using the command "/creategame@O\_Bridge\_Bot" in a group chat
 4. For creating practice games, instead us the command "/createpracticegame@O_Bridge_Bot" and then select one of the three modes
 5. Users can join with the command "/joingame@O\_Bridge\_Bot" or clicking the "Join Game" button
 6. Once four players have joined, will automatically start 
 
#### 2.2. Playing the game
 1. On game start: Players will be sent their hand with via private messaging<br/>
 ![sample player hand](/images/player_hands.png)
 2. Bidding process: A random player will be chosen to start the bidding. A request will be sent via private messaging to players when it is their turn to bid. 
 The bot will send players an array of buttons with the next five (or fewer if the bid is high) larger bids and a "Pass" option. Players can also create their own bid based on the format specified by the request.
 Players will be notified of each other's bid via the group chat.<br/>
 ![sample player bids](/images/bidding.png)
 3. Choosing a partner card: Once a highest bid is found, the player who had the highest bid will be asked for a partner. 
 The player will have to send his chosen partner card based on the format specified by the request
 4. Trick-taking play (How to play a card): Once bidding has ended, players will be notified when it is their turn to play a card via private messaging.
 Players choose which card they would like to play by clicking on the corresponding button in their 'hand'
 5. Trick-taking play (Updates): After each card is played, an update is sent to the group. The game feed is updated showing the newest card played.
 If it is the fourth card played in a trick, the trick is awarded to the winner and the number of tricks each player has is updated.<br/>
 ![sample_game_feed](/images/sample_game_feed.jpg)
 6. Ending the game: Once either pairs of players have reached the required number of tricks, the game terminates, announcing the winners.
 Two buttons will be sent:
    1. "Start a new game": Immediately starts a new game in the same game mode with the same players
    2. "View the replay": This opens a link to a page where users are able to view a replay of the game that just concluded
    
#### 2.3. Other features and commands:
 - "/cancelgame": Used for prematurely ending the current game
    - Only usable in a group chat
    - Group adminstrators that use the command will be able to cancel the game immediately
    - Only current players will be able to vote to end the game. 2 votes (out of 4) are required to cancel the game
 - "/help": Used for requesting help from the bot 
 - "/leavegame": For players to leave a game during the joining phase
 - "/resend": The bot resends the current game feed (for group chats) or player hand (for private chats)
    - This command is mainly used for reducing the amount of scrolling that users need to do to view the game feed
    
#### 2.4. Miscellaneous bot features
 - The bot has a default timer of 300s before the game automatically cancels due to inactivity
    
## 3. Program Structure
#### 3.1. Overall program structure
![general_program_structure](/images/general_program_structure.jpg)
 
- _Bridge Telegram Bot_: Only class that directly interacts with the Telegram API. As such, it handles all user inputs and bot commands. It is also the only program that sends messages to users/group chats. All other programs have to go through this program to reply to users.

- _Bridge Program_: Handles all the game interactions. It accepts an input (bid during the bidding phase and a card during the trick-taking phase) and changes its internal state. Then returns a GameUpdate based on the current state of the game to be broadcasted. The GameUpdate will include:
    - Visual updates (e.g. card being removed from the hand)
    - Errors (e.g. player submits an invalid response)
    - Requests (e.g. request for the next card to be played by the specific player)

- *Player Interface*: Represents a Player object that is aware of its own hand and is capable of bidding, choosing a partner card and choosing a card to play. They will have to override the following methods:
      
      Bid getBid();
      Card getPartnerCard();
      Card.getFirstCard(boolean trumpBroken, char trumpSuit);
      Card getNextCard(char firstSuit, char trumpSuit);
    This is to accommodate for other types of Player objects besides a TelegramPlayer such as any form of self-playing bots that we might create for testing or additional features. 


- *Mediator Implementation*: This class handles the interactions between Player objects, the IOInterface and the GameEngine. This is to decouple the programs so that they are all reliant only on the mediator and not each other.

#### 3.2. Basic building blocks:
##### 3.2.1. Card: Encapsulates a poker card
- The suits are represented by their first letters
- Number representations for ACE, KING, QUEEN and JACK are as follows:
    - ACE = 1 (char representation is 'A')
    - KING = 13 (char representation is 'K')
    - QUEEN  = 12 (char representation is 'Q')
    - JACK = 11 (char representation is 'J')
- Overrides `int hashCode()` and `boolean equals(Object o)` for ease of use
- Use *Card.createCard(String s)* to create a card
    - The factory method accepts a string of length 2 or 3 of the form "**XO**"
    - where **X** is the suit represented by one of the 4 letters (case-insensitive)
    - where **O** is the number representing the card's value
        - ACE can be created with "1" or "A"
        - KING, QUEEN, JACK can be created with "K", "Q", "J" or "13", "12", "11" respectively
    - This method also allows for creation of a card using the emoji suits instead. Specifically (♠, ♥, ♣, ♦)
- Use the available Comparators to compare the cards depending on the situation 

##### 3.2.2. Bid: Encapsulates a bid from a player
- The suits are represented similarly as to a Card
- A NO TRUMP bid is represented internally with 'N' and a pass bid by 'P'
- Implements `Comparable<Bid>` which allows comparison between Bids in the same way that floating bridge bidding is done
- Use *Bid.createBid(String s)* to create a bid
    - The factory method accepts a string of length 1 to 4 (2 to 3 for normal bids with 1 and 4 being pass bids)
    - Pass bids are created with an input string of either "P" or "PASS" (case-insensitive)
    - NT bids are created with "**X**NT" where **X** is the number of the bid
    - All other bids are represented by **OX**
        - where **O** is the bid number
        - where **X** is the suit for bidding
    - This method also allows for creation of a card using the emoji suits instead. Specifically (♠, ♥, ♣, ♦)

##### 3.2.3. IndexUpdate: Represents a sub-update from the engine
- Contains three components
    1. the receiver (represented by an integer between 0 and 5)
    2. message
    3. UpdateType (an enum field)
- On each iteration, the engine returns a series of IndexUpdates in the form of a GameUpdate. A mediator then decides how to broadcast these updates

##### 3.2.4. Comparators:
- `BridgeStandardComparator` allows comparison of cards with the following rules
    1. 'S' > 'H' > 'D' > 'C'
    2. ACE = 1 > 13 > 12 > ... > 2
- `BridgeTrumpComparator` allows comparison of cards for a specific trick
    - Requires two arguments as inputs, the trump suit (*T*) and the suit of the first card played (*F*)
        1. if (*T* != *F*), then *T* > *F* > other suits
        1. else *T* = *F* > other suits
        2. ACE = 1 > 13 > 12 > ... > 2 

##### 3.2.5. Other basic classes:
- `Deck`: Encasuplates a deck. Able to shuffle itself and distribute cards
- `CardCollection`: List of Cards
- `GameUpdate`: List of IndexUpdates
- `PlayerState`: Encapsulates what a player 'owns' in a game (e.g. hand and current tricks)

#### 3.3. Game engine-related classes
##### 3.3.1. Game Engine
- This is a steady state engine which holds all the information of its current state. Using it requires querying whether 
the engine is currently in the bidding phase or the trick-taking phase
- Players are identified with integers 1-4, with 0 representing the group itself. The engine has no knowledge of the identity of Telegram users <br/>
![engine program structure](/images/game_engine_structure.jpg)
- The engine contains a `GameLogger` and a `BidCoordinator`
- GameUpdate returns includes all relevant information updates to players and requests from players. For example, after player 3 wins a trick, the GameUpdate look similar to the following
    - `3 (the player this is targeted to): "Your turn to play a card!", UpdateType.REQUEST`
    - `0 (group chat): "Player 3 wins trick X", UpdateType.UPDATE`
    - `0: "(empty new trick)", UpdateType.EDIT_HAND`
    - `3: "(new hand with the previously played card removed)", UpdateType.EDIT_HAND`
- Invalid Cards are handled here by sending an Error-typed update and the same request again
    - Error updates contain the reason why their Bid/Card was rejected
- With each valid Bid made, or Card played, the game logger is updated. Additionally, all GameUpdates will be added to the logs

##### 3.3.2. GameLogger: Interface to record the proceedings of a game
    void addUpdate(GameUpdate update); //Adds a GameUpdate
    void addBid(int player, Bid bid); //Adds a bid
    void addCardPlayed(int player, int turn, Card card); //Adds details of the card played
    void addPartnerCard(Card card);
    void setLastTrickWinner(int player);
    void addCardsNotPlayed(int player, CardCollection cards);
    Iterator<GameUpdate> getUpdateHistory();

    GameReplay getGameReplay();
    long getGameId();
    
- The main reason for this class is to obtain the `GameReplay` for hashing and for bug fixing

##### 3.3.3. GameReplay: 
    int getFirstPlayer();
    
    int getNumBids();
    Bid getBid(int bidNumber);;

    Card getPartnerCard();

    int getNumTricks();
    Card getCardPlayed(int trickNumber, int player);
    int getTrickWinner(int trickNumber);
    CardCollection getUnplayedCards(int player);
    
- The main reason for this class is for hashing the game replay in order to be run on our website

##### 3.3.4. BidCoordinator: A minor program used to simulate the bidding process

#### 3.4. Player: An abstract class with the following contract
    Bid getBid();
    Card getPartnerCard();
    Card.getFirstCard(boolean trumpBroken, char trumpSuit);
    Card getNextCard(char firstSuit, char trumpSuit);
 - This allows us to create new classes such as AI-bots to play the game
    - This can help us with testing of the game engine locally and more importantly, create auto-playing bots
 - Visit section 4.1 to see how Telegram user's responses are obtained
 
#### 3.5. BridgeBot: The program that controls the Telegram Bot
- In general, Telegram bots can only respond to user prompts via the method `onUpdateReceived(Update update)`. 
Every button pressed and every message sent will be sent to the bot as an update
- In order to reduce clutter, we have reduced the number of ways that users can interact with the bot 
    - In groups, the bot only responds to bot commands (the buttons also send a bot command)
    - In private messaging, the bot is able to accept messages as responses but we try to use buttons as much as possible
- The bot only ever uses the mediator to start a game by providing the relevant user and group IDs to the mediator

#### 3.6. Mediator `TeleEngineMediator`: 
![mediator structure](/images/mediator_structure.jpg)
- This program controls the flow of information between the engine, the bot and the users
- It is capable of using the `IOInterface`'s methods to send messages directly to Telegram users
    - Thus, this program determines how updates are broadcasted as well `broadcastUpdatesFromEngine(GameEngine engine, GameUpdate update)`
    - We have created ViewerInterfaces to separately control how Telegram users and group chats receive the messages from the engine

##### 3.6.1. Viewer Interfaces:
- The main purpose of these interfaces is to separately control how IndexUpdates are viewed by the user/group
- It allows us to incorporate additional graphics and keep relevant information that are not sent with each update
- There consists of two main types of implementations of these
    1. `BridgeUserInterface`: Controls what a user sees in the private chat with the bot
    2. `BridgeGroupInterface`: Controls what the group sees and updates the current state of the game
    
##### 3.6.2. Logs Manager:
- After each game concludes, this program retrieves the game logs of the game that just concluded and saves it locally to a designated directory
- For reviewing games after they have ended. Used for verifying that other features are working

## 4. Program flow and how they work
In this section, we will be going through how some of the more important systems work.
#### 4.1. Interacting with the bot
![processing user updates](/images/processing_user_update.jpg) <br/>
- `creategame`: creates a new GameChatId object (holder for 5 chat IDs with the 0th index being the group chatID by default) and add it to a hash map with the key being the group ID
    - Checks that the group ID does not already have a running game
- `joingame`: Adds the ID of the user who sent the command to the GameChatId object
    - Checks that the user is not currently in another game in a different group
    - Checks that the user has not already joined the same game
    - Checks that the game does not already have four players
- `leavegame`: Removes the ID of the user from the current GameChatID object
    - Checks that the user is in the current game
    - Checks that the game has not already started
- `cancelgame`: Cancels the game that is currently running
    - Checks whether the user is an adminstrator of the group, if they are, the game is cancelled
    - Otherwise, check that the player is in the game. Only players in-game can vote
    - If this is the first time the command is called in the game, a vote is started and the user's ID is recorded
    - If this is not the first time the command is called, we check that the user ID is not the same as the first one. If they are distinct, we cancel the game

#### 4.2. Registering user response
Here, we look at an instance of how we obtain user response. The main idea is that when the `TelegramPlayer`'s `getNextBid(..)` is called, 
the class signals that it is waiting for a response by setting `boolean awaitingBidResponse = true`. It then creates a new thread
and waits for 300 seconds. If during these 300 seconds, the appropriate user sends an appropriate response, then the response is saved, 
the thread is interrupted and the response is returned

##### 4.2.1. Overall flow example (Requesting a card to be played)
![overall flow](/images/sample_program_flow.jpg) <br/>
1. After a GameUpdate is returned by the engine, the mediator registers it and broadcasts the update
2. One of the updates will be a request from a player for the next Card to be played
3. At the same time, the mediator asks the TelegramPlayer object for its next Card. This causes the player to start waiting
4. When the user receives the request from the bot, they will (hopefully) respond
5. The response is received in the form of either a message or callback query (from a button pressed). This response is registered
6. The TelegramPlayer object reads the response and if it is valid, interrupts the sleeping thread and returns the response
7. The mediator acquires the response from the TelegramPlayer and passes it on to the engine
8. The engine processes the Card played and returns a new GameUpdate and the process repeats

#### 4.3. Ending a game and replay system
![game ends](/images/ending_game.jpg) <br/>
We have chosen for the BridgeBot program to be the one handling the hashing as only the Telegram user needs to know about the 
replay.
1. As the mediator runs the game as a while loop, it consistently queries the engine to see if the game has ended
2. When the game ends, the mediator records a copy of the logs by giving it to the `LogsManagement` class 
(which saves a copy of it)
3. The mediator passes another copy of the `GameLogger` object to the `BridgeBot`
4. `BridgeBot` retrieves the `GameReplay`, parses it through a `GameHash` which returns a String. We then link the user directly to the site
with the replay String

For more details on the replay's hashing, please visit the repository linked in Section 1.





    

