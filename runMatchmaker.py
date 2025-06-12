import asyncio
import httpx
import json
import time
from kafka import KafkaConsumer
from threading import Thread

API_URL = "http://localhost:8000/join-queue"
KAFKA_TOPIC = "match-results-1"
KAFKA_BOOTSTRAP = "localhost:9092"

def collect_results(results_list, timeout=30):
    consumer = KafkaConsumer(
        KAFKA_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP,
        auto_offset_reset='earliest',
        enable_auto_commit=True,
        group_id='thamm-result-analyzer',
        value_deserializer=lambda m: json.loads(m.decode('utf-8'))
    )

    start_time = time.time()
    print("[Kafka] Listening to match-results...")

    while time.time() - start_time < timeout:
        msg_pack = consumer.poll(timeout_ms=1000)
        for tp, messages in msg_pack.items():
            for message in messages:
                match = message.value
                match['received_timestamp'] = time.time()
                results_list.append(match)
                print(f"[Kafka] Match: {match}")

async def send_players(ts_map):
    with open("players.json") as f:
        players = json.load(f)

    async with httpx.AsyncClient() as session:
        tasks = [send_player(session, player, ts_map) for player in players]
        for i in range(0, len(players), 100):
            await asyncio.gather(*tasks[i:i+100])
            print(f"[HTTP] Sent {min(i + 100, len(players))} players...")

async def send_player(session, player, ts_map):
    timestamp = time.time()
    ts_map[player["id"]] = timestamp
    try:
        response = await session.post(API_URL, json=player)
        if response.status_code != 200:
            print(f"[HTTP ERROR] Player {player['id']} failed: {response.text}")
    except Exception as e:
        print(f"[HTTP EXCEPTION] Player {player['id']} failed: {e}")

def main():
    ts_map = {}
    match_results = []

    # Start Kafka consumer in a separate thread
    kafka_thread = Thread(target=collect_results, args=(match_results,))
    kafka_thread.start()

    # Run asyncio part to send players
    asyncio.run(send_players(ts_map))

    # Wait for Kafka thread to finish
    kafka_thread.join()

    # Save logs
    with open("eomm_player_timestamps.json", "w") as f:
        json.dump(ts_map, f, indent=2)

    with open("eomm_match_results.json", "w") as f:
        json.dump(match_results, f, indent=2)

    print("[Done] Sent players and collected match results.")

if __name__ == "__main__":
    main()