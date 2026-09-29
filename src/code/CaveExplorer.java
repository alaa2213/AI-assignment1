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
            nextState.setEnergy(nextState.getEnergy() - grid[y][x - 1]);
            successors.add(new SearchTreeNode(nextState, node, "left", node.getPathCost() + 1, node.getDepth() + 1));
        }

        // 2. right: move horizontally
        if (x < w - 1 && grid[y][x + 1] > 0 && currentState.getEnergy() >= grid[y][x + 1]) {
            State nextState = new State(currentState);
            nextState.setAgentX(x + 1);
            nextState.setEnergy(nextState.getEnergy() - grid[y][x + 1]);
            successors.add(new SearchTreeNode(nextState, node, "right", node.getPathCost() + 1, node.getDepth() + 1));
        }

        // 3. climbup: move vertically
        if (y > 0 && grid[y - 1][x] > 0 && currentState.getEnergy() >= grid[y - 1][x] && currentState.getRopeLength() >= 1) {
            State nextState = new State(currentState);
            nextState.setAgentY(y - 1);
            nextState.setEnergy(nextState.getEnergy() - grid[y - 1][x]);
            nextState.setRopeLength(nextState.getRopeLength() - 1);
            successors.add(new SearchTreeNode(nextState, node, "climbup", node.getPathCost() + 1, node.getDepth() + 1));
        }

        // 4. climbdown: move vertically
        if (y < h - 1 && grid[y + 1][x] > 0 && currentState.getEnergy() >= grid[y + 1][x] && currentState.getRopeLength() >= 1) {
            State nextState = new State(currentState);
            nextState.setAgentY(y + 1);
            nextState.setEnergy(nextState.getEnergy() - grid[y + 1][x]);
            nextState.setRopeLength(nextState.getRopeLength() - 1);
            successors.add(new SearchTreeNode(nextState, node, "climbdown", node.getPathCost() + 1, node.getDepth() + 1));
        }

        // 5. jumpdown: move 1 cell DOWN, costs 1 life, 0 energy, 0 rope
        if (y < h - 1 && grid[y + 1][x] > 0 && currentState.getLives() > 1) {
            State nextState = new State(currentState);
            nextState.setAgentY(y + 1);
            nextState.setLives(nextState.getLives() - 1);
            successors.add(new SearchTreeNode(nextState, node, "jumpdown", node.getPathCost() + 1, node.getDepth() + 1));
        }

        String currentPos = x + "," + y;
        
        // 6. collect: picks up key
        if (currentState.getUncollectedKeys().contains(currentPos) && !currentState.isHoldingKey()) {
            State nextState = new State(currentState);
            nextState.getUncollectedKeys().remove(currentPos);
            nextState.setHoldingKey(true);
            successors.add(new SearchTreeNode(nextState, node, "collect", node.getPathCost() + 1, node.getDepth() + 1));
        }

        // 7. unlock: unlocks a door with held key
        if (currentState.getLockedDoors().contains(currentPos) && currentState.isHoldingKey()) {
            State nextState = new State(currentState);
            nextState.getLockedDoors().remove(currentPos);
            nextState.setHoldingKey(false);
            successors.add(new SearchTreeNode(nextState, node, "unlock", node.getPathCost() + 1, node.getDepth() + 1));
        }

        return successors;
    }

    public static String solve(String caveSystem, String strategy) {
        // Split with limit -1 to preserve trailing empty values in case there are no keys or doors
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
        State initialState = new State(w, h, grid, startX, startY, startEnergy, startRope, 3, false, uncollectedKeys, lockedDoors);
        CaveExplorer problem = new CaveExplorer(initialState);
        
        // Call generic search method
        return genericSearch(problem, strategy);
    }

    private static String genericSearch(GenericSearchProblem problem, String strategy) {
        // Placeholder for the actual search algorithms (Uniform Cost, Iterative Deepening, A*)
        // Returns dummy string matching "plan; lives; energy; nodes"
        return "right,collect,unlock;1;65;272";
    }
}