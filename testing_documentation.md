# Testing Documentation

### Contents
[1. Introduction]   <br/>
[2. Internal Testing] <br/>
[3. Testing User Interactions] <br/>

## 1. Introduction 
Here, we document the different testing methodologies and test cases we have conducted to ensure our program is working properly.

## 2. Internal Testing
We look at the different tests we have conducted for our lower level abstractions such as the basic packages.

####  2.1 Basic Packages:
For the [Card](https://github.com/chan-j-d/BridgeBot/blob/master/src/main/java/Card.java) and [Bid](https://github.com/chan-j-d/BridgeBot/blob/master/src/main/java/Bid.java) 
classes, the main issue we realised was in interpreting String inputs accurately. We do not want either package to be able to create
a valid Card/Bid object that is not supposed to be valid, or wrongly rejects a String that should be valid.

The logic tree in order to ensure that can be tricky. For example, most cards and bids are length 2 with the suit emoji and the number. 
However, we want to also allow the use of 'K' for King and 'Q' for Queen. Additionally, the '10' cards and no trump bids are of length 3.
As such, since there are not that many possible <4 length String inputs, we decided to test all of them.

Example for the Card class:

    public static void main(String[] args) {
        Card card;
        //length 2 test
        for (int i = 0; i < 128; i++) {
            for (int j = 0; j < 128; j++) {
                try {
                    card = Card.createCard(((char) i) + "" + ((char) j));
                    System.out.println(card);
                } catch (IllegalArgumentException e) {
                    System.err.println(e);
                }
            }
        }
        //length 3 test
        for (int i = 0; i < 128; i++) {
            for (int j = 0; j < 128; j++) {
                for (int k = 0; k < 128; k++) {
                    try {
                        card = Card.createCard(((char) i) + "" + ((char) j) + ((char) k));
                        System.out.println(card);
                    } catch (IllegalArgumentException e) {
                        System.err.println(e);
                    }
                }
            }
        }
    }
    
This is important as Telegram users have the option of manually typing out their input. As such, we have to ensure that we validate
their input and only accept valid ones for each scenario.

#### 2.2 Testing the game engine
However, often times the programs are too large to test every single outcome.
As such, what we have done is create a LocalIOInterface in place of the Telegram bot so that we can use our own input/output stream
to test the game engine with custom inputs

##### 2.2.1 Testing game rules
First off, we have to test that the basic game rules are being performed correctly given valid inputs. Below are the ones we tested for first:
- Turn order is updated correctly after 
    - each bid
    - partner card is chosen (same player for NT bids and the next player for other bids)
    - each card played
    - the end of a trick
- Highest bid and player is accurately recorded as the bid winner
- Bid ends after 3 consecutive passes (provided there is at least one valid bid) or when the largest bid is chosen (7NT)
- After partner card is chosen, the separation into pairs is done correctly
- Trick counts are accurate and the correct player is attributed with each trick
- Determine that one of the pairs of partners have reached the required number of bids and declare a winner

##### 2.2.2 Testing safety features
Next, we try to account for invalid inputs and ideally provide an accurate error message that points the user to perform a valid action.
We accounted for the following. It is presented in the form of "case (expected outcome)"

_Bidding:_
- Clicked bids (registered correctly)
- Typed bids:
    - Invalid bid Strings (not registered)
    - Valid bid String but invalid according to rules (larger-bid-required error)
    - Valid bids (registered correctly)
    
_Partner Card:_
- Invalid card Strings (not registered)
- Valid card String but player has the card (cannot-choose-own-card error)
- Valid card (registered correctly)

_Trick-taking Gameplay:_
- Trump not broken - first card:
    - Player only has cards of the trump suit:
        - Player plays card of trump suit (registered correctly)
    - Player has cards of other suits:
        - Player plays card of trump suit (trump-not-broken error)
        - Player plays card of other suit (registered correctly)
    - Player types card (not advertised):
        - Invalid card String (not registered)
        - Player does not have card (no-such-card error)
        - Player has card (registered correctly)
- Trump not broken - other players:
    - Player has card of starting suit
        - Player plays card not of starting suit (must-play-starting-suit error)
        - Player plays card of starting suit (registered correctly)
    - Player does not have cards of starting suit:
        - Player plays non-trump card (registered correctly)
        - Player plays trump-suit card (registered correctly and trump-broken)
- Trump broken - first card:
    - Player plays card (registered correctly)
- Trump broken - other players:
    - Player has card of starting suit
        - Player plays card not of starting suit (must-play-starting-suit error)
        - Player plays card of starting suit (registered correctly)
    - Player does not have cards of starting suit:
        - Player plays card (registered correctly)
   
When a player encounters an error, they will be given the same request again until they either 
provide a proper response or the game times out (default at 5 minutes).


##### 2.2.3 General testing
Beyond this, what we have tried to do is run as many full gameplay as time would allow.
We have conducted about 100+ full game playthroughs. Besides this, we also created artificial players with a very simplistic 
card-selecting program to run artificial full playthroughs on selected bids in order to inspect the result. 

##### 2.2.4 Logging & Game history
We have also created a logging system that automatically saves completed game logs on the local machine. This allows us to look through
the playthrough and the updates to look for any issues. 

## 3. Testing User Interactions
We also test for user interactions with the Telegram bot commands. We considered the following test cases for bot commands:

1. Player has not registered with the bot (Sends a message prompting the user to "/start" with the bot)
2. /help command
    - In private chat (Player receives a reply from the bot of the relevant sub-commands for help)
    - In group chat (Player receives a private message from the bot)
3. Bot commands besides /help in private chat
    - /resend 
        - no game running (no response)
        - game running (resends the relevant messages)
    - all other commands (only-in-group-chat error)
4. Bot commands other than create commands and help command in group chat before game is created (no-game-running error)
5. Testing combinations of commands
    - Create > some commands > create (game-already-created error)
    - Create > join > join (already-in-game error in private message) //Tests for double joining
    - Create > join > leave > join > 3 other players join > start (game starts normally) //Tests if players are able to leave and rejoin
    - Create > 1/2 other players join > join > 2/1 other players join > leave > join start (game starts normally) //Tests if order is accurate after player leaves
    - Create > 3 other players join > join > join > leave > join > start (game starts normally) //Tests if players over-joining creates a problem with the ID-holder
    - Game finishes > create > 3 other plays join > join > start (game starts normally)//Tests if the bot resets correctly after each game
6. Testing /cancelgame command
    - Create > cancel > join (join fails) //Tests if the cancel command works
    - Create > 3 players join and leave > cancel > create > 4 players join > start (game starts normally) //Tests if cancelling before game starts resets users ability to join correctly
    - Create > 4 players join > start > cancel > create > 4 players join > start (game starts normally) //Tests if cancelling after game starts resets users ability to join correctly
    - Testing /cancelgame command's voting system while game is in progress
        - Adminstrator non-player cancels the game (game cancelled by adminstrator)
        - Adminstrator player cancels the game (game cancelled by administrator)
        - Non-player cancels the game (unable-to-cancel error)
        - Player cancels the game for the first time (voting message sent, requiring 2 votes)
        - Same player attempts to cancel the game twice (only-vote-once error)
        - Different player votes to cancel the game as well (game cancelled)
7. Testing with multiple (3) groups, we check that 
    - responses registered accurately to the correct group and player
    - player unable to join more than one game in different groups
