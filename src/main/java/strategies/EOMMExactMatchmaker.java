package strategies;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.jgrapht.alg.matching.GreedyWeightedMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import types.Match;
import types.Player;

import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class EOMMExactMatchmaker extends KeyedProcessFunction<String, Player, Match> {

    private transient Connection connection;
    private transient ListState<Player> playerPool;
    private static final String URL = "jdbc:postgresql://host.docker.internal:5432/my_database";
    private static final String USER = "myuser";
    private static final String PASSWORD = "password";

    @Override
    public void open(Configuration parameters) throws Exception {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
        ListStateDescriptor<Player> descriptor = new ListStateDescriptor<>("playerPool", Player.class);
        playerPool = getRuntimeContext().getListState(descriptor);
    }

    @Override
    public void processElement(Player player, Context ctx, Collector<Match> out) throws Exception {
        List<Player> players = new ArrayList<>();
        for (Player p : playerPool.get()) {
            players.add(p);
        }
        players.add(player);

        if (players.size() % 2 != 0) {
            playerPool.update(players);
            return;
        }

        List<Player> enrichedPlayers = players.stream()
                .map(this::enrich)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Tuple3<Integer, Integer, Double>> edges = buildChurnGraph(enrichedPlayers);
        List<Tuple2<Player, Player>> matchedPairs = greedyMatching(enrichedPlayers, edges);

        for (Tuple2<Player, Player> pair : matchedPairs) {
            String matchId = UUID.randomUUID().toString();
            Match match = new Match(matchId, pair.f0, pair.f1);
            out.collect(match);
        }

        playerPool.clear();
    }

    private Player enrich(Player player) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player WHERE player_id = ?");
            stmt.setInt(1, player.getId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                player.setWinCount(rs.getInt("games_won"));
                player.setLossCount(rs.getInt("games_lost"));
                player.setTieCount(rs.getInt("games_tied"));
                player.setLastGameResult(rs.getInt("last_game_result"));
                player.setStreak(rs.getInt("streak_count"));
            }

            return player;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Tuple3<Integer, Integer, Double>> buildChurnGraph(List<Player> players) {
        List<Tuple3<Integer, Integer, Double>> edges = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                Player pi = players.get(i);
                Player pj = players.get(j);

                double[] probs = computeOutcomeProbabilities(pi, pj);
                double churnI = computeChurn(pi, probs[0], probs[1], probs[2]);
                double churnJ = computeChurn(pj, 1 - probs[0], probs[1], probs[2]);

                double combined = churnI + churnJ;
                edges.add(new Tuple3<>(i, j, combined));
            }
        }
        return edges;
    }

    private double[] computeOutcomeProbabilities(Player p1, Player p2) {
        double elo1 = p1.getMMR();
        double elo2 = p2.getMMR();

        // Base Elo probability
        double win1 = 1 / (1 + Math.pow(10, (elo2 - elo1) / 400.0));
        double draw = 0.05;
        double loss1 = 1 - win1 - draw;

        // Win/loss ratio adjustment
        int total1 = p1.getWinCount() + p1.getLossCount() + p1.getTieCount();
        int total2 = p2.getWinCount() + p2.getLossCount() + p2.getTieCount();

        if (total1 > 0 && total2 > 0) {
            double winRatio1 = (double) p1.getWinCount() / total1;
            double winRatio2 = (double) p2.getWinCount() / total2;
            double ratioBoost = (winRatio1 - winRatio2) * 0.1;
            win1 += ratioBoost;
            loss1 -= ratioBoost;
        }

        // Streak-based adjustments (based on last game result)
        if (p1.getLastGameResult() == 2) {
            win1 += 0.01 * p1.getStreak();     // winning streak → more likely to win
            loss1 -= 0.005 * p1.getStreak();   // less likely to lose
        } else if (p1.getLastGameResult() == 0) {
            loss1 += 0.01 * p1.getStreak();    // losing streak → more likely to lose
            win1 -= 0.005 * p1.getStreak();    // less likely to win
        }

        if (p2.getLastGameResult() == 2) {
            win1 -= 0.01 * p2.getStreak();     // opponent on win streak → less likely to win
            loss1 += 0.005 * p2.getStreak();   // more likely to lose
        }  else if (p1.getLastGameResult() == 0) {
            win1 += 0.01 * p2.getStreak();     // opponent on loss streak → more likely to win
            loss1 -= 0.005 * p2.getStreak();   // less likely to lose
        }

        // Clamp to safe range
        win1 = Math.max(0.01, Math.min(0.94, win1));
        loss1 = Math.max(0.01, loss1);

        return new double[]{win1, draw, loss1};
    }

    private double computeChurn(Player p, double winProb, double drawProb, double lossProb) {
        double streakFactor = 1 + (p.getStreak() * 0.1);  // Longer streaks (good or bad) amplify emotions

        // Adjust churn sensitivity based on the last game result
        double churnFromLoss = lossProb * (p.getLastGameResult() == 0 ? 1.2 : 1.0) * streakFactor;
        double churnFromWin = winProb * (p.getLastGameResult() == 2 ? 0.6 : 0.8);
        double churnFromDraw = drawProb * 0.7;

        // Final churn is more influenced by negative experiences
        return churnFromLoss + churnFromDraw - churnFromWin;
    }

    private List<Tuple2<Player, Player>> greedyMatching(List<Player> players, List<Tuple3<Integer, Integer, Double>> edges) {
        DefaultUndirectedWeightedGraph<Integer, DefaultWeightedEdge> graph =
                new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

        for (int i = 0; i < players.size(); i++) {
            graph.addVertex(i);
        }

        for (Tuple3<Integer, Integer, Double> edge : edges) {
            DefaultWeightedEdge e = graph.addEdge(edge.f0, edge.f1);
            graph.setEdgeWeight(e, edge.f2);
        }

        GreedyWeightedMatching<Integer, DefaultWeightedEdge> matchingAlg =
                new GreedyWeightedMatching<>(graph, true); // true = minimize weight

        Matching<Integer, DefaultWeightedEdge> matching = matchingAlg.getMatching();

        List<Tuple2<Player, Player>> result = new ArrayList<>();
        for (DefaultWeightedEdge e : matching.getEdges()) {
            int src = graph.getEdgeSource(e);
            int tgt = graph.getEdgeTarget(e);
            result.add(new Tuple2<>(players.get(src), players.get(tgt)));
        }

        return result;
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}