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
        CREATE TABLE IF NOT EXISTS PLAYER (
          player_id INT PRIMARY KEY,
          games_won INT,
          games_lost INT,
          last_game_win BOOLEAN
        );
        """;

      newStmt.executeUpdate(createTableSQL);
      System.out.println("PLAYER table created or already exists.");

      StringBuilder insertBuilder = new StringBuilder("INSERT INTO PLAYER (player_id, games_won, games_lost, last_game_win) VALUES\n");
      Random rng = new Random();
      int totalPlayers = 1000;
      for (int i = 0; i < totalPlayers; i++) {
        double winRate = Math.min(0.5 + 0.15 * rng.nextGaussian(), 1);
        int totalGames = 10 + rng.nextInt(40); // between 10 and 50 games
        int gamesWon = (int) Math.round(winRate * totalGames);
        int gamesLost = totalGames - gamesWon;
        boolean lastGameWin = rng.nextDouble() < winRate;

        insertBuilder.append(String.format("(%d, %d, %d, %s)%s\n",
                i, gamesWon, gamesLost, lastGameWin, (i < totalPlayers - 1 ? "," : "")));
      }
      insertBuilder.append("ON CONFLICT (player_id) DO NOTHING;");
      newStmt.executeUpdate(insertBuilder.toString());
      System.out.println("Sample data inserted (skipped duplicates).");

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
