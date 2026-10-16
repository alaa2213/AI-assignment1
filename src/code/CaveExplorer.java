// package code;

// import java.util.ArrayList;
// import java.util.Comparator;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.PriorityQueue;
// import java.util.Set;

// public class CaveExplorer extends GenericSearchProblem {

//     private State initialState;

//     public CaveExplorer(State initialState) {
//         this.initialState = initialState;
//     }

//     @Override
//     public State getInitialState() {
//         return initialState;
//     }

//     @Override
//     public boolean isGoal(State state) {
//         // Goal is reached when 0 locked doors remain and agent has at least 1 life.
//         return state.getLockedDoors().isEmpty() && state.getLives() >= 1;
//     }

//     /**
//      * Helper that builds a child node. The child's costs are the parent's costs
//      * plus the energy / lives consumed by this single action.
//      */
//     private SearchTreeNode makeChild(SearchTreeNode parent, State nextState, String action,
//             int energySpent, int livesSpent) {
//         return new SearchTreeNode(nextState, parent, action,
//                 parent.getPathCost() + energySpent,
//                 parent.getLivesLost() + livesSpent,
//                 parent.getDepth() + 1);
//     }

//     @Override
//     public List<SearchTreeNode> getSuccessors(SearchTreeNode node) {
//         List<SearchTreeNode> successors = new ArrayList<>();
//         State currentState = node.getState();
//         int x = currentState.getAgentX();
//         int y = currentState.getAgentY();
//         int[][] grid = currentState.getGrid();
//         int w = currentState.getWidth();
//         int h = currentState.getHeight();

//         // 1. left: move horizontally (costs destination difficulty in energy)
//         if (x > 0 && grid[y][x - 1] > 0 && currentState.getEnergy() >= grid[y][x - 1]) {
//             int cost = grid[y][x - 1];
//             State nextState = new State(currentState);
//             nextState.setAgentX(x - 1);
//             nextState.setEnergy(nextState.getEnergy() - cost);
//             successors.add(makeChild(node, nextState, "left", cost, 0));
//         }

//         // 2. right: move horizontally (costs destination difficulty in energy)
//         if (x < w - 1 && grid[y][x + 1] > 0 && currentState.getEnergy() >= grid[y][x + 1]) {
//             int cost = grid[y][x + 1];
//             State nextState = new State(currentState);
//             nextState.setAgentX(x + 1);
//             nextState.setEnergy(nextState.getEnergy() - cost);
//             successors.add(makeChild(node, nextState, "right", cost, 0));
//         }

//         // 3. climbup: move vertically (costs energy + 1 rope)
//         if (y > 0 && grid[y - 1][x] > 0 && currentState.getEnergy() >= grid[y - 1][x]
//                 && currentState.getRopeLength() >= 1) {
//             int cost = grid[y - 1][x];
//             State nextState = new State(currentState);
//             nextState.setAgentY(y - 1);
//             nextState.setEnergy(nextState.getEnergy() - cost);
//             nextState.setRopeLength(nextState.getRopeLength() - 1);
//             successors.add(makeChild(node, nextState, "climbup", cost, 0));
//         }

//         // 4. climbdown: move vertically (costs energy + 1 rope)
//         if (y < h - 1 && grid[y + 1][x] > 0 && currentState.getEnergy() >= grid[y + 1][x]
//                 && currentState.getRopeLength() >= 1) {
//             int cost = grid[y + 1][x];
//             State nextState = new State(currentState);
//             nextState.setAgentY(y + 1);
//             nextState.setEnergy(nextState.getEnergy() - cost);
//             nextState.setRopeLength(nextState.getRopeLength() - 1);
//             successors.add(makeChild(node, nextState, "climbdown", cost, 0));
//         }

//         // 5. jumpdown: move 1 cell DOWN, costs 1 life + destination difficulty in
//         // energy, 0 rope.
//         // NOTE: the provided checker charges the destination difficulty for jumpdown
//         // too
//         // (it rejects plans that report 0 energy for a jump with "Incorrect energy cost
//         // calculation").
//         if (y < h - 1 && grid[y + 1][x] > 0 && currentState.getLives() > 1
//                 && currentState.getEnergy() >= grid[y + 1][x]) {
//             int cost = grid[y + 1][x];
//             State nextState = new State(currentState);
//             nextState.setAgentY(y + 1);
//             nextState.setEnergy(nextState.getEnergy() - cost);
//             nextState.setLives(nextState.getLives() - 1);
//             successors.add(makeChild(node, nextState, "jumpdown", cost, 1));
//         }

//         String currentPos = x + "," + y;

//         // 6. collect: picks up key (free)
//         if (currentState.getUncollectedKeys().contains(currentPos) && !currentState.isHoldingKey()) {
//             State nextState = new State(currentState);
//             nextState.getUncollectedKeys().remove(currentPos);
//             nextState.setHoldingKey(true);
//             successors.add(makeChild(node, nextState, "collect", 0, 0));
//         }

//         // 7. unlock: unlocks a door with held key (free)
//         if (currentState.getLockedDoors().contains(currentPos) && currentState.isHoldingKey()) {
//             State nextState = new State(currentState);
//             nextState.getLockedDoors().remove(currentPos);
//             nextState.setHoldingKey(false);
//             successors.add(makeChild(node, nextState, "unlock", 0, 0));
//         }

//         return successors;
//     }

//     public static String solve(String caveSystem, String strategy) {
//         // Split with limit -1 to preserve trailing empty values in case there are no
//         // keys or doors
//         String[] parts = caveSystem.split(";", -1);

//         // Parse H,W
//         String[] hw = parts[0].split(",");
//         int h = Integer.parseInt(hw[0].trim());
//         int w = Integer.parseInt(hw[1].trim());

//         // Parse X,Y
//         String[] xy = parts[1].split(",");
//         int startX = Integer.parseInt(xy[0].trim());
//         int startY = Integer.parseInt(xy[1].trim());

//         // Parse EN, RL
//         String[] enrl = parts[2].split(",");
//         int startEnergy = Integer.parseInt(enrl[0].trim());
//         int startRope = Integer.parseInt(enrl[1].trim());

//         // Parse Grid (starts at part 3, continues for H rows)
//         int[][] grid = new int[h][w];
//         for (int r = 0; r < h; r++) {
//             String[] rowVals = parts[3 + r].split(",");
//             for (int c = 0; c < w; c++) {
//                 grid[r][c] = Integer.parseInt(rowVals[c].trim());
//             }
//         }

//         // Parse Doors
//         Set<String> lockedDoors = new HashSet<>();
//         int doorIndex = 3 + h;
//         if (parts.length > doorIndex && !parts[doorIndex].trim().isEmpty()) {
//             String[] dCoords = parts[doorIndex].split(",");
//             for (int i = 0; i + 1 < dCoords.length; i += 2) {
//                 lockedDoors.add(dCoords[i].trim() + "," + dCoords[i + 1].trim());
//             }
//         }

//         // Parse Keys
//         Set<String> uncollectedKeys = new HashSet<>();
//         int keyIndex = 3 + h + 1;
//         if (parts.length > keyIndex && !parts[keyIndex].trim().isEmpty()) {
//             String[] kCoords = parts[keyIndex].split(",");
//             for (int i = 0; i + 1 < kCoords.length; i += 2) {
//                 uncollectedKeys.add(kCoords[i].trim() + "," + kCoords[i + 1].trim());
//             }
//         }

//         // Initialize first state (Lives always start with 3, not holding a key)
//         State initialState = new State(w, h, grid, startX, startY, startEnergy, startRope, 3, false, uncollectedKeys,
//                 lockedDoors);
//         CaveExplorer problem = new CaveExplorer(initialState);

//         // Call generic search method
//         return genericSearch(problem, strategy);
//     }

//     // -------------------------------------------------------------------------
//     // Generic Search Procedure
//     // Matches the lecture pseudocode: GENERAL-SEARCH(problem, QING-FUN)
//     //   nodes  <- MAKE-Q(MAKE-NODE(INIT-STATE(problem)))
//     //   while nodes not empty:
//     //     node <- REMOVE-FIRST(nodes)
//     //     if GOAL-TEST(problem, STATE(node)) -> return node
//     //     children <- EXPAND(node, OPERATORS(problem))
//     //     QING-FUN(nodes, children)
//     //   return failure
//     //
//     // Each concrete strategy (ID, UC, A*) is an instantiation of this template
//     // with a different queuing function supplied via the Frontier interface.
//     // -------------------------------------------------------------------------

//     /**
//      * A Frontier encapsulates the queuing function (QING-FUN from the lecture).
//      * Different strategies supply different implementations.
//      */
//     private interface Frontier {
//         /** Add a child node to the frontier (QING-FUN's insertion logic). */
//         void add(SearchTreeNode node);
//         /** Remove and return the next node to expand (REMOVE-FIRST). */
//         SearchTreeNode removeFirst();
//         /** True when the frontier is empty. */
//         boolean isEmpty();
//         /** True if the frontier already contains a better or equal copy of this state. */
//         boolean containsBetterOrEqual(SearchTreeNode node);

//         /**
//          * Allows depth-limited frontiers to stop expansion while still
//          * participating in the same general-search loop.
//          */
//         default boolean canExpand(SearchTreeNode node) {
//             return true;
//         }

//         /**
//          * Indicates that the search stopped at a depth cutoff rather than
//          * because the complete reachable state space was exhausted.
//          */
//         default boolean reachedCutoff() {
//             return false;
//         }

//         /**
//          * Iterative deepening performs path-based cycle checking rather than
//          * using one explored set for the whole depth-limited pass.
//          */
//         default boolean usesExploredSet() {
//             return true;
//         }

//         /**
//          * Returns whether this frontier considers the node a repeated state.
//          */
//         default boolean isRepeated(SearchTreeNode node, Set<State> explored) {
//             return explored.contains(node.getState());
//         }
//     }

//     /**
//      * The single generic search loop, parameterised by the Frontier (QING-FUN).
//      * Explored-set-based repeated-state elimination is applied here; strategies
//      * that need finer control (ID, AS) override containsBetterOrEqual.
//      *
//      * @param problem  the search problem
//      * @param frontier the queuing strategy
//      * @return solution string or "No Solution"
//      */
//     private static String genericSearch(GenericSearchProblem problem, Frontier frontier) {
//         // MAKE-NODE(INIT-STATE(problem))
//         SearchTreeNode root = new SearchTreeNode(problem.getInitialState(), null, null, 0, 0, 0);
//         frontier.add(root);

//         // Explored set for repeated-state elimination
//         Set<State> explored = new HashSet<>();
//         int nodesExpanded = 0;

//         while (!frontier.isEmpty()) {
//             // REMOVE-FIRST(nodes)
//             SearchTreeNode node = frontier.removeFirst();

//             // Skip stale duplicates that a priority-queue strategy may have re-queued
//             if (frontier.isRepeated(node, explored)) {
//                 continue;
//             }
//             if (frontier.usesExploredSet()) {
//                 explored.add(node.getState());
//             }

//             // GOAL-TEST(problem, STATE(node))
//             if (problem.isGoal(node.getState())) {
//                 return formatSolution(node, nodesExpanded);
//             }

//             if (!frontier.canExpand(node)) {
//                 continue;
//             }

//             nodesExpanded++;

//             // EXPAND(node, OPERATORS(problem)) then QING-FUN(nodes, children)
//             for (SearchTreeNode child : problem.getSuccessors(node)) {
//                 if ((!frontier.usesExploredSet() || !explored.contains(child.getState()))
//                         && !frontier.containsBetterOrEqual(child)) {
//                     frontier.add(child);
//                 }
//             }
//         }

//         return "No Solution";
//     }

//     /**
//      * Dispatches to the requested search strategy by constructing the appropriate
//      * Frontier (queuing function) and invoking the generic search loop.
//      */
//     private static String genericSearch(GenericSearchProblem problem, String strategy) {
//         switch (strategy) {
//             case "UC":
//                 return genericSearch(problem, new UniformCostFrontier());
//             case "ID":
//                 return iterativeDeepeningSearch(problem);
//             case "AS":
//                 return genericSearch(problem, new AStarFrontier());
//             default:
//                 return "No Solution";
//         }
//     }

//     /**
//      * Uniform Cost Search (QING-FUN = insert into priority queue ordered by cost).
//      *
//      * The optimality criterion is lexicographic: minimise lives lost first,
//      * then minimise energy spent. Ties are broken by insertion order (FIFO).
//      * Because every action has a non-negative cost, the first goal node dequeued
//      * is guaranteed to be optimal.
//      */
//     private static final class UniformCostFrontier implements Frontier {
//         private long insertionCounter;
//         private final PriorityQueue<SearchTreeNode> queue = new PriorityQueue<>(
//                 Comparator.comparingInt(SearchTreeNode::getLivesLost)
//                         .thenComparingInt(SearchTreeNode::getPathCost)
//                         .thenComparingLong(node -> insertionOrder(node)));
//         private final Map<SearchTreeNode, Long> insertionOrders = new HashMap<>();

//         private long insertionOrder(SearchTreeNode node) {
//             return insertionOrders.get(node);
//         }

//         @Override
//         public void add(SearchTreeNode node) {
//             insertionOrders.put(node, insertionCounter++);
//             queue.add(node);
//         }

//         @Override
//         public SearchTreeNode removeFirst() {
//             SearchTreeNode node = queue.poll();
//             insertionOrders.remove(node);
//             return node;
//         }

//         @Override
//         public boolean isEmpty() {
//             return queue.isEmpty();
//         }

//         @Override
//         public boolean containsBetterOrEqual(SearchTreeNode node) {
//             for (SearchTreeNode queued : queue) {
//                 if (queued.getState().equals(node.getState())
//                         && queued.getLivesLost() <= node.getLivesLost()
//                         && queued.getPathCost() <= node.getPathCost()) {
//                     return true;
//                 }
//             }
//             return false;
//         }
//     }

//     /**
//      * Large constant used to combine lives and energy into one scalar cost.
//      * Must be strictly greater than the maximum possible energy expenditure
//      * (initial energy ≤ 500) so that saving one life is always preferred over
//      * any amount of energy savings.
//      */
//     private static final int LIVES_PENALTY = 100_000;

//     /**
//      * Returns the combined scalar cost for a node:
//      *   g(n) = livesLost × LIVES_PENALTY + pathCost
//      * Minimising this combined value is equivalent to minimising lives first,
//      * then energy — matching the assignment's optimality criterion.
//      */
//     private static int combinedCost(SearchTreeNode n) {
//         return n.getLivesLost() * LIVES_PENALTY + n.getPathCost();
//     }

//     /**
//      * A* Search (QING-FUN = priority queue ordered by f(n) = g(n) + h(n)).
//      *
//      * g(n) = livesLost × LIVES_PENALTY + pathCost  (combined optimality cost)
//      * h(n) = admissible energy heuristic (see calculateHeuristic)
//      *
//      * Using a combined g(n) ensures A* respects the same lexicographic
//      * (lives, energy) optimality criterion as Uniform Cost Search.
//      * h(n) estimates remaining energy only; since lives_remaining × LIVES_PENALTY
//      * ≥ 0 ≥ h(n), admissibility w.r.t. the combined cost is preserved.
//      *
//      * A bestCost map tracks the lowest g(n) seen per state to discard stale
//      * duplicate nodes re-queued after a cheaper path was found.
//      */
//     private static final class AStarFrontier implements Frontier {
//         private final PriorityQueue<SearchTreeNode> queue = new PriorityQueue<>(
//                 Comparator.comparingInt(node -> combinedCost(node) + calculateHeuristic(node.getState())));
//         private final Map<State, Integer> bestCost = new HashMap<>();

//         @Override
//         public void add(SearchTreeNode node) {
//             int cost = combinedCost(node);
//             if (cost < bestCost.getOrDefault(node.getState(), Integer.MAX_VALUE)) {
//                 bestCost.put(node.getState(), cost);
//                 queue.add(node);
//             }
//         }

//         @Override
//         public SearchTreeNode removeFirst() {
//             return queue.poll();
//         }

//         @Override
//         public boolean isEmpty() {
//             return queue.isEmpty();
//         }

//         @Override
//         public boolean containsBetterOrEqual(SearchTreeNode node) {
//             return combinedCost(node) >= bestCost.getOrDefault(node.getState(), Integer.MAX_VALUE);
//         }

//         @Override
//         public boolean usesExploredSet() {
//             return false;
//         }

//         @Override
//         public boolean isRepeated(SearchTreeNode node, Set<State> explored) {
//             return combinedCost(node) > bestCost.getOrDefault(
//                     node.getState(), Integer.MAX_VALUE);
//         }
//     }

//     /**
//      * Admissible heuristic h(n) for A* search.
//      *
//      * EXPLANATION:
//      * -----------
//      * Goal: unlock ALL remaining locked doors (each door needs exactly one key).
//      *
//      * Lower-bound reasoning:
//      *  - Every movement step costs at least `minCellEnergy` (the cheapest non-wall
//      *    cell in the grid).  Manhattan distance is a lower bound on actual steps
//      *    because walls may force longer detours.
//      *  - To unlock N remaining doors the agent must make N separate key→door trips
//      *    (one key unlocks exactly one door, so it cannot reuse a key).
//      *
//      * Case 1 – agent is NOT holding a key:
//      *   Total steps ≥  dist(agent → nearest key)        [reach first key]
//      *               +  N × min(key→door Manhattan dist) [each trip: key cell to door cell]
//      *   Multiply by minCellEnergy to convert steps to energy.
//      *
//      * Case 2 – agent IS holding a key:
//      *   The first door is already covered (key in hand).
//      *   Total steps ≥  dist(agent → nearest door)           [deliver held key]
//      *               +  (N-1) × min(key→door Manhattan dist) [remaining trips]
//      *   Multiply by minCellEnergy.
//      *
//      * Why admissible:
//      *  - We use the global minimum cell energy → never overestimates per-step cost.
//      *  - We use Manhattan distance → never overestimates step count.
//      *  - We count every remaining door → never underestimates the number of trips.
//      *
//      * Why non-trivial (beyond plain Manhattan/Euclidean):
//      *  - It incorporates the actual grid's minimum traversal energy, not just geometry.
//      *  - It accounts for ALL remaining locked doors, not just the nearest one.
//      *  - The key→door round-trip structure reflects the problem's collect-then-unlock
//      *    requirement, which pure distance metrics completely ignore.
//      */
//     private static int calculateHeuristic(State state) {
//         int numDoors = state.getLockedDoors().size();
//         if (numDoors == 0)
//             return 0; // Already at goal with respect to doors.

//         // --- Step 1: find minimum energy cost per movement step in this grid. ---
//         int minCellEnergy = Integer.MAX_VALUE;
//         int[][] grid = state.getGrid();
//         for (int row = 0; row < state.getHeight(); row++) {
//             for (int col = 0; col < state.getWidth(); col++) {
//                 if (grid[row][col] > 0 && grid[row][col] < minCellEnergy) {
//                     minCellEnergy = grid[row][col];
//                 }
//             }
//         }
//         if (minCellEnergy == Integer.MAX_VALUE)
//             return 0; // Grid is all walls — no movement possible.

//         int agentX = state.getAgentX();
//         int agentY = state.getAgentY();

//         // --- Step 2: find shortest key→door Manhattan distance (used for every trip). ---
//         int minKeyToDoor = Integer.MAX_VALUE;
//         for (String key : state.getUncollectedKeys()) {
//             String[] kp = key.split(",");
//             int kx = Integer.parseInt(kp[0]);
//             int ky = Integer.parseInt(kp[1]);
//             for (String door : state.getLockedDoors()) {
//                 String[] dp = door.split(",");
//                 int dx = Integer.parseInt(dp[0]);
//                 int dy = Integer.parseInt(dp[1]);
//                 int dist = Math.abs(kx - dx) + Math.abs(ky - dy);
//                 if (dist < minKeyToDoor)
//                     minKeyToDoor = dist;
//             }
//         }
//         if (minKeyToDoor == Integer.MAX_VALUE)
//             minKeyToDoor = 0; // No uncollected keys; holding key case handles this.

//         int totalSteps;

//         if (state.isHoldingKey()) {
//             // Agent already has a key. Deliver it to the nearest door first.
//             int minDistToDoor = Integer.MAX_VALUE;
//             for (String door : state.getLockedDoors()) {
//                 String[] dp = door.split(",");
//                 int dx = Integer.parseInt(dp[0]);
//                 int dy = Integer.parseInt(dp[1]);
//                 int dist = Math.abs(agentX - dx) + Math.abs(agentY - dy);
//                 if (dist < minDistToDoor)
//                     minDistToDoor = dist;
//             }
//             // First door: walk to it with held key. Remaining N-1 doors: one key→door trip each.
//             totalSteps = minDistToDoor + (numDoors - 1) * minKeyToDoor;
//         } else {
//             // Agent has no key. Walk to the nearest key first.
//             int minDistToKey = Integer.MAX_VALUE;
//             for (String key : state.getUncollectedKeys()) {
//                 String[] kp = key.split(",");
//                 int kx = Integer.parseInt(kp[0]);
//                 int ky = Integer.parseInt(kp[1]);
//                 int dist = Math.abs(agentX - kx) + Math.abs(agentY - ky);
//                 if (dist < minDistToKey)
//                     minDistToKey = dist;
//             }
//             if (minDistToKey == Integer.MAX_VALUE)
//                 return 0; // No keys and doors remain — should not happen.

//             // Walk to nearest key, then make N key→door trips (one per locked door).
//             totalSteps = minDistToKey + numDoors * minKeyToDoor;
//         }

//         return totalSteps * minCellEnergy;
//     }

//     /**
//      * Iterative Deepening Search (ID).
//      *
//      * Repeatedly runs a depth-limited DFS with an increasing depth limit.
//      * Combines the space efficiency of DFS with the completeness guarantee
//      * of BFS — finds the shallowest solution first.
//      *
//      * Repeated-state elimination: within a single DL-DFS pass we track the
//      * current path (to avoid cycles) AND a bestRemainingDepth map so that we
//      * skip re-visiting a state we have already visited with >= remaining budget.
//      */
//     private static String iterativeDeepeningSearch(GenericSearchProblem problem) {
//         for (int depthLimit = 0;; depthLimit++) {
//             DepthLimitedFrontier frontier = new DepthLimitedFrontier(depthLimit);
//             String result = genericSearch(problem, frontier);
//             if (!"No Solution".equals(result)) {
//                 return result;
//             }
//             if (!frontier.reachedCutoff()) {
//                 return "No Solution"; // Entire state space exhausted — no solution exists.
//             }
//         }
//     }

//     private static final class DepthLimitedFrontier implements Frontier {
//         private final int depthLimit;
//         private final List<SearchTreeNode> stack = new ArrayList<>();
//         private final Map<State, Integer> bestRemainingDepth = new HashMap<>();
//         private boolean cutoff;

//         DepthLimitedFrontier(int depthLimit) {
//             this.depthLimit = depthLimit;
//         }

//         @Override
//         public void add(SearchTreeNode node) {
//             stack.add(node);
//         }

//         @Override
//         public SearchTreeNode removeFirst() {
//             return stack.remove(stack.size() - 1);
//         }

//         @Override
//         public boolean isEmpty() {
//             return stack.isEmpty();
//         }

//         @Override
//         public boolean containsBetterOrEqual(SearchTreeNode node) {
//             SearchTreeNode ancestor = node.getParent();
//             while (ancestor != null) {
//                 if (ancestor.getState().equals(node.getState())) {
//                     return true;
//                 }
//                 ancestor = ancestor.getParent();
//             }
//             return false;
//         }

//         @Override
//         public boolean canExpand(SearchTreeNode node) {
//             if (node.getDepth() >= depthLimit) {
//                 cutoff = true;
//                 return false;
//             }
//             return true;
//         }

//         @Override
//         public boolean usesExploredSet() {
//             return false;
//         }

//         @Override
//         public boolean isRepeated(SearchTreeNode node, Set<State> explored) {
//             int remainingDepth = depthLimit - node.getDepth();
//             Integer previous = bestRemainingDepth.get(node.getState());
//             if (previous != null && previous >= remainingDepth) {
//                 return true;
//             }
//             bestRemainingDepth.put(node.getState(), remainingDepth);
//             return false;
//         }

//         @Override
//         public boolean reachedCutoff() {
//             return cutoff;
//         }
//     }

//     /**
//      * Formats the solution string from a goal node.
//      * Output format: "action1,action2,...;livesLost;energySpent;nodesExpanded"
//      *
//      * Uses the node's own tracked livesLost and pathCost fields (which are
//      * cumulative from the root) so all search strategies produce consistent output.
//      */
//     private static String formatSolution(SearchTreeNode goal, int nodesExpanded) {
//         return goal.getPlan() + ";" + goal.getLivesLost() + ";" + goal.getPathCost() + ";" + nodesExpanded;
//     }

//     public static void main(String[] args) {
//         // This case has the same optimal solution for UC and AS, but AS uses
//         // the heuristic to expand fewer nodes (364 vs. 964).
//         String cave = "5,6;0,4;500,20;1,2,7,1,5,1;9,0,0,0,6,7;2,0,7,0,6,7;2,6,0,0,9,0;1,4,6,7,2,4;5,0;0,0;";

//         String uniformCostResult = CaveExplorer.solve(cave, "UC");
//         String aStarResult = CaveExplorer.solve(cave, "AS");
//         System.out.println("UC: " + uniformCostResult);
//         System.out.println("AS: " + aStarResult);
//     }
// }

package code;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    public int getHeuristic(State state) {
        return calculateHeuristic(state);
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

        // Call generic search method inherited from GenericSearchProblem
        return genericSearch(problem, strategy);
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

    public static void main(String[] args) {
        String cave = "5,6;0,4;500,20;1,2,7,1,5,1;9,0,0,0,6,7;2,0,7,0,6,7;2,6,0,0,9,0;1,4,6,7,2,4;5,0;0,0;";

        String uniformCostResult = CaveExplorer.solve(cave, "UC");
        String aStarResult = CaveExplorer.solve(cave, "AS");
        System.out.println("UC: " + uniformCostResult);
        System.out.println("AS: " + aStarResult);
    }
}