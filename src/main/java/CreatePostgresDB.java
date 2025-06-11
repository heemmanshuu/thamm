import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.Random;

public class CreatePostgresDB {
  public static void main(String[] args) {
    String host = "jdbc:postgresql://localhost:5432/";
    String defaultDb = "postgres";
    String newDb = "my_database";
    String user = "myuser";
    String password = "password"; // Set if needed

    // Step 1: Connect to default DB and check if the target DB exists
    try (Connection conn = DriverManager.getConnection(host + defaultDb, user, password);
         Statement stmt = conn.createStatement()) {

      String checkDbSQL = "SELECT 1 FROM pg_database WHERE datname = '" + newDb + "'";
      ResultSet rs = stmt.executeQuery(checkDbSQL);
      if (!rs.next()) {
        stmt.executeUpdate("CREATE DATABASE " + newDb);
        System.out.println("Database created successfully!");
      } else {
        System.out.println("Database already exists.");
      }

    } catch (SQLException e) {
      e.printStackTrace();
      return;
    }

    // Step 2: Connect to the new DB and create the PLAYER table if it doesn't exist
    try (Connection newConn = DriverManager.getConnection(host + newDb, user, password);
         Statement newStmt = newConn.createStatement()) {

      String createTableSQL = """
        DROP TABLE IF EXISTS PLAYER;
        CREATE TABLE IF NOT EXISTS PLAYER (
          player_id INT PRIMARY KEY,
          games_won INT,
          games_lost INT,
          games_tied INT,
          last_game_result INT,
          streak_count INT
        );
        """;

      newStmt.executeUpdate(createTableSQL);
      System.out.println("PLAYER table created (or recreated).");

      StringBuilder insertBuilder = new StringBuilder("INSERT INTO PLAYER (player_id, games_won, games_lost, games_tied, last_game_result, streak_count) VALUES\n");
      Random rng = new Random();
      int totalPlayers = 1000;
      double tieMargin = 0.05; // increasing this would make ties happen more often
      for (int i = 0; i < totalPlayers; i++) {
        double winRate = Math.max(Math.min(0.5 + 0.15 * rng.nextGaussian(), 1), 0);
        int totalGames = rng.nextInt(100); // starts from 0, exclusive to upper bound
        int gamesWon = (int) Math.round(winRate * totalGames * (1.0 - tieMargin));
        int gamesTied = (int) Math.round(totalGames * tieMargin);
        int gamesLost = totalGames - gamesWon - gamesTied;
        double winProbability = rng.nextDouble();
        int lastGameResult = 0; // 0 = tie

        int streakCount = (gamesTied > 0) ? rng.nextInt(gamesTied) : 0;

        double winThreshold = winRate - (tieMargin / 2.0);
        double loseThreshold = winRate + (tieMargin / 2.0);

        if (winProbability < winThreshold) {
            lastGameResult = 1;  // win
            streakCount = (gamesWon > 0) ? rng.nextInt(gamesWon) : 0;
        } else if (winProbability > loseThreshold) {
            lastGameResult = -1; // loss
            streakCount = (gamesLost > 0) ? rng.nextInt(gamesLost) : 0;
        }

        insertBuilder.append(String.format("(%d, %d, %d, %d, %d, %d)%s\n",
                i, gamesWon, gamesLost, gamesTied, lastGameResult, streakCount, (i < totalPlayers - 1 ? "," : "")));
      }
      insertBuilder.append("ON CONFLICT (player_id) DO NOTHING;");
      newStmt.executeUpdate(insertBuilder.toString());
      System.out.println("Sample data inserted (skipped duplicates).");

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
