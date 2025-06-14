import json
import psycopg2
import numpy as np
import os

DB_CONFIG = {
    "dbname": "my_database",
    "user": "myuser",
    "password": "password",
    "host": "localhost",
    "port": "5432"
}

def load_matches(filename):
    with open(filename, 'r') as f:
        return json.load(f)

def load_timestamps(filename):
    if os.path.exists(filename):
        with open(filename, 'r') as f:
            return json.load(f)
    return {}

def enrich_player(conn, player):
    with conn.cursor() as cur:
        cur.execute("""
            SELECT games_won, games_lost, games_tied, last_game_result, streak_count
            FROM player WHERE player_id = %s
        """, (player['id'],))
        row = cur.fetchone()
        if row:
            player['winCount'], player['lossCount'], player['tieCount'], player['lastGameResult'], player['streak'] = row
    return player

def compute_outcome_probs(p1, p2):
    elo1, elo2 = p1['mmr'], p2['mmr']
    win1 = 1 / (1 + 10 ** ((elo2 - elo1) / 400))
    draw = 0.05
    loss1 = 1 - win1 - draw

    total1 = p1['winCount'] + p1['lossCount'] + p1['tieCount']
    total2 = p2['winCount'] + p2['lossCount'] + p2['tieCount']
    if total1 > 0 and total2 > 0:
        win_ratio1 = p1['winCount'] / total1
        win_ratio2 = p2['winCount'] / total2
        boost = (win_ratio1 - win_ratio2) * 0.1
        win1 += boost
        loss1 -= boost

    if p1['lastGameResult'] == 2:
        win1 += 0.01 * p1['streak']
        loss1 -= 0.005 * p1['streak']
    elif p1['lastGameResult'] == 0:
        loss1 += 0.01 * p1['streak']
        win1 -= 0.005 * p1['streak']

    if p2['lastGameResult'] == 2:
        win1 -= 0.01 * p2['streak']
        loss1 += 0.005 * p2['streak']
    elif p2['lastGameResult'] == 0:
        win1 += 0.01 * p2['streak']
        loss1 -= 0.005 * p2['streak']

    win1 = max(0.01, min(0.94, win1))
    loss1 = max(0.01, loss1)
    return win1, draw, loss1

def compute_churn(player, win, draw, loss):
    streak_factor = 1 + (player['streak'] * 0.1)
    churn_loss = loss * (1.2 if player['lastGameResult'] == 0 else 1.0) * streak_factor
    churn_win = win * (0.6 if player['lastGameResult'] == 2 else 0.8)
    churn_draw = draw * 0.7
    return churn_loss + churn_draw - churn_win

def analyze_matches(matches, conn, timestamps):
    mmr_diffs = []
    churn_values = []
    latencies = []
    timestamps_list = []

    for match in matches:
        p1 = enrich_player(conn, match['player1'])
        p2 = enrich_player(conn, match['player2'])

        mmr_diff = abs(p1['mmr'] - p2['mmr'])
        mmr_diffs.append(mmr_diff)

        win, draw, loss = compute_outcome_probs(p1, p2)
        churn1 = compute_churn(p1, win, draw, loss)

        win2, draw2, loss2 = 1 - loss, draw, 1 - win
        churn2 = compute_churn(p2, win2, draw2, loss2)

        churn_values.extend([churn1, churn2])

        # Latency: match timestamp - player sent timestamp
        match_ts = match.get('received_timestamp')
        if match_ts:
            for player in (p1, p2):
                sent_ts = timestamps.get(str(player['id']))
                if sent_ts:
                    latency = match_ts - sent_ts
                    if latency >= 0:
                        latencies.append(latency)

            timestamps_list.append(match_ts)

    total_matches = len(matches)
    avg_mmr_diff = np.mean(mmr_diffs)
    std_mmr_diff = np.std(mmr_diffs)

    avg_churn = np.mean(churn_values)
    std_churn = np.std(churn_values)

    avg_latency = np.mean(latencies) if latencies else 0.0
    throughput = total_matches / (max(timestamps_list) - min(timestamps_list)) if len(timestamps_list) > 1 else 0.0

    return {
        "total_matches": total_matches,
        "avg_mmr_discrepancy": avg_mmr_diff,
        "std_mmr_discrepancy": std_mmr_diff,
        "avg_churn_likelihood": avg_churn,
        "std_churn_likelihood": std_churn,
        "avg_latency_per_player": avg_latency,
        "match_throughput_per_sec": throughput
    }

def main():
    conn = psycopg2.connect(**DB_CONFIG)

    matchmaking_types = ["eomm", "sbmm"]
    player_counts = [1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000] #  2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000

    for mm_type in matchmaking_types:
        for num_players in player_counts:
            match_file = f"{mm_type}_{num_players}_player_matches.json"
            ts_file = f"{mm_type}_{num_players}_player_timestamps.json"

            try:
                matches = load_matches(match_file)
                timestamps = load_timestamps(ts_file)
            except FileNotFoundError as e:
                print(f"[WARN] Skipping {mm_type.upper()} {num_players} players: {e}")
                continue

            result = analyze_matches(matches, conn, timestamps)

            print(f"\n📊 Results for {mm_type.upper()} ({num_players} players):")
            for key, val in result.items():
                print(f"{key.replace('_', ' ').capitalize()}: {val:.4f}")

    conn.close()

if __name__ == "__main__":
    main()