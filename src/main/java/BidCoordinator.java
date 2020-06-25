import java.util.ArrayList;
import java.util.Iterator;

public class BidCoordinator {

    private int currentPlayer;

    private int consecutivePasses;

    private int currentHighestBidder;
    private Bid currentHighestBid;

    private static Bid passBid = Bid.createPassBid();

    private GameLogger logger;

    public BidCoordinator(int startingPlayer, GameLogger logger) {
        this.currentPlayer = startingPlayer;
        this.logger = logger;
    }

    public GameUpdate startBid() {

        //Setting the bid states to the default state
        consecutivePasses = 0;
        currentHighestBid = null;
        currentHighestBidder = 0;

        GameUpdate update = new GameUpdate();
        update.add(IndexUpdateGenerator.createPlayerBidRequest(currentPlayer, currentHighestBid));
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

                //UPDATING LOGGER
                logger.addBid(currentPlayer, newBid);

                currentHighestBidder = currentPlayer;
                currentHighestBid = newBid;
                IndexUpdate groupBidEdit = IndexUpdateGenerator.createBidGroupEdit(currentPlayer,
                        newBid,
                        consecutivePasses = 0);
                IndexUpdate playerBidAcknowledge = IndexUpdateGenerator.createPlayerBidAcknowledgement(
                        currentPlayer, newBid);
                IndexUpdate groupUpdate = IndexUpdateGenerator.createBidGroupUpdate(currentPlayer++, newBid);
                if (currentPlayer == 5) currentPlayer = 1;
                IndexUpdate bidRequest = IndexUpdateGenerator.createPlayerBidRequest(currentPlayer, newBid);
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
                update.add(IndexUpdateGenerator.createPlayerBidRequest(currentPlayer, currentHighestBid));
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

}
