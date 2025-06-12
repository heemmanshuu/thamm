package strategies;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import types.Match;
import types.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class SkillBasedMatchmaker extends KeyedProcessFunction<Integer, Player, Match> {

    private transient ListState<Player> waitingPlayers;

    @Override
    public void open(Configuration parameters) {
        ListStateDescriptor<Player> descriptor = new ListStateDescriptor<>("waitingPlayers", Player.class);
        waitingPlayers = getRuntimeContext().getListState(descriptor);
    }

    @Override
    public void processElement(Player player, Context ctx, Collector<Match> out) throws Exception {
        // Add incoming player to waiting list (per key)
        waitingPlayers.add(player);

        // Load waiting players into a list
        List<Player> buffer = new ArrayList<>();
        for (Player p : waitingPlayers.get()) {
            buffer.add(p);
        }

        // Sort players by MMR ascending
        buffer.sort(Comparator.comparingDouble(Player::getMMR));

        // Try to match players greedily with closest MMR pairs
        List<Player> unmatched = new ArrayList<>();
        int i = 0;
        while (i < buffer.size()) {
            if (i + 1 < buffer.size()) {
                Player p1 = buffer.get(i);
                Player p2 = buffer.get(i + 1);

                // max allowed mmr difference to match players
                double MMR_THRESHOLD = 100.0;
                if (Math.abs(p1.getMMR() - p2.getMMR()) <= MMR_THRESHOLD) {
                    // Match found - emit match
                    Match match = new Match(UUID.randomUUID().toString(), p1, p2);
                    out.collect(match);
                    i += 2;  // skip matched pair
                    continue;
                }
            }
            // No match, keep player unmatched for next round
            unmatched.add(buffer.get(i));
            i++;
        }

        // Clear and update waiting state with unmatched players
        waitingPlayers.clear();
        for (Player p : unmatched) {
            waitingPlayers.add(p);
        }
    }
}