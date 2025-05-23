import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.*;

public class TeamMatchmaker extends KeyedProcessFunction<Integer, Map<String, Object>, Map<String, Object>> {

    // State to hold waiting players per rank
    private transient ListState<Map<String, Object>> waitingPlayers;

    // Number of players needed to form a match group
    private final int groupSize;

    public TeamMatchmaker(int groupSize) {
        this.groupSize = groupSize;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        ListStateDescriptor<Map<String, Object>> descriptor =
                new ListStateDescriptor<>("waitingPlayers", (Class<Map<String, Object>>) (Class<?>) Map.class);
        waitingPlayers = getRuntimeContext().getListState(descriptor);
    }

    @Override
    public void processElement(Map<String, Object> player, Context ctx, Collector<Map<String, Object>> out) throws Exception {
        // Load current waiting players
        List<Map<String, Object>> currentPlayers = new ArrayList<>();
        for (Map<String, Object> p : waitingPlayers.get()) {
            currentPlayers.add(p);
        }

        // Add new player
        currentPlayers.add(player);
        // Get player's rank
        int rank = (int) player.get("rank");

        // While we have enough players to form a group
        while (currentPlayers.size() >= groupSize) {
            List<Map<String, Object>> teamPlayers = new ArrayList<>(currentPlayers.subList(0, groupSize));

            // Generate unique match ID
            String teamId = UUID.randomUUID().toString();

            Map<String, Object> team = new HashMap<>();
            team.put("teamId", teamId);
            team.put("teamRank", rank);
            team.put("players", teamPlayers);

            // Emit the team info
            out.collect(team);

            // Remove matched players from waiting list
            currentPlayers.subList(0, groupSize).clear();
        }

        // Update the state with remaining players
        waitingPlayers.update(currentPlayers);
    }
}