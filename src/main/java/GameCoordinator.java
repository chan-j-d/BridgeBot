import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class GameCoordinator {

    protected BridgePlayingField playingField;
    protected Player[] players;

    protected Bid winningBid;

    protected Partners partners1; //bid winners
    protected Partners partners2;

    public GameCoordinator(Player[] players) {
        this.players = players;
    }

    public Pair<Player, Player> startGame() {

        //initialise playing field
        this.playingField = BridgePlayingField.init();
        //set cards of players to their respective hands

        for (int i = 0; i < 4; i++) {
            this.players[i].setHand(this.playingField.getHandOfPlayer(i));
        }

        //Starting bidding process
        Pair<Bid, Integer> pair = BidCoordinator.bidding(this.players);
        this.winningBid = pair.first;
        int winningBidPlayerIndex = pair.second;

        System.out.println(pair);

        //Getting partner card
        Card partnerCard = this.players[winningBidPlayerIndex].getPartnerCard();

        //Process partner card
        this.processPartners(winningBidPlayerIndex, partnerCard, this.winningBid);

        System.out.println(partnerCard);
        for (int i = 0; i < 4; i++) {
            System.out.println(this.players[i].hand);
        }

        System.out.println(partners1 + "\n" + partners2);

        //Get starting player (bid winner's left unless 'NT' bid where the bid winner starts.
        int startingPlayerIndex = this.winningBid.isNoTrumpBid() ? winningBidPlayerIndex :
                winningBidPlayerIndex == 3 ? 0 : winningBidPlayerIndex + 1;

        char trumpSuit = winningBid.getSuit();

        //start gameplay
        for (int turnCycle = 0; turnCycle < 13; turnCycle++) {

            System.out.println("Turn cycle: " + turnCycle);

            Pair<Integer, CardCollection> turnResult = conductTurnCycle(startingPlayerIndex, trumpSuit);
            int winningPlayerIndex = turnResult.first;
            CardCollection set = turnResult.second;

            if (updateTurnResult(winningPlayerIndex, set)) {
                Partners winningTeam = partners1.checkContainsIndex(winningPlayerIndex) ? partners1 : partners2;
                return new Pair<>(
                        players[winningTeam.firstPlayerIndex],
                        players[winningTeam.secondPlayerIndex]
                );
            }
            System.out.println(partners1 + "\n" + partners2);


            startingPlayerIndex = winningPlayerIndex;
        }

        return null;
    }

    private Pair<Integer, CardCollection> conductTurnCycle(int startingPlayerIndex, char trumpSuit) {
        Card cardPlayed;

        BridgeTrumpComparator comparator;

        Iterator<Integer> indexIterator = getTurnCycle(startingPlayerIndex);
        int currPlayerIndex = indexIterator.next();

        int winningPlayerIndex = currPlayerIndex;
        Card currHighestCard = players[currPlayerIndex].getNextCard();

        playingField.cardPlayed(winningPlayerIndex, currHighestCard);

        comparator = new BridgeTrumpComparator(trumpSuit, currHighestCard.getSuit());

        System.out.println("Player " + currPlayerIndex + " plays " + currHighestCard);

        while (indexIterator.hasNext()) {
            currPlayerIndex = indexIterator.next();

            cardPlayed = players[currPlayerIndex].getNextCard();

            System.out.println("Player " + currPlayerIndex + " plays " + cardPlayed);

            playingField.cardPlayed(currPlayerIndex, cardPlayed);

            if (comparator.compare(cardPlayed, currHighestCard) > 0) {
                currHighestCard = cardPlayed;
                winningPlayerIndex = currPlayerIndex;
            }
        }

        CardCollection set = playingField.retrieveSet();

        System.out.println(winningPlayerIndex + " " + set);

        for (Player p : players) {
            p.updateInfo(set);
        }

        return new Pair<>(winningPlayerIndex, set);

    }

    private void processPartners(int winningBidderIndex, Card partnerCard, Bid winningBid) {
        int partnerIndex = this.playingField.getIndexOfPlayerWithCard(partnerCard);
        this.partners1 = new Partners(winningBidderIndex, partnerIndex, winningBid.getRequiredNumber());

        int firstOtherPlayer = -1;
        for (int i = 0; i < 4; i++) {
            if (i != winningBidderIndex && i != partnerIndex) {
                if (firstOtherPlayer == -1) {
                    firstOtherPlayer = i;
                } else {
                    this.partners2 = new Partners(firstOtherPlayer, i, winningBid.getOtherRequiredNumber());
                }
            }
        }

    }

    private boolean updateTurnResult(int winningPlayerIndex, CardCollection set) {
        Partners winningPartner = partners1.checkContainsIndex(winningPlayerIndex) ? partners1 : partners2;
        winningPartner.addSetForPlayer(winningPlayerIndex, set);
        return winningPartner.winCheck();
    }

    private Iterator<Integer> getTurnCycle(int firstPlayer) {
        ArrayList<Integer> turnCycle = new ArrayList<>();
        int turnOrder = firstPlayer;
        do {
            turnCycle.add(turnOrder++);
            if (turnOrder == 4) turnOrder = 0;
        } while (turnOrder != firstPlayer);
        return turnCycle.iterator();
    }

    private class Partners {

        int firstPlayerIndex;
        int secondPlayerIndex;

        ArrayList<CardCollection> firstPlayerSets;
        ArrayList<CardCollection> secondPlayerSets;

        int requiredNumber;

        Partners(int first, int second, int requiredNumber) {
            this.firstPlayerIndex = first;
            this.secondPlayerIndex = second;
            this.requiredNumber = requiredNumber;
            this.firstPlayerSets = new ArrayList<>();
            this.secondPlayerSets = new ArrayList<>();
        }

        boolean checkContainsIndex(int playerIndex) {
            return playerIndex == firstPlayerIndex || playerIndex == secondPlayerIndex;
        }

        void addSetForPlayer(int playerIndex, CardCollection set) {
            if (playerIndex != firstPlayerIndex && playerIndex != secondPlayerIndex) {
                throw new IllegalArgumentException("Invalid player index!");
            } else if (playerIndex == firstPlayerIndex) {
                firstPlayerSets.add(set);
            } else {
                secondPlayerSets.add(set);
            }
        }

        int getNumSetsForPlayer(int playerIndex) {
            if (playerIndex != firstPlayerIndex && playerIndex != secondPlayerIndex) {
                throw new IllegalArgumentException("Invalid player index!");
            } else if (playerIndex == firstPlayerIndex) {
                return firstPlayerSets.size();
            } else {
                return secondPlayerSets.size();
            }
        }

        int getTotalNumSets() {
            return firstPlayerSets.size() + secondPlayerSets.size();
        }

        int numSetsToWin() {
            return this.requiredNumber - getTotalNumSets();
        }

        boolean winCheck() {
            return this.requiredNumber == this.getTotalNumSets();
        }

        public String toString() {
            return firstPlayerIndex + ": " + firstPlayerSets.size() + " numSets\n" +
                    secondPlayerIndex + ": " + secondPlayerSets.size() +
                    " numSets\nTotal required: " + this.requiredNumber;
        }

    }

}
