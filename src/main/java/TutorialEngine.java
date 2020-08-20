import java.util.HashMap;
import java.util.Optional;

public class TutorialEngine extends OpenHandEngine {

    private HashMap<Integer, int[]> phaseProgress;
    private HashMap<Integer, Integer> phaseLimit;
    private int currentPhase;
    private int numPhases;

    private boolean[] skippedCheck;

    private int partnerShownTurn;
    private int trumpBrokenTurn;

    private boolean trumpBrokenThisTurn;
    private boolean partnerRevealedTurn;
    private boolean partnerShown;

    private boolean introduction;

    public static final String SKIP_STRING = "/skip";
    public static final String NEXT_STRING = "/next";

    private static final String BIDDING_STRING = "You must bid something larger than the current bid or pass. Bid ordering goes by number followed by suit with NT > ♠ > ♥ > ♦ > ♣" +
            "Choose a suit where you have many large cards or NT if all your suits are strong. It is not necessary to take part in bidding (especially if your hands are weak).";

    private static final String DEFAULT_PARTNER_STRING = "Ideally, we would like to pick a partner whose hands synergizes best with us. " +
            "However, in a normal game, we do not know what cards other players have and can only choose one card our partner has.";
    private static final String TRUMP_SUIT_PARTNER_STRING = "For a game with a trump suit, one suggestion is to pick the largest card in the suit " +
            "that you do not have. E.g. You have A, K, J so you choose Q of the trump suit.";
    private static final String NO_TRUMP_PARTNER_STRING = "For a game with a no trump bid, one suggestion is to pick the Ace of a suit" +
            " that you are weak in.";
    private static final String TRUMP_BROKEN_STRING = "Note that trump has been broken. You can now start tricks with cards from the trump suit.";
    private static final String FIRST_CARD_T_NTB_STRING = "You can only start with cards not of the trump suit as trump has not been broken.";
    private static final String FIRST_CARD_T_TB_STRING = "You can start with any card as trump has already been broken.";
    private static final String FIRST_CARD_NT_STRING = "You can start with cards of any suit.";

    public TutorialEngine(long chatId) {
        super(chatId);
        this.phaseProgress = new HashMap<>();
        this.phaseLimit = new HashMap<>();
        phaseLimit.put(1, 5);
        currentPhase = 1;
        numPhases = 1;
        for (int i = 1; i <= numPhases; i++) {
            phaseProgress.put(i, new int[5]);
        }
        introduction = true;
        trumpBrokenThisTurn = false;
        partnerRevealedTurn = false;
        partnerShown = false;
        partnerShownTurn = -1;
        trumpBrokenTurn = -1;

        skippedCheck = new boolean[] {false, false, false, false, false};
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
        if (!checkPhaseCompleted(player)) {
            if (response.equals(NEXT_STRING)) {
                update.add(TutorialUpdates.createTutorialUpdate(player, currentPhase,
                        getCurrentProgressOfPhase(player)));
                progressPlayer(player);
            } else if (response.equals(SKIP_STRING)) {
                update.add(TutorialUpdates.createSkipTutorialUpdate(player));
                skipPlayer(player);
            }
            if (checkGoNextPhase()) leaveIntroduction();
        }
        return update;
    }

    private void leaveIntroduction() {
        introduction = false;
    }

    @Override
    public GameUpdate processPlay(Bid bid) {
        GameUpdate update = super.processPlay(bid);
        if (!biddingInProgress()) {
            int bidWinner = update.get(0).getIndex();
            update.add(createOtherHandsUpdate(bidWinner));
        } else {
            update.add(createOtherHandsUpdate(bidCoordinator.getCurrentPlayer()));
        }
        return update;
    }

    @Override
    protected GameUpdate registerCardPlayed(Card card) {
        GameUpdate update = super.registerCardPlayed(card);
        if (!trickFirstCard) {
            update.add(createOtherHandsUpdate(currentPlayer));
        }
        return update;
    }

    @Override
    protected GameUpdate registerPartnerCard(Card card) {
        GameUpdate update = super.registerPartnerCard(card);
        update.add(createOtherHandsUpdate(currentPlayer));
        return update;
    }

    @Override
    protected IndexUpdate createOtherHandsUpdate(int player) {
        IndexUpdate oldUpdate = super.createOtherHandsUpdate(player);
        return addTutorialMessage(oldUpdate, getAdditionalString(player));

    }

    @Override
    protected GameUpdate registerTrick(Card card) {
        if (!partnerShown && currentTrick.checkContains(partnerCard)) {
            partnerShown = true;
            partnerShownTurn = turnCycle + 1;
        }
        return super.registerTrick(card);
    }

    protected String getAdditionalString(int player) {
        if (playerSkipCheck(player)) {
            return null;
        }
        switch (getGameStatus()) {
            case COMMUNICATING:
                return null;
            case BIDDING:
                return BIDDING_STRING;
            case PARTNER:
                if (!bidCoordinator.getWinningBid().first.isNoTrumpBid()) {
                    return DEFAULT_PARTNER_STRING + '\n' + TRUMP_SUIT_PARTNER_STRING;
                } else {
                    return DEFAULT_PARTNER_STRING + '\n' + NO_TRUMP_PARTNER_STRING;
                }
            case FIRST_CARD:
                String tempString = "";
                if (turnCycle == partnerShownTurn) {
                    tempString = createPartnerCardShownString();
                }
                if (player == currentPlayer) {
                    tempString = tempString + (winningBid.isNoTrumpBid() ? FIRST_CARD_NT_STRING :
                            (brokenTrump ? FIRST_CARD_T_TB_STRING : FIRST_CARD_T_NTB_STRING));
                    if (!trumpBrokenThisTurn && brokenTrump || trumpBrokenTurn == turnCycle) {
                        tempString = tempString + '\n' + TRUMP_BROKEN_STRING;
                        trumpBrokenThisTurn = true;
                        trumpBrokenTurn = turnCycle;
                    }
                } else {
                    if (!trumpBrokenThisTurn && brokenTrump || trumpBrokenTurn == turnCycle) {
                        tempString = tempString + TRUMP_BROKEN_STRING;
                        trumpBrokenThisTurn = true;
                        trumpBrokenTurn = turnCycle;
                    }
                }
                return tempString.equals("") ? null : tempString;
            case OTHER_CARDS:
                return player == currentPlayer ? createOtherCardString() : null;
            default:
                return null;


        }

    }

    protected IndexUpdate addTutorialMessage(IndexUpdate update, String message) {
        String oldMessage = update.getMessage();
        StringBuilder builder = new StringBuilder(oldMessage);
        Optional<String> additionalString = Optional.ofNullable(message);
        additionalString.ifPresent(s -> builder.append("\n=====TUTORIAL======\n" + s));
        update.setMessage(builder.toString());
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

    public boolean checkPhaseCompleted(int player) {
        int phaseNumber = getCurrentPhase();
        return getCurrentProgressOfPhase(player) >= phaseLimit.get(phaseNumber);
    }

    private void progressPlayer(int player) {
        getCurrentPhaseProgress()[player]++;
    }

    @Override
    public boolean expectingResponse(int player) {
        return !checkPhaseCompleted(player);
    }

    private int getPhaseLimit(int phase) {
        return phaseLimit.get(phase);
    }

    private void skipPlayer(int player) {
        getCurrentPhaseProgress()[player] = getPhaseLimit(getCurrentPhase());
        registerPlayerSkip(player);
    }

    private void progressPhase() {
        currentPhase++;
    }

    private boolean checkGoNextPhase() {
        for (int i = 1; i <= 4; i++) {
            if (!checkPhaseCompleted(i)) return false;
        }
        return true;
    }

    private boolean playerSkipCheck(int player) {
        return skippedCheck[player];
    }

    private void registerPlayerSkip(int player) {
        skippedCheck[player] = true;
    }

    private String createPartnerCardShownString() {
        return "Partner card " + partnerCard + " has been played. Pairs are " + partners1 + ", " + partners2 + '\n';
    }

    private String createOtherCardString() {
        return "You must play a card of the same suit as the first card played (" +
                Card.charToEmoji(firstCardSuit) + ") unless you do not have any card of that suit. Then you can play any card.";

    }


}
