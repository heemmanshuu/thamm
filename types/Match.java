// File: Match.java
package thamm.types;

import java.util.List;

/**
 * Represents a formed match.
 */
public class Match {
    public List<Player> players;
    public long timestamp;

    public Match(List<Player> players, long timestamp) {
        this.players = players;
        this.timestamp = timestamp;
    }
}