package types;
import java.io.Serializable;

public class Player implements Serializable {
    private int id;
    private int mmr;
    private String region;
    private double churnLikelihood;

    private int winCount;
    private int lossCount;
    private int tieCount;
    private int lastGameResult; // 0 - loss, 1 - tie, 2 - win
    private int streak;

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

    public double getChurnLikelihood() { return churnLikelihood; }
    public void setChurnLikelihood(double likelihood) { this.churnLikelihood = likelihood; }

    public int getWinCount() { return winCount; }
    public void setWinCount(int winCount) { this.winCount = winCount; }

    public int getLossCount() { return lossCount; }
    public void setLossCount(int lossCount) { this.lossCount = lossCount; }

    public int getTieCount() { return tieCount; }
    public void setTieCount(int tieCount) { this.tieCount = tieCount; }

    public int getLastGameResult() { return lastGameResult; }
    public void setLastGameResult(int lastGameResult) { this.lastGameResult = lastGameResult; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }

    @Override
    public String toString() {
        return "Player{id=" + id + ", mmr=" + mmr + ", region='" + region + "'}";
    }
}