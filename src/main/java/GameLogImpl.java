import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class GameLogImpl implements GameLogger {

    private ArrayList<GameUpdate> updates;
    private int[] bidArray;
    private int[][] cardArray;
    private boolean[] cardsPlayed;
    private Card partnerCard;

    private long gameId;
    private String processedString;

    private static final int NUM_BIDS = 35;
    private static final int NUM_TRICKS = 13;
    private static final int NUM_CARDS = 52;

    public GameLogImpl(long gameId) {
        this.gameId = gameId;
        updates = new ArrayList<>();
        bidArray = new int[NUM_BIDS];
        cardArray = new int[NUM_TRICKS][];
        for (int i = 0; i < NUM_TRICKS; i++) {
            cardArray[i] = new int[5];
        }
        processedString = null;
        cardsPlayed = new boolean[52];
    }

    public void addUpdate(GameUpdate update) {
        this.updates.add(update);
    }

    public void addBid(int player, Bid bid) {
        bidArray[convertBidToIndex(bid)] = player;
    }

    public void addCardPlayed(int player, int turn, Card card) {
        int cardIndex = convertCardToIndex(card);
        cardArray[--turn][player] = cardIndex;
        cardsPlayed[cardIndex] = true;
        if (cardArray[turn][0] == 0) {
            cardArray[turn][0] = player;
        }
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
            returnString.append("Bidding: ");
            int playerIndex = 0;
            Bid highestBid = null;
            for (int i = 0; i < 35; i++) {
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

            returnString.append("\n\nUnplayed cards: \n");

            boolean firstCard = true;
            for (int i = 0; i < NUM_CARDS; i++) {
                boolean cardPlayed = cardsPlayed[i];
                if (!cardPlayed) {
                    Card card = convertIndexToCard(i);
                    if (firstCard) {
                        firstCard = false;
                        returnString.append(card);
                    } else {
                        returnString.append(", " + card);
                    }
                }
            }

            processedString = returnString.toString();


        }

        return processedString;
    }

    public Iterator<GameUpdate> getUpdateHistory() {
        return updates.iterator();
    }

}
