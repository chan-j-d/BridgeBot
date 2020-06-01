import java.util.ArrayList;
import java.util.Iterator;

public class BidCoordinator {

    public static Pair<Bid, Integer> bidding(Player[] players) {
        boolean prevPass = false;
        boolean twoPrevPass = false;
        boolean threePrevPass = false;

        int currentHighestBidderIndex = -1;
        Bid currentHighestBid = Bid.createPassBid();
        Bid passBid = Bid.createPassBid();

        int index = 0; //can consider randomising this
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


    }




}
