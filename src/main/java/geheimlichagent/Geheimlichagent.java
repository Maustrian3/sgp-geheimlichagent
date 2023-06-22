package geheimlichagent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.util.pair.ImmutablePair;
import at.ac.tuwien.ifs.sge.util.pair.Pair;
import heimlich_and_co.HeimlichAndCo;
import heimlich_and_co.actions.HeimlichAndCoAction;
import heimlich_and_co.enums.Agent;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Geheimlichagent extends AbstractGameAgent<HeimlichAndCo, HeimlichAndCoAction> implements GameAgent<HeimlichAndCo, HeimlichAndCoAction> {

    /**
     * determines the depth of termination for random playouts
     * can be set to -1 to always play out till the game ends
     */
    private static final int TERMINATION_DEPTH = 64;

    /**
     * Determines the strategy for dealing with the randomness of a die roll.
     * <p>
     * True means that all possible outcomes of the die roll will be simulated. Meaning that for each die roll, 6 child
     * states will be added (one for each outcome). The selection can then be done by choosing a random outcome each
     * time the algorithm comes to a "die roll node".
     * <p>
     * False means that when first visiting the "die roll node", a random outcome will be chosen, and that is the only
     * outcome that is considered in that tree.
     */
    private static final boolean SIMULATE_ALL_DIE_OUTCOMES = true;

    /**
     * Boltzmann constant
     * sqrt of 2 in the first test
     */
    private static final double k = Math.sqrt(2);
    ;

    public Geheimlichagent(Logger logger) {
        super(logger);
    }

    /**
     * Average reward statistic for each action+player pair -> reward+visit count pair, use in simulation.
     *
     */
    private final Map<ImmutablePair<HeimlichAndCoAction, Integer>, ImmutablePair<Double, Integer>> averageRewardStats = new HashMap<>();
    // Reduced complexity of the statistic with only player X moves his own agent to field Y
    //private final Map<ImmutablePair<Integer, Integer>, ImmutablePair<Double, Integer>> averageRewardStats = new HashMap<>();

    private final Comparator<ImmutablePair<HeimlichAndCoAction, Integer>> actionComparatorBoltz = Comparator.comparingDouble(this::calcBoltzmann);

    @Override
    public HeimlichAndCoAction computeNextAction(HeimlichAndCo game, long l, TimeUnit timeUnit) {
        log.deb("MctsAgent: Computing next action\n");
        super.setTimers(l, timeUnit);

        Set<HeimlichAndCoAction> possibleActions = game.getPossibleActions();
        if (possibleActions.size() == 1) {
            return game.getPossibleActions().iterator().next();
        }

        try {
            log.deb("MctsAgent: Adding information to the game");
            addInformationToGame(game);

            if (SIMULATE_ALL_DIE_OUTCOMES) {
                game.setAllowCustomDieRolls(true);
            }
            MctsNode.setPlayerId(this.playerId);
            MctsNode tree = new MctsNode(0, 0, game, null);
            log.deb("MctsAgent: Doing MCTS");
            while (!this.shouldStopComputation()) {
                try {
                    Pair<MctsNode, HeimlichAndCoAction> selectionPair = mctsSelection(tree, SIMULATE_ALL_DIE_OUTCOMES);
                    MctsNode newNode = mctsExpansion(selectionPair.getA(), selectionPair.getB());
                    int win = mctsSimulation(newNode);
                    mctsBackpropagation(newNode, win);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    log.err(exception);
                }
            }
            log.inf("MctsAgent: Playouts done from root node: " + tree.getPlayouts() + "\n");
            log.inf("MctsAgent: Wins/playouts from selected child node: " + tree.getBestChild().getA().getWins() + "/" + tree.getBestChild().getA().getPlayouts() + "\n");
            log.inf("MctsAgent: Q(s,a) of chosen action: " + tree.calculateQsaOfChild(tree.getBestChild().getB()) + "\n");
            return tree.getBestChild().getB();

        } catch (Exception ex) {
            log.err(ex);
            log.err("MctsAgent: An error occurred while calculating the best action. Playing a random action.\n");
        }
        //If an exception is encountered, we play a random action s.t. we do not automatically lose the game
        HeimlichAndCoAction[] actions = game.getPossibleActions().toArray(new HeimlichAndCoAction[0]);
        return actions[super.random.nextInt(actions.length)];
    }

    /**
     * Adds information that was removed by the game (i.e. hidden information).
     * Therefore, adds entries to the map which maps agents to players and entries to the map mapping the cards of players.
     * The agents are randomly assigned to players. And players are assumed to have no cards.
     *
     * @param game to add information to
     */
    private void addInformationToGame(HeimlichAndCo game) {
        //we need to determinize the tree, i.e. add information that is secret that the game hid from us
        //here we just guess
        Map<Integer, Agent> playersToAgentsMap = game.getPlayersToAgentsMap();
        List<Agent> unassignedAgents = Arrays.asList(game.getBoard().getAgents());
        unassignedAgents = new LinkedList<>(unassignedAgents);
        unassignedAgents.remove(playersToAgentsMap.get(this.playerId));
        for (int i = 0; i < game.getNumberOfPlayers(); i++) {
            if (i == this.playerId) {
                continue;
            }
            Agent chosenAgent = unassignedAgents.get(super.random.nextInt(unassignedAgents.size()));
            playersToAgentsMap.put(i, chosenAgent); //choose a random agent
            unassignedAgents.remove(chosenAgent);
            if (game.isWithCards()) {
                game.getCards().put(i, new LinkedList<>()); //other players do not get cards for now
            }
        }
    }

    private void mctsBackpropagation(MctsNode node, int win) {
        log.deb("MctsAgent: In Backpropagation\n");
        node.backpropagation(win, averageRewardStats);
    }

    private MctsNode mctsExpansion(MctsNode node, HeimlichAndCoAction action) {
        log.deb("MctsAgent: In Expansion\n");
        return node.expansion(action);
    }

    private Pair<MctsNode, HeimlichAndCoAction> mctsSelection(MctsNode node, boolean simulateAllDieOutcomes) {
        log.deb("MctsAgent: In Selection\n");
        return node.selection(simulateAllDieOutcomes);
    }

    /**
     * Does the simulation step of MCTS. This function is implemented here and not in the MctsNode as that makes it
     * easier to handle how much time there is (left) for computation before timing out.
     *
     * @param node from where simulation should take place
     * @return 2, 1 or 0, depending on whether the agent belonging to the player of this agent wins/draws/loses
     */
    private int mctsSimulation(MctsNode node) {
        log.deb("MctsAgent: In Simulation\n");
        HeimlichAndCo game = new HeimlichAndCo(node.getGame());
        //use a termination depth were the game is evaluated and stopped
        int simulationDepth = 0;
        while (!game.isGameOver() && !this.shouldStopComputation()) {
            if (TERMINATION_DEPTH >= 0 && simulationDepth >= TERMINATION_DEPTH) {
                break;
            }
            Set<HeimlichAndCoAction> possibleActions = game.getPossibleActions();
            List<HeimlichAndCoAction> maximumValuedActions = getMaximumValuedActions(possibleActions, game.getCurrentPlayer(), this.actionComparatorBoltz);
            HeimlichAndCoAction selectedAction = maximumValuedActions.get(random.nextInt(maximumValuedActions.size()));
//            HeimlichAndCoAction selectedAction = possibleActions.toArray(new HeimlichAndCoAction[1])[super.random.nextInt(possibleActions.size())];
            game.applyAction(selectedAction);
            simulationDepth++;
        }

        Map<Agent, Integer> scores = game.getBoard().getScores();
        int maxValue = 0;
        for (int i : scores.values()) {
            if (i > maxValue) {
                maxValue = i;
            }
        }
        // Check if scores contains the maxValue more than once
        if (Collections.frequency(scores.values(), maxValue) > 1) {
            return 1; // Draw
        }
        //the game is regarded as won if the player has the highest score
        if (maxValue == scores.get(game.getPlayersToAgentsMap().get(this.playerId))) {
            return 2; // Won
        }
        return 0; // Lost
    }

    private double calcBoltzmann(ImmutablePair<HeimlichAndCoAction, Integer> actionPlayerPair) {
        ImmutablePair<Double, Integer> rewardStats = averageRewardStats.get(actionPlayerPair);
        if (rewardStats == null) {
            return 0;
        }
        return Math.exp((rewardStats.getA()) / (k * rewardStats.getB()));
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
    private static List<HeimlichAndCoAction> getMaximumValuedActions(Set<HeimlichAndCoAction> actions, Integer player, Comparator<ImmutablePair<HeimlichAndCoAction, Integer>> comparator) {
        List<HeimlichAndCoAction> selectedActions = new LinkedList<>();
        for (HeimlichAndCoAction action : actions) {
            if (selectedActions.isEmpty()) { //this is only true in the first iteration
                selectedActions.add(action);
            } else if (comparator.compare(new ImmutablePair<>(selectedActions.get(0), player), new ImmutablePair<>(action, player)) == 0) { //the current action is equal
                selectedActions.add(action);
            } else if (comparator.compare(new ImmutablePair<>(selectedActions.get(0), player), new ImmutablePair<>(action, player)) < 0) { //the current action has a larger value
                //i.e. clear the list and add the current action to it
                selectedActions = new LinkedList<>();
                selectedActions.add(action);
            }
        }
        return selectedActions;
    }
}
