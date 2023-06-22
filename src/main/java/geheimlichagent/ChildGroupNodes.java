//package geheimlichagent;
//
//import heimlich_and_co.HeimlichAndCo;
//import heimlich_and_co.actions.HeimlichAndCoAction;
//
//import java.util.Comparator;
//import java.util.LinkedList;
//import java.util.List;
//
//public class ChildGroupNodes {
//    /**
//     * the current game (state)
//     */
//    private final HeimlichAndCo game;
//
//    GroupNode low;
//    GroupNode mid;
//    GroupNode high;
//
//    public ChildGroupNodes(int curPos, int dieRoll, HeimlichAndCo game) {
//        this.game = game;
//        low = new GroupNode(game);
//        mid = new GroupNode(game);
//        high = new GroupNode(game);
//        for (int i = 0; i <= dieRoll; i++) {
//            int newPossiblePos = (curPos + i) % 12;
//            if (newPossiblePos == 11 || newPossiblePos <= 4) {
//                low.addMoveAction(i, dieRoll);
//                continue;
//            }
//            if (newPossiblePos > 4 && newPossiblePos <= 7) {
//                mid.addMoveAction(i, dieRoll);
//                continue;
//            }
//            if(newPossiblePos > 7 && newPossiblePos <= 10) {
//                high.addMoveAction(i, dieRoll);
//                continue;
//            }
//        }
//    }
//
//    /**
//     * Calculates the UCT score of an group.
//     * In the case that no playout has been done yet for an action, the maximum Double value is returned. This is in line with exploring
//     * each state/group at least once before exploring a state/action twice. // TODO explore a certain amount of times as described in the paper
//     * <p>
//     * Note: The action has to be a valid action in the current game state.
//     *
//     * @param group for which UCT score should be calculated
//     * @return UCT score of action
//     */
//    private double calculateUCT(GroupNode group) {
//        if (this.group.isEmpty()) {
//            return Double.MAX_VALUE;
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
//     * Gets the groups which have the maximum value according to some comparator. If multiple groups have the same
//     * (maximum) value, all of them will be contained in the returned list.
//     * This method runs in O(n) where n is the number of groups
//     *
//     * @param comparator comparator which should be used to compare two actions (e.g. tree policy like UCT)
//     * @return a List of actions which have the maximum value when compared with the given Comparator
//     */
//    private LinkedList<GroupNode> getMaximumValuedGroups(Comparator<GroupNode> comparator) {
//        LinkedList<GroupNode> groups = new LinkedList<>(List.of(this.low, this.mid, this.high));
//        LinkedList<GroupNode> maxGroups = new LinkedList<>();
//        for (GroupNode group : groups) {
//            if (maxGroups.isEmpty()) { //this is only true in the first iteration
//                maxGroups.add(group);
//            } else if (comparator.compare(maxGroups.get(0), group) == 0) { //the current action is equal
//                maxGroups.add(group);
//            } else if (comparator.compare(maxGroups.get(0), group) < 0) { //the current action has a larger value
//                //i.e. clear the list and add the current action to it
//                maxGroups = new LinkedList<>();
//                maxGroups.add(group);
//            }
//        }
//        return maxGroups;
//    }
//}
