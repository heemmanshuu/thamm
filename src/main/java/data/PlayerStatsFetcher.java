package data;
import java.sql.*;

import types.PlayerStats;

public class PlayerStatsFetcher {
    private static final String URL = "jdbc:postgresql://localhost:5432/my_database";
    private static final String USER = "myuser";
    private static final String PASSWORD = "password"; // change as needed

    public static PlayerStats getPlayerStats(int playerId) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT games_won, games_lost, last_game_win FROM PLAYER WHERE player_id = ?")) {

            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new PlayerStats(
                        rs.getInt("games_won"),
                        rs.getInt("games_lost"),
                        rs.getBoolean("last_game_win")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}