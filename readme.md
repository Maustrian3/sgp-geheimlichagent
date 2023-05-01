# sgp-geheimlichagent

## Implemented

#### EXP3

Implemented EXP3 according to [this](https://jamesrledoux.com/algorithms/bandit-algorithms-epsilon-ucb-exp-python/)
article and the lecture slides.
We weren't able to see a difference between EXP3 and the default algorithm. We need to test it more, and tune the
parameters to find the best ones.

#### maxMCTS

Was implemented according to the
paper [On the Analysis of Complex Backup Strategies in Monte Carlo Tree Search](http://proceedings.mlr.press/v48/khandelwal16.pdf)
but only one test
was done, and it won 50% of the games against default MCTS.
We have to find the best parameters for this algorithm, and also improve/change it because the Qsa value is updated
according to some delta value, but we currently calculate it with wins/playouts.

#### Move Reduction

One approach we had was to reduce the branching factor in the move phase by allowing fewer agents to be moved in the
game. For example only allow the own agent and the currently worst performing agent to be moved.
The tests showed that this optimization in its current state is performing worse than the default MCTS agent. It could
be more useful when combined with mapping agents to agents in the game and using more information about the game to
choose which agents not to pick.

## Planned

* Neural network instead of the current simulation where random actions are played and so the outcome is random. (Game
  state as input and based on this make an assumption if we win, lose or draw)
* Improve the "addInformationToGame" method to use a smart approach to add information (for example count all points a
  player has collected for each agent and make guesses based on this information)
* Translate the different types of cards into additional movement points as seen in a die roll to generate the
  additional possible moves the agent can make.
* Use the information of where the safe is currently located in the selection phase
* Consider the value of the fields the agents are currently on in the selection phase and if it's viable to move them to
  one with a lower score.
* Try to improve already implemented algorithms
* Try to implement at least one improvement for each MCTS step