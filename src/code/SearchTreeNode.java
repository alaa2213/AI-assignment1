// package code;

// import java.util.LinkedList;

// /**
//  * A node in the search tree. Wraps a {@link State} together with the
//  * bookkeeping a search algorithm needs (parent, action, costs, depth).
//  */
// public class SearchTreeNode {
//     private final State state;
//     private final SearchTreeNode parent;
//     private final String action;   // Action taken from the parent to reach this node (null for the root)
//     private final int pathCost;    // Total ENERGY spent from the root to this node
//     private final int livesLost;   // Total LIVES lost from the root to this node
//     private final int depth;       // Depth of the node in the search tree (root = 0)

//     public SearchTreeNode(State state, SearchTreeNode parent, String action,
//                           int pathCost, int livesLost, int depth) {
//         this.state = state;
//         this.parent = parent;
//         this.action = action;
//         this.pathCost = pathCost;
//         this.livesLost = livesLost;
//         this.depth = depth;
//     }

//     public State getState() { return state; }
//     public SearchTreeNode getParent() { return parent; }
//     public String getAction() { return action; }
//     public int getPathCost() { return pathCost; }
//     public int getLivesLost() { return livesLost; }
//     public int getDepth() { return depth; }

//     /**
//      * Rebuilds the plan by following parent pointers back to the root.
//      * @return the actions from the root to this node, joined by commas (e.g. "right,collect,unlock").
//      */
//     public String getPlan() {
//         LinkedList<String> actions = new LinkedList<>();
//         SearchTreeNode current = this;
//         while (current.parent != null) {
//             actions.addFirst(current.action);
//             current = current.parent;
//         }
//         return String.join(",", actions);
//     }
// }

package code;

import java.util.LinkedList;

/**
 * A node in the search tree as defined in Lecture 2 (Slide 6):
 * A search tree node is a 5-tuple:
 *   1. The state this node corresponds to.
 *   2. The parent node.
 *   3. The operator applied from the parent state to reach this current state.
 *   4. The depth of the node in the tree.
 *   5. The path cost from the root to this state.
 */
public class SearchTreeNode {
    private final State state;
    private final SearchTreeNode parent;
    private final String action;   // Operator applied from the parent to reach this node (null for the root)
    private final int pathCost;    // Total ENERGY spent from the root to this node
    private final int livesLost;   // Total LIVES lost from the root to this node
    private final int depth;       // Depth of the node in the search tree (root = 0)

    public SearchTreeNode(State state, SearchTreeNode parent, String action,
                          int pathCost, int livesLost, int depth) {
        this.state = state;
        this.parent = parent;
        this.action = action;
        this.pathCost = pathCost;
        this.livesLost = livesLost;
        this.depth = depth;
    }

    public State getState() { return state; }
    public SearchTreeNode getParent() { return parent; }
    public String getAction() { return action; }
    public String getOperator() { return action; }
    public int getPathCost() { return pathCost; }
    public int getLivesLost() { return livesLost; }
    public int getDepth() { return depth; }

    /**
     * Rebuilds the plan by following parent pointers back to the root.
     * @return the actions from the root to this node, joined by commas (e.g. "right,collect,unlock").
     */
    public String getPlan() {
        LinkedList<String> actions = new LinkedList<>();
        SearchTreeNode current = this;
        while (current.parent != null) {
            actions.addFirst(current.action);
            current = current.parent;
        }
        return String.join(",", actions);
    }
}