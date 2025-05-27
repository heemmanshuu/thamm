import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public class KafkaPlayerProducer {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting kafka producer...");
        // Step 1: Define producer configuration
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"); // Kafka broker
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        ObjectMapper mapper = new ObjectMapper();

        // Step 2: Create the Kafka producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // Step 3: Create a record (message) to send
        String topic = "matchmaking-system-2";
        String key = "player"; // Optional, but used to determine partitioning
        for (int i = 0; i < 4; i++)
        {
            Map<String, Object> playerInfo = new HashMap<String, Object>(); // TODO: THIS SHOULD BE THE DICT OF PLAYER INFO
            playerInfo.put("id", i);
            playerInfo.put("mmr", 1000);
            playerInfo.put("region", "NA");
            try {
                String jsonPlayerInfo = mapper.writeValueAsString(playerInfo);
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, jsonPlayerInfo);
                RecordMetadata metadata = producer.send(record).get();
                System.out.printf("Sent message to topic:%s partition:%d offset:%d%n",
                        metadata.topic(), metadata.partition(), metadata.offset());
            } catch (JsonProcessingException e) {
                System.err.println("Failed to serialize playerInfo: " + e.getMessage());
            }
        }

        producer.close();
    }
}