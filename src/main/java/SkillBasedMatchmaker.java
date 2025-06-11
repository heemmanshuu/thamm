import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SkillBasedMatchmaker extends ProcessFunction<Player, Match> {

    private transient ListState<Player> waitingPlayers;

    @Override
    public void open(Configuration parameters) {
        ListStateDescriptor<Player> descriptor = new ListStateDescriptor<>("waitingPlayers", Player.class);
        waitingPlayers = getRuntimeContext().getListState(descriptor);
    }

    @Override
    public void processElement(Player player, Context ctx, Collector<Match> out) throws Exception {
        // Fetch churn risk (synchronously, consider async IO in production)
        PlayerStats stats = PlayerStatsFetcher.getPlayerStats(player.getId());
        double churnRisk = (stats != null) ? stats.getChurnLikelihood() : 0.5;
        player.setChurnLikelihood(churnRisk);

        // Add to internal buffer
        waitingPlayers.add(player);

        List<Player> buffer = new ArrayList<>();
        for (Player p : waitingPlayers.get()) {
            buffer.add(p);
        }

        if (buffer.size() >= 2) {
            Player p1 = buffer.get(0);
            Player p2 = buffer.get(1);

            // Match players with similar churn risk
            if (Math.abs(p1.getChurnLikelihood() - p2.getChurnLikelihood()) < 0.5) {
                // Remove matched players from buffer
                waitingPlayers.clear();
                for (int i = 2; i < buffer.size(); i++) {
                    waitingPlayers.add(buffer.get(i));
                }

                Match match = new Match(UUID.randomUUID().toString(), p1, p2); // no rank key now
                out.collect(match);
            }
        }
    }
}