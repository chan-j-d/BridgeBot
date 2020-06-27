public class GameHashImpl1 implements GameHash {

    public String hashGame(GameReplay game) {
        StringBuilder finalHash = new StringBuilder();

        //First player
        finalHash.append((game.getFirstPlayer() - 1) + "");

        //Part one, bidding
        for (int i = 1; i <= game.getNumBids(); i++) {
            finalHash.append(convertBidToLetter(game.getBid(i)));
        }
        finalHash.append("-");

        //Part two, partner card
        finalHash.append(convertCardToLetter(game.getPartnerCard()));
        finalHash.append("-");

        //Part three, player hands

        //Num tricks played
        finalHash.append(convertNumTricks(game.getNumTricks()));

        StringBuilder[] playerHands = new StringBuilder[4];
        for (int i = 0; i < 4; i++) {
            playerHands[i] = new StringBuilder();
        }
        for (int i = 1; i <= game.getNumTricks(); i++) {
            for (int player = 1; player <= 4; player++) {
                playerHands[player - 1].append(
                        convertCardToLetter(game.getCardPlayed(i, player)));
            }
        }

        for (int player = 1; player <= 4; player++) {
            playerHands[player - 1].append(
                    game.getUnplayedCards(player).stream()
                            .reduce("", (x, y) -> x + convertCardToLetter(y), (x, y) -> x + y));
        }

        for (int player = 1; player <= 4; player++) {
            finalHash.append(playerHands[player - 1]);
        }

        return finalHash.toString();
    }

    private static char convertBidToLetter(Bid bid) {
        if (bid.equals(Bid.createPassBid())) {
            return 'z';
        } else {
            char suit = bid.getSuit();
            int suitNumber = -1;
            switch (suit) {
                case 'C':
                    suitNumber = 0;
                    break;
                case 'D':
                    suitNumber = 1;
                    break;
                case 'H':
                    suitNumber = 2;
                    break;
                case 'S':
                    suitNumber = 3;
                    break;
                case 'N':
                    suitNumber = 4;
                    break;
            }
            int value = (bid.getNumber() - 1) * 5 + suitNumber;

            if (value <= 25) {
                return (char) ('A' + value);
            } else {
                return (char) ('a' + value - 26);
            }
        }
    }

    private static char convertCardToLetter(Card card) {
        char suit = card.getSuit();
        int suitNumber = -1;
        switch (suit) {
            case 'C':
                suitNumber = 0;
                break;
            case 'D':
                suitNumber = 1;
                break;
            case 'H':
                suitNumber = 2;
                break;
            case 'S':
                suitNumber = 3;
                break;
        }
        int value = card.getNumber() == 1 ? suitNumber * 13 + (14 - 2)
                : suitNumber * 13 + card.getNumber() - 2;

        if (value <= 25) {
            return (char) ('A' + value);
        } else {
            return (char) ('a' + value - 26);
        }

    }

    private char convertNumTricks(int numTricks) {
        return (char) (numTricks + 'a' - 1);
    }

    public static void main(String[] args) {
        System.out.println(convertBidToLetter(Bid.createBid("5NT")));
        System.out.println(convertBidToLetter(Bid.createBid("6C")));
        System.out.println(convertBidToLetter(Bid.createBid("6D")));

        System.out.println(convertCardToLetter(Card.createCard("c2")));
        System.out.println(convertCardToLetter(Card.createCard("d3")));
        System.out.println(convertCardToLetter(Card.createCard("s3")));
    }

}
