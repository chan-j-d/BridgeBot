import java.util.HashMap;

public class SuitPlayedEngine extends GameEngine {

    private HashMap<Character, Integer> suitCountPlayed;


    public SuitPlayedEngine(long chatId) {
        super(chatId);
        suitCountPlayed = new HashMap<>();
        for (char c : new char[] {'S', 'H', 'D', 'C'}) {
            suitCountPlayed.put(c, 0);
        }
    }

    @Override
    protected GameUpdate registerPartnerCard(Card partnerCard) {
        GameUpdate update = super.registerPartnerCard(partnerCard);
        for (int i = 1; i < 5; i++) {
            update.add(1, createSuitPlayedUpdate(i));
        }
        return update;
    }

    @Override
    protected GameUpdate registerTrick(Card cardPlayed) {
        GameUpdate update = super.registerTrick(cardPlayed);
        registerCards(currentTrick);

        for (int i = 1; i < 5; i++) {
            update.add(1, createSuitPlayedUpdate(i));
        }
        return update;
    }

    private void registerCards(Trick trick) {
        for (int player = 1; player <= 4; player++) {
            Card card = trick.getCardPlayedBy(player);
            char suit = card.getSuit();
            suitCountPlayed.put(suit, suitCountPlayed.get(suit) + 1);
        }
    }


    private IndexUpdate createSuitPlayedUpdate(int player) {
        StringBuilder finalString = new StringBuilder();
        finalString.append("*Number of each suit played*: \n```");

        for (char c : new char[] {'S', 'H', 'D', 'C'}) {
            finalString.append(String.format(
                    "\n%c: %d", Card.charToEmoji(c), suitCountPlayed.get(c)
            ));
        }

        finalString.append("```");

        return new IndexUpdate(player, finalString.toString(), UpdateType.TUTORIAL);
    }





}
