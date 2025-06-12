from fastapi import FastAPI
from pydantic import BaseModel
from confluent_kafka import Producer
from mmrBucketizer import MMRBucketizer
import json
import asyncio
import threading
import time

app = FastAPI()
message_queue = asyncio.Queue()

# Kafka config
producer = Producer({'bootstrap.servers': 'localhost:9092'})
topic = "matchmaking-system-7"
num_partitions = 10 
bucketizer = MMRBucketizer(mean=1500.0, stddev=300.0, k=num_partitions)

# Define the expected player input schema
class Player(BaseModel):
    id: str
    mmr: float
    region: str

@app.post("/join-queue")
async def join_queue(player: Player):
    # Determine the partition
    partition = bucketizer.get_bucket(player.mmr)
    player_data = json.dumps(player.model_dump())

    # Produce to Kafka partition
    producer.produce(topic, value=player_data, partition=partition, callback=lambda err, msg: print(f"[Kafka] Error: {err}" if err else f"[Kafka] Sent to partition {msg.partition()}"))
    producer.flush()

    return {"status": "queued", "partition": partition}