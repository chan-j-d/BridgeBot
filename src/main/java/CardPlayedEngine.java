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
            if (card.getNumber() < suit.get(index).getNumber()) {
                suit.add(index, card);
                return;
            } else index++;
        }
        suit.add(card);
    }

    private IndexUpdate createCardPlayedUpdate(int player) {
        StringBuilder finalString = new StringBuilder();
        finalString.append("Cards Played: ```\n");
        for (char c : new char[] {'S', 'H', 'D', 'C'}) {
            finalString.append(Card.charToEmoji(c) + ": [" + cardsPlayed.get(c) + "]\n");
        }
        finalString.deleteCharAt(finalString.length () - 1);
        finalString.append("```");
        return new IndexUpdate(player, finalString.toString(), UpdateType.TUTORIAL);
    }





}
