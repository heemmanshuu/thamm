package thamm;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import java.util.concurrent.TimeUnit;
import org.apache.flink.api.common.time.Time;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import com.fasterxml.jackson.databind.ObjectMapper;

import strategies.EOMMExactMatchmaker;
import strategies.SkillBasedMatchmaker;
import types.Match;
import types.Player;
import types.MMRBucketizer;


public class FlinkMatchmaker {

    public static void main(String[] args) throws Exception {
        // Set up the execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(10);

        // checkpointing code for fault tolerance
        env.enableCheckpointing(10000);
        env.getCheckpointConfig().setCheckpointTimeout(60000);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(5000);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.of(10, TimeUnit.SECONDS)));


        MMRBucketizer bucketizer = new MMRBucketizer(1500.0, 300.0, 10);

        // Configure Kafka consumer
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("kafka:29092")
                .setTopics("matchmaking-system-7")
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


        // transform the json stream into stream of players who sent join requests
        DataStream<Player> players = jsonStream
        .map(new MapFunction<String, Player>() {
            private final ObjectMapper objectMapper = new ObjectMapper();

            @Override
            public Player map(String json) throws Exception {
                return objectMapper.readValue(json, Player.class);
            }
        })
        .name("Parse JSON to player")
        .returns(Player.class);

        players.print().name("Print Players");

        DataStream<Match> matches = players
                .keyBy(player -> bucketizer.getBucket(player.getMMR()))
                .process(new EOMMExactMatchmaker())
                .name("EOMM Matchmaker");

        matches.print();

        // write the matches using a Kafka Sink to a topic that will be processed by the game server
        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("kafka:29092")
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("match-results-1")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build())
                .build();

        matches.map(match -> new ObjectMapper().writeValueAsString(match))
                .name("Serialize match to JSON")
                .sinkTo(sink);

        env.execute("Flink Matchmaker with KafkaSource");
    }
}