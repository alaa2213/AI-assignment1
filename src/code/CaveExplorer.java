package code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.PriorityQueue;
import java.util.Comparator;

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
    public List<SearchTreeNode> getSuccessors(SearchTreeNode node) {
        List<SearchTreeNode> successors = new ArrayList<>();
        State currentState = node.getState();
        int x = currentState.getAgentX();
        int y = currentState.getAgentY();
        int[][] grid = currentState.getGrid();
        int w = currentState.getWidth();
        int h = currentState.getHeight();

        // 1. left: move horizontally
        if (x > 0 && grid[y][x - 1] > 0 && currentState.getEnergy() >= grid[y][x - 1]) {
            State nextState = new State(currentState);
            nextState.setAgentX(x - 1);
            int cost = grid[y][x - 1];
            nextState.setEnergy(nextState.getEnergy() - cost);
            successors.add(new SearchTreeNode(nextState, node, "left", node.getPathCost() + cost, node.getDepth() + 1));
        }

        // 2. right: move horizontally
        if (x < w - 1 && grid[y][x + 1] > 0 && currentState.getEnergy() >= grid[y][x + 1]) {
            State nextState = new State(currentState);
            nextState.setAgentX(x + 1);
            int cost = grid[y][x + 1];
            nextState.setEnergy(nextState.getEnergy() - cost);
            successors
                    .add(new SearchTreeNode(nextState, node, "right", node.getPathCost() + cost, node.getDepth() + 1));
        }

        // 3. climbup: move vertically
        if (y > 0 && grid[y - 1][x] > 0 && currentState.getEnergy() >= grid[y - 1][x]
                && currentState.getRopeLength() >= 1) {
            State nextState = new State(currentState);
            nextState.setAgentY(y - 1);
            int cost = grid[y - 1][x];
            nextState.setEnergy(nextState.getEnergy() - cost);
            nextState.setRopeLength(nextState.getRopeLength() - 1);
            successors.add(
                    new SearchTreeNode(nextState, node, "climbup", node.getPathCost() + cost, node.getDepth() + 1));
        }

        // 4. climbdown: move vertically
        if (y < h - 1 && grid[y + 1][x] > 0 && currentState.getEnergy() >= grid[y + 1][x]
                && currentState.getRopeLength() >= 1) {
            State nextState = new State(currentState);
            nextState.setAgentY(y + 1);
            int cost = grid[y + 1][x];
            nextState.setEnergy(nextState.getEnergy() - cost);
            nextState.setRopeLength(nextState.getRopeLength() - 1);
            successors.add(
                    new SearchTreeNode(nextState, node, "climbdown", node.getPathCost() + cost, node.getDepth() + 1));
        }

        // 5. jumpdown: move 1 cell DOWN, costs 1 life and the destination energy, 0
        // rope
        if (y < h - 1 && grid[y + 1][x] > 0
                && currentState.getEnergy() >= grid[y + 1][x]
                && currentState.getLives() > 1) {
            State nextState = new State(currentState);
            nextState.setAgentY(y + 1);
            int destEnergy = grid[y + 1][x];
            int cost = destEnergy + 1000000;
            nextState.setEnergy(nextState.getEnergy() - destEnergy);
            nextState.setLives(nextState.getLives() - 1);
            successors.add(
                    new SearchTreeNode(nextState, node, "jumpdown", node.getPathCost() + cost, node.getDepth() + 1));
        }

        String currentPos = x + "," + y;

        // 6. collect: picks up key
        if (currentState.getUncollectedKeys().contains(currentPos) && !currentState.isHoldingKey()) {
            State nextState = new State(currentState);
            nextState.getUncollectedKeys().remove(currentPos);
            nextState.setHoldingKey(true);
            successors.add(new SearchTreeNode(nextState, node, "collect", node.getPathCost(), node.getDepth() + 1));
        }

        // 7. unlock: unlocks a door with held key
        if (currentState.getLockedDoors().contains(currentPos) && currentState.isHoldingKey()) {
            State nextState = new State(currentState);
            nextState.getLockedDoors().remove(currentPos);
            nextState.setHoldingKey(false);
            successors.add(new SearchTreeNode(nextState, node, "unlock", node.getPathCost(), node.getDepth() + 1));
        }

        return successors;
    }

    public static String solve(String caveSystem, String strategy) {
        // Split with limit -1 to preserve trailing empty values in case there are no
        // keys or doors
        String[] parts = caveSystem.split(";", -1);

        // Parse H,W
        String[] hw = parts[0].split(",");
        int h = Integer.parseInt(hw[0]);
        int w = Integer.parseInt(hw[1]);

        // Parse X,Y
        String[] xy = parts[1].split(",");
        int startX = Integer.parseInt(xy[0]);
        int startY = Integer.parseInt(xy[1]);

        // Parse EN, RL
        String[] enrl = parts[2].split(",");
        int startEnergy = Integer.parseInt(enrl[0]);
        int startRope = Integer.parseInt(enrl[1]);

        // Parse Grid (starts at part 3, continues for H rows)
        int[][] grid = new int[h][w];
        for (int r = 0; r < h; r++) {
            String[] rowVals = parts[3 + r].split(",");
            for (int c = 0; c < w; c++) {
                grid[r][c] = Integer.parseInt(rowVals[c]);
            }
        }

        // Parse Doors
        Set<String> lockedDoors = new HashSet<>();
        int doorIndex = 3 + h;
        if (parts.length > doorIndex && !parts[doorIndex].trim().isEmpty()) {
            String[] dCoords = parts[doorIndex].split(",");
            for (int i = 0; i < dCoords.length; i += 2) {
                lockedDoors.add(dCoords[i] + "," + dCoords[i + 1]);
            }
        }

        // Parse Keys
        Set<String> uncollectedKeys = new HashSet<>();
        int keyIndex = 3 + h + 1;
        if (parts.length > keyIndex && !parts[keyIndex].trim().isEmpty()) {
            String[] kCoords = parts[keyIndex].split(",");
            for (int i = 0; i < kCoords.length; i += 2) {
                uncollectedKeys.add(kCoords[i] + "," + kCoords[i + 1]);
            }
        }

        // Initialize first state (Lives always start with 3, not holding a key)
        State initialState = new State(w, h, grid, startX, startY, startEnergy, startRope, 3, false, uncollectedKeys,
                lockedDoors);
        CaveExplorer problem = new CaveExplorer(initialState);

        // Call generic search method
        return genericSearch(problem, strategy);
    }

    private static String genericSearch(GenericSearchProblem problem, String strategy) {
        if ("ID".equals(strategy)) {
            return iterativeDeepeningSearch(problem);
        } else if ("AS".equals(strategy)) {
            return aStarSearch(problem);
        }
        return "No Solution";
    }

    private static String aStarSearch(GenericSearchProblem problem) {
        PriorityQueue<SearchTreeNode> pq = new PriorityQueue<>(
                Comparator.comparingInt(n -> n.getPathCost() + calculateHeuristic(n.getState())));
        Map<State, Integer> bestCost = new HashMap<>();

        SearchTreeNode root = new SearchTreeNode(problem.getInitialState(), null, "", 0, 0);
        pq.add(root);
        bestCost.put(root.getState(), 0);

        int nodesExpanded = 0;

        while (!pq.isEmpty()) {
            SearchTreeNode current = pq.poll();

            if (current.getPathCost() > bestCost.getOrDefault(current.getState(), Integer.MAX_VALUE)) {
                continue;
            }

            if (problem.isGoal(current.getState())) {
                return formatSolution(current, nodesExpanded);
            }

            nodesExpanded++;

            for (SearchTreeNode successor : problem.getSuccessors(current)) {
                int newCost = successor.getPathCost();
                if (newCost < bestCost.getOrDefault(successor.getState(), Integer.MAX_VALUE)) {
                    bestCost.put(successor.getState(), newCost);
                    pq.add(successor);
                }
            }
        }

        return "No Solution";
    }

    // Explanation of the heuristic:
    // The heuristic calculates an admissible lower bound on the energy required to
    // reach the goal.
    // It first finds the minimum energy cost (minCellEnergy) required to enter any
    // non-wall cell in the grid.
    // If there are locked doors remaining:
    // 1. If the agent is holding a key, it must travel to at least one locked door.
    // The minimum cost is the minimum Manhattan distance to any locked door
    // multiplied by minCellEnergy.
    // 2. If the agent is not holding a key, it must travel to a key, and then to a
    // door.
    // We find the minimum Manhattan distance to any key, plus the minimum Manhattan
    // distance from that key to any door.
    // This sum is multiplied by minCellEnergy.
    // Because every movement step consumes at least minCellEnergy (and jumps cost a
    // massive life penalty),
    // this heuristic strictly never overestimates the actual cost and accounts for
    // energy properly.
    private static int calculateHeuristic(State state) {
        if (state.getLockedDoors().isEmpty())
            return 0;

        int minCellEnergy = Integer.MAX_VALUE;
        int[][] grid = state.getGrid();
        for (int y = 0; y < state.getHeight(); y++) {
            for (int x = 0; x < state.getWidth(); x++) {
                if (grid[y][x] > 0 && grid[y][x] < minCellEnergy) {
                    minCellEnergy = grid[y][x];
                }
            }
        }
        if (minCellEnergy == Integer.MAX_VALUE)
            minCellEnergy = 0;

        int agentX = state.getAgentX();
        int agentY = state.getAgentY();

        if (state.isHoldingKey()) {
            int minDistToDoor = Integer.MAX_VALUE;
            for (String door : state.getLockedDoors()) {
                String[] parts = door.split(",");
                int dx = Integer.parseInt(parts[0]);
                int dy = Integer.parseInt(parts[1]);
                int dist = Math.abs(agentX - dx) + Math.abs(agentY - dy);
                if (dist < minDistToDoor)
                    minDistToDoor = dist;
            }
            return minDistToDoor * minCellEnergy;
        } else {
            int minDistToKey = Integer.MAX_VALUE;
            for (String key : state.getUncollectedKeys()) {
                String[] parts = key.split(",");
                int kx = Integer.parseInt(parts[0]);
                int ky = Integer.parseInt(parts[1]);
                int dist = Math.abs(agentX - kx) + Math.abs(agentY - ky);
                if (dist < minDistToKey) {
                    minDistToKey = dist;
                }
            }

            if (minDistToKey == Integer.MAX_VALUE)
                return 0;

            int minKeyToDoor = Integer.MAX_VALUE;
            for (String key : state.getUncollectedKeys()) {
                String[] kparts = key.split(",");
                int kx = Integer.parseInt(kparts[0]);
                int ky = Integer.parseInt(kparts[1]);
                for (String door : state.getLockedDoors()) {
                    String[] dparts = door.split(",");
                    int dx = Integer.parseInt(dparts[0]);
                    int dy = Integer.parseInt(dparts[1]);
                    int dist = Math.abs(kx - dx) + Math.abs(ky - dy);
                    if (dist < minKeyToDoor)
                        minKeyToDoor = dist;
                }
            }
            if (minKeyToDoor == Integer.MAX_VALUE)
                minKeyToDoor = 0;

            return (minDistToKey + minKeyToDoor) * minCellEnergy;
        }
    }

    // Iterative deepening repeats depth-limited DFS, preserving the first
    // shallowest plan.
    private static String iterativeDeepeningSearch(GenericSearchProblem problem) {
        SearchTreeNode root = new SearchTreeNode(problem.getInitialState(), null, "", 0, 0);
        int nodesExpanded = 0;

        for (int depthLimit = 0;; depthLimit++) {
            Set<State> pathStates = new HashSet<>();
            Map<State, Integer> bestRemainingDepth = new HashMap<>();
            SearchResult result = depthLimitedSearch(problem, root, depthLimit,
                    pathStates, bestRemainingDepth);
            nodesExpanded += result.nodesExpanded;
            if (result.goal != null) {
                return formatSolution(result.goal, nodesExpanded);
            }
            if (result.exhausted) {
                return "No Solution";
            }
        }
    }

    private static SearchResult depthLimitedSearch(GenericSearchProblem problem,
            SearchTreeNode node,
            int remainingDepth,
            Set<State> pathStates,
            Map<State, Integer> bestRemainingDepth) {
        SearchResult result = new SearchResult();
        if (problem.isGoal(node.getState())) {
            result.goal = node;
            return result;
        }
        if (remainingDepth == 0) {
            result.exhausted = false;
            return result;
        }
        Integer previousDepth = bestRemainingDepth.get(node.getState());
        if (previousDepth != null && previousDepth >= remainingDepth) {
            return result;
        }
        bestRemainingDepth.put(node.getState(), remainingDepth);

        pathStates.add(node.getState());
        boolean reachedCutoff = false;
        for (SearchTreeNode successor : problem.getSuccessors(node)) {
            if (pathStates.contains(successor.getState())) {
                continue;
            }
            SearchResult childResult = depthLimitedSearch(problem, successor,
                    remainingDepth - 1, pathStates, bestRemainingDepth);
            result.nodesExpanded += childResult.nodesExpanded + 1;
            if (childResult.goal != null) {
                pathStates.remove(node.getState());
                result.goal = childResult.goal;
                return result;
            }
            reachedCutoff |= !childResult.exhausted;
        }
        pathStates.remove(node.getState());
        result.exhausted = !reachedCutoff;
        return result;
    }

    private static String formatSolution(SearchTreeNode goal, int nodesExpanded) {
        List<String> actions = new ArrayList<>();
        SearchTreeNode current = goal;
        while (current.getParent() != null) {
            actions.add(0, current.getAction());
            current = current.getParent();
        }
        State initialState = current.getState();
        State goalState = goal.getState();
        int livesUsed = initialState.getLives() - goalState.getLives();
        int energyUsed = initialState.getEnergy() - goalState.getEnergy();
        return String.join(",", actions) + ";" + livesUsed + ";" + energyUsed + ";" + nodesExpanded;
    }

    private static class SearchResult {
        private SearchTreeNode goal;
        private int nodesExpanded;
        private boolean exhausted = true;
    }

    public static void main(String[] args) {
        String cave = "1,3;0,0;5,0;1,1,1;2,0;1,0;";

        String result = CaveExplorer.solve(cave, "AS");
        System.out.println(result);
    }
}
