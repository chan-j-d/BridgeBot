import java.util.NoSuchElementException;

public class BridgePlayingField {

    protected CardCollection[] playingField;

    private BridgePlayingField() {
        this.playingField = new CardCollection[5];
    }

    public static BridgePlayingField init() {
        BridgePlayingField newPlayingField = new BridgePlayingField();

        //players
        for (int i = 0; i < 4; i++) {
            newPlayingField.playingField[i] = new CardCollection();
        }

        //
        newPlayingField.playingField[4] = new CardCollection();

        Deck newDeck = Deck.init();

        int index = 0;
        for (int i = 0; i < 52; i++) {
            newPlayingField.playingField[index++].add(newDeck.draw());

            if (index == 4) {
                index = 0;
            }
        }

        for (CardCollection hand : newPlayingField.playingField) {
            hand.sort(new BridgeStandardComparator());
        }

        return newPlayingField;
    }

    public CardCollection getHandOfPlayer(int index) {
        return this.playingField[index];
    }

    public int getIndexOfPlayerWithCard(Card card) {
        for (int i = 0; i < 4; i++) {
            if (this.playingField[i].contains(card)) {
                return i;
            }
        }
        throw new IllegalStateException(card + " has already been used!");
    }

    public void cardPlayed(int player, Card card) {
        Action action = Action.makeAction(playingField[player], playingField[4], card);
        action.run();
    }

    public CardCollection retrieveSet() {
        CardCollection temp = playingField[4];
        playingField[4] = new CardCollection();
        return temp;
    }


}
