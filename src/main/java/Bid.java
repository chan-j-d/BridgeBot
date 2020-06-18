public class Bid implements Comparable<Bid> {

    protected int number;
    protected char suit;

    private Bid(int number, char suit) {
        this.number = number;
        this.suit = suit;
    }

    protected static Bid createNoTrumpBid(int number) {
        return new Bid(number, 'N');
    }

    protected static Bid createPassBid() {
        return new Bid(0, 'P');
    }

    protected boolean isNoTrumpBid() {
        return this.suit == 'N';
    }

    protected int getRequiredNumber() {
        if (this.number == 0) {
            throw new IllegalStateException("Not a valid request!");
        } else {
            return this.number + 6;
        }
    }

    protected int getOtherRequiredNumber() {
        if (this.number == 0) {
            throw new IllegalStateException("Not a valid request!");
        } else {
            return 8 - this.number;
        }
    }

    protected char getSuit() {
        if (this.suit == 'P') {
            throw new IllegalStateException("Not a valid request!");
        } else {
            return this.suit;
        }
    }

    public static Bid createBid(String bidString) {
        String passCheck = bidString.toUpperCase();
        if (passCheck.equals("P") || passCheck.equals("PASS")) {
            return new Bid(0, 'P');
        } else if (bidString.length() < 2 || bidString.length() > 3) {
            throw new IllegalArgumentException("Invalid bid!");
        } else if (!Character.isDigit(bidString.charAt(0)) ||
                Character.isDigit(bidString.charAt(0)) && Character.isDigit(bidString.charAt(1))) {
            throw new IllegalArgumentException("Invalid number for the bid!");
        }

        //valid number
        int bidNumber = Integer.parseInt(bidString.substring(0, 1));
        if (bidNumber < 1 || bidNumber > 7) {
            throw new IllegalArgumentException("Invalid number for the bid!");
        }

        //valid bid number
        if (bidString.length() == 3) {
            if (bidString.substring(1).toUpperCase().equals("NT")) {
                return Bid.createNoTrumpBid(bidNumber);
            } else {
                throw new IllegalArgumentException("Invalid suit for the bid!");
            }
        } else {
            char suitSymbol = Character.toUpperCase(bidString.charAt(1));
            switch (suitSymbol) {
                case 'C':
                case 'D':
                case 'H':
                case 'S':
                    return new Bid(bidNumber, suitSymbol);
                default:
                    throw new IllegalArgumentException("Invalid suit for the bid!");
            }
        }

    }

    public static boolean isValidBidString(String bidString) {
        String passCheck = bidString.toUpperCase();
        if (passCheck.equals("P") || passCheck.equals("PASS")) {
            return true;
        } else if (bidString.length() < 2 || bidString.length() > 3) {
            return false;
        } else if (!Character.isDigit(bidString.charAt(0)) ||
                Character.isDigit(bidString.charAt(0)) && Character.isDigit(bidString.charAt(1))) {
            return false;
        }

        //valid number
        int bidNumber = Integer.parseInt(bidString.substring(0, 1));
        if (bidNumber < 1 || bidNumber > 7) {
            return false;
        }

        //valid bid number
        if (bidString.length() == 3) {
            if (bidString.substring(1).toUpperCase().equals("NT")) {
                return true;
            } else {
                return false;
            }
        } else {
            char suitSymbol = Character.toUpperCase(bidString.charAt(1));
            switch (suitSymbol) {
                case 'C':
                case 'D':
                case 'H':
                case 'S':
                    return true;
                default:
                    return false;
            }
        }
    }

    public int compareTo(Bid otherBid) {
        if (this.number != otherBid.number) {
            return this.number - otherBid.number;
        } else if (this.suit == otherBid.suit) {
            return 0;
        } else if (this.suit == 'N' || otherBid.suit == 'N') {
            return this.suit == 'N' ? 1 : -1;
        } else {
            return this.suit - otherBid.suit;
        }
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Bid) {
            return ((Bid) o).suit == this.suit && ((Bid) o).number == this.number;
        } else {
            return false;
        }
    }

    public String toString() {
        String symbol = "";
        if (this.suit == 'P') {
            return "PASS";
        } else if (this.suit == 'N') {
            symbol = "NT";
        } else {
            switch (this.suit) {
                case 'S':
                    symbol = "♠";
                    break;
                case 'H':
                    symbol = "♥";
                    break;
                case 'D':
                    symbol = "♦";
                    break;
                case 'C':
                    symbol = "♣";
                    break;
            }
        }
        return this.number + symbol;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 128; i++) {
            for (int j = 0; j < 128; j++) {
                try {
                    Bid bid = Bid.createBid("" + ((char) i) + ((char) j));
                    System.out.println(bid);
                } catch (IllegalArgumentException e) {
                }
            }
        }
        System.out.println(Bid.createBid("pAsS"));
        System.out.println(Bid.createBid("6NT"));
    }

}
