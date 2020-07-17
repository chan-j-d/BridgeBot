import java.util.HashMap;
import java.util.Map;

/*
This engine showcases all cards that have already been played by all players
*/
public class CardPlayedEngine extends GameEngine {

     private HashMap<Character, CardCollection> cardsPlayed;

    public CardPlayedEngine(long chatId) {
        super(chatId);
        cardsPlayed = new HashMap<>();
        for (char c : new char[] {'S', 'H', 'D', 'C'}) {
            cardsPlayed.put(c, new CardCollection());
        }
    }

    @Override
    protected GameUpdate registerPartnerCard(Card partnerCard) {
        GameUpdate update = super.registerPartnerCard(partnerCard);
        for (int i = 1; i < 5; i++) {
            update.add(1, createCardPlayedUpdate(i));
        }
        return update;
    }

    @Override
    protected GameUpdate registerTrick(Card cardPlayed) {
        GameUpdate update = super.registerTrick(cardPlayed);
        registerCards(currentTrick);

        for (int i = 1; i < 5; i++) {
            update.add(1, createCardPlayedUpdate(i));
        }
        return update;
    }

    private void registerCards(CardCollection trick) {
        for (Card card : trick) {
            addCard(card, cardsPlayed.get(card.getSuit()));
        }
    }

    private void addCard(Card card, CardCollection suit) {
        if (card.getNumber() == 1) {
            suit.add(card);
            return;
        }
        int index = 0;
        while (index < suit.size()) {
            int currentNumber = suit.get(index).getNumber();
            if (currentNumber == 1) {
                suit.add(index, card);
                return;
            } else if (card.getNumber() < currentNumber) {
                suit.add(index, card);
                return;
            } else index++;
        }
        suit.add(card);
    }

    private IndexUpdate createCardPlayedUpdate(int player) {
        int longestSuit = -1;
        StringBuilder finalString = new StringBuilder();
        finalString.append("*Cards Played*: ```");

        for (char c : new char[] {'S', 'H', 'D', 'C'}) {
            int currentSuitLength = cardsPlayed.get(c).size();
            if (currentSuitLength > longestSuit) longestSuit = currentSuitLength;
        }

        for (int index = 0; index < longestSuit; index++) {
            finalString.append('\n');
            for (char c : new char[] {'S', 'H', 'D', 'C'}) {
                CardCollection suitCardsPlayed = cardsPlayed.get(c);
                if (suitCardsPlayed.size() <= index) {
                    finalString.append("     ");
                } else {
                    finalString.append(String.format(" %s ", suitCardsPlayed.get(
                            suitCardsPlayed.size() - index - 1
                    )));
                }
                if (c != 'C') finalString.append('|');
            }
        }
        finalString.append("```");
        return new IndexUpdate(player, finalString.toString(), UpdateType.TUTORIAL);
    }





}
