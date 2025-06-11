# generate_players.py
import json
import random

REGIONS = ['NA', 'EU', 'ASIA', 'SA', 'AF']

players = []
for i in range(1000):
    player = {
        "id": str(i),
        "mmr": random.randint(0, 3000),
        "region": random.choice(REGIONS)
    }
    players.append(player)

with open("players.json", "w") as f:
    json.dump(players, f, indent=2)