# generate_players.py
import json
import random

MEAN_MMR = 1500
STDDEV_MMR = 300

def generate_mmr():
    mmr = int(random.gauss(MEAN_MMR, STDDEV_MMR))
    return max(0, min(3000, mmr))

REGIONS = ['NA', 'EU', 'ASIA', 'SA', 'AF']


players = []
for i in range(1000):
    player = {
        "id": str(i),
        "mmr":  generate_mmr(),
        "region": random.choice(REGIONS)
    }
    players.append(player)

with open("players.json", "w") as f:
    json.dump(players, f, indent=2)