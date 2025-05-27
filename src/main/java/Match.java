public class Match {
    private String matchId;
    private int teamRank;
    private Player player1;
    private Player player2;

    public Match() {
        // Default constructor needed for serialization/deserialization
    }

    public Match(String matchId, int teamRank, Player player1, Player player2) {
        this.matchId = matchId;
        this.teamRank = teamRank;
        this.player1 = player1;
        this.player2 = player2;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public int getTeamRank() {
        return teamRank;
    }

    public void setTeamRank(int teamRank) {
        this.teamRank = teamRank;
    }

    public Player getPlayer1() {
        return player1;
    }

    public void setPlayer1(Player player1) {
        this.player1 = player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    @Override
    public String toString() {
        return "Match{" +
                "matchId='" + matchId + '\'' +
                ", teamRank=" + teamRank +
                ", player1=" + player1 +
                ", player2=" + player2 +
                '}';
    }
}
