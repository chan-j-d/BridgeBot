import java.util.HashMap;
import java.util.Map;

/*
This engine showcases all cards that have already been played by all players
*/
public class CardPlayedEngine extends GameEngine {

     private StringBuilder cardsPlayedString;

    public CardPlayedEngine(long chatId) {
        super(chatId);
        cardsPlayedString = new StringBuilder();
        cardsPlayedString.append("*Cards played*: \n```     N      E     S      W  ");
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
        registerCards(currentTrick);
        GameUpdate update = super.registerTrick(cardPlayed);

        for (int i = 1; i < 5; i++) {
            update.add(1, createCardPlayedUpdate(i));
        }
        return update;
    }

    private void registerCards(Trick trick) {
        cardsPlayedString.append("\n" + (turnCycle >= 10 ? turnCycle : turnCycle + " "));

        for (int player = 1; player <= 4; player++) {
            Card card = trick.getCardPlayedBy(player);
            if (card.getNumber() != 10) {
                cardsPlayedString.append("| " + card + " ");
            } else {
                cardsPlayedString.append("| " + card);
            }
        }
    }

    private IndexUpdate createCardPlayedUpdate(int player) {
        return new IndexUpdate(player, cardsPlayedString.toString() + "```",
                UpdateType.TUTORIAL);
    }





}
