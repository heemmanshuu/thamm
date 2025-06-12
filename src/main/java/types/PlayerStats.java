package types;
public class PlayerStats {
    public final int gamesWon;
    public final int gamesLost;
    public final boolean lastGameWin;

    public PlayerStats(int gamesWon, int gamesLost, boolean lastGameWin) {
        this.gamesWon = gamesWon;
        this.gamesLost = gamesLost;
        this.lastGameWin = lastGameWin;
    }

    public double getChurnLikelihood() {
        int totalGames = gamesWon + gamesLost;
        if (totalGames == 0) return 0.5; // Default
        double winRate = (double) gamesWon / totalGames;
        return lastGameWin ? 1 - winRate * 0.3 : 1 - winRate;
    }
}