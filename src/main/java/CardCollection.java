import java.util.ArrayList;

public class CardCollection extends ArrayList<Card> {

    @Override
    public String toString() {
        String baseString = super.toString();
        return baseString.substring(1, baseString.length() - 1);
    }

}
