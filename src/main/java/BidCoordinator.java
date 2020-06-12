import java.util.ArrayList;
import java.util.Iterator;

public class BidCoordinator {

    private int currentPlayer;

    private boolean prevPass;
    private boolean twoPrevPass;
    private boolean threePrevPass;

    private int currentHighestBidder;
    private Bid currentHighestBid;

    private static Bid passBid = Bid.createPassBid();

    public BidCoordinator(int startingPlayer) {
        this.currentPlayer = startingPlayer;
    }

    public GameUpdate startBid() {

        //Setting the bid states to the default state
        prevPass = false;
        twoPrevPass = false;
        threePrevPass = false;
        currentHighestBid = null;
        currentHighestBidder = 0;

        GameUpdate update = new GameUpdate();
        IndexUpdate indexUpdate = createPlayerBidRequest(currentPlayer);

        IndexUpdate indexUpdate2 = new IndexUpdate(0,
                "Bid starting!\nPlayer " + currentPlayer + " starts the bid!");
        update.add(indexUpdate);
        update.add(indexUpdate2);
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
                IndexUpdate update1 = createPlayerBidUpdate(currentPlayer++, newBid);
                if (currentPlayer == 5) currentPlayer = 1;
                IndexUpdate update2 = createPlayerBidRequest(currentPlayer);
                update.add(update2);
                update.add(update1);
            } else {
                update.add(createInvalidBidUpdate(currentPlayer));
            }
            prevPass = false;
            twoPrevPass = false;
            threePrevPass = false;

        } else {
            IndexUpdate update1 = createPlayerBidUpdate(currentPlayer++, newBid);
            if (currentPlayer == 5) currentPlayer = 1;
            if (!prevPass) {
                prevPass = true;
                update.add(createPlayerBidRequest(currentPlayer));
                update.add(update1);
            } else if (!twoPrevPass) {
                twoPrevPass = true;
                update.add(createPlayerBidRequest(currentPlayer));
                update.add(update1);
            } else if (!threePrevPass) {
                threePrevPass = true;
                update.add(createPartnerCardRequest(currentHighestBidder));
                update.add(update1);
                update.add(createPlayerBidWonUpdate(currentHighestBidder, currentHighestBid));
            }
        }

        return update;

    }

    public boolean biddingInProgress() {
        return !threePrevPass || currentHighestBidder == 0;
    }

    public Pair<Bid, Integer> getWinningBid() {
        if (biddingInProgress()) {
            throw new IllegalStateException("bidding has not concluded!");
        }
        return new Pair<>(currentHighestBid, currentHighestBidder);
    }

    private IndexUpdate createPlayerBidUpdate(int player, Bid bid) {
        return new IndexUpdate(0, player + " bids " + bid);
    }

    private IndexUpdate createInvalidBidUpdate(int player) {
        return new IndexUpdate(player, "The bid by " + player + " was invalid, please try again.");
    }

    private IndexUpdate createPlayerBidRequest(int player) {
        return new IndexUpdate(player, "Please make a bid in this chat");
    }

    private IndexUpdate createPlayerBidWonUpdate(int player, Bid bid) {
        return new IndexUpdate(0, player + " wins with a bid of " + bid);
    }

    private IndexUpdate createPartnerCardRequest(int player) {
        return new IndexUpdate(player, "Please choose a partner card");
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
