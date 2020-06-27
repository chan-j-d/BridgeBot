import java.util.ArrayList;

public class CardCollection extends ArrayList<Card> {

    @Override
    public String toString() {
        String baseString = super.toString();
        return baseString.substring(1, baseString.length() - 1);
    }


    protected static int countPoints(CardCollection hand) {
        char prevSuit = '\0';
        int totalPoints = 0;
        int currentSuitCount = 0;

        char currentSuit;
        for (Card card : hand) {
            totalPoints += getPoints(card);

            currentSuit = card.getSuit();
            if (prevSuit != currentSuit) {
                totalPoints += currentSuitCount > 4 ? currentSuitCount - 4 : 0;
                currentSuitCount = 1;
                prevSuit = currentSuit;
            } else {
                currentSuitCount++;
            }
        }

        totalPoints += currentSuitCount > 4 ? currentSuitCount - 4 : 0;

        return totalPoints;
    }

    private static int getPoints(Card card) {
        int number = card.getNumber();
        switch (number) {
            case 1:
                return 4;
            case 13:
            case 12:
            case 11:
                return number - 10;
            default:
                return 0;
        }
    }



}
