import java.util.HashMap;

public class TutorialEngine extends OpenHandEngine {

    private HashMap<Integer, int[]> phaseProgress;
    private HashMap<Integer, Integer> phaseLimit;
    private int currentPhase;
    private int numPhases;

    private boolean introduction;

    public static final String SKIP_STRING = "/skip";
    public static final String NEXT_STRING = "/next";


    public TutorialEngine(long chatId) {
        super(chatId);
        this.phaseProgress = new HashMap<>();
        this.phaseLimit = new HashMap<>();
        phaseLimit.put(1, 3);
        currentPhase = 1;
        numPhases = 1;
        for (int i = 1; i <= numPhases; i++) {
            phaseProgress.put(i, new int[5]);
        }
        introduction = true;
    }

    public GameUpdate startGame() {
        GameUpdate update = super.startGame();
        if (phaseLimit.get(1) >= 1) {
            for (int player = 1; player <= 4; player++) {
                update.add(TutorialUpdates.createTutorialUpdate(player, 1, 0));
                progressPlayer(player);
            }
        }
        return update;
    }

    @Override
    public GameStatus getGameStatus() {
        if (introduction) return GameStatus.COMMUNICATING;
        else return super.getGameStatus();
    }

    @Override
    public GameUpdate processResponse(int player, String response) {
        GameUpdate update = new GameUpdate();
        if (expectingResponse(player)) {
            if (response.equals(NEXT_STRING)) {
                update.add(TutorialUpdates.createTutorialUpdate(player, currentPhase,
                        getCurrentProgressOfPhase(player)));
                progressPlayer(player);
            } else if (response.equals(SKIP_STRING)) {
                update.add(TutorialUpdates.createSkipTutorialUpdate(player));
                skipPlayer(player);
            }
            if (checkPhaseCompleted()) introduction = false;
        }
        return update;
    }


    private int getCurrentProgressOfPhase(int player) {
        return getCurrentPhaseProgress()[player];
    }

    private int getCurrentPhase() {
        return currentPhase;
    }

    private int[] getCurrentPhaseProgress() {
        return phaseProgress.get(getCurrentPhase());
    }

    public boolean expectingResponse(int player) {
        int phaseNumber = getCurrentPhase();
        return getCurrentProgressOfPhase(player) < phaseLimit.get(phaseNumber);
    }

    private void progressPlayer(int player) {
        getCurrentPhaseProgress()[player]++;
    }

    private int getPhaseLimit(int phase) {
        return phaseLimit.get(phase);
    }

    private void skipPlayer(int player) {
        getCurrentPhaseProgress()[player] = getPhaseLimit(getCurrentPhase());
    }

    private void progressPhase() {
        currentPhase++;
    }

    private boolean checkPhaseCompleted() {
        for (int i = 1; i <= 4; i++) {
            if (expectingResponse(i)) return false;
        }
        return true;
    }

}
