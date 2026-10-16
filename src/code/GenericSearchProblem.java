// package code;

// import java.util.List;

// public abstract class GenericSearchProblem {
//     // Defines the initial state of the problem
//     public abstract State getInitialState();
    
//     // Checks if the given state satisfies the goal condition
//     public abstract boolean isGoal(State state);
    
//     // Generates a list of valid successor nodes from the given node
//     public abstract List<SearchTreeNode> getSuccessors(SearchTreeNode node);
// }

package code;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Abstract Data Type for a Generic Search Problem (Lecture 2, Slide 2)
 * and the General Search Procedure (Lecture 2, Slide 7).
 */
public abstract class GenericSearchProblem {

    /**
     * Large constant used to combine lives and energy into one scalar cost.
     * Must be strictly greater than the maximum possible energy expenditure
     * (initial energy <= 500) so that saving one life is always preferred over
     * any amount of energy savings.
     */
    protected static final int LIVES_PENALTY = 100_00;

    // 1. Initial State representing the initial world configuration (INIT-STATE)
    public abstract State getInitialState();

    // 2. Goal test Function (GOAL-TEST)
    public abstract boolean isGoal(State state);

    // 3. State Space & Operators expansion (EXPAND(node, OPERATORS(problem)))
    public abstract List<SearchTreeNode> getSuccessors(SearchTreeNode node);

    // 4. Heuristic function for informed search strategies (default 0)
    public int getHeuristic(State state) {
        return 0;
    }

    /**
     * Returns the combined scalar path cost for a node:
     *   g(n) = livesLost * LIVES_PENALTY + pathCost
     */
    protected static int combinedCost(SearchTreeNode n) {
        return n.getLivesLost() * LIVES_PENALTY + n.getPathCost();
    }

    // -------------------------------------------------------------------------
    // Generic Search Procedure
    // Matches the Lecture 2 pseudocode (Slide 7):
    //   function GENERAL-SEARCH(problem, QING-FUN)
    //     nodes <- MAKE-Q(MAKE-NODE(INIT-STATE(problem)))
    //     while nodes is not empty do
    //       node <- REMOVE-FIRST(nodes)
    //       if GOAL-TEST(problem, STATE(node)) then
    //         return node
    //       end if
    //       children <- EXPAND(node, OPERATORS(problem))
    //       QING-FUN(nodes, children)
    //     end while
    //     return failure
    // -------------------------------------------------------------------------

    /**
     * Encapsulates the queue `nodes` and the queuing function `QING-FUN(nodes, children)`
     * from Lecture 2, Slide 7.
     */
    protected interface QueuingFunction {
        /** MAKE-Q(MAKE-NODE(INIT-STATE(problem))) */
        void makeQueue(SearchTreeNode initialNode);

        /** True when `nodes` is empty */
        boolean isEmpty();

        /** REMOVE-FIRST(nodes) */
        SearchTreeNode removeFirst();

        /** QING-FUN(nodes, children) */
        void enqueue(List<SearchTreeNode> children);

        /** Repeated-state check upon removing a node from the frontier */
        boolean isRepeatedOrDominated(SearchTreeNode node);

        /** Allows Depth-Limited Search to enforce its depth cutoff */
        default boolean canExpand(SearchTreeNode node) {
            return true;
        }
    }

    /**
     * Implementation of GENERAL-SEARCH(problem, QING-FUN) from Lecture 2, Slide 7.
     */
    public static String generalSearch(GenericSearchProblem problem, QueuingFunction qingFun) {
        // nodes <- MAKE-Q(MAKE-NODE(INIT-STATE(problem)))
        SearchTreeNode root = new SearchTreeNode(problem.getInitialState(), null, null, 0, 0, 0);
        qingFun.makeQueue(root);

        int nodesExpanded = 0;

        // while nodes is not empty do
        while (!qingFun.isEmpty()) {
            // node <- REMOVE-FIRST(nodes)
            SearchTreeNode node = qingFun.removeFirst();

            // Eliminate redundant / repeated states (Lecture 2, Slide 20)
            if (qingFun.isRepeatedOrDominated(node)) {
                continue;
            }

            // if GOAL-TEST(problem, STATE(node)) then return node
            if (problem.isGoal(node.getState())) {
                return formatSolution(node, nodesExpanded);
            }

            if (!qingFun.canExpand(node)) {
                continue;
            }

            // children <- EXPAND(node, OPERATORS(problem))
            nodesExpanded++;
            List<SearchTreeNode> children = problem.getSuccessors(node);

            // QING-FUN(nodes, children)
            qingFun.enqueue(children);
        }

        // return failure
        return "No Solution";
    }

    /**
     * Generic search procedure taking a problem and a search strategy as inputs,
     * as required by Section 4a of the Assignment Description.
     */
    public static String genericSearch(GenericSearchProblem problem, String strategy) {
        switch (strategy) {
            case "UC":
                // function UC-SEARCH(problem): return GENERAL-SEARCH(problem, ORDERED-INSERT)
                return generalSearch(problem, new UniformCostQueuingFunction());
            case "ID":
                // function ID-SEARCH(problem): for depth <- 0 to infinity do DL-SEARCH(problem, depth)
                return iterativeDeepeningSearch(problem);
            case "AS":
                // A* Search: return GENERAL-SEARCH(problem, A*-ORDERED-INSERT)
                return generalSearch(problem, new AStarQueuingFunction(problem));
            default:
                return "No Solution";
        }
    }

    // -------------------------------------------------------------------------
    // Repeated-State Dominance Pruning (Lecture 2, Slide 20)
    // -------------------------------------------------------------------------

    private static final class ResourceProfile {
        final int energy;
        final int rope;
        final int lives;
        final int remainingDepth;

        ResourceProfile(int energy, int rope, int lives, int remainingDepth) {
            this.energy = energy;
            this.rope = rope;
            this.lives = lives;
            this.remainingDepth = remainingDepth;
        }

        boolean dominates(int otherEnergy, int otherRope, int otherLives, int otherRemainingDepth) {
            return this.energy >= otherEnergy
                    && this.rope >= otherRope
                    && this.lives >= otherLives
                    && this.remainingDepth >= otherRemainingDepth;
        }
    }

    private static boolean checkDominatedOrRecord(
            Map<State.ConfigurationKey, List<ResourceProfile>> visited,
            State state,
            int remainingDepth) {
        State.ConfigurationKey key = state.getConfigurationKey();
        List<ResourceProfile> profiles = visited.computeIfAbsent(key, k -> new ArrayList<>(4));

        int e = state.getEnergy();
        int r = state.getRopeLength();
        int l = state.getLives();

        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).dominates(e, r, l, remainingDepth)) {
                return true;
            }
        }

        profiles.removeIf(p -> e >= p.energy && r >= p.rope && l >= p.lives && remainingDepth >= p.remainingDepth);
        profiles.add(new ResourceProfile(e, r, l, remainingDepth));
        return false;
    }

    // -------------------------------------------------------------------------
    // Uniform Cost Search (Lecture 2, Slide 15: ORDERED-INSERT)
    // -------------------------------------------------------------------------

    private static final class UniformCostQueuingFunction implements QueuingFunction {
        private long insertionCounter = 0;
        private final PriorityQueue<Entry> queue = new PriorityQueue<>(
                Comparator.comparingInt((Entry e) -> e.node.getLivesLost())
                        .thenComparingInt(e -> e.node.getPathCost())
                        .thenComparingLong(e -> e.order)
        );
        private final Map<State, Integer> bestCost = new HashMap<>();
        private final Map<State.ConfigurationKey, List<ResourceProfile>> expanded = new HashMap<>();

        private static final class Entry {
            final SearchTreeNode node;
            final long order;

            Entry(SearchTreeNode node, long order) {
                this.node = node;
                this.order = order;
            }
        }

        @Override
        public void makeQueue(SearchTreeNode initialNode) {
            bestCost.put(initialNode.getState(), 0);
            queue.add(new Entry(initialNode, insertionCounter++));
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }

        @Override
        public SearchTreeNode removeFirst() {
            return queue.poll().node;
        }

        @Override
        public boolean isRepeatedOrDominated(SearchTreeNode node) {
            int cost = combinedCost(node);
            if (cost > bestCost.getOrDefault(node.getState(), Integer.MAX_VALUE)) {
                return true;
            }
            return checkDominatedOrRecord(expanded, node.getState(), 0);
        }

        @Override
        public void enqueue(List<SearchTreeNode> children) {
            for (SearchTreeNode child : children) {
                int cost = combinedCost(child);
                if (cost < bestCost.getOrDefault(child.getState(), Integer.MAX_VALUE)) {
                    bestCost.put(child.getState(), cost);
                    queue.add(new Entry(child, insertionCounter++));
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Iterative Deepening Search (Lecture 2, Slides 17 & 18)
    // -------------------------------------------------------------------------

    private static String iterativeDeepeningSearch(GenericSearchProblem problem) {
        for (int depth = 0; ; depth++) {
            DepthLimitedQueuingFunction dlQingFun = new DepthLimitedQueuingFunction(depth);
            String result = generalSearch(problem, dlQingFun);
            if (!"No Solution".equals(result)) {
                return result;
            }
            if (!dlQingFun.reachedCutoff()) {
                return "No Solution"; // Entire reachable state space exhausted
            }
        }
    }

    private static final class DepthLimitedQueuingFunction implements QueuingFunction {
        private final int depthLimit;
        private final List<SearchTreeNode> stack = new ArrayList<>();
        private final Map<State.ConfigurationKey, List<ResourceProfile>> visited = new HashMap<>();
        private boolean cutoff = false;

        DepthLimitedQueuingFunction(int depthLimit) {
            this.depthLimit = depthLimit;
        }

        @Override
        public void makeQueue(SearchTreeNode initialNode) {
            stack.add(initialNode);
        }

        @Override
        public boolean isEmpty() {
            return stack.isEmpty();
        }

        @Override
        public SearchTreeNode removeFirst() {
            return stack.remove(stack.size() - 1);
        }

        @Override
        public boolean isRepeatedOrDominated(SearchTreeNode node) {
            int remainingDepth = depthLimit - node.getDepth();
            return checkDominatedOrRecord(visited, node.getState(), remainingDepth);
        }

        @Override
        public boolean canExpand(SearchTreeNode node) {
            if (node.getDepth() >= depthLimit) {
                cutoff = true;
                return false;
            }
            return true;
        }

        @Override
        public void enqueue(List<SearchTreeNode> children) {
            // ENQUEUE-AT-FRONT (push in reverse order so first child is expanded first)
            for (int i = children.size() - 1; i >= 0; i--) {
                SearchTreeNode child = children.get(i);
                if (!isAncestor(child)) {
                    stack.add(child);
                }
            }
        }

        private boolean isAncestor(SearchTreeNode node) {
            State target = node.getState();
            SearchTreeNode ancestor = node.getParent();
            while (ancestor != null) {
                if (ancestor.getState().equals(target)) {
                    return true;
                }
                ancestor = ancestor.getParent();
            }
            return false;
        }

        boolean reachedCutoff() {
            return cutoff;
        }
    }

    // -------------------------------------------------------------------------
    // A* Search (QING-FUN = priority queue ordered by f(n) = g(n) + h(n))
    // -------------------------------------------------------------------------

    private static final class AStarQueuingFunction implements QueuingFunction {
        private final GenericSearchProblem problem;
        private long insertionCounter = 0;
        private final PriorityQueue<Entry> queue = new PriorityQueue<>(
                Comparator.comparingInt((Entry e) -> e.fCost)
                        .thenComparingInt(e -> e.node.getLivesLost())
                        .thenComparingInt(e -> e.node.getPathCost())
                        .thenComparingLong(e -> e.order)
        );
        private final Map<State, Integer> bestCost = new HashMap<>();
        private final Map<State.ConfigurationKey, List<ResourceProfile>> expanded = new HashMap<>();

        private static final class Entry {
            final SearchTreeNode node;
            final int fCost;
            final long order;

            Entry(SearchTreeNode node, int fCost, long order) {
                this.node = node;
                this.fCost = fCost;
                this.order = order;
            }
        }

        AStarQueuingFunction(GenericSearchProblem problem) {
            this.problem = problem;
        }

        @Override
        public void makeQueue(SearchTreeNode initialNode) {
            bestCost.put(initialNode.getState(), 0);
            int h = problem.getHeuristic(initialNode.getState());
            queue.add(new Entry(initialNode, h, insertionCounter++));
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }

        @Override
        public SearchTreeNode removeFirst() {
            return queue.poll().node;
        }

        @Override
        public boolean isRepeatedOrDominated(SearchTreeNode node) {
            int cost = combinedCost(node);
            if (cost > bestCost.getOrDefault(node.getState(), Integer.MAX_VALUE)) {
                return true;
            }
            return checkDominatedOrRecord(expanded, node.getState(), 0);
        }

        @Override
        public void enqueue(List<SearchTreeNode> children) {
            for (SearchTreeNode child : children) {
                int g = combinedCost(child);
                if (g < bestCost.getOrDefault(child.getState(), Integer.MAX_VALUE)) {
                    bestCost.put(child.getState(), g);
                    int h = problem.getHeuristic(child.getState());
                    queue.add(new Entry(child, g + h, insertionCounter++));
                }
            }
        }
    }

    /**
     * Formats the solution string from a goal node.
     * Output format: "action1,action2,...;livesLost;energySpent;nodesExpanded"
     */
    protected static String formatSolution(SearchTreeNode goal, int nodesExpanded) {
        return goal.getPlan() + ";" + goal.getLivesLost() + ";" + goal.getPathCost() + ";" + nodesExpanded;
    }
}