import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CreatePostgresDB {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/postgres"; // connect to default 'postgres' DB
        String user = "I588770";
        String password = ""; // update with your actual password

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE DATABASE my_database";
            stmt.executeUpdate(sql);
            System.out.println("Database created successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
