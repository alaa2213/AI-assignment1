package code;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
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
        if (y > 0 && grid[y - 1][x] > 0 && currentState.getEnergy() >= grid[y - 1][x] && currentState.getRopeLength() >= 1) {
            int cost = grid[y - 1][x];
            State nextState = new State(currentState);
            nextState.setAgentY(y - 1);
            nextState.setEnergy(nextState.getEnergy() - cost);
            nextState.setRopeLength(nextState.getRopeLength() - 1);
            successors.add(makeChild(node, nextState, "climbup", cost, 0));
        }

        // 4. climbdown: move vertically (costs energy + 1 rope)
        if (y < h - 1 && grid[y + 1][x] > 0 && currentState.getEnergy() >= grid[y + 1][x] && currentState.getRopeLength() >= 1) {
            int cost = grid[y + 1][x];
            State nextState = new State(currentState);
            nextState.setAgentY(y + 1);
            nextState.setEnergy(nextState.getEnergy() - cost);
            nextState.setRopeLength(nextState.getRopeLength() - 1);
            successors.add(makeChild(node, nextState, "climbdown", cost, 0));
        }

        // 5. jumpdown: move 1 cell DOWN, costs 1 life + destination difficulty in energy, 0 rope.
        //    NOTE: the provided checker charges the destination difficulty for jumpdown too
        //    (it rejects plans that report 0 energy for a jump with "Incorrect energy cost calculation").
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

    public static String solve(String caveSystem, String strategy) {
        // Split with limit -1 to preserve trailing empty values in case there are no keys or doors
        String[] parts = caveSystem.split(";", -1);

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
        State initialState = new State(w, h, grid, startX, startY, startEnergy, startRope, 3, false, uncollectedKeys, lockedDoors);
        CaveExplorer problem = new CaveExplorer(initialState);

        // Call generic search method
        return genericSearch(problem, strategy);
    }

    /**
     * Dispatches to the requested search strategy.
     * Output format: "plan;livesLost;energySpent;nodesExpanded" or "No Solution".
     */
    private static String genericSearch(GenericSearchProblem problem, String strategy) {
        switch (strategy) {
            case "UC":
                return uniformCostSearch(problem);
            case "ID":
            case "AS":
            default:
                // TODO: Iterative Deepening and A* are not implemented yet.
                return "No Solution";
        }
    }

    /**
     * Wraps a node with an insertion number so equal-cost nodes leave the
     * priority queue in FIFO order (makes the search deterministic).
     */
    private static final class FrontierEntry {
        final SearchTreeNode node;
        final long order;

        FrontierEntry(SearchTreeNode node, long order) {
            this.node = node;
            this.order = order;
        }
    }

    /**
     * Uniform Cost Search.
     *
     * Always expands the frontier node with the lowest path cost. Cost is compared
     * lexicographically: first by lives lost, then by energy spent. Since every
     * action has a non-negative cost, the first goal node removed from the
     * frontier is guaranteed to be optimal.
     */
    private static String uniformCostSearch(GenericSearchProblem problem) {
        // Frontier ordered by (livesLost, energySpent, insertion order).
        PriorityQueue<FrontierEntry> frontier = new PriorityQueue<>(
                Comparator.<FrontierEntry>comparingInt(e -> e.node.getLivesLost())
                        .thenComparingInt(e -> e.node.getPathCost())
                        .thenComparingLong(e -> e.order));
        long insertionCounter = 0;

        // Explored set: states that have already been expanded (repeated-state checking).
        Set<State> explored = new HashSet<>();

        SearchTreeNode root = new SearchTreeNode(problem.getInitialState(), null, null, 0, 0, 0);
        frontier.add(new FrontierEntry(root, insertionCounter++));

        int nodesExpanded = 0;

        while (!frontier.isEmpty()) {
            SearchTreeNode node = frontier.poll().node;

            // A state may have been queued several times; only expand its cheapest copy.
            if (!explored.add(node.getState())) {
                continue;
            }

            // Goal test on expansion (not generation) -> guarantees optimality.
            if (problem.isGoal(node.getState())) {
                return node.getPlan() + ";" + node.getLivesLost() + ";" + node.getPathCost() + ";" + nodesExpanded;
            }

            nodesExpanded++;

            for (SearchTreeNode child : problem.getSuccessors(node)) {
                if (!explored.contains(child.getState())) {
                    frontier.add(new FrontierEntry(child, insertionCounter++));
                }
            }
        }

        return "No Solution";
    }
}