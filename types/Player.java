// File: Player.java
package thamm.types;

/**
 * Represents a player in the matchmaking queue.
 */
public class Player {
    public String id;
    public int mmr;
    public String region;

    public Player(String id, int mmr, String region) {
        this.id = id;
        this.mmr = mmr;
        this.region = region;
    }
}