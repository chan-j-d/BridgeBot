import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BidCoordinator {

    private int currentPlayer;
    private int firstPlayer;

    private int consecutivePasses;

    private int currentHighestBidder;
    private Bid currentHighestBid;

    private List<Bid> bids;

    private static Bid passBid = Bid.createPassBid();

    private GameLogger logger;

    public BidCoordinator(int startingPlayer, GameLogger logger) {
        this.currentPlayer = startingPlayer;
        this.firstPlayer = startingPlayer;
        this.logger = logger;
        this.bids = new ArrayList<>();
        for (int i = 1; i < startingPlayer; i++) this.bids.add(null);
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
                consecutivePasses = 0;

                bids.add(newBid);

                //UPDATING LOGGER
                logger.addBid(currentPlayer, newBid);

                currentHighestBidder = currentPlayer;
                currentHighestBid = newBid;
                IndexUpdate groupBidEdit = IndexUpdateGenerator.createBidGroupEdit(bids);
                IndexUpdate playerBidAcknowledge = IndexUpdateGenerator.createPlayerBidAcknowledgement(
                        currentPlayer, newBid);

                if (newBid.equals(Bid.createNoTrumpBid(7))) {
                    update.add(IndexUpdateGenerator.createPartnerCardRequest(currentHighestBidder));
                    update.add(playerBidAcknowledge);
                    update.add(groupBidEdit);
                    update.add(IndexUpdateGenerator.createBidWonEdit(currentHighestBidder, currentHighestBid));
                    update.add(IndexUpdateGenerator.createSevenNTBidUpdate(currentHighestBidder));

                } else {
                    IndexUpdate groupUpdate = IndexUpdateGenerator.createBidGroupUpdate(currentPlayer, newBid);
                    currentPlayer = nextPlayerIndex(currentPlayer);

                    IndexUpdate bidRequest = IndexUpdateGenerator.createPlayerBidRequest(currentPlayer, newBid);
                    update.add(bidRequest);
                    update.add(playerBidAcknowledge);
                    update.add(groupBidEdit);
                    update.add(groupUpdate);
                }

            } else {
                update.add(IndexUpdateGenerator.createInvalidBidUpdate(currentPlayer,
                        "Please bid something bigger!"));
            }

        } else {

            bids.add(newBid);

            IndexUpdate groupUpdate = IndexUpdateGenerator.createBidGroupUpdate(currentPlayer, newBid);
            IndexUpdate playerBidAcknowledge = IndexUpdateGenerator.createPlayerBidAcknowledgement(
                    currentPlayer, newBid);

            if (++consecutivePasses < 3 || currentHighestBidder == 0) {

                currentPlayer = nextPlayerIndex(currentPlayer);

                update.add(IndexUpdateGenerator.createPlayerBidRequest(currentPlayer, currentHighestBid));
                if (currentHighestBidder == 0) {
                    update.add(IndexUpdateGenerator.createNoBidEdit());
                } else {
                    update.add(IndexUpdateGenerator.createBidGroupEdit(bids));
                }
                update.add(playerBidAcknowledge);
                update.add(groupUpdate);

                if (consecutivePasses == 4) {
                    for (int i = 0; i < 4; i++) {
                        bids.remove(Bid.createPassBid());
                    }
                }
            } else {
                update.add(IndexUpdateGenerator.createPartnerCardRequest(currentHighestBidder));
                update.add(playerBidAcknowledge);
                update.add(IndexUpdateGenerator.createBidGroupEdit(bids));
                update.add(IndexUpdateGenerator.createBidWonEdit(currentHighestBidder, currentHighestBid));
                update.add(IndexUpdateGenerator.createBidWonUpdate(currentPlayer,
                        currentHighestBidder,
                        currentHighestBid));
            }
        }

        return update;

    }

    public boolean biddingInProgress() {
        return (consecutivePasses < 3 || currentHighestBidder == 0) &&
                !(currentHighestBid != null && currentHighestBid.equals(Bid.createNoTrumpBid(7)));
    }

    public Pair<Bid, Integer> getWinningBid() {
        if (biddingInProgress()) {
            throw new IllegalStateException("bidding has not concluded!");
        }
        return new Pair<>(currentHighestBid, currentHighestBidder);
    }

    public List<Bid> getBids() {
        return this.bids;
    }

    public int getFirstPlayer() {
        return this.firstPlayer;
    }

    private int nextPlayerIndex(int currentPlayerIndex) {
        return currentPlayerIndex == 4 ? 1 : currentPlayerIndex + 1;
    }


}
