//package geheimlichagent;
//
//import heimlich_and_co.HeimlichAndCo;
//import heimlich_and_co.actions.HeimlichAndCoAction;
//import heimlich_and_co.actions.HeimlichAndCoAgentMoveAction;
//import heimlich_and_co.enums.Agent;
//
//import java.util.*;
//
//public class GroupNode {
//
//    /**
//     * This constant balances between exploration and exploitation.
//     * The usually recommended value for this is square root of 2, but performance may be improved by changing it.
//     */
//    private static final double C = Math.sqrt(2);
//
//    /**
//     * Saves the player id of the player for which the tree is build. I.e. the player for which the best action should
//     * be chosen in the end.
//     */
//    private final int playerId;
//    /**
//     * the current game (state)
//     */
//    private final HeimlichAndCo game;
//
//    private final Random random;
//
//    private Map<HeimlichAndCoAction, MctsNode> children;
//    private List<HeimlichAndCoAction> possibleGroupActions;
//    private int groupUct;
//    /**
//     * The number of actions used for calculating the groupUct value;
//     */
//    private final int GROUP_UCT_SAMPLE_SIZE = 3;
//
//    public GroupNode(int playerId, HeimlichAndCo game, int startRange, int endRange) {
//        this.playerId = playerId;
//        this.game = game;
//        this.random = new Random();
//
//        // Get curr player
//        Agent curAgent = Agent.values()[game.getCurrentPlayer()];
//        // Get current player position
//        final int curPos = game.getBoard().getAgentsPositions().get(Agent.values()[game.getCurrentPlayer()]);
//        // Get die roll
//        final int dieRoll = this.game.getBoard().getLastDieRoll();
//        boolean ruinsIsIncluded = false;
//
//        if (startRange == 11) {
//            ruinsIsIncluded = true;
//            startRange = 0;
//        }
//
//        Set<HeimlichAndCoAction> possibleActions = this.game.getPossibleActions();
//
//        // filter the possible actions in the actions that are possible for the current group nodes range
//        for (HeimlichAndCoAction action : possibleActions) {
//            if (action instanceof HeimlichAndCoAgentMoveAction) {
//                EnumMap<Agent, Integer> moveActionMap = ActionHelper.getAgentMovesFromMoveAction((HeimlichAndCoAgentMoveAction) action);
//
//                int moveAmount = moveActionMap.get(curAgent);
//                int newPossiblePos = (curPos + moveAmount) % 12;
//                if ((newPossiblePos >= startRange && newPossiblePos <= endRange) || (ruinsIsIncluded && newPossiblePos == 11)) {
//                    this.possibleGroupActions.add(action);
//                }
//            }
//        }
//    }
//
//
//    /**
//     * Calculates the UCT score of a sample of actions representing the group.
//     * In the case that no playout has been done yet for an action, the maximum Double value is returned. This is in line with exploring
//     * each state/action at least once before exploring a state/action twice. // TODO how to handle this?
//     * <p>
//     * Note: The action has to be a valid action in the current game state.
//     *
//     * @return UCT score of the group
//     */
//    public double calculateGroupUCT() {
//        if (this.children.isEmpty()) {
//            return Double.MAX_VALUE;
//        }
//
//
//        // TODO only do this random sampling if there are only children with 0 playouts?
//        for (int i = 0; i < GROUP_UCT_SAMPLE_SIZE; i++) {
//            HeimlichAndCoAction randomAction = possibleGroupActions.get(random.nextInt(possibleGroupActions.size()));
//            possibleGroupActions.remove(randomAction);
//
//            if (this.children.containsKey(randomAction)) {
//                MctsNode child = this.children.get(randomAction);
//
//            }
//        }
//
//        if (this.children.containsKey(action)) {
//            MctsNode child = this.children.get(action);
//            if (child.playouts == 0 || this.playouts == 0) { //this should never happen
//                throw new IllegalStateException("Illegal 0 value in calculateUCT");
//            }
//            double qSA;
//            if (this.game.getCurrentPlayer() == MctsNode.playerId) {
//                qSA = ((double) child.wins / child.playouts);
//            } else {
//                //if the current player is not the player we are maximizing for, we have to 'invert' the wins, as the
//                //other players of course do not want 'our' player to win. Meaning, they of course don't take the action
//                //which benefits 'our' player
//                qSA = ((double) (child.playouts - child.wins) / child.playouts);
//            }
//
//            double nS = this.playouts;
//            double nSA = child.playouts;
//            return qSA + C * Math.sqrt(Math.log(nS) / nSA);
//        }
//        return Double.MAX_VALUE;
//    }
//
//    /**
//     * Calculates the UCT score of an action.
//     * In the case that no playout has been done yet for an action, the maximum Double value is returned. This is in line with exploring
//     * each state/action at least once before exploring a state/action twice.
//     * <p>
//     * Note: The action has to be a valid action in the current game state.
//     *
//     * @param action for which UCT score should be calculated
//     * @return UCT score of action
//     */
//    private double calculateUCT(HeimlichAndCoAction action) {
//        if (action == null || !this.game.isValidAction(action)) {
//            throw new IllegalArgumentException("Action must not be null, and allowed in the current state");
//        }
//        if (this.children.isEmpty()) {
//            return Double.MAX_VALUE;
//        }
//
//        if (this.children.containsKey(action)) {
//            MctsNode child = this.children.get(action);
////            if (child.playouts == 0 || this.playouts == 0) { //this should never happen
////                throw new IllegalStateException("Illegal 0 value in calculateUCT");
////            }
//            double qSA;
//            if (this.game.getCurrentPlayer() == this.playerId) {
//                qSA = ((double) child.wins / child.playouts);
//            } else {
//                //if the current player is not the player we are maximizing for, we have to 'invert' the wins, as the
//                //other players of course do not want 'our' player to win. Meaning, they of course don't take the action
//                //which benefits 'our' player
//                qSA = ((double) (child.playouts - child.wins) / child.playouts);
//            }
//
//            double nS = this.playouts; // Should be from mcts node? or just do everything in mcts? prbly easier?
//            double nSA = child.playouts;
//            return qSA + C * Math.sqrt(Math.log(nS) / nSA);
//        }
//        return Double.MAX_VALUE;
//    }
//}
