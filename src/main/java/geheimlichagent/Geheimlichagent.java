package geheimlichagent;

import at.ac.tuwien.ifs.sge.agent.AbstractGameAgent;
import at.ac.tuwien.ifs.sge.agent.GameAgent;
import at.ac.tuwien.ifs.sge.engine.Logger;
import heimlich_and_co.HeimlichAndCo;
import heimlich_and_co.actions.HeimlichAndCoAction;

import java.util.concurrent.TimeUnit;

public class Geheimlichagent extends AbstractGameAgent<HeimlichAndCo, HeimlichAndCoAction> implements GameAgent<HeimlichAndCo, HeimlichAndCoAction> {

    public Geheimlichagent(Logger logger) {
        super(logger);
    }

    @Override
    public HeimlichAndCoAction computeNextAction(HeimlichAndCo heimlichAndCo, long l, TimeUnit timeUnit) {
        super.log.inf("Selecting random action\n");
        HeimlichAndCoAction[] actions = heimlichAndCo.getPossibleActions().toArray(new HeimlichAndCoAction[0]);
        return actions[super.random.nextInt(actions.length)];
    }
}
