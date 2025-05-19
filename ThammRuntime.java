// File: ThammRuntime.java
package thamm;

import thamm.types.Player;
import thamm.types.Match;
import thamm.strategies.MatchStrategy;
import thamm.strategies.PairByMMRStrategy;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.Properties;

/**
 * Main Flink job that runs THAMM.
 */
public class ThammRuntime {
    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        String kafkaBootstrap = params.get("kafka", "localhost:9092");
        String strategyName = params.get("strategy", "PairByMMRStrategy");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", kafkaBootstrap);
        kafkaProps.setProperty("group.id", "thamm-matchmaker");

        FlinkKafkaConsumer<String> playerStream = new FlinkKafkaConsumer<>(
                "player-events",
                new SimpleStringSchema(),
                kafkaProps
        );
        playerStream.assignTimestampsAndWatermarks(WatermarkStrategy.noWatermarks());

        DataStream<Player> players = env
            .addSource(playerStream)
            .flatMap(new PlayerParser());

        MatchStrategy strategy = new PairByMMRStrategy();

        DataStream<String> matches = players
            .countWindowAll(10) // buffer 10 players at a time
            .apply((window, iterable, collector) -> {
                List<Player> buffer = new ArrayList<>();
                iterable.forEach(buffer::add);
                List<Match> result = strategy.generateMatches(buffer);
                ObjectMapper mapper = new ObjectMapper();
                for (Match m : result) {
                    collector.collect(mapper.writeValueAsString(m));
                }
            });

        matches.addSink(new FlinkKafkaProducer<>(
                "match-events",
                new SimpleStringSchema(),
                kafkaProps
        ));

        env.execute("THAMM Matchmaking Runtime");
    }

    static class PlayerParser implements FlatMapFunction<String, Player> {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public void flatMap(String value, Collector<Player> out) throws Exception {
            try {
                Player p = mapper.readValue(value, Player.class);
                out.collect(p);
            } catch (Exception ignored) {}
        }
    }
}