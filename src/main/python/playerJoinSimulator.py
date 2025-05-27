from kafka import KafkaProducer
import json
import random
import time
from datetime import datetime

# Kafka config
KAFKA_TOPIC = 'matchmaking-system-7' #'player-join-queue'
KAFKA_SERVER = 'localhost:9092'

# Sample data
REGIONS = ['NA', 'EU', 'ASIA', 'SA', 'AF']

# Initialize Kafka producer
producer = KafkaProducer(
    bootstrap_servers=KAFKA_SERVER,
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

# Generate and send 1000 players
for i in range(1000):
    player = {
        "id": i,
        "mmr": random.randint(0, 3000),
        "region": random.choice(REGIONS) #, "timestamp": datetime.utcnow().isoformat()
    }

    # Send to Kafka
    producer.send(KAFKA_TOPIC, player)

    if i % 100 == 0:
        print(f"Sent {i} players...")

# Wait for all messages to be sent
producer.flush()
print("Finished sending 1000 players.")
