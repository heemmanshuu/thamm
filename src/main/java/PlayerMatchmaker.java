import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.UUID;

public class PlayerMatchmaker extends KeyedProcessFunction<Integer, Player, Match> {

    // Buffer to hold waiting players with the same rank (key)
    private transient ListState<Player> waitingPlayers;

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        ListStateDescriptor<Player> descriptor = new ListStateDescriptor<>("waitingPlayers", Player.class);
        waitingPlayers = getRuntimeContext().getListState(descriptor);
    }

    @Override
    public void processElement(Player player, Context ctx, Collector<Match> out) throws Exception {
        // Add new player to the buffer
        waitingPlayers.add(player);

        // Collect all waiting players
        int count = 0;
        Player firstPlayer = null;
        Player secondPlayer = null;

        for (Player p : waitingPlayers.get()) {
            if (count == 0) {
                firstPlayer = p;
            } else if (count == 1) {
                secondPlayer = p;
            }
            count++;
            if (count >= 2) {
                break;
            }
        }

        if (count >= 2) {
            // Remove matched players from state
            waitingPlayers.clear();

            // If there were more than 2 players waiting, re-add the extras
            // For example, if count > 2, re-add players after the first two
            // This avoids losing players if more than 2 arrived before this call
            int index = 0;
            for (Player p : waitingPlayers.get()) {
                if (index >= 2) {
                    waitingPlayers.add(p);
                }
                index++;
            }

            // Output a Match between the two players
            String matchId = UUID.randomUUID().toString();
            Match match = new Match(matchId, ctx.getCurrentKey(), firstPlayer, secondPlayer);
            out.collect(match);
        }
    }
}
