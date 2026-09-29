package code;

import java.util.List;

public abstract class GenericSearchProblem {
    // Defines the initial state of the problem
    public abstract State getInitialState();
    
    // Checks if the given state satisfies the goal condition
    public abstract boolean isGoal(State state);
    
    // Generates a list of valid successor nodes from the given node
    public abstract List<SearchTreeNode> getSuccessors(SearchTreeNode node);
}
