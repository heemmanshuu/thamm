import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.*;

public class TeamVsTeamMatchmaker extends KeyedProcessFunction<Integer, Map<String, Object>, Map<String, Object>> {

    private transient ListState<Map<String, Object>> waitingTeams;

    private final int matchSize;

    public TeamVsTeamMatchmaker(int matchSize) {
        this.matchSize = matchSize;
    }

    @Override
    public void open(Configuration parameters) {
        ListStateDescriptor<Map<String, Object>> descriptor =
                new ListStateDescriptor<>("waitingTeams", (Class<Map<String, Object>>) (Class<?>) Map.class);
        waitingTeams = getRuntimeContext().getListState(descriptor);
    }

    @Override
    public void processElement(Map<String, Object> team, Context ctx, Collector<Map<String, Object>> out) throws Exception {
        List<Map<String, Object>> currentTeams = new ArrayList<>();
        for (Map<String, Object> t : waitingTeams.get()) {
            currentTeams.add(t);
        }

        currentTeams.add(team);
        Integer teamRank = (Integer) team.get("teamRank");

        while (currentTeams.size() >= matchSize) {
            List<Map<String, Object>> matchedTeams = new ArrayList<>(currentTeams.subList(0, matchSize));
            String matchId = UUID.randomUUID().toString();

            Map<String, Object> match = new HashMap<>();
            match.put("matchId", matchId);
            match.put("teamRank", teamRank);
            match.put("teams", matchedTeams);

            out.collect(match); // Emit match info

            currentTeams.subList(0, matchSize).clear(); // Remove matched teams
        }

        waitingTeams.update(currentTeams); // Save remaining teams
    }
}