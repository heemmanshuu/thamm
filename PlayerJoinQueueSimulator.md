🧱 Project Structure
```
├── src/main/           # FastAPI-based matchmaking API server
│   └── python-api/
│       └── main.py
│       └── mmrBucketizer.py
│       └── playerJoinSimulator.py
└── PlayerJoinQueueSimulator.md             # Setup instructions
```

⚙️ Requirements
- Python 3.9+
- Kafka running locally on localhost:9092
- Kafka topic created and partitioned (e.g., matchmaking-system-7)
- Docker Desktop (In terminal: "cd docker", then use "docker ps" to check if an existing Kafka matchmaking topic exists)

📦 Installation
Make sure to install the necessary python libraries in main.py and playerJoinSimulator.py

# Start Kafka in Docker
docker compose up -d

# Running the API Server

Open a new terminal and run:
-cd thamm/src/main/python-api
-uvicorn main:app --host 127.0.0.1 --port 8000 --workers 4

![image](https://github.com/user-attachments/assets/975a165e-df5c-41bd-8c8d-7d970cc43018)

# Running the Simulator

Open another terminal and run:
-cd thamm/src/main/python-api
-python simulator.py

This will asynchronously send join requests to the API to simulate real load.

# Kafka Topic Setup
Create the topic with partitions in docker:

docker exec -it docker-kafka-1 kafka-topics --bootstrap-server kafka:29092 --create --topic matchmaking-system-7 --partitions 10 --replication-factor 1

Delete the topic (in case you need to reset):

docker-kafka-1 kafka-topics --bootstrap-server kafka:29092 --delete --topic matchmaking-system-7
