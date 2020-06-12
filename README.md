#BridgeBot

Telegram bot for playing floating/Singaporean bridge. (Work in progress)

###Program Structure

####Player
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
