import java.util.Arrays;

public class GameEngine implements Engine {

    protected long chatId;

    protected PlayerState[] players;

    protected BidCoordinator bidCoordinator;

    //MACRO GAME STATES
    protected int currentPlayer;
    protected int bidWinner;
    protected Bid winningBid;
    protected boolean brokenTrump;
    protected char trumpSuit;
    protected Card partnerCard;
    protected int turnCycle;
    protected boolean gameOver;

    //TURN-BASED GAME STATES
    protected CardCollection currentTrick;
    protected boolean trickFirstCard;
    protected char firstCardSuit;
    protected int trickHighestPlayer;
    protected Card trickHighestCard;
    protected BridgeTrumpComparator turnComparator;

    //PARTNERS
    protected Partners partners1; //bid winners
    protected Partners partners2;

    //GAME LOGGER
    private GameLogger logger;

    public GameEngine(long chatId) {
        this.currentPlayer = (int) (Math.random() * (4)) + 1;
        this.players = new PlayerState[5];
        this.chatId = chatId;
        this.brokenTrump = false;
        this.logger = new GameLogImpl(chatId, currentPlayer);
        this.bidCoordinator = new BidCoordinator(currentPlayer, logger);
        this.partnerCard = null;
        this.winningBid = null;
        this.trickFirstCard = true;
        this.turnCycle = 1;
        this.gameOver = false;
        this.currentTrick = new CardCollection();

        //Initialising PlayerState objects
        for (int i = 0; i < 5; i++) {
            if (i == 0) {
                players[i] = null;
            } else {
                players[i] = new PlayerState();
            }
        }

        //Ensuring that hands have at least 4 points
        boolean wash;
        CardCollection[] hands = new CardCollection[4];
        do {
            wash = false;

            //Distributing cards to hands
            Deck newDeck = Deck.init();
            for (int i = 0; i < 4; i++) {
                hands[i] = new CardCollection();
            }
            int index = 0;
            while (!newDeck.isEmpty()) {
                hands[index].add(newDeck.draw());
                index = index == 3 ? 0 : index + 1;
            }

            BridgeStandardComparator handSorter = new BridgeStandardComparator();

            for (int i = 0; i < 4; i++) {
                hands[i].sort(handSorter);
                int pointCount = CardCollection.countPoints(hands[i]);
                if (pointCount < 4) {
                    wash = true;
                    break;
                }
            }

        } while (wash);

        //Assigning hands to playerStates
        for (int i = 1; i < 5; i++) {
            players[i].setHand(hands[i - 1]);
        }

        logger.addHands(hands);
    }

    public GameUpdate startBid() {
        GameUpdate update = bidCoordinator.startBid();
        int firstPlayer = update.get(0).getIndex();
        update.add(0, IndexUpdateGenerator
                .createPlayerHandInitialUpdate(firstPlayer, players[firstPlayer].getHand()));
        for (int i = 1; i < 5; i++) {
            if (i == firstPlayer) continue;
            update.add(IndexUpdateGenerator.createPlayerHandInitialUpdate(i, players[i].getHand()));
        }

        update.add(IndexUpdateGenerator.createGameStartNotice());

        logger.addUpdate(update);
        return update;
    }

    public GameUpdate processPlay(Bid bid) {
        if (biddingInProgress()) {
            GameUpdate update = bidCoordinator.processBidding(bid);
            logger.addUpdate(update);
            return update;
        } else {
            throw new IllegalStateException("Bidding has ended!");
        }
    }

    public GameUpdate processPlay(Card card) {
        GameUpdate update;
        if (partnerCard == null) {
            update = processPartnerCard(card);
        } else {
            update = processCardPlayed(card);
        }
        logger.addUpdate(update);
        return update;
    }

    private GameUpdate processPartnerCard(Card card) {
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
        } else {
            partnerCard = card;
            processPartners(card);
            currentPlayer = getFirstPlayer();
            update.add(IndexUpdateGenerator.createPlayerCardRequest(currentPlayer));
            update.add(IndexUpdateGenerator.createPartnerGroupEdit(card));
            update.add(IndexUpdateGenerator.createPartnerGroupUpdate(card, currentPlayer));
            update.add(IndexUpdateGenerator.createPartnerCardAcknowledgement(bidWinner, card));
            update.add(IndexUpdateGenerator.createTrickCountUpdate(new int[] {0, 0, 0, 0}));
            update.add(IndexUpdateGenerator.createCurrentTrickUpdate(turnCycle, currentTrick));

            logger.addPartnerCard(card);
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

    //Checks if the card played is valid
    protected GameUpdate processCardPlayed(Card card) {
        GameUpdate update = new GameUpdate();
        //checks if the player has the card and the card played is valid for that turn
        if (isValidCard(card)) {
            update.addAll(registerCardPlayed(card));
        //Otherwise, we request another card
        } else {
            update.add(IndexUpdateGenerator.createInvalidCardUpdate(currentPlayer, getInvalidReason(card)));
        }
        return update;
    }

    //Registers the valid card played
    protected GameUpdate registerCardPlayed(Card card) {
        GameUpdate update = new GameUpdate();

        Card cardPlayed = players[currentPlayer].playCard(card);
        update.add(IndexUpdateGenerator.createPlayerHandUpdate(
                currentPlayer,
                players[currentPlayer].getHand()));
        update.add(IndexUpdateGenerator.createPlayerCardAcknowledgement(
                currentPlayer,
                cardPlayed));

        logger.addCardPlayed(currentPlayer, turnCycle, card);

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

        update.add(IndexUpdateGenerator.createCurrentTrickUpdate(turnCycle, currentTrick));

        //Check if it is the final card of the set. If it is, we reset the trick and increase the turnCycle.
        if (currentTrick.size() == 4) {
            update.addAll(registerTrick(cardPlayed));
        } else {
            //update player number
            update.add(IndexUpdateGenerator.createCardGroupUpdate(currentPlayer, cardPlayed));
            currentPlayer = currentPlayer == 4 ? 1 : currentPlayer + 1;
            update.add(0, IndexUpdateGenerator.createPlayerCardRequest(currentPlayer));
        }

        return update;
    }

    //Update for when a trick is complete
    protected GameUpdate registerTrick(Card cardPlayed) {
        GameUpdate update = new GameUpdate();
        players[trickHighestPlayer].addTrick(currentTrick);
        update.add(IndexUpdateGenerator.createTrickCountUpdate(getGameState()));

        trickFirstCard = true;

        IndexUpdate trickUpdate = IndexUpdateGenerator.createTrickGroupUpdate(currentPlayer,
                cardPlayed, trickHighestPlayer, turnCycle++);

        update.add(trickUpdate);

        //check if game has concluded
        if (checkWin(trickHighestPlayer)) {
            update.add(IndexUpdateGenerator.createWinnerGroupUpdate(trickHighestPlayer,
                    getPartnerOf(trickHighestPlayer)));
            gameOver = true;

            logger.setLastTrickWinner(trickHighestPlayer);
            //Adds remaining cards into logger as unplayed
            for (int i = 1; i < 5; i++) {
                logger.addCardsNotPlayed(i, players[i].getHand());
            }

        } else {
            currentPlayer = trickHighestPlayer;
        }
        return update;
    }

    public boolean gameInProgress() {
        return !gameOver;
    }

    private boolean isValidCard(Card card) {
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

    private String getInvalidReason(Card card) {
        PlayerState state = players[currentPlayer];
        if (!state.containsCard(card)) {
            return "You do not have " + card;
        } else if (trickFirstCard) {
            return "Trump has not yet been broken! You cannot start a trick with a trump suit!";
        } else {
            return "You still have cards of the same suit as the current trick. You must play one of them!";
        }
    }


    private boolean checkWin(int trickWinner) {
        Partners winningPartner = partners1.checkContainsPlayer(trickWinner) ?
                partners1 :
                partners2;
        return winningPartner.winCheck();
    }

    protected int[] getGameState() {

        int[] trickCount = new int[4];

        for (int i = 0; i < 4; i++) {
            trickCount[i] = players[i + 1].countTricks();
        }

        return trickCount;
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

    public boolean gettingPartnerCard() {
        return partnerCard == null;
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

    public GameLogger getGameLogger() {
        return this.logger;
    }



    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println((int) (Math.random() * (4)) + 1);
        }
    }
}
