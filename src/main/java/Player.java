import java.io.Serializable;

public class Player implements Serializable {
    private int id;
    private int mmr;
    private String region;
    private double churnLikelihood;

    // Required for Jackson deserialization
    public Player() {}

    public Player(int id, int mmr, String region) {
        this.id = id;
        this.mmr = mmr;
        this.region = region;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMMR() { return mmr; }
    public void setMMR(int mmr) { this.mmr = mmr; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public double getChurnLikelihood() { return churnLikelihood;}
    public void setChurnLikelihood(double likelihood) { this.churnLikelihood = likelihood;}


    @Override
    public String toString() {
        return "Player{id=" + id + ", mmr=" + mmr + ", region='" + region + "'}";
    }
}
