import java.util.ArrayList;
import java.util.Iterator;

public class BidCoordinator {

    private int currentPlayer;

    private int consecutivePasses;

    private int currentHighestBidder;
    private Bid currentHighestBid;

    private static Bid passBid = Bid.createPassBid();

    public BidCoordinator(int startingPlayer) {
        this.currentPlayer = startingPlayer;
    }

    public GameUpdate startBid() {

        //Setting the bid states to the default state
        consecutivePasses = 0;
        currentHighestBid = null;
        currentHighestBidder = 0;

        GameUpdate update = new GameUpdate();
        update.add(IndexUpdateGenerator.createPlayerBidRequest(currentPlayer));
        update.add(IndexUpdateGenerator.createBidGroupInitialUpdate(currentPlayer));
        return update;
    }

    public GameUpdate processBidding(Bid newBid) {

        if (!biddingInProgress()) {
            throw new IllegalStateException("Bidding has concluded!");
        }

        GameUpdate update = new GameUpdate();

        if (!newBid.equals(passBid)) {
            if (currentHighestBidder == 0 || newBid.compareTo(currentHighestBid) > 0) {
                currentHighestBidder = currentPlayer;
                currentHighestBid = newBid;
                IndexUpdate groupBidEdit = IndexUpdateGenerator.createBidGroupEdit(currentPlayer,
                        newBid,
                        consecutivePasses = 0);
                IndexUpdate playerBidAcknowledge = IndexUpdateGenerator.createPlayerBidAcknowledgement(
                        currentPlayer, newBid);
                IndexUpdate groupUpdate = IndexUpdateGenerator.createBidGroupUpdate(currentPlayer++, newBid);
                if (currentPlayer == 5) currentPlayer = 1;
                IndexUpdate bidRequest = IndexUpdateGenerator.createPlayerBidRequest(currentPlayer);
                update.add(bidRequest);
                update.add(playerBidAcknowledge);
                update.add(groupBidEdit);
                update.add(groupUpdate);
            } else {
                update.add(IndexUpdateGenerator.createInvalidBidUpdate(currentPlayer,
                        "Please bid something bigger!"));
            }

        } else {
            IndexUpdate groupUpdate = IndexUpdateGenerator.createBidGroupUpdate(currentPlayer, newBid);
            IndexUpdate playerBidAcknowledge = IndexUpdateGenerator.createPlayerBidAcknowledgement(
                    currentPlayer++, newBid);
            if (currentPlayer == 5) currentPlayer = 1;
            if (++consecutivePasses < 3 || currentHighestBidder == 0) {
                update.add(IndexUpdateGenerator.createPlayerBidRequest(currentPlayer));
                if (currentHighestBidder == 0) {
                    update.add(IndexUpdateGenerator.createNoBidEdit());
                } else {
                    update.add(IndexUpdateGenerator.createBidGroupEdit(
                            currentHighestBidder, currentHighestBid, consecutivePasses));
                }
                update.add(playerBidAcknowledge);
                update.add(groupUpdate);
            } else {
                update.add(IndexUpdateGenerator.createPartnerCardRequest(currentHighestBidder));
                update.add(playerBidAcknowledge);
                update.add(IndexUpdateGenerator.createBidWonEdit(currentHighestBidder, currentHighestBid));
                update.add(IndexUpdateGenerator.createBidWonUpdate(currentPlayer,
                        currentHighestBidder,
                        currentHighestBid));
            }
        }

        return update;

    }

    public boolean biddingInProgress() {
        return consecutivePasses < 3 || currentHighestBidder == 0;
    }

    public Pair<Bid, Integer> getWinningBid() {
        if (biddingInProgress()) {
            throw new IllegalStateException("bidding has not concluded!");
        }
        return new Pair<>(currentHighestBid, currentHighestBidder);
    }

/*
    public static Pair<Bid, Integer> bidding(ClientEngineMediator mediator) {
        boolean prevPass = false;
        boolean twoPrevPass = false;
        boolean threePrevPass = false;

        int currentHighestBidderIndex = -1;
        Bid currentHighestBid = Bid.createPassBid();
        Bid passBid = Bid.createPassBid();

        int player = 1; //can consider randomising this

        while (!threePrevPass || currentHighestBid.equals(passBid)) {

            Bid currentBid;
            Player currentPlayer = players[index];
            do {
                try {
                    currentBid = currentPlayer.getBid();
                    //System.out.println("current player: " + index);
                    //System.out.println(currentBid + " " + currentHighestBid);
                    if (currentBid.equals(passBid)) {
                        break;
                    } else if (currentBid.compareTo(currentHighestBid) > 0) {
                        currentHighestBid = currentBid;
                        currentHighestBidderIndex = index;
                        break;
                    //} else {
                        //sendMessage("Bid something higher or pass!");
                    }
                } catch (IllegalArgumentException e){
                    //sendMessage(e.getMessage());
                }
            } while (true);

            index++;

            if (currentBid.equals(passBid)) {
                if (!prevPass) {
                    prevPass = true;
                } else if (!twoPrevPass) {
                    twoPrevPass = true;
                } else if (!threePrevPass) {
                    threePrevPass = true;
                //} else {
                    //sendMessage("Bidding restarting! Please bid something");
                }
            } else {
                prevPass = false;
                twoPrevPass = false;
                threePrevPass = false;
            }
            if (index == 4) {
                index = 0;
            }
        }

        return new Pair<>(currentHighestBid,currentHighestBidderIndex);


    } */




}
