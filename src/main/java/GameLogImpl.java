import java.util.ArrayList;
import java.util.Iterator;

public class GameLogImpl implements GameLogger {

    private ArrayList<GameUpdate> updates;
    private CardCollection[] hands;
    private int firstPlayer;
    private int[] bidArray;
    private int[][] cardArray;
    private ArrayList<CardCollection> cardsUnplayed;
    private Card partnerCard;
    private int lastTrickWinner;

    private long gameId;
    private String processedString;

    private static final int NUM_BIDS = 35;
    private static final int NUM_TRICKS = 13;
    private static final int NUM_CARDS = 52;

    public GameLogImpl(long gameId, int firstPlayer) {
        this.firstPlayer = firstPlayer;
        this.gameId = gameId;
        updates = new ArrayList<>();
        bidArray = new int[NUM_BIDS];
        cardArray = new int[NUM_TRICKS][];
        for (int i = 0; i < NUM_TRICKS; i++) {
            cardArray[i] = new int[5];
        }
        processedString = null;
        cardsUnplayed = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            cardsUnplayed.add(new CardCollection());
        }

        this.hands = new CardCollection[4];
    }

    public void addUpdate(GameUpdate update) {
        this.updates.add(update);
    }

    public void addHands(CardCollection[] hands) {
        for (int i = 0; i < 4; i++) {
            this.hands[i] = hands[i].copy();
        }
    }

    public void addBid(int player, Bid bid) {
        bidArray[convertBidToIndex(bid)] = player;
    }

    public void addCardPlayed(int player, int turn, Card card) {
        int cardIndex = convertCardToIndex(card);
        cardArray[--turn][player] = cardIndex;
        if (cardArray[turn][0] == 0) {
            cardArray[turn][0] = player;
        }
    }

    public void addCardsNotPlayed(int player, CardCollection cards) {
        cardsUnplayed.set(player - 1, cards);
    }

    public void setLastTrickWinner(int player) {
        this.lastTrickWinner = player;
    }

    public void addPartnerCard(Card card) {
        this.partnerCard = card;
    }

    private int convertBidToIndex(Bid bid) {
        char suit = bid.getSuit();
        int index = (bid.getNumber() - 1) * 5;
        int add = -1;
        switch (suit) {
            case 'C':
                add = 0;
                break;
            case 'D':
                add = 1;
                break;
            case 'H':
                add = 2;
                break;
            case 'S':
                add = 3;
                break;
            case 'N':
                add = 4;
                break;
        }
        return index + add;
    }

    private Bid convertIndexToBid(int index) {
        int number = index / 5 + 1;
        int suitNumber = index - (number - 1) * 5;
        String suit = null;
        switch (suitNumber) {
            case 0:
                suit = "C";
                break;
            case 1:
                suit = "D";
                break;
            case 2:
                suit = "H";
                break;
            case 3:
                suit = "S";
                break;
            case 4:
                suit = "NT";
                break;
        }
        return Bid.createBid(number + suit);
    }

    private int convertCardToIndex(Card card) {
        char suit = card.getSuit();
        int index = (card.getNumber() - 1) * 4;
        int add = - 1;
        switch (suit) {
            case 'C':
                add = 0;
                break;
            case 'D':
                add = 1;
                break;
            case 'H':
                add = 2;
                break;
            case 'S':
                add = 3;
                break;
        }
        return index + add;
    }

    private Card convertIndexToCard(int index) {
        int number = index / 4 + 1;
        int suitNumber = index - (number - 1) * 4;
        String suit = null;
        switch (suitNumber) {
            case 0:
                suit = "C";
                break;
            case 1:
                suit = "D";
                break;
            case 2:
                suit = "H";
                break;
            case 3:
                suit = "S";
                break;
        }
        return Card.createCard(suit + number);
    }

    public String toString() {
        if (processedString == null) {
            StringBuilder returnString = new StringBuilder();

            returnString.append("Player hands: ");
            for (int i = 0; i < 4; i++) {
                returnString.append("\nP" + (i + 1) + ": " + hands[i].toString());
            }

            returnString.append("\n\nBidding: ");
            int playerIndex = firstPlayer - 1;
            Bid highestBid = null;
            for (int i = 0; i < NUM_BIDS; i++) {
                if (bidArray[i] == 0) {
                    continue;
                } else {
                    playerIndex = playerIndex == 4 ? 1 : playerIndex + 1;
                    while (bidArray[i] != playerIndex) {
                        returnString.append("\nPlayer " + playerIndex + " passes.");
                        playerIndex = playerIndex == 4 ? 1 : playerIndex + 1;
                    }
                    highestBid = convertIndexToBid(i);
                    returnString.append("\nPlayer " + playerIndex + " bids " + highestBid);
                }
            }

            returnString.append("\nPlayer " + playerIndex + " wins the bid with a bid of " + highestBid);
            returnString.append("\nPlayer " + playerIndex + " chooses " + partnerCard + " as the partner card");

            returnString.append("\n\nTrick-taking Play: ");
            for (int i = 0; i < 13; i++) {
                int firstPlayer = cardArray[i][0];
                if (firstPlayer == 0) {
                    returnString.append("\nPlayer " + lastTrickWinner + " wins trick " + i);
                    break;
                }
                playerIndex = firstPlayer;
                if (i == 0) {
                    returnString.append("\nPlayer " + playerIndex + " goes first");
                } else {
                    returnString.append("\nPlayer " + playerIndex + " wins trick " + i);
                }
                do {
                    returnString.append("\nPlayer " + playerIndex + " plays " +
                            convertIndexToCard(cardArray[i][playerIndex]));
                    playerIndex = playerIndex == 4 ? 1 : playerIndex + 1;
                } while (firstPlayer != playerIndex);

            }
            GameUpdate update = updates.get(updates.size() - 1);
            String winningMessage = update.get((update.size() - 1)).getMessage();
            returnString.append("\n" + winningMessage);

            returnString.append("\n\nUnplayed cards: ");

            for (int i = 0; i < 4; i++) {
                returnString.append("\nP" + (i + 1) + ": " + cardsUnplayed.get(i).toString());
            }

            processedString = returnString.toString();


        }

        return processedString;
    }

    public Iterator<GameUpdate> getUpdateHistory() {
        return updates.iterator();
    }

    public GameReplay getGameReplay() {
        ArrayList<Bid> bidList = new ArrayList<>();
        int playerIndex = firstPlayer - 1;
        for (int i = 0; i < 35; i++) {
            if (bidArray[i] == 0) {
                continue;
            } else {
                playerIndex = playerIndex == 4 ? 1 : playerIndex + 1;
                while (bidArray[i] != playerIndex) {
                    bidList.add(Bid.createPassBid());
                    playerIndex = playerIndex == 4 ? 1 : playerIndex + 1;
                }
                bidList.add(convertIndexToBid(i));
            }
        }

        int numTricks = NUM_TRICKS - cardsUnplayed.get(0).size();
        CardCollection[] tricks = new CardCollection[numTricks];
        int[] firstPlayerOfTrick = new int[numTricks];

        for (int i = 0; i < numTricks; i++) {
            if (cardArray[i][0] == 0) {
                break;
            }
            CardCollection currentTrick = new CardCollection();
            int[] currentTrickArray = cardArray[i];
            for (int j = 0; j < 5; j++) {
                if (j == 0) {
                    firstPlayerOfTrick[i] = currentTrickArray[j];
                } else {
                    currentTrick.add(convertIndexToCard(currentTrickArray[j]));
                }
            }
            tricks[i] = currentTrick;
        }

        CardCollection[] unplayedCards = cardsUnplayed.toArray(new CardCollection[1]);

        return new GameReplayImpl(firstPlayer, bidList, partnerCard, tricks, firstPlayerOfTrick, unplayedCards);
    }

    public long getGameId() {
        return this.gameId;
    }

}
