package geheimlichagent;

import heimlich_and_co.HeimlichAndCo;
import heimlich_and_co.actions.HeimlichAndCoAction;
import heimlich_and_co.actions.HeimlichAndCoAgentMoveAction;
import heimlich_and_co.enums.Agent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class GroupNode {
    /**
     * the current game (state)
     */
    private final HeimlichAndCo game;

    private final Random random;

    private final Map<HeimlichAndCoAction, MctsNode> children;

    /**
     * saves how many wins were achieved from this node
     */
    private int wins;
    /**
     * saves how many playouts were done from this node (or descendents of this node)
     */
    private int playouts;

    public GroupNode(HeimlichAndCo game, int startRange, int endRange) {
        this.game = game;
        this.random = new Random();

        // Get current player position
        final int curPos = game.getBoard().getAgentsPositions().get(Agent.values()[game.getCurrentPlayer()]);
        // Get die roll
        final int dieRoll = this.game.getBoard().getLastDieRoll();
        boolean ruinsIsIncluded = false;

        if(startRange == 11) {
            ruinsIsIncluded = true;
            startRange++;
        }

        for (int i = 0; i <= dieRoll; i++) {
            int newPossiblePos = (curPos + i) % 12;
            if((newPossiblePos >= startRange && newPossiblePos <= endRange) || (ruinsIsIncluded && newPossiblePos == 11))
            if (newPossiblePos == 11 || newPossiblePos <= 4) {
                this.children.put();
            }
        }
    }


    public void addMoveAction(int ownAgentMoveAmount, int dieRoll) {
        ArrayList<Agent> playingAgents = new ArrayList<>();
        playingAgents.add(Agent.values()[game.getCurrentPlayer()]);
        // Add random to take rest of the move points TODO improve later
        int randomOtherAgent;
        do {
            randomOtherAgent = random.nextInt(this.game.getBoard().getAgents().length);
        } while (randomOtherAgent != game.getCurrentPlayer());
        playingAgents.add(Agent.values()[randomOtherAgent]);

        // Add move of own agent to map
        // Add random moves of other agents to map to completely spend the move points from the die roll
        // TODO Optimize, figure out whats the amount of spots the other agents need to be moved to receive the minimum amount of points
        // TODO or just add all combinations with the own agent number set
        Map<Agent, Integer> agentsMoves = agentsMoveHelper(playingAgents, ownAgentMoveAmount, (dieRoll - ownAgentMoveAmount));
        HeimlichAndCoAgentMoveAction newMoveAction = new HeimlichAndCoAgentMoveAction(agentsMoves);
        moveActions.add(newMoveAction);
    }

    /**
     * creates a Map with Pairs of entries denoting moves for agents
     * <p>
     * This method was taken from the HeimlichAndCoAgentMoveAction class
     *
     * @param playingAgents Array of agents that are playing
     * @param agentsMoves   integers denoting the amount the corresponding agent should be moved forward
     * @return Map denoting moves for agents
     */
    private static Map<Agent, Integer> agentsMoveHelper(ArrayList<Agent> playingAgents, int... agentsMoves) {
        if (playingAgents.size() != agentsMoves.length) {
            throw new IllegalArgumentException("There must be the same amount of agents and numbers given.");
        } else {
            EnumMap<Agent, Integer> agentsMovesMap = new EnumMap<>(Agent.class);

            for (int i = 0; i < agentsMoves.length; ++i) {
                if (agentsMoves[i] > 0) {
                    agentsMovesMap.put(playingAgents.get(i), agentsMoves[i]);
                }
            }

            return agentsMovesMap;
        }
    }
}
