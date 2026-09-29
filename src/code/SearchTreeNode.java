package code;

public class SearchTreeNode {
    private State state;
    private SearchTreeNode parent;
    private String action; // Action taken to reach this node (e.g., "left", "climbup")
    private int pathCost;  // Total cost from root to this node (e.g., energy used, or generic cost)
    private int depth;     // Depth of the node in the search tree

    public SearchTreeNode(State state, SearchTreeNode parent, String action, int pathCost, int depth) {
        this.state = state;
        this.parent = parent;
        this.action = action;
        this.pathCost = pathCost;
        this.depth = depth;
    }

    public State getState() { return state; }
    public SearchTreeNode getParent() { return parent; }
    public String getAction() { return action; }
    public int getPathCost() { return pathCost; }
    public int getDepth() { return depth; }
}
