package geheimlichagent;

import heimlich_and_co.actions.HeimlichAndCoAgentMoveAction;
import heimlich_and_co.enums.Agent;

import java.util.EnumMap;

public class ActionHelper {

    public static EnumMap<Agent, Integer> getAgentMovesFromMoveAction(HeimlichAndCoAgentMoveAction action) {
        EnumMap<Agent, Integer> agentMoves = new EnumMap<>(Agent.class);

        String[] parts = action.toString().split("[:;]");

        // Überspringen des ersten Teils "AgentMoveAction"
        for (int i = 1; i < parts.length - 1; i += 2) {
            String nameStr = parts[i].trim();
            String valueStr = parts[i + 1].trim();

            Agent name = Agent.valueOf(nameStr);
            int value = Integer.parseInt(valueStr);

            agentMoves.put(name, value);
        }
        return agentMoves;
    }
}
