public class IndexUpdateGenerator {

    public static IndexUpdate createPlayerCardRequest(int player) {
        return new IndexUpdate(player, "Your turn to play a card!", UpdateType.SEND);
    }

    public static IndexUpdate createPartnerCardRequest(int bidWinner) {
        return new IndexUpdate(bidWinner,"Choose a partner card!", UpdateType.SEND);
    }

    public static IndexUpdate createCardGroupUpdate(int player, Card card) {
        return new IndexUpdate(0, card + " was played by P" + player + "!", UpdateType.EDIT_UPDATE);
    }

    public static IndexUpdate createTrickStartUpdate(int player) {
        return new IndexUpdate(0, "P" + player + " goes first!", UpdateType.SEND_UPDATE);
    }

    public static IndexUpdate createWinnerGroupUpdate(int player1, int player2) {
        int firstPlayer = player1 < player2 ? player1 : player2;
        int secondPlayer = player1 > player2 ? player1 : player2;
        return new IndexUpdate(0, "Partners P" + firstPlayer + " and P" +
                secondPlayer + " are your winners!",
                UpdateType.SEND);
    }

    public static IndexUpdate createTrickGroupUpdate(int player, int turnNumber) {
        return new IndexUpdate(0, "P" + player + " wins trick " + turnNumber, UpdateType.SEND);
    }

    public static IndexUpdate createPartnerGroupUpdate(Card card) {
        return new IndexUpdate(0, "The partner card chosen is " + card,
                UpdateType.SEND);
    }

    public static IndexUpdate createPlayerHandInitialUpdate(int player, CardCollection hand) {
        return new IndexUpdate(player, "Your hand: \n" + hand, UpdateType.SEND_HAND);
    }

    public static IndexUpdate createPlayerHandUpdate(int player, CardCollection hand) {
        return new IndexUpdate(player, "Your hand: \n" + hand, UpdateType.EDIT_HAND);
    }

    public static IndexUpdate createCurrentTrickInitialUpdate(CardCollection trick) {
        return new IndexUpdate(0, "Current trick: " + trick, UpdateType.SEND_HAND);
    }

    public static IndexUpdate createCurrentTrickUpdate(CardCollection trick) {
        return new IndexUpdate(0, "Current trick: " + trick, UpdateType.EDIT_HAND);
    }

    public static IndexUpdate createInvalidCardUpdate(int player, String reason) {
        return new IndexUpdate(player, "Card chosen is invalid: " + reason, UpdateType.SEND);
    }

    public static IndexUpdate createPlayerBidRequest(int player) {
        return new IndexUpdate(player, "Make your bid in this chat!", UpdateType.SEND);
    }

    public static IndexUpdate createInvalidBidUpdate(int player, String reason) {
        return new IndexUpdate(player, "Bid is invalid: " + reason, UpdateType.SEND);
    }

    public static IndexUpdate createBidGroupInitialUpdate(int player) {
        return new IndexUpdate(0,
                "Bid starting!\nP" + player + " starts the bid!",
                UpdateType.SEND_UPDATE);
    }

    public static IndexUpdate createBidGroupUpdate(int player, Bid bid) {
        return new IndexUpdate(0, "P" + player + " bids " + bid, UpdateType.EDIT_UPDATE);
    }

    public static IndexUpdate createBidWonUpdate(int player, Bid bid) {
        return new IndexUpdate(0, "P" + player + " wins with a bid of " + bid, UpdateType.SEND_UPDATE);
    }

}
