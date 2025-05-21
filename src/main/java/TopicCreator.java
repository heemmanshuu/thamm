import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class TopicCreator {

    private final AdminClient adminClient;

    public TopicCreator(String bootstrapServers) {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        this.adminClient = AdminClient.create(config);
    }

    public void createTopic(String topicName, int numPartitions, short replicationFactor) {
        NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor);

        try {
            adminClient.createTopics(Collections.singleton(newTopic)).all().get();
            System.out.println("Topic '" + topicName + "' created successfully");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                System.out.println("Topic '" + topicName + "' already exists");
            } else {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    public void close() {
        adminClient.close();
    }

    public static void main(String[] args) {
        String bootstrapServers = "localhost:9092"; // your Kafka broker address
        TopicCreator topicCreator = new TopicCreator(bootstrapServers);

        // Example: create a topic for a new matchmaking system
        topicCreator.createTopic("matchmaking-system-1", 3, (short) 1);

        topicCreator.close();
    }
}