# THAMM: Stream-Native Matchmaking Middleware

**THAMM** is a real-time matchmaking middleware system built on Apache Kafka and Flink. It allows pluggable matchmaking strategies to be applied to incoming player streams with minimal infrastructure setup.

---

## 🚀 Features

- Stream-based matchmaking using Apache Flink
- Pluggable matchmaking strategies (e.g., pair by MMR)
- Kafka-based player input and match output pipelines
- Minimal deployment with Docker Compose
- Ready for metrics, fault tolerance, and scaling (coming next)

---

## 🛠️ Requirements

- [Java 17+](https://adoptium.net/)
- [Apache Maven](https://maven.apache.org/)
- [Docker](https://www.docker.com/)
- [Docker Compose](https://docs.docker.com/compose/)

---

## 🗂️ Project Structure

thamm/
├── src/                 # Flink job source code
├── strategies/          # Pluggable strategy implementations
├── types/               # Player and Match types
├── docker/              # Kafka, Zookeeper, Flink setup
├── pom.xml              # Maven dependency and build config
└── README.md            # You’re reading this

---

## ⚙️ Step-by-Step Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-username/thamm
cd thamm
```

### 2. Start the infrastructure stack

```bash
cd docker
docker-compose up -d
```

This launches:
	•	Kafka on localhost:9092
	•	ZooKeeper on localhost:2181
	•	Flink JobManager UI at localhost:8081

📌 Wait a few seconds for all services to fully start.

⸻

### 3. Build the project with Maven

```bash
cd ..
mvn clean package
```

This will create target/thamm-1.0-SNAPSHOT.jar.

⸻

### 4. Run the THAMM Flink job (local mode)

```bash
java -jar target/thamm-1.0-SNAPSHOT.jar --kafka localhost:9092
```

This will:
	•	Listen to player join events on Kafka topic player-events
	•	Apply the matchmaking strategy
	•	Output matches to Kafka topic match-events

⸻

### 👥 Simulate Player Input (Coming Next)

In development: a simple Kafka producer will push fake players into player-events.

For now, you can use the Kafka CLI or a Python script to send sample events.

⸻

## 🧪 Test Your Setup

To see results, try:
```bash
docker exec -it docker-kafka-1 \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic match-events --from-beginning
```

⸻

## 📈 Flink Dashboard

Visit http://localhost:8081 to view:
	•	Task status
	•	Job graph
	•	Event throughput

⸻

## ✅ Next Goals
	•	Add stateful buffering and window-based control
	•	Enable checkpointing and fault tolerance
	•	Create multiple strategy modules
	•	Add Grafana + Prometheus integration for metrics
	•	Stress test with high player input volume

⸻

## 🧠 Authors

Built with 💡 for a graduate-level middleware systems course at UC Irvine.

⸻

## 📜 License

MIT — feel free to use, modify, or build on top of THAMM.

---