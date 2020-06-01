import org.telegram.telegrambots.meta.api.objects.games.Game;

public class Main {

    public static void main(String[] args) {

        Player[] players = new Player[4];

        for (int i = 0; i < 4; i++) {
            players[i] = new TestPlayer("TestPlayer " + (i + 1));
        }

        GameCoordinator newGame = new GameCoordinator(players);
        Pair<Player, Player> pair = newGame.startGame();

        System.out.println("Player " + pair.first + " and " + pair.second + " are your winners!");

    }

}
