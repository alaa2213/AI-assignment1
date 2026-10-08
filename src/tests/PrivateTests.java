package tests;

import code.CaveExplorer;
import code.SearchTreeNode;
import code.State;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateTests {
    private static final String ASTAR_ADVANTAGE_CASE =
            "5,6;0,4;500,20;1,2,7,1,5,1;9,0,0,0,6,7;2,0,7,0,6,7;2,6,0,0,9,0;1,4,6,7,2,4;5,0;0,0;";

    @Test
    void aStarExpandsFewerNodesThanUniformCostSearch() {
        String uniformCostResult = CaveExplorer.solve(ASTAR_ADVANTAGE_CASE, "UC");
        String aStarResult = CaveExplorer.solve(ASTAR_ADVANTAGE_CASE, "AS");

        assertNotEquals("No Solution", uniformCostResult);
        assertNotEquals("No Solution", aStarResult);
        assertTrue(nodesExpanded(aStarResult) < nodesExpanded(uniformCostResult),
                "A* should expand fewer nodes for the comparison case");
    }

    @Test
    void aStarAndUniformCostReturnTheSameOptimalCost() {
        String uniformCostResult = CaveExplorer.solve(ASTAR_ADVANTAGE_CASE, "UC");
        String aStarResult = CaveExplorer.solve(ASTAR_ADVANTAGE_CASE, "AS");

        assertEquals(uniformCostResult.split(";")[1], aStarResult.split(";")[1],
                "Both strategies must minimize lives lost first");
        assertEquals(uniformCostResult.split(";")[2], aStarResult.split(";")[2],
                "Both strategies must minimize energy among equal-life solutions");
    }

    @Test
    void solutionContainsPlanAndSearchStatistics() {
        String result = CaveExplorer.solve(ASTAR_ADVANTAGE_CASE, "AS");
        String[] fields = result.split(";", -1);

        assertEquals(4, fields.length);
        assertFalse(fields[0].isEmpty(), "A successful solution should contain actions");
        assertEquals("0", fields[1], "The comparison case should not spend a life");
        assertEquals("30", fields[2], "The known optimal energy cost should be 30");
        assertTrue(nodesExpanded(result) > 0);
    }

    @Test
    void impossibleCaveIsReportedWithoutInventingAPlan() {
        String impossibleCave = "1,3;0,0;10,0;1,0,1;2,0;1,0;";

        assertEquals("No Solution", CaveExplorer.solve(impossibleCave, "UC"));
        assertEquals("No Solution", CaveExplorer.solve(impossibleCave, "AS"));
    }

    @Test
    void successorGenerationRespectsWallsEnergyAndRope() {
        int[][] grid = {{1, 0}, {2, 1}};
        State initial = new State(
                2, 2, grid, 0, 1, 0, 0, 3, false,
                new HashSet<>(), Collections.singleton("1,1"));
        CaveExplorer problem = new CaveExplorer(initial);
        SearchTreeNode root = new SearchTreeNode(initial, null, null, 0, 0, 0);

        List<SearchTreeNode> successors = problem.getSuccessors(root);

        assertTrue(successors.isEmpty(),
                "The wall, insufficient energy, and zero-rope constraints should block all moves");
    }

    private static int nodesExpanded(String result) {
        return Integer.parseInt(result.split(";", -1)[3]);
    }
}
