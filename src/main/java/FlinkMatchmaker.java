import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.api.common.functions.MapFunction;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class FlinkMatchmaker {

    public static class JsonToMap implements MapFunction<String, Map<String, Object>>, ResultTypeQueryable<Map<String, Object>> {
        private static final ObjectMapper mapper = new ObjectMapper();

        @Override
        public Map<String, Object> map(String json) throws Exception {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        }

        @Override
        public TypeInformation<Map<String, Object>> getProducedType() {
            return TypeInformation.of(new TypeHint<Map<String, Object>>() {});
        }
    }

    public static void main(String[] args) throws Exception {
        // Set up the execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        int teamSize = 1;
        int numTeams = 2;

        // Configure Kafka consumer
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("localhost:9092")
                .setTopics("matchmaking-system-1")
                .setGroupId("matchmaking-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // create a flink DataStream to process player records
        DataStream<String> jsonStream = env.fromSource(
                source,
                WatermarkStrategy.noWatermarks(), // change to a real watermark strategy for time-based processing
                "Incoming Players"
        );

        jsonStream.print();

        // transform the json stream into stream of maps representing a join_queue request
        //SingleOutputStreamOperator<Map<String, Object>> players = jsonStream.map(new JsonToMap());

        DataStream<Player> players = jsonStream.map(new MapFunction<String, Player>() {
            private final ObjectMapper objectMapper = new ObjectMapper();

            @Override
            public Player map(String json) throws Exception {
                return objectMapper.readValue(json, Player.class);
            }
        }).returns(Player.class);

        players.print();

        DataStream<Match> matches = players
                .keyBy(Player::getRank)
                .process(new PlayerMatchmaker());

        matches.print();

        env.execute("Flink Matchmaker with KafkaSource");
    }
}