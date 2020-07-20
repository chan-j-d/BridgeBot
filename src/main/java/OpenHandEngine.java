public class OpenHandEngine extends GameEngine {

    public OpenHandEngine(long chatId) {
        super(chatId);
    }

    @Override
    protected GameUpdate registerTrick(Card cardPlayed) {
        GameUpdate update = super.registerTrick(cardPlayed);
        for (int i = 1; i < 5; i++) {
            update.add(1, createOtherHandsUpdate(i));
        }
        return update;
    }

    @Override
    protected GameUpdate registerPartnerCard(Card partnerCard) {
        GameUpdate update = super.registerPartnerCard(partnerCard);
        for (int i = 1; i < 5; i++) {
            update.add(1, createOtherHandsUpdate(i));
        }
        return update;
    }

    private IndexUpdate createOtherHandsUpdate(int player) {
        StringBuilder finalString = new StringBuilder();
        finalString.append("*Number of each suit played*: ```");

        for (int i = 1; i <= 4; i++) {
            if (i == player) continue;
            else if ((i != 1 && player != 1) || (player == 1 && i != 2)) {
                finalString.append("\n-----------\n");
            } else finalString.append('\n');

            finalString.append("*P" + i + "'s hand*:\n");
            finalString.append(processHandBySuit(this.getPlayerState(i).getHand()));
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
