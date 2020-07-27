import java.util.List;

public class IndexUpdateGenerator {

    /*

    Trick gameplay updates.

     */

    //Request for a card
    public static IndexUpdate createPlayerCardRequest(int player) {
        return new IndexUpdate(player, "Your turn to play a card!", UpdateType.SEND_REQUEST );
    }

    //Acknowledgement Update-type
    public static IndexUpdate createPlayerCardAcknowledgement(int player, Card card) {
        return new IndexUpdate(player, "You played " + card + " !", UpdateType.SEND_UPDATE);
    }

    //Request type for a partner card
    public static IndexUpdate createPartnerCardRequest(int bidWinner) {
        return new IndexUpdate(bidWinner,"Choose a partner card!" +
                "\nPartner card should be in the form of OX: " +
                "\n - O: Suit (S: spades, H: hearts, etc.)" +
                "\n - X: Number (A: ace, K: king, Q: queen, etc.)", UpdateType.SEND_REQUEST);
    }

    //Update type for partner card acknowledgement
    public static IndexUpdate createPartnerCardAcknowledgement(int player, Card card) {
        return new IndexUpdate(player,
                "You have selected " + card + " as your partner card!",
                UpdateType.SEND_UPDATE);
    }

    //Update type for the group
    public static IndexUpdate createCardGroupUpdate(int player, Card card) {
        return new IndexUpdate(0, card + " was played by P" + player + "!", UpdateType.SEND_UPDATE);
    }

    public static IndexUpdate createWinnerGroupUpdate(int player1, int player2) {
        int firstPlayer = player1 < player2 ? player1 : player2;
        int secondPlayer = player1 > player2 ? player1 : player2;
        return new IndexUpdate(0, "Partners P" + firstPlayer + " and P" +
                secondPlayer + " are your winners!",
                UpdateType.GAME_END);
    }

    public static IndexUpdate createTrickGroupUpdate(int lastPlayer, Card lastCard, int winningPlayer, int turnNumber) {
        return new IndexUpdate(0, String.format("%s was played by P%d!\nP%d wins trick %d!",
                lastCard, lastPlayer, winningPlayer, turnNumber), UpdateType.SEND_UPDATE);
    }

    public static IndexUpdate createPartnerGroupUpdate(Card card, int player) {
        return new IndexUpdate(0, String.format("%s chosen as the partner card!\nP%d goes first",
                card, player),
                UpdateType.SEND_UPDATE);
    }

    public static IndexUpdate createPartnerGroupEdit(Card card) {
        return new IndexUpdate(0, "*Partner Card*: " + card, UpdateType.PARTNER_CARD);
    }

    public static IndexUpdate createPlayerHandInitialUpdate(int player, CardCollection hand) {
        return new IndexUpdate(player, hand.toString(), UpdateType.SEND_HAND);
    }

    public static IndexUpdate createPlayerHandUpdate(int player, CardCollection hand) {
        return new IndexUpdate(player, hand.toString(), UpdateType.EDIT_HAND);
    }

    public static IndexUpdate createCurrentTrickUpdate(int trickCount, Trick trick) {
        return new IndexUpdate(0, trickCount + ": " + trick, UpdateType.EDIT_HAND);
    }

    public static IndexUpdate createInvalidCardUpdate(int player, String reason) {
        return new IndexUpdate(player, "Card chosen is invalid: " + reason, UpdateType.ERROR);
    }

    public static IndexUpdate createTrickCountUpdate(int[] trickCount) {
        return new IndexUpdate(0, String.format("P1: %d, P2: %d, P3: %d, P4: %d",
                trickCount[0], trickCount[1], trickCount[2], trickCount[3]), UpdateType.EDIT_STATE);
    }

    /*

    Bidding IndexUpdates

    */
    //start game feed, sets orientation
    public static IndexUpdate createGameStartNotice() {
        return new IndexUpdate(0,
                "*Orientation*: \n" +
                "North: P1\n" +
                "East: P2\n" +
                "South: P3\n" +
                "West: P4",
                UpdateType.GAME_START);
    }

    //Acknowledge player bid
    public static IndexUpdate createPlayerBidAcknowledgement(int player, Bid bid) {
        return new IndexUpdate(player, bid.equals(Bid.createPassBid()) ?
                "You pass!" : "You bid " + bid + "!", UpdateType.SEND_UPDATE);
    }

    //Send player private Request for bid
    public static IndexUpdate createPlayerBidRequest(int player, Bid bid) {
        return new IndexUpdate(player, "Make your bid in this chat!" +
                "\nYou can send your own bid in the form of XO" +
                "\n - X: Bid number" +
                "\n - O: Suit (NT: no trump, S: spades, etc.)" +
                (bid == null ? "" : "\n*Previous Highest Bid*: " + bid),
                UpdateType.SEND_BID_REQUEST);
    }

    //Send player notification of invalid bid
    public static IndexUpdate createInvalidBidUpdate(int player, String reason) {
        return new IndexUpdate(player, "Bid is invalid: " + reason, UpdateType.ERROR);
    }

    //Send to group the initial bid notification signalling start of bidding. Considered primary message
    public static IndexUpdate createBidGroupInitialUpdate(int player) {
        return new IndexUpdate(0,
                "Bid starting!\nP" + player + " starts the bid!",
                UpdateType.SEND_BID);
    }

    //Send to group the recent bid Update. Considered an Update.
    public static IndexUpdate createBidGroupUpdate(int player, Bid bid) {
        return !bid.equals(Bid.createPassBid()) ?
                new IndexUpdate(0, "P" + player + " bids " + bid, UpdateType.SEND_UPDATE) :
                new IndexUpdate(0, "P" + player + " passes", UpdateType.SEND_UPDATE);
    }

    //Edits the current bid message in the group
    public static IndexUpdate createBidGroupEdit(List<Bid> bids) {
        String listString = bids.toString();
        return new IndexUpdate(0, listString.substring(1, listString.length() - 1),
                UpdateType.EDIT_BID);
    }

    //Edits the bid message to show that there is currently no bids.
    public static IndexUpdate createNoBidEdit() {
        return new IndexUpdate(0, "No bids!", UpdateType.EDIT_BID);
    }

    //Creates the a final edit to notify the interface that bidding is over.
    public static IndexUpdate createBidWonEdit(int player, Bid bid) {
        return new IndexUpdate(0, String.format("*Winning Bid*: %s by P%d\n*Required Tricks*: %d (%d)",
                bid, player, bid.getRequiredNumber(), bid.getOtherRequiredNumber()), UpdateType.BID_END);
    }

    public static IndexUpdate createBidWonUpdate(int lastPlayer, int bidWinner, Bid bid) {
        return new IndexUpdate(0, String.format("P%d passes!\nP%d wins the bidding with a bid of %s!",
                lastPlayer, bidWinner, bid), UpdateType.SEND_UPDATE);
    }

    public static IndexUpdate createSevenNTBidUpdate(int bidWinner) {
        return new IndexUpdate(0, String.format("P%d bids 7NT!\nP%d wins the bidding with a bid of 7NT!",
                bidWinner, bidWinner), UpdateType.SEND_UPDATE);
    }

}
