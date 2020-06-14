import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class GameEngine implements Engine {

    private int STARTING_PLAYER = 1;

    private long chatId;

    private PlayerState[] players;

    private BidCoordinator bidCoordinator;

    //MACRO GAME STATES
    private int currentPlayer;
    private int bidWinner;
    private Bid winningBid;
    private boolean brokenTrump;
    private char trumpSuit;
    private Card partnerCard;
    private int turnCycle;
    private boolean gameOver;

    //TURN-BASED GAME STATES
    private CardCollection currentTrick;
    private boolean trickFirstCard;
    private char firstCardSuit;
    private int trickHighestPlayer;
    private Card trickHighestCard;
    private BridgeTrumpComparator turnComparator;

    //PARTNERS
    protected Partners partners1; //bid winners
    protected Partners partners2;

    private GameEngine(long chatId) {
        this.players = new PlayerState[5];
        this.chatId = chatId;
        this.brokenTrump = false;
        this.bidCoordinator = new BidCoordinator(STARTING_PLAYER);
        this.partnerCard = null;
        this.winningBid = null;
        this.trickFirstCard = true;
        this.turnCycle = 1;
        this.gameOver = false;
        this.currentTrick = new CardCollection();
    }

    public static GameEngine init(long chatId) {
        GameEngine engine = new GameEngine(chatId);

        //Initialising PlayerState objects
        for (int i = 0; i < 5; i++) {
            if (i == 0) {
                engine.players[i] = null;
            } else {
                engine.players[i] = new PlayerState();
            }
        }

        //Distributing cards to hands
        Deck newDeck = Deck.init();
        CardCollection[] hands = new CardCollection[4];
        for (int i = 0; i < 4; i++) {
            hands[i] = new CardCollection();
        }
        int index = 0;
        while (!newDeck.isEmpty()) {
            hands[index].add(newDeck.draw());
            index = index == 3 ? 0 : index + 1;
        }

        //Assigning hands to playerStates
        for (int i = 1; i < 5; i++) {
            hands[i - 1].sort(new BridgeStandardComparator());
            engine.players[i].setHand(hands[i - 1]);
        }

        return engine;
    }

    public GameUpdate startBid() {
        GameUpdate update = bidCoordinator.startBid();
        for (int i = 1; i < 5; i++) {
            update.add(IndexUpdateGenerator.createPlayerHandInitialUpdate(i, players[i].getHand()));
        }
        return update;
    }

    public GameUpdate processPlay(Bid bid) {
        if (biddingInProgress()) {
            return bidCoordinator.processBidding(bid);
        } else {
            throw new IllegalStateException("Bidding has ended!");
        }
    }

    public GameUpdate processPlay(Card card) {
        if (partnerCard == null) {
            return processPartnerCard(card);
        } else {
            return processCardPlayed(card);
        }
    }

    public GameUpdate processPartnerCard(Card card) {
        GameUpdate update = new GameUpdate();

        if (winningBid == null) {
            Pair<Bid, Integer> pair = bidCoordinator.getWinningBid();
            winningBid = pair.first;
            bidWinner = pair.second;
            trumpSuit = winningBid.getSuit();
        }

        if (playerHasCard(bidWinner, card)) {
            update.add(IndexUpdateGenerator.createInvalidCardUpdate(bidWinner,
                    "You cannot choose a card you have!"));
            update.add(IndexUpdateGenerator.createPartnerCardRequest(bidWinner));
        } else {
            partnerCard = card;
            processPartners(card);
            currentPlayer = getFirstPlayer();
            update.add(IndexUpdateGenerator.createPlayerCardRequest(currentPlayer));
            update.add(IndexUpdateGenerator.createPartnerGroupUpdate(card));
            update.add(IndexUpdateGenerator.createTrickStartUpdate(currentPlayer));
            update.add(IndexUpdateGenerator.createCurrentTrickInitialUpdate(currentTrick));
        }

        return update;
    }

    private void processPartners(Card card) {

        //find player with the partner card
        int partnerIndex = 1;
        for (int i = 1; i < 5; i++) {
            if (playerHasCard(i, card)) {
                partnerIndex = i;
                break;
            }
        }

        this.partners1 = new Partners(
                bidWinner,
                partnerIndex,
                winningBid.getRequiredNumber());

        int firstOtherPlayer = -1;
        for (int i = 1; i < 5; i++) {
            if (i != bidWinner && i != partnerIndex) {
                if (firstOtherPlayer == -1) {
                    firstOtherPlayer = i;
                } else {
                    this.partners2 = new Partners(
                            firstOtherPlayer,
                            i,
                            winningBid.getOtherRequiredNumber());
                }
            }
        }

    }

    public boolean biddingInProgress() {
        return this.bidCoordinator.biddingInProgress();
    }

    private GameUpdate processCardPlayed(Card card) {

        GameUpdate update = new GameUpdate();
        IndexUpdate trickUpdate = null;

        //checks if the player has the card and the card played is valid for that turn
        if (isValidCard(card)) {
            Card cardPlayed = players[currentPlayer].playCard(card);
            IndexUpdate cardPlayedHandUpdate = IndexUpdateGenerator.createPlayerHandUpdate(
                    currentPlayer,
                    players[currentPlayer].getHand());
            if (trickFirstCard) {
                currentTrick = new CardCollection();
                trickFirstCard = false;
                firstCardSuit = card.getSuit();
                turnComparator = new BridgeTrumpComparator(trumpSuit, firstCardSuit);
                trickHighestPlayer = currentPlayer;
                trickHighestCard = card;
            } else {
                if (turnComparator.compare(cardPlayed, trickHighestCard) > 0) {
                    trickHighestCard = cardPlayed;
                    trickHighestPlayer = currentPlayer;
                }
            }

            currentTrick.add(cardPlayed);
            if (card.getSuit() == trumpSuit && !brokenTrump) {
                brokenTrump = true;
            }
            IndexUpdate cardGroupUpdate = IndexUpdateGenerator.createCardGroupUpdate(currentPlayer, cardPlayed);
            IndexUpdate trickGroupUpdate = IndexUpdateGenerator.createCurrentTrickUpdate(currentTrick);

            //Check if it is the final card of the set. If it is, we reset the trick and increase the turnCycle.
            if (currentTrick.size() == 4) {
                players[trickHighestPlayer].addTrick(currentTrick);
                trickFirstCard = true;

                trickUpdate = IndexUpdateGenerator.createTrickGroupUpdate(trickHighestPlayer, turnCycle++);

                //check if game has concluded
                if (checkWin(trickHighestPlayer)) {
                    update.add(cardGroupUpdate);
                    update.add(IndexUpdateGenerator.createWinnerGroupUpdate(trickHighestPlayer,
                            getPartnerOf(trickHighestPlayer)));
                    update.add(trickGroupUpdate);
                    gameOver = true;
                    return update;
                }
                currentPlayer = trickHighestPlayer;

            } else {
                //update player number
                currentPlayer = currentPlayer == 4 ? 1 : currentPlayer + 1;
            }
            update.add(IndexUpdateGenerator.createPlayerCardRequest(currentPlayer));
            update.add(cardPlayedHandUpdate);
            update.add(cardGroupUpdate);
            update.add(trickGroupUpdate);

            if (currentTrick.size() == 4) {
                update.add(trickUpdate);
            }

        //Otherwise, we request another card
        } else {
            update.add(IndexUpdateGenerator.createInvalidCardUpdate(currentPlayer, getInvalidReason(card)));
            update.add(IndexUpdateGenerator.createPlayerCardRequest(currentPlayer));
        }


        return update;
    }

    public boolean gameInProgress() {
        return !gameOver;
    }

    public boolean isValidCard(Card card) {
        PlayerState state = players[currentPlayer];
        if (!state.containsCard(card)) {
            return false;
        } else if (trickFirstCard) {
            return !(card.getSuit() == trumpSuit && !brokenTrump) || state.containsOnlySuit(trumpSuit);
        } else if (firstCardSuit != card.getSuit()) {
            return !state.containsSuit(firstCardSuit);
        }
        return true;
    }

    public String getInvalidReason(Card card) {
        PlayerState state = players[currentPlayer];
        if (!state.containsCard(card)) {
            return "You do not have " + card;
        } else if (trickFirstCard) {
            return "Trump has not yet been broken! You cannot start a trick with a trump suit!";
        } else {
            return "You still have cards of the same suit as the current trick. You must play one of them!";
        }
    }


    public boolean checkWin(int trickWinner) {
        Partners winningPartner = partners1.checkContainsPlayer(trickWinner) ?
                partners1 :
                partners2;
        return winningPartner.winCheck();
    }

    protected String getGameState() {
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < 5; i++) {
            PlayerState player = players[i];
            builder.append(player.toString() + " has " + player.countTricks() + " tricks\n");
        }

        return builder.toString();
    }

    private class Partners {

        int player1;
        int player2;

        int requiredNumber;

        Partners(int player1, int player2, int requiredNumber) {
            this.player1 = player1;
            this.player2 = player2;
            this.requiredNumber = requiredNumber;
        }

        boolean checkContainsPlayer(int player) {
            return player == player1 || player == player2;
        }

        int getTotalNumTricks() {
            return players[player1].countTricks() + players[player2].countTricks();
        }

        int numTricksToWin() {
            return this.requiredNumber - getTotalNumTricks();
        }

        boolean winCheck() {
            return this.requiredNumber == this.getTotalNumTricks();
        }

        public String toString() {
            return player1 + ": " + players[player1].countTricks() + " numTricks\n" +
                    player2 + ": " + players[player2].countTricks() +
                    " numTricks\nTotal required: " + this.requiredNumber;
        }

    }

    private int getFirstPlayer() {
        if (winningBid.isNoTrumpBid()) {
            return bidWinner;
        } else {
            return bidWinner == 4 ? 1 : bidWinner + 1;
        }
    }

    private int getPartnerOf(int player) {
        Partners partners = partners1.checkContainsPlayer(player) ?
                partners1 :
                partners2;
        return partners.player1 == player ? partners.player2 : partners.player1;
    }

    private boolean playerHasCard(int player, Card card) {
        return players[player].containsCard(card);
    }

    @Override
    public long getChatId() {
        return this.chatId;
    }

    @Override
    public String queryEngine(String query) {
        return "";
    }

    public PlayerState getPlayerState(int index) {
        return this.players[index];
    }

    public boolean firstCardOfTrick() {
        return trickFirstCard;
    }

    public char getTrumpSuit() {
        return trumpSuit;
    }

    public char getFirstCardSuit() {
        return firstCardSuit;
    }

    public boolean getTrumpBroken() {
        return brokenTrump;
    }

}
