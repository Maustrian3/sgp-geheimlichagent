package geheimlichagent;

import at.ac.tuwien.ifs.sge.util.pair.ImmutablePair;
import at.ac.tuwien.ifs.sge.util.pair.Pair;
import heimlich_and_co.HeimlichAndCo;
import heimlich_and_co.actions.HeimlichAndCoAction;
import heimlich_and_co.actions.HeimlichAndCoAgentMoveAction;
import heimlich_and_co.actions.HeimlichAndCoDieRollAction;
import heimlich_and_co.actions.HeimlichAndCoSafeMoveAction;
import heimlich_and_co.enums.Agent;
import heimlich_and_co.enums.Agent;
import heimlich_and_co.actions.HeimlichAndCoSafeMoveAction;
import heimlich_and_co.enums.Agent;
import heimlich_and_co.enums.HeimlichAndCoPhase;

import java.util.*;

public class MctsNode {

    /**
     * This constant balances between exploration and exploitation.
     * The usually recommended value for this is square root of 2, but performance may be improved by changing it.
     */
    private static final double C = Math.sqrt(2);

    /**
     * Saves the player id of the player for which the tree is build. I.e. the player for which the best action should
     * be chosen in the end.
     */
    private static int playerId;
    /**
     * the depth of this node; 0 for root node
     */
    private final int depth;
    /**
     * the current game (state)
     */
    private final HeimlichAndCo game;
    /**
     * The action taken that lead to this node.
     */
    private HeimlichAndCoAction actionToNode;
    /**
     * All resulting child states that have been explored at least once.
     * A child node is reached by taking (applying) the action that is used as the key.
     */
    private final Map<HeimlichAndCoAction, MctsNode> children;
    /**
     * All child group nodes
     */
    private ArrayList<HeimlichAndCoAction> lowGroup;
    private ArrayList<HeimlichAndCoAction> midGroup;
    private ArrayList<HeimlichAndCoAction> highGroup;
    /**
     * parent of this node; null for root node
     */
    private final MctsNode parent;

    private final Random random;
    /**
     * saves how many wins were achieved from this node
     */
    public int wins;
    /**
     * saves how many playouts were done from this node (or descendents of this node)
     */
    public int playouts;

    private final Comparator<HeimlichAndCoAction> actionComparatorUct = Comparator.comparingDouble(this::calculateUCT);
    private final Comparator<HeimlichAndCoAction> actionComparatorQsa = Comparator.comparingDouble(this::calculateQsaOfChild);

    public MctsNode(int wins, int playouts, HeimlichAndCo game, MctsNode parent) {
        this(game, parent);
        this.wins = wins;
        this.playouts = playouts;
    }

    public MctsNode(HeimlichAndCo game, MctsNode parent) {
        this.game = new HeimlichAndCo(game, false);
        this.parent = parent;
        if (parent != null) {
            this.depth = parent.depth + 1;
        } else {
            this.depth = 0;
        }
        this.children = new HashMap<>();
        this.lowGroup = new ArrayList<>();
        this.midGroup = new ArrayList<>();
        this.highGroup = new ArrayList<>();
        this.random = new Random();
    }

    public static void setPlayerId(int playerId) {
        MctsNode.playerId = playerId;
    }

    /**
     * Does backpropagation starting from the current node.
     * Therefore, always increases playouts and increases wins depending on win.
     *
     * @param win indicating whether the game was won or not (2 on win, 1 on draw, 0 on loss).
     */
    public void backpropagation(int win, Map<ImmutablePair<HeimlichAndCoAction, Integer>, ImmutablePair<Double, Integer>> averageRewardStats) {
        if (win != 0 && win != 1 && win != 2) {
            throw new IllegalArgumentException("Win must be either 0, 1 or 2");
        }
        this.playouts++;
        this.wins += win;
        if (this.parent != null) {
            // Update the averageRewardStats:
            // if player/action pair not in map, add it with win/1 otherwise update it with (win + oldWin)/(oldPlayouts + 1)
            actionToNode = (actionToNode instanceof HeimlichAndCoAgentMoveAction) ? reduceActionComplexity((HeimlichAndCoAgentMoveAction) actionToNode) : actionToNode;
            averageRewardStats.computeIfAbsent(
                    new ImmutablePair<>(actionToNode, this.game.getCurrentPlayer()),
                    k -> new ImmutablePair<>((double) win, 1));
            averageRewardStats.computeIfPresent(
                    new ImmutablePair<>(actionToNode, this.game.getCurrentPlayer()),
                    (k, v) -> new ImmutablePair<>((v.getA() + win), v.getB() + 1));
            this.parent.backpropagation(win, averageRewardStats);
        }
    }

    private HeimlichAndCoAction reduceActionComplexity(HeimlichAndCoAgentMoveAction action) {
        Agent curAgent = Agent.values()[game.getCurrentPlayer()];
        EnumMap<Agent, Integer> moveActionMap = ActionHelper.getAgentMovesFromMoveAction(action);
        EnumMap<Agent, Integer> moveActionMapCurPlayer = new EnumMap<>(Agent.class);
        int moveAmount = 0;
        if(moveActionMap.containsKey(curAgent)) {
            moveAmount = moveActionMap.get(curAgent);
        }
        moveActionMapCurPlayer.put(curAgent, moveAmount);
        return new HeimlichAndCoAgentMoveAction(moveActionMapCurPlayer) ;
    }

    /**
     * Calculates the Q(s,a) of a state (i.e. current game state) and an action. This is the expected percentage of wins when taking action a in state s.
     * Formula: #wins/ #playouts
     * Therefore the best score that can be achieved is 1, the worst is 0.
     * <p>
     * Note: The action has to be contained in the children of this node.
     *
     * @param action for which to calculate the percentage
     * @return expected percentage of wins when playing action in the current state
     */
    public double calculateQsaOfChild(HeimlichAndCoAction action) {
        if (!this.children.containsKey(action)) {
            throw new IllegalArgumentException("Action is not contained in children");
        }
        return ((double) children.get(action).wins) / children.get(action).playouts;
    }

    /**
     * Expands the current node with the given action and returns the created node.
     * Action must be null or a valid action that was not used for expansion with this node already.
     * <p>
     * When action is null, returns this node (useful for doing MCTS when dealing with terminal nodes).
     *
     * @param action to apply
     * @return Game node that
     */
    public MctsNode expansion(HeimlichAndCoAction action) {
        if (action == null) {
            return this;
        }
        if (!game.isValidAction(action)) {
            throw new IllegalArgumentException("The given action must be valid.");
        }
        if (this.children.containsKey(action)) {
            throw new IllegalArgumentException("The current node was already expanded with the given action");
        }
        MctsNode newNode = new MctsNode(game.doAction(action), this);
        this.children.put(action, newNode);
        newNode.actionToNode = action;
        return newNode;
    }

    /**
     * selects a node with UCT strategy
     * during the first round checks all possible actions before selecting an action twice
     *
     * @return this node and the selected action -> in the expansion phase the action can be taken from this node to get the new node
     */
    public Pair<MctsNode, HeimlichAndCoAction> selection(boolean simulateAllDiceOutcomes) {
        Set<HeimlichAndCoAction> possibleActions = game.getPossibleActions();
        // this means that this is a terminal game state
        if (possibleActions.isEmpty()) {
            return new ImmutablePair<>(this, null);
        }
        HeimlichAndCoAction selectedAction;
        if (simulateAllDiceOutcomes && game.getCurrentPhase() == HeimlichAndCoPhase.DIE_ROLL_PHASE) {
            possibleActions.remove(HeimlichAndCoDieRollAction.getRandomRollAction());
            selectedAction = possibleActions.toArray(new HeimlichAndCoAction[1])[random.nextInt(possibleActions.size())];
        } else if(game.getCurrentPhase() == HeimlichAndCoPhase.AGENT_MOVE_PHASE) {
            // Get curr agent
            Agent curAgent = Agent.values()[game.getCurrentPlayer()];
            // Get curr player pos
            final int curPos = game.getBoard().getAgentsPositions().get(Agent.values()[game.getCurrentPlayer()]);
            // Get die roll (or extract it from the possible actions?)
            final int dieRoll = this.game.getBoard().getLastDieRoll();

            // Split possible actions into groups
            for (HeimlichAndCoAction action : possibleActions) {
                    EnumMap<Agent, Integer> moveActionMap = ActionHelper.getAgentMovesFromMoveAction((HeimlichAndCoAgentMoveAction) action);
                    int moveAmount = 0;
                    if(moveActionMap.containsKey(curAgent)) {
                        moveAmount = moveActionMap.get(curAgent);
                    }
                    int newPossiblePos = (curPos + moveAmount) % 12;
                    if (newPossiblePos >= 4 && newPossiblePos <= 7) {
                        this.midGroup.add(action);
                    } else if (newPossiblePos >= 8 && newPossiblePos <= 10) {
                        this.highGroup.add(action);
                    } else if ((newPossiblePos >= 0 && newPossiblePos <= 3) || (newPossiblePos == 11)) {
                        this.lowGroup.add(action);
                    } else {
                        throw new IllegalStateException("Action could not be assigned to group, because new position on impossible field");
                    }
            }

//            // Get maximum valued group
//            // by calculating the uct just of a sample of actions of each group
//            ArrayList<HeimlichAndCoAction> bestGroup = this.lowGroup;
//            if(calculateGroupUCT(this.midGroup) > calculateGroupUCT(bestGroup)) {
//                bestGroup = midGroup;
//            }
//            if(calculateGroupUCT(this.highGroup) > calculateGroupUCT(bestGroup)) {
//                bestGroup = highGroup;
//            }
//            if (bestGroup.isEmpty()) {
//                throw new IllegalStateException("There are no actions in the chosen best group");
//            }

            // Get maximum valued group
            // by choosing the larger groups first (as described in the sgp-slides)
            // TODO maybe add a random factor to this? (not only choose the largest group directly)
            ArrayList<HeimlichAndCoAction> bestGroup = this.lowGroup;
            if(this.midGroup.size() > bestGroup.size()) {
                bestGroup = midGroup;
            }
            if(this.highGroup.size() > bestGroup.size()) {
                bestGroup = highGroup;
            }
            if (bestGroup.isEmpty()) {
                throw new IllegalStateException("There are no actions in the chosen best group");
            }

            List<HeimlichAndCoAction> maximumValuedActions = getMaximumValuedActions(new HashSet<>(bestGroup), this.actionComparatorUct);
            selectedAction = maximumValuedActions.get(random.nextInt(maximumValuedActions.size()));
        } else {
            List<HeimlichAndCoAction> maximumValuedActions = getMaximumValuedActions(possibleActions, this.actionComparatorUct);
            selectedAction = maximumValuedActions.get(random.nextInt(maximumValuedActions.size()));
        }

        if (this.children.containsKey(selectedAction)) {
            return this.children.get(selectedAction).selection(simulateAllDiceOutcomes);
        }
        return new ImmutablePair<>(this, selectedAction);
    }

    /**
     * Selects the best action to take in the current node.
     * This means taking the best action according to Q(s,a).
     *
     * @return the action with the best expected result.
     */
    public ImmutablePair<MctsNode, HeimlichAndCoAction> getBestChild() {
        Set<HeimlichAndCoAction> possibleActions = children.keySet();
        if (possibleActions.isEmpty()) {
            throw new IllegalStateException("Could not find best child, because there are no children.");
        }
        List<HeimlichAndCoAction> maximumValuedActions = getMaximumValuedActions(possibleActions, this.actionComparatorQsa);
        HeimlichAndCoAction selectedAction = maximumValuedActions.get(random.nextInt(maximumValuedActions.size()));
        return new ImmutablePair<>(this.children.get(selectedAction), selectedAction);
    }

    public HeimlichAndCo getGame() {
        return new HeimlichAndCo(game);
    }

    public int getPlayouts() {
        return this.playouts;
    }

    public int getWins() {
        return this.wins;
    }

    private double calculateGroupUCT(ArrayList<HeimlichAndCoAction> group) {
        // Are there already some explored children? Are they at least 3? Take those
        // Otherwise choose remaining filling up to 3 randomly

        if(group.isEmpty()) {
             return 0;
        }

        double groupUCT = 0;

        if (this.children.isEmpty()) {
            return Double.MAX_VALUE;
        }

        int sampleCnt = 0;
        for (HeimlichAndCoAction action : group) {
            if (this.children.containsKey(action)) {
                MctsNode child = this.children.get(action);
                if (child.playouts == 0 || this.playouts == 0) { //this should never happen
                    throw new IllegalStateException("Illegal 0 value in calculateUCT");
                }
                double qSA;
                if (this.game.getCurrentPlayer() == MctsNode.playerId) {
                    qSA = ((double) child.wins / child.playouts);
                } else {
                    //if the current player is not the player we are maximizing for, we have to 'invert' the wins, as the
                    //other players of course do not want 'our' player to win. Meaning, they of course don't take the action
                    //which benefits 'our' player
                    qSA = ((double) (child.playouts - child.wins) / child.playouts);
                }

                double nS = this.playouts;
                double nSA = child.playouts;
                groupUCT += qSA + C * Math.sqrt(Math.log(nS) / nSA);
                sampleCnt++;
            }
        }

        // If there are less than 33% of actions explored, this group should be explored more
        if (((double) sampleCnt /group.size()) < 0.33) { // TODO adjust this limit
            return Double.MAX_VALUE;
        }

        return groupUCT / sampleCnt;
    }

    /**
     * Calculates the UCT score of an action.
     * In the case that no playout has been done yet for an action, the maximum Double value is returned. This is in line with exploring
     * each state/action at least once before exploring a state/action twice.
     * <p>
     * Note: The action has to be a valid action in the current game state.
     *
     * @param action for which UCT score should be calculated
     * @return UCT score of action
     */
    private double calculateUCT(HeimlichAndCoAction action) {
        if (action == null || !this.game.isValidAction(action)) {
            throw new IllegalArgumentException("Action must not be null, and allowed in the current state");
        }
        if (this.children.isEmpty()) {
            return Double.MAX_VALUE;
        }

        if (this.children.containsKey(action)) {
            MctsNode child = this.children.get(action);
            if (child.playouts == 0 || this.playouts == 0) { //this should never happen
                throw new IllegalStateException("Illegal 0 value in calculateUCT");
            }
            double qSA;
            if (this.game.getCurrentPlayer() == MctsNode.playerId) {
                qSA = ((double) child.wins / child.playouts);
            } else {
                //if the current player is not the player we are maximizing for, we have to 'invert' the wins, as the
                //other players of course do not want 'our' player to win. Meaning, they of course don't take the action
                //which benefits 'our' player
                qSA = ((double) (child.playouts - child.wins) / child.playouts);
            }

            double nS = this.playouts;
            double nSA = child.playouts;
            double uctValue = qSA + C * Math.sqrt(Math.log(nS) / nSA);

            // Add domain knowledge
            // TODO discuss if this makes sense for inverted wins as well (enemy)?
            if (game.getCurrentPhase() == HeimlichAndCoPhase.AGENT_MOVE_PHASE) {
                uctValue = addFieldValueKnowledge(uctValue, action);
            }

            if (game.getCurrentPhase() == HeimlichAndCoPhase.SAFE_MOVE_PHASE) {
                uctValue = addSafeMoveFieldValueKnowledge(uctValue, action);
            }

            return uctValue;
        }
        return Double.MAX_VALUE;
    }

    private double addSafeMoveFieldValueKnowledge(double uctValue, HeimlichAndCoAction action) {
        final HeimlichAndCoSafeMoveAction safeMoveAction = (HeimlichAndCoSafeMoveAction) action;
        final int newSafePos = ActionHelper.getSafeLocationFromSafeMoveAction(safeMoveAction);
        if (newSafePos == 11) { // Give incentive to move safe to ruins to penalize the one finding it
            return uctValue * 1.5;
        }
        return uctValue;
    }

    private double addFieldValueKnowledge(double uctValue, HeimlichAndCoAction action) {
        HeimlichAndCoAgentMoveAction moveAction = (HeimlichAndCoAgentMoveAction) action;
        EnumMap<Agent, Integer> movesMap = ActionHelper.getAgentMovesFromMoveAction(moveAction);
        Agent curAgent = Agent.values()[game.getCurrentPlayer()];
        int curPos = game.getBoard().getAgentsPositions().get(curAgent);
        Map<Agent, Integer> agentsPositions = this.game.getBoard().getAgentsPositions();

        int moveAmount = 0;
        if (movesMap.containsKey(curAgent)) {
            moveAmount = movesMap.get(curAgent);
        }
        int newPos = (moveAmount + curPos) % 12;

        // Give incentive to move to safe when own agents benefits more than enemy agent
        if(newPos == this.game.getBoard().getSafePosition()) {
            Map<Integer, Agent> playersToAgents = this.game.getPlayersToAgentsMap();

            // Sum up current possible score of enemy players if the safe were found
            int enemyScoreSumGain = 0;
            int ownScoreGain = this.game.getBoard().getPointsForField(agentsPositions.get(curAgent));
            for (Integer player : playersToAgents.keySet()) {
                if(player == game.getCurrentPlayer()) {
                    continue;
                }
                Agent enemeyAgent = playersToAgents.get(player);
                if (agentsPositions.containsKey(enemeyAgent)) {
                    enemyScoreSumGain += this.game.getBoard().getPointsForField(agentsPositions.get(enemeyAgent));
                }
            }

            if (ownScoreGain > enemyScoreSumGain / playersToAgents.size()) {
                uctValue *= 2;
            }
        }

        // Move groups
        ArrayList<Integer> lowPos = new ArrayList<>(List.of(11, 0, 1, 2, 3)); // 11 is the ruins field (-3)
        ArrayList<Integer> midPos = new ArrayList<>(List.of(4, 5, 6, 7));
        ArrayList<Integer> highPos = new ArrayList<>(List.of(8, 9, 10));

        // Boost action where own agent is moved to higher value field or more secure field
        if (movesMap.containsKey(curAgent)) {
            if (lowPos.contains(newPos)) {
                return (uctValue * 0.6);
            }
            if (midPos.contains(newPos)) {
                return (uctValue * 1.7);
            }
            if (highPos.contains(newPos)) {
                return (uctValue * 1.3);
            }
        }
        return uctValue;
    }

    /**
     * Gets the actions which have the maximum value according to some comparator. If multiple actions have the same
     * (maximum) value, all of them will be contained in the returned list.
     * This method runs in O(n) where n is the number of actions
     *
     * @param actions    possible actions in the current game state
     * @param comparator comparator which should be used to compare two actions (e.g. tree policy like UCT)
     * @return a List of actions which have the maximum value when compared with the given Comparator
     */
    private static List<HeimlichAndCoAction> getMaximumValuedActions(Set<HeimlichAndCoAction> actions, Comparator<HeimlichAndCoAction> comparator) {
        List<HeimlichAndCoAction> selectedActions = new LinkedList<>();
        for (HeimlichAndCoAction action : actions) {
            if (selectedActions.isEmpty()) { //this is only true in the first iteration
                selectedActions.add(action);
            } else if (comparator.compare(selectedActions.get(0), action) == 0) { //the current action is equal
                selectedActions.add(action);
            } else if (comparator.compare(selectedActions.get(0), action) < 0) { //the current action has a larger value
                //i.e. clear the list and add the current action to it
                selectedActions = new LinkedList<>();
                selectedActions.add(action);
            }
        }
        return selectedActions;
    }

}
