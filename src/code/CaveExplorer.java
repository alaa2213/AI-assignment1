package code;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.ToIntFunction;

public class CaveExplorer extends GenericSearchProblem {

    private State initialState;

    public CaveExplorer(State initialState) {
        this.initialState = initialState;
    }

    @Override
    public State getInitialState() {
        return initialState;
    }

    @Override
    public boolean isGoal(State state) {
        // Goal is reached when 0 locked doors remain and agent has at least 1 life.
        return state.getLockedDoors().isEmpty() && state.getLives() >= 1;
    }

    /**
     * Helper that builds a child node. The child's costs are the parent's costs
     * plus the energy / lives consumed by this single action.
     */
    private SearchTreeNode makeChild(SearchTreeNode parent, State nextState, String action,
            int energySpent, int livesSpent) {
        return new SearchTreeNode(nextState, parent, action,
                parent.getPathCost() + energySpent,
                parent.getLivesLost() + livesSpent,
                parent.getDepth() + 1);
    }

    @Override
    public List<SearchTreeNode> getSuccessors(SearchTreeNode node) {
        List<SearchTreeNode> successors = new ArrayList<>();
        State currentState = node.getState();
        int x = currentState.getAgentX();
        int y = currentState.getAgentY();
        int[][] grid = currentState.getGrid();
        int w = currentState.getWidth();
        int h = currentState.getHeight();

        // 1. left: move horizontally (costs destination difficulty in energy)
        if (x > 0 && grid[y][x - 1] > 0 && currentState.getEnergy() >= grid[y][x - 1]) {
            int cost = grid[y][x - 1];
            State nextState = new State(currentState);
            nextState.setAgentX(x - 1);
            nextState.setEnergy(nextState.getEnergy() - cost);
            successors.add(makeChild(node, nextState, "left", cost, 0));
        }

        // 2. right: move horizontally (costs destination difficulty in energy)
        if (x < w - 1 && grid[y][x + 1] > 0 && currentState.getEnergy() >= grid[y][x + 1]) {
            int cost = grid[y][x + 1];
            State nextState = new State(currentState);
            nextState.setAgentX(x + 1);
            nextState.setEnergy(nextState.getEnergy() - cost);
            successors.add(makeChild(node, nextState, "right", cost, 0));
        }

        // 3. climbup: move vertically (costs energy + 1 rope)
        if (y > 0 && grid[y - 1][x] > 0 && currentState.getEnergy() >= grid[y - 1][x]
                && currentState.getRopeLength() >= 1) {
            int cost = grid[y - 1][x];
            State nextState = new State(currentState);
            nextState.setAgentY(y - 1);
            nextState.setEnergy(nextState.getEnergy() - cost);
            nextState.setRopeLength(nextState.getRopeLength() - 1);
            successors.add(makeChild(node, nextState, "climbup", cost, 0));
        }

        // 4. climbdown: move vertically (costs energy + 1 rope)
        if (y < h - 1 && grid[y + 1][x] > 0 && currentState.getEnergy() >= grid[y + 1][x]
                && currentState.getRopeLength() >= 1) {
            int cost = grid[y + 1][x];
            State nextState = new State(currentState);
            nextState.setAgentY(y + 1);
            nextState.setEnergy(nextState.getEnergy() - cost);
            nextState.setRopeLength(nextState.getRopeLength() - 1);
            successors.add(makeChild(node, nextState, "climbdown", cost, 0));
        }

        // 5. jumpdown: move 1 cell DOWN, costs 1 life + destination difficulty in
        // energy, 0 rope.
        // NOTE: the provided checker charges the destination difficulty for jumpdown
        // too
        // (it rejects plans that report 0 energy for a jump with "Incorrect energy cost
        // calculation").
        if (y < h - 1 && grid[y + 1][x] > 0 && currentState.getLives() > 1
                && currentState.getEnergy() >= grid[y + 1][x]) {
            int cost = grid[y + 1][x];
            State nextState = new State(currentState);
            nextState.setAgentY(y + 1);
            nextState.setEnergy(nextState.getEnergy() - cost);
            nextState.setLives(nextState.getLives() - 1);
            successors.add(makeChild(node, nextState, "jumpdown", cost, 1));
        }

        String currentPos = x + "," + y;

        // 6. collect: picks up key (free)
        if (currentState.getUncollectedKeys().contains(currentPos) && !currentState.isHoldingKey()) {
            State nextState = new State(currentState);
            nextState.getUncollectedKeys().remove(currentPos);
            nextState.setHoldingKey(true);
            successors.add(makeChild(node, nextState, "collect", 0, 0));
        }

        // 7. unlock: unlocks a door with held key (free)
        if (currentState.getLockedDoors().contains(currentPos) && currentState.isHoldingKey()) {
            State nextState = new State(currentState);
            nextState.getLockedDoors().remove(currentPos);
            nextState.setHoldingKey(false);
            successors.add(makeChild(node, nextState, "unlock", 0, 0));
        }

        return successors;
    }

    public static String solve(String initString, String strategy) {
        // Split with limit -1 to preserve trailing empty values in case there are no
        // keys or doors
        String[] parts = initString.split(";", -1);

        // Parse H,W
        String[] hw = parts[0].split(",");
        int h = Integer.parseInt(hw[0].trim());
        int w = Integer.parseInt(hw[1].trim());

        // Parse X,Y
        String[] xy = parts[1].split(",");
        int startX = Integer.parseInt(xy[0].trim());
        int startY = Integer.parseInt(xy[1].trim());

        // Parse EN, RL
        String[] enrl = parts[2].split(",");
        int startEnergy = Integer.parseInt(enrl[0].trim());
        int startRope = Integer.parseInt(enrl[1].trim());

        // Parse Grid (starts at part 3, continues for H rows)
        int[][] grid = new int[h][w];
        for (int r = 0; r < h; r++) {
            String[] rowVals = parts[3 + r].split(",");
            for (int c = 0; c < w; c++) {
                grid[r][c] = Integer.parseInt(rowVals[c].trim());
            }
        }

        // Parse Doors
        Set<String> lockedDoors = new HashSet<>();
        int doorIndex = 3 + h;
        if (parts.length > doorIndex && !parts[doorIndex].trim().isEmpty()) {
            String[] dCoords = parts[doorIndex].split(",");
            for (int i = 0; i + 1 < dCoords.length; i += 2) {
                lockedDoors.add(dCoords[i].trim() + "," + dCoords[i + 1].trim());
            }
        }

        // Parse Keys
        Set<String> uncollectedKeys = new HashSet<>();
        int keyIndex = 3 + h + 1;
        if (parts.length > keyIndex && !parts[keyIndex].trim().isEmpty()) {
            String[] kCoords = parts[keyIndex].split(",");
            for (int i = 0; i + 1 < kCoords.length; i += 2) {
                uncollectedKeys.add(kCoords[i].trim() + "," + kCoords[i + 1].trim());
            }
        }

        // Initialize first state (Lives always start with 3, not holding a key)
        State initialState = new State(w, h, grid, startX, startY, startEnergy, startRope, 3, false, uncollectedKeys,
                lockedDoors);
        CaveExplorer problem = new CaveExplorer(initialState);

        // Call generic search method
        return genericSearch(problem, strategy);
    }

    // -------------------------------------------------------------------------
    // Generic Search Procedure
    // Matches the lecture pseudocode: GENERAL-SEARCH(problem, QING-FUN)
    //   nodes  <- MAKE-Q(MAKE-NODE(INIT-STATE(problem)))
    //   while nodes not empty:
    //     node <- REMOVE-FIRST(nodes)
    //     if GOAL-TEST(problem, STATE(node)) -> return node
    //     children <- EXPAND(node, OPERATORS(problem))
    //     QING-FUN(nodes, children)
    //   return failure
    //
    // Each concrete strategy (UC, A*, depth-limited DFS used by ID) is an
    // instantiation of this single template with a different queuing function
    // supplied via the Frontier interface. Iterative deepening simply calls the
    // template repeatedly with an increasing depth limit, exactly as in the lecture
    // (ID-SEARCH calls DL-SEARCH, which is GENERAL-SEARCH with ENQUEUE-AT-FRONT
    // and a depth cutoff).
    // -------------------------------------------------------------------------

    /**
     * A Frontier encapsulates the queuing function (QING-FUN from the lecture).
     * Different strategies supply different implementations. Each frontier also
     * does repeated-state elimination itself (see DominanceTable), so add() may
     * silently reject a node that is no better than one already seen.
     */
    private interface Frontier {
        /** Add a child node to the frontier (QING-FUN's insertion logic). */
        void add(SearchTreeNode node);

        /** Remove and return the next node to expand (REMOVE-FIRST). */
        SearchTreeNode removeFirst();

        /** True when the frontier is empty (outdated nodes are dropped first). */
        boolean isEmpty();
    }

    /**
     * The single generic search loop, parameterised by the Frontier (QING-FUN).
     *
     * @return a SearchResult whose goal is null if the search failed
     */
    private static SearchResult search(GenericSearchProblem problem, Frontier frontier) {
        SearchResult result = new SearchResult();

        // MAKE-NODE(INIT-STATE(problem)) and MAKE-Q
        frontier.add(new SearchTreeNode(problem.getInitialState(), null, null, 0, 0, 0));

        while (!frontier.isEmpty()) {
            // REMOVE-FIRST(nodes)
            SearchTreeNode node = frontier.removeFirst();

            // GOAL-TEST(problem, STATE(node))
            if (problem.isGoal(node.getState())) {
                result.goal = node;
                return result;
            }

            result.nodesExpanded++;

            // EXPAND(node, OPERATORS(problem)) then QING-FUN(nodes, children)
            for (SearchTreeNode child : problem.getSuccessors(node)) {
                frontier.add(child);
            }
        }

        // return failure
        return result;
    }

    /** Runs the generic search with the given frontier and formats the outcome. */
    private static String genericSearch(GenericSearchProblem problem, Frontier frontier) {
        SearchResult r = search(problem, frontier);
        return r.goal == null ? "No Solution" : formatSolution(r.goal, r.nodesExpanded);
    }

    /**
     * Dispatches to the requested search strategy by constructing the appropriate
     * Frontier (queuing function) and invoking the generic search loop.
     */
    private static String genericSearch(GenericSearchProblem problem, String strategy) {
        Frontier frontier;
        switch (strategy) {
            case "UC":
                // ORDERED-INSERT by g(n): fewest lives lost, then least energy
                frontier = new PriorityFrontier(CaveExplorer::combinedCost);
                break;
            case "ID":
                return iterativeDeepeningSearch(problem);
            case "AS":
                // ORDERED-INSERT by f(n) = g(n) + h(n)
                frontier = new PriorityFrontier(n -> combinedCost(n) + calculateHeuristic(n.getState()));
                break;
            default:
                return "No Solution";
        }
        return genericSearch(problem, frontier);
    }

    /**
     * Repeated-state elimination by DOMINANCE.
     *
     * Two nodes are "at the same place" when they share position, held key,
     * remaining keys and remaining doors. Among those, node A dominates node B
     * when A has at least as much energy, rope and lives (and, for depth-limited
     * search, is no deeper). Anything B can still do, A can do at no higher cost,
     * so B is useless and is rejected.
     *
     * Why this matters: energy and rope are part of the State, so without this
     * rule "same cell with 1 less energy" counts as a brand-new state and the
     * search explodes on large grids (especially when there is no solution).
     *
     * It also lets A* re-open a place when a strictly cheaper path to it is found
     * (the cheaper node is not dominated, so it is accepted and the old, worse
     * node is marked stale). This keeps A* optimal even though our heuristic is
     * admissible but not consistent.
     */
    private static final class DominanceTable {
        private final boolean useDepth;
        private final Map<List<Object>, List<SearchTreeNode>> seen = new HashMap<>();

        DominanceTable(boolean useDepth) {
            this.useDepth = useDepth;
        }

        /** Everything in the state except the "resources" (energy, rope, lives). */
        private static List<Object> placeOf(State s) {
            return Arrays.asList(s.getAgentX(), s.getAgentY(), s.isHoldingKey(),
                    s.getUncollectedKeys(), s.getLockedDoors());
        }

        /** True if a is at least as good as b in every resource. */
        private boolean dominates(SearchTreeNode a, SearchTreeNode b) {
            State sa = a.getState();
            State sb = b.getState();
            return sa.getEnergy() >= sb.getEnergy()
                    && sa.getRopeLength() >= sb.getRopeLength()
                    && sa.getLives() >= sb.getLives()
                    && (!useDepth || a.getDepth() <= b.getDepth());
        }

        /**
         * Records the node unless an existing node dominates it.
         * Existing nodes that the new node dominates are forgotten (they become stale).
         *
         * @return true if the node was recorded (i.e. it is worth adding to the frontier)
         */
        boolean tryRecord(SearchTreeNode node) {
            List<SearchTreeNode> nodesHere = seen.computeIfAbsent(placeOf(node.getState()), k -> new ArrayList<>());
            for (SearchTreeNode old : nodesHere) {
                if (dominates(old, node)) {
                    return false;
                }
            }
            nodesHere.removeIf(old -> dominates(node, old));
            nodesHere.add(node);
            return true;
        }

        /** True if this node was replaced by a better one after it entered the frontier. */
        boolean isStale(SearchTreeNode node) {
            List<SearchTreeNode> nodesHere = seen.get(placeOf(node.getState()));
            if (nodesHere != null) {
                for (SearchTreeNode n : nodesHere) {
                    if (n == node) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /**
     * Entry wrapper for the priority queue. The priority is computed ONCE when the
     * node is inserted (so the heuristic is not recomputed on every comparison),
     * and equal priorities leave in FIFO order, making the search deterministic.
     */
    private static final class FrontierEntry {
        final SearchTreeNode node;
        final int priority;
        final long order; // insertion sequence number

        FrontierEntry(SearchTreeNode node, int priority, long order) {
            this.node = node;
            this.priority = priority;
            this.order = order;
        }
    }

    /**
     * ORDERED-INSERT frontier shared by UC and A*. The only difference between
     * the two strategies is the priority function passed in:
     *   UC: g(n)           A*: g(n) + h(n)
     */
    private static final class PriorityFrontier implements Frontier {
        private final PriorityQueue<FrontierEntry> queue = new PriorityQueue<>(
                Comparator.comparingInt((FrontierEntry e) -> e.priority)
                        .thenComparingLong(e -> e.order));
        private final DominanceTable table = new DominanceTable(false);
        private final ToIntFunction<SearchTreeNode> priorityOf;
        private long insertionCounter;

        PriorityFrontier(ToIntFunction<SearchTreeNode> priorityOf) {
            this.priorityOf = priorityOf;
        }

        @Override
        public void add(SearchTreeNode node) {
            if (table.tryRecord(node)) {
                queue.add(new FrontierEntry(node, priorityOf.applyAsInt(node), insertionCounter++));
            }
        }

        @Override
        public SearchTreeNode removeFirst() {
            return queue.remove().node;
        }

        @Override
        public boolean isEmpty() {
            // Drop outdated nodes (a better node for the same place was found later)
            while (!queue.isEmpty() && table.isStale(queue.peek().node)) {
                queue.poll();
            }
            return queue.isEmpty();
        }
    }

    /**
     * Frontier for depth-limited DFS (the DL-SEARCH of the lecture): a LIFO stack
     * (ENQUEUE-AT-FRONT) that rejects nodes deeper than the limit.
     *
     * Repeated-state elimination is depth-aware: a node is only redundant if a node
     * with at least the same resources was already reached at the same or a
     * shallower depth. (Ignoring depth would wrongly block a place first reached
     * deep in one branch from being re-expanded at a shallower depth.)
     */
    private static final class DepthLimitedFrontier implements Frontier {
        private final Deque<SearchTreeNode> stack = new ArrayDeque<>();
        private final DominanceTable table = new DominanceTable(true);
        private final int limit;
        private boolean cutoff = false; // true if any node was rejected for exceeding the limit

        DepthLimitedFrontier(int limit) {
            this.limit = limit;
        }

        /** True if some node was cut off, i.e. a deeper limit might still find a solution. */
        boolean hitCutoff() {
            return cutoff;
        }

        @Override
        public void add(SearchTreeNode node) {
            if (node.getDepth() > limit) {
                cutoff = true;
                return;
            }
            if (table.tryRecord(node)) {
                stack.push(node);
            }
        }

        @Override
        public SearchTreeNode removeFirst() {
            return stack.pop();
        }

        @Override
        public boolean isEmpty() {
            while (!stack.isEmpty() && table.isStale(stack.peek())) {
                stack.pop();
            }
            return stack.isEmpty();
        }
    }

    /**
     * Large constant used to combine lives and energy into one scalar cost.
     * Must be strictly greater than the maximum possible energy expenditure
     * (initial energy ≤ 500) so that saving one life is always preferred over
     * any amount of energy savings.
     */
    private static final int LIVES_PENALTY = 100_000;

    /**
     * Returns the combined scalar cost for a node:
     *   g(n) = livesLost × LIVES_PENALTY + pathCost
     * Minimising this combined value is equivalent to minimising lives first,
     * then energy — matching the assignment's optimality criterion.
     */
    private static int combinedCost(SearchTreeNode n) {
        return n.getLivesLost() * LIVES_PENALTY + n.getPathCost();
    }

    /**
     * Admissible heuristic h(n) for A* search.
     *
     * EXPLANATION:
     * -----------
     * Goal: unlock ALL remaining locked doors (each door needs exactly one key).
     *
     * Lower-bound reasoning:
     *  - Every movement step costs at least `minCellEnergy` (the cheapest non-wall
     *    cell in the grid).  Manhattan distance is a lower bound on actual steps
     *    because walls may force longer detours.
     *  - To unlock N remaining doors the agent must make N separate key→door trips
     *    (one key unlocks exactly one door, so it cannot reuse a key).
     *
     * Case 1 – agent is NOT holding a key:
     *   Total steps ≥  dist(agent → nearest key)        [reach first key]
     *               +  N × min(key→door Manhattan dist) [each trip: key cell to door cell]
     *   Multiply by minCellEnergy to convert steps to energy.
     *
     * Case 2 – agent IS holding a key:
     *   The first door is already covered (key in hand).
     *   Total steps ≥  dist(agent → nearest door)           [deliver held key]
     *               +  (N-1) × min(key→door Manhattan dist) [remaining trips]
     *   Multiply by minCellEnergy.
     *
     * Why admissible:
     *  - We use the global minimum cell energy → never overestimates per-step cost.
     *  - We use Manhattan distance → never overestimates step count.
     *  - We count every remaining door → never underestimates the number of trips.
     *
     * Why non-trivial (beyond plain Manhattan/Euclidean):
     *  - It incorporates the actual grid's minimum traversal energy, not just geometry.
     *  - It accounts for ALL remaining locked doors, not just the nearest one.
     *  - The key→door round-trip structure reflects the problem's collect-then-unlock
     *    requirement, which pure distance metrics completely ignore.
     */
    private static int calculateHeuristic(State state) {
        int numDoors = state.getLockedDoors().size();
        if (numDoors == 0)
            return 0; // Already at goal with respect to doors.

        // --- Step 1: find minimum energy cost per movement step in this grid. ---
        int minCellEnergy = Integer.MAX_VALUE;
        int[][] grid = state.getGrid();
        for (int row = 0; row < state.getHeight(); row++) {
            for (int col = 0; col < state.getWidth(); col++) {
                if (grid[row][col] > 0 && grid[row][col] < minCellEnergy) {
                    minCellEnergy = grid[row][col];
                }
            }
        }
        if (minCellEnergy == Integer.MAX_VALUE)
            return 0; // Grid is all walls — no movement possible.

        int agentX = state.getAgentX();
        int agentY = state.getAgentY();

        // --- Step 2: find shortest key→door Manhattan distance (used for every trip). ---
        int minKeyToDoor = Integer.MAX_VALUE;
        for (String key : state.getUncollectedKeys()) {
            String[] kp = key.split(",");
            int kx = Integer.parseInt(kp[0]);
            int ky = Integer.parseInt(kp[1]);
            for (String door : state.getLockedDoors()) {
                String[] dp = door.split(",");
                int dx = Integer.parseInt(dp[0]);
                int dy = Integer.parseInt(dp[1]);
                int dist = Math.abs(kx - dx) + Math.abs(ky - dy);
                if (dist < minKeyToDoor)
                    minKeyToDoor = dist;
            }
        }
        if (minKeyToDoor == Integer.MAX_VALUE)
            minKeyToDoor = 0; // No uncollected keys; holding key case handles this.

        int totalSteps;

        if (state.isHoldingKey()) {
            // Agent already has a key. Deliver it to the nearest door first.
            int minDistToDoor = Integer.MAX_VALUE;
            for (String door : state.getLockedDoors()) {
                String[] dp = door.split(",");
                int dx = Integer.parseInt(dp[0]);
                int dy = Integer.parseInt(dp[1]);
                int dist = Math.abs(agentX - dx) + Math.abs(agentY - dy);
                if (dist < minDistToDoor)
                    minDistToDoor = dist;
            }
            // First door: walk to it with held key. Remaining N-1 doors: one key→door trip each.
            totalSteps = minDistToDoor + (numDoors - 1) * minKeyToDoor;
        } else {
            // Agent has no key. Walk to the nearest key first.
            int minDistToKey = Integer.MAX_VALUE;
            for (String key : state.getUncollectedKeys()) {
                String[] kp = key.split(",");
                int kx = Integer.parseInt(kp[0]);
                int ky = Integer.parseInt(kp[1]);
                int dist = Math.abs(agentX - kx) + Math.abs(agentY - ky);
                if (dist < minDistToKey)
                    minDistToKey = dist;
            }
            if (minDistToKey == Integer.MAX_VALUE)
                return 0; // No keys and doors remain — should not happen.

            // Walk to nearest key, then make N key→door trips (one per locked door).
            totalSteps = minDistToKey + numDoors * minKeyToDoor;
        }

        return totalSteps * minCellEnergy;
    }

    /**
     * Iterative Deepening Search (ID).
     *
     * Mirrors the lecture's ID-SEARCH: for depth = 0, 1, 2, ... run a depth-limited
     * search (DL-SEARCH) until it succeeds. Each DL-SEARCH is the SAME generic
     * search loop used by UC and A*, instantiated with a depth-limited LIFO
     * frontier. Node counts are accumulated across all iterations.
     *
     * If an iteration finishes without any node being cut off by the limit, the
     * whole reachable state space was exhausted, so no solution exists.
     */
    private static String iterativeDeepeningSearch(GenericSearchProblem problem) {
        int totalNodes = 0;
        for (int limit = 0;; limit++) {
            DepthLimitedFrontier frontier = new DepthLimitedFrontier(limit);
            SearchResult r = search(problem, frontier);
            totalNodes += r.nodesExpanded;
            if (r.goal != null) {
                return formatSolution(r.goal, totalNodes);
            }
            if (!frontier.hitCutoff()) {
                return "No Solution";
            }
        }
    }

    /**
     * Formats the solution string from a goal node.
     * Output format: "action1,action2,...;livesLost;energySpent;nodesExpanded"
     *
     * Uses the node's own tracked livesLost and pathCost fields (which are
     * cumulative from the root) so all search strategies produce consistent output.
     */
    private static String formatSolution(SearchTreeNode goal, int nodesExpanded) {
        return goal.getPlan() + ";" + goal.getLivesLost() + ";" + goal.getPathCost() + ";" + nodesExpanded;
    }

    /** Outcome of one run of the generic search loop (goal == null means failure). */
    private static class SearchResult {
        private SearchTreeNode goal;
        private int nodesExpanded;
    }

    public static void main(String[] args) {
        // This case has the same optimal solution for UC and AS, but AS uses
        // the heuristic to expand fewer nodes (18 vs. 27).
        String cave = "5,6;0,4;500,20;1,2,7,1,5,1;9,0,0,0,6,7;2,0,7,0,6,7;2,6,0,0,9,0;1,4,6,7,2,4;5,0;0,0;";

        String uniformCostResult = CaveExplorer.solve(cave, "UC");
        String aStarResult = CaveExplorer.solve(cave, "AS");
        String idResult = CaveExplorer.solve(cave, "ID");
        System.out.println("UC: " + uniformCostResult);
        System.out.println("AS: " + aStarResult);
        System.out.println("ID: " + idResult);
    }
}
