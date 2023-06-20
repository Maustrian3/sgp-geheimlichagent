package geheimlichagent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import at.ac.tuwien.ifs.sge.game.ActionRecord;
import at.ac.tuwien.ifs.sge.util.pair.Pair;
import heimlich_and_co.HeimlichAndCo;
import heimlich_and_co.actions.HeimlichAndCoAction;
import heimlich_and_co.actions.HeimlichAndCoAgentMoveAction;
import heimlich_and_co.actions.HeimlichAndCoSafeMoveAction;
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

    private List<Map<Agent, Integer>> playerPointsList;

    public Geheimlichagent(Logger logger) {
        super(logger);
    }

    @Override
    public HeimlichAndCoAction computeNextAction(HeimlichAndCo game, long l, TimeUnit timeUnit) {
        if (playerPointsList == null) {
            playerPointsList = new ArrayList<>(game.getNumberOfPlayers());
            EnumMap<Agent, Integer> zeroMap = new EnumMap<>(Agent.class);
            for (Agent agent : Agent.values()) {
                zeroMap.put(agent, 0);
            }
            for (int i = 0; i < game.getNumberOfPlayers(); i++) {
                playerPointsList.add(zeroMap.clone());
            }
        }
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
                Pair<MctsNode, HeimlichAndCoAction> selectionPair = mctsSelection(tree, SIMULATE_ALL_DIE_OUTCOMES);
                MctsNode newNode = mctsExpansion(selectionPair.getA(), selectionPair.getB());
                int win = mctsSimulation(newNode);
                mctsBackpropagation(newNode, win);
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

        //Give points for certain actions
        //e.g.
        //move to minus field = -3
        //move to low point field = 3
        //don't move = 0
        //move to high point field = 5
        //+points of field if moved on safe field
        //iterate over all agents and assign the player to the agent for which he collected the most points if
        //if equal special case: e.g.: skip agent and assign other ones first, if conflict remains, assign randomly

        Map<Integer, Agent> playersToAgentsMap = game.getPlayersToAgentsMap();
        Agent ownAgent = playersToAgentsMap.get(this.playerId);
        Map<Agent, MyPair<Integer, Integer>> agentToPlayerPoints = new HashMap<>();

        updatePlayerPointsForActions(game);

        Queue<Integer> playerQ = new LinkedList<>();
        for (int i = 0; i < game.getNumberOfPlayers(); i++) {
            if (i == this.playerId) {
                continue;
            }
            playerQ.add(i);
        }
        while (!playerQ.isEmpty()) {
            int player = playerQ.poll();
            var playerPointsCopy = new EnumMap<>(playerPointsList.get(player));
            for (int i = 0; i < game.getBoard().getAgents().length; i++) {
                Agent chosenAgent = playerPointsCopy.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
                if (chosenAgent == ownAgent) {
                    playerPointsCopy.remove(chosenAgent);
                    continue;
                }
                Integer points = playerPointsCopy.get(chosenAgent);
                if (!agentToPlayerPoints.containsKey(chosenAgent)) {
                    agentToPlayerPoints.put(chosenAgent, new MyPair<>(player, points));
                    break;
                } else {
                    var lastAssignedPlayerPoints = agentToPlayerPoints.get(chosenAgent);
                    if (lastAssignedPlayerPoints.getB() < points) {
                        var lastPair = agentToPlayerPoints.put(chosenAgent, new MyPair<>(player, points));
                        playerQ.add(lastPair.getA());
                        break;
                    } else {
                        playerPointsCopy.remove(chosenAgent);
                    }
                }
            }
            if (game.isWithCards()) {
                game.getCards().put(player, new LinkedList<>()); //other players do not get cards for now
            }
        }
        for (var agent : agentToPlayerPoints.keySet()) {
            playersToAgentsMap.put(agentToPlayerPoints.get(agent).getA(), agent);
        }

        log.inf(agentToPlayerPoints);
        //assign agents to agentToPlayer map
    }

    private void updatePlayerPointsForActions(HeimlichAndCo game) {
        var actions = game.getActionRecords();
        log.inf("----------------------------------------------------------\n");
        log.inf(actions.size() + "\n");
        //Last action is the action we played
        boolean isThisPlayerLastMove = true;
        var agentPositions = new EnumMap<>(game.getBoard().getAgentsPositions());
        for (var action : actions) {
            if (action.getPlayer() != this.playerId) {
                isThisPlayerLastMove = false;
            }
            if (action.getPlayer() == this.playerId && !isThisPlayerLastMove) {
                break;
            }
            updatePlayerPoints(game, agentPositions, action);
        }
    }

    private Map<Agent, Integer> revertMoveAction(Map<Agent, Integer> agentPositions, Map<Agent, Integer> moveAction) {
        for (var agent : moveAction.keySet()) {
            var newAgentPos = (agentPositions.get(agent) - moveAction.get(agent) + 12) % 12;
            agentPositions.put(agent, newAgentPos);
        }
        return agentPositions;
    }

    private void updatePlayerPoints(HeimlichAndCo game, Map<Agent, Integer> agentPositions, ActionRecord<HeimlichAndCoAction> actionRecord) {
        int currentPlayer = actionRecord.getPlayer();
        var currentPlayerPoints = playerPointsList.get(currentPlayer);
        HeimlichAndCoAction action = actionRecord.getAction();
        game.getBoard().getAgents();
        if (action instanceof HeimlichAndCoAgentMoveAction) {
            var moveAction = ActionHelper.getAgentMovesFromMoveAction((HeimlichAndCoAgentMoveAction) action);

            log.inf("Player:" + currentPlayer + "\n");

            if (this.playerId == currentPlayer) {
                revertMoveAction(agentPositions, moveAction);
                return;
            }
            for (var agent : moveAction.keySet()) {
                var currentPoints = currentPlayerPoints.get(agent);
                int bonus;
                if (agentPositions.get(agent) == 11) {
                    bonus = -3;
                } else if (agentPositions.get(agent) > 6) {
                    bonus = 1;
                } else {
                    bonus = 0;
                }
                currentPlayerPoints.put(agent, currentPoints + bonus);
            }
            revertMoveAction(agentPositions, moveAction);
        } else if (action instanceof HeimlichAndCoSafeMoveAction) {
            for (var agent : game.getBoard().getAgents()) {
                var fieldValue = game.getBoard().getPointsForField(agentPositions.get(agent));
                var points = currentPlayerPoints.get(agent);
                currentPlayerPoints.put(agent, points + fieldValue);
            }
        }
    }

    private void mctsBackpropagation(MctsNode node, int win) {
        log.deb("MctsAgent: In Backpropagation\n");
        node.backpropagation(win);
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
     * @return 1 or 0, depending on whether the agent belonging to the player of this agent wins
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
            HeimlichAndCoAction selectedAction = possibleActions.toArray(new HeimlichAndCoAction[1])[super.random.nextInt(possibleActions.size())];
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
        //the game is regarded as won if the player has the highest score
        if (maxValue == scores.get(game.getPlayersToAgentsMap().get(this.playerId))) {
            return 1;
        } else {
            return 0;
        }
    }
}
