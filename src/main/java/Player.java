import java.io.Serializable;

public class Player implements Serializable {
    private int id;
    private int rank;
    private String region;

    // Required for Jackson deserialization
    public Player() {}

    public Player(int id, int rank, String region) {
        this.id = id;
        this.rank = rank;
        this.region = region;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    @Override
    public String toString() {
        return "Player{id=" + id + ", rank=" + rank + ", region='" + region + "'}";
    }
}
