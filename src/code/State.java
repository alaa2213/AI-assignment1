package code;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class State {
    // Grid dimensions and static layout
    private int width;
    private int height;
    private int[][] grid; // 0 = wall, > 0 = cave difficulty

    // Agent's dynamic state
    private int agentX;
    private int agentY;
    private int energy;
    private int ropeLength;
    private int lives;
    private boolean holdingKey;

    // Remaining collectables/interactables
    private Set<String> uncollectedKeys;
    private Set<String> lockedDoors;

    public State(int width, int height, int[][] grid, int agentX, int agentY, 
                 int energy, int ropeLength, int lives, boolean holdingKey, 
                 Set<String> uncollectedKeys, Set<String> lockedDoors) {
        this.width = width;
        this.height = height;
        this.grid = grid;
        this.agentX = agentX;
        this.agentY = agentY;
        this.energy = energy;
        this.ropeLength = ropeLength;
        this.lives = lives;
        this.holdingKey = holdingKey;
        this.uncollectedKeys = new HashSet<>(uncollectedKeys);
        this.lockedDoors = new HashSet<>(lockedDoors);
    }
    
    // Copy constructor for creating successor states
    public State(State other) {
        this.width = other.width;
        this.height = other.height;
        this.grid = other.grid; // Share the static grid reference
        this.agentX = other.agentX;
        this.agentY = other.agentY;
        this.energy = other.energy;
        this.ropeLength = other.ropeLength;
        this.lives = other.lives;
        this.holdingKey = other.holdingKey;
        this.uncollectedKeys = new HashSet<>(other.uncollectedKeys);
        this.lockedDoors = new HashSet<>(other.lockedDoors);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int[][] getGrid() { return grid; }
    
    public int getAgentX() { return agentX; }
    public void setAgentX(int agentX) { this.agentX = agentX; }
    
    public int getAgentY() { return agentY; }
    public void setAgentY(int agentY) { this.agentY = agentY; }
    
    public int getEnergy() { return energy; }
    public void setEnergy(int energy) { this.energy = energy; }
    
    public int getRopeLength() { return ropeLength; }
    public void setRopeLength(int ropeLength) { this.ropeLength = ropeLength; }
    
    public int getLives() { return lives; }
    public void setLives(int lives) { this.lives = lives; }
    
    public boolean isHoldingKey() { return holdingKey; }
    public void setHoldingKey(boolean holdingKey) { this.holdingKey = holdingKey; }
    
    public Set<String> getUncollectedKeys() { return uncollectedKeys; }
    public Set<String> getLockedDoors() { return lockedDoors; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        // We do not include width, height, and grid in the equality check
        // because they are static across all states in a single problem instance.
        return agentX == state.agentX &&
               agentY == state.agentY &&
               energy == state.energy &&
               ropeLength == state.ropeLength &&
               lives == state.lives &&
               holdingKey == state.holdingKey &&
               Objects.equals(uncollectedKeys, state.uncollectedKeys) &&
               Objects.equals(lockedDoors, state.lockedDoors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentX, agentY, energy, ropeLength, lives, holdingKey, uncollectedKeys, lockedDoors);
    }
}
