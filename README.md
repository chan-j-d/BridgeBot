#BridgeBot

![poster](/images/poster.jpg)

#### Contents


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
 4. Users can join with the command "/joingame@O\_Bridge\_Bot" or clicking the "Join Game" button
 5. Once four players have joined, the game can be started with the command "/startgame@O\_Bridge\_Bot" or by clicking the "Start Game" button
 
#### 2.2. Playing the game
 1. On game start: Players will be sent their hand in the form of 13 Telegram buttons in a 3 x 5 grid via private messaging
 ![sample player hand](/images/player_hands.png)
 2. Bidding process: A random player will be chosen to start the bidding. A request will be sent via private messaging to players when it is their turn to bid. 
 The bot will send players an array of buttons with the next five (or fewer if the bid is high) larger bids and a "Pass" option. Players can also create their own bid based on the format specified by the request.
 Players will be notified of each other's bid via the group chat.
 ![sample player bids](/images/bidding.png)
 3. Choosing a partner card: Once a highest bid is found, the player who had the highest bid will be asked for a partner. 
 The player will have to send his chosen partner card based on the format specified by the request
 4. Trick-taking play (How to play a card): Once bidding has ended, players will be notified when it is their turn to play a card via private messaging.
 Players choose which card they would like to play by clicking on the corresponding button in their 'hand'
 5. Trick-taking play (Updates): After each card is played, an update is sent to the group. The game feed is updated showing the newest card played.
 If it is the fourth card played in a trick, the trick is awarded to the winner and the number of tricks each player has is updated.
 ![sample_game_feed](/images/sample_game_feed.jpg)
 6. Ending the game: Once either pairs of players have reached the required number of tricks, the game terminates, announcing the winners.
 Two buttons will be sent:
    1. "Create a new game"
    2. "View the replay": This opens a link to a page where users are able to view a replay of the game that just concluded
    
#### 2.3. Other features and commands:
 - "/cancelgame": Used for prematurely ending the current game
    - Only usable in a group chat
    - Group adminstrators that use the command will be able to cancel the game immediately
    - Only current players will be able to vote to end the game. 2 votes (out of 4) are required to cancel the game
 - "/help": Used for requesting help from the bot (Work-in-progress)
 - "/leavegame": For players to leave a game during the joining phase
 - "/resend": The bot resends the current game feed (for group chats) or player hand (for private chats)
    - This command is mainly used for reducing the amount of scrolling that users need to do to view the game feed
    
#### 2.4. Miscellaneous bot features
 - The bot has a default timer of 90s before the game automatically cancels due to inactivity
    - (To be implemented) Users will be able to set the amount of time given for each turn
    
 
 
 
### Program Structure

#### Player
Player is an abstract class which requires the implementing class to override the following methods:
```$xslt
Bid getBit(); //Gets the next bid from the player
Card getPartnerCard(); //Gets the partner card from the player if they win the bid
Card getNextCard(); //Gets the next card that the player wants to play for that trick
```
This forms one of the more important modules in this program. We can customise the methods to create TestPlayers, TelegramPlayers and AI-players in the future.

####GameCoordinator & BidCoordinator
An instance of the GameCoordinator class will run one simulation of a game. It consists of 4 phases:
1. Bidding
    - This is handled by the BidCoordinator's static **bidding(Player[] players)** method
    - BidCoordinator utilises the Player's **getBid()** method in order to determine the winning bid
2. Determining a partner
    - Utilises the Player's **getPartnerCard()** method
3. Trick-taking play
    - The GameCoordinator will handle the trick-taking play until one team has won sufficient tricks
    - Handled by the **conductTurnCycle(...)** method in GameCoordinator
4. Declaring a winner
    - If the game is not cancelled prematurely, then a winning pair will be declared after one team has won sufficient tricks

####Basic building blocks: Card, Bid, CardCollection
#####Card: encapsulates a poker card with a symbol and number
- The suits are represented by their first letters
- Number representations for ACE, KING, QUEEN and JACK are as follows:
    - ACE = 1 (char representation is 'A')
    - KING = 13 (char representation is 'K')
    - QUEEN  = 12 (char representation is 'Q')
    - JACK = 11 (char representation is 'J')
- Overrides **int hashCode()** and **boolean equals(Object o)**
- Use *Card.createCard(String s)* to create a card
    - The factory method accepts a string of length 2 or 3 of the form "*XN*"
    - where *X* is the suit represented by one of the 4 letters (case-insensitive)
    - where *N* is the number representing the card's value
        - ACE can be created with "1" or "A"
        - KING, QUEEN, JACK can be created with "K", "Q", "J" or "13", "12", "11" respectively
- Use the available Comparators to compare the cards depending on the situation 

#####Bid: encapsulates a bid from a player with a number and a suit
- The suits are represented similarly as a card
- A NO TRUMP bid is represented internally with 'N' and a pass bid by 'P'
- Implements Comparable\<Bid> which allows comparison between Bids in the same way that floating bridge bidding is done
- Use *Bid.createBid(String s)* to create a bid
    - The factory method accepts a string of length 1 to 4 (2 to 3 for normal bids with 1 and 4 being pass bids)
    - Pass bids are created with an input string of either "P" or "PASS" (case-insensitive)
    - NT bids are created with "*X*NT" where *X* is the number of the bid

#####CardCollection: encapsulates any collection of Cards
- Directly extends *java.util.ArrayList\<Card>* and thus can use all methods that an *ArrayList* can

####Intermediate Blocks: Deck, BridgePlayingField, Action
#####Deck: encapsulates a deck of poker cards
- Use the *Deck.init()* method to create a new deck of poker cards that is shuffled (*Deck.initWithoutShuffling()* for an unshuffled deck)
- Use the *deck.draw()* to remove and return the top card of the deck. Can only be done 52 times before it throws an exception

#####Action: encapsulates a move of a Card from one CardCollection to another
- The use of this is mainly for adding Listeners

#####BridgePlayingField
- Contains the hands of the four players and trick-taking 
- Players that play a card from their hand is represented by an *Action* object that moves the card from a player's hands to a holding area

####Comparators:
- *BridgeStandardComparator* allows comparison of cards with the following rules
    1. 'S' > 'H' > 'D' > 'C'
    2. ACE = 1 > 13 > 12 > ... > 2
- *BridgeTrumpComparator* allows comparison of cards for a specific trick
    - Requires two arguments as inputs, the trump suit (*T*) and the suit of the first card played (*F*)
        1. if (*T* != *F*), then *T* > *F* > other suits
        1. else *T* = *F* > other suits
        2. ACE = 1 > 13 > 12 > ... > 2 

###To be implemented:
1. TelegramPlayer 
    - handles the interaction with the Telegram user
2. AIPlayer
    - has its own logic to perform the *getBid()*, *getPartnerCard()* and *getNextCard()* methods
    
###Current known issues:
1. No restrictions on the type of cards a player can play in their turn

###Potential changes:
1. Change GameCoordinator into an abstract class which requires the implementation of 
    - **void updateToGroup(String message)**: sends an update to a group depending on how this program is currently ran. For example, a testing environment could consist of directly printing the message whereas one that interacts directly with the Telegram group could send the message to the group
    - **void updateToPlayer(Player player, String message)**: similar functionality as above 
