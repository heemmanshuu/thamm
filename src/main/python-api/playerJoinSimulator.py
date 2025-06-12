import asyncio
import random
import time
import httpx

API_URL = "http://localhost:8000/join-queue"
REGIONS = ['NA', 'EU', 'ASIA', 'SA', 'AF']
MEAN_MMR = 1500
STDDEV_MMR = 300

def generate_mmr():
    mmr = int(random.gauss(MEAN_MMR, STDDEV_MMR))
    return max(0, min(3000, mmr))

async def send_player(session, i):
    player = {
        "id": str(i),
        "mmr": generate_mmr(),
        "region": random.choice(REGIONS)
        #"timestamp": time.time()
    }
    try:
        response = await session.post(API_URL, json=player)
        if response.status_code != 200:
            print(f"[ERROR] Player {i} failed: {response.text}")
    except Exception as e:
        print(f"[EXCEPTION] Player {i} failed: {e}")

async def main():
    async with httpx.AsyncClient() as session:
        tasks = [send_player(session, i) for i in range(1000)]
        for i in range(0, 1000, 100):  # chunk in 100s to avoid overwhelming the server
            await asyncio.gather(*tasks[i:i+100])
            print(f"Sent {i + 100} players...")

asyncio.run(main())