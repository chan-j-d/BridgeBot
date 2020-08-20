public class OpenHandEngine extends GameEngine {

    public OpenHandEngine(long chatId) {
        super(chatId);
    }

    @Override
    public GameUpdate startBid() {
        GameUpdate update = super.startBid();
        for (int i = 1; i < 5; i++) {
            update.add(1, createOtherHandsUpdate(i));
        }
        return update;

    }

    @Override
    protected GameUpdate registerTrick(Card cardPlayed) {
        GameUpdate update = super.registerTrick(cardPlayed);
        for (int i = 1; i < 5; i++) {
            update.add(1, createOtherHandsUpdate(i));
        }
        return update;
    }

    protected IndexUpdate createOtherHandsUpdate(int player) {
        StringBuilder finalString = new StringBuilder();
        finalString.append("*Other player hands*: ");

        for (int i = 1; i <= 4; i++) {
            if (i == player) continue;
            finalString.append("\n-----------\n");

            finalString.append("P" + i + "*'s hand*:\n```\n");
            finalString.append(processHandBySuit(this.getPlayerState(i).getHand()));
            finalString.append("```");
        }

        return new IndexUpdate(player, finalString.toString(), UpdateType.TUTORIAL);
    }

    private String processHandBySuit(CardCollection hand) {
        StringBuilder string = new StringBuilder();
        for (char c : new char[] {'S', 'H', 'D', 'C'}) {
            if (c != 'S') string.append('\n');
            string.append(Card.charToEmoji(c) + ": ");
            boolean first = true;
            for (Card card : hand) {
                if (card.getSuit() == c) {
                    if (!first) string.append(", ");
                    else first = false;
                    string.append(processNumber(card.getNumber()));
                }
            }
        }
        return string.toString();
    }

    private String processNumber(int number) {
        switch (number) {
            case 1:
                return "A";
            case 11:
                return "J";
            case 12:
                return "Q";
            case 13:
                return "K";
            case 10:
                return "10";
            default:
                return number + "";
        }
    }

}
