import json
import statistics

# Load JSON from file
with open('match_results.json', 'r') as f:
    data = json.load(f)

# Calculate MMR discrepancies
mmr_discrepancies = [
    abs(match["player1"]["mmr"] - match["player2"]["mmr"])
    for match in data
]

print(mmr_discrepancies)

# Basic stats
count = len(mmr_discrepancies)
total = sum(mmr_discrepancies)
average = statistics.mean(mmr_discrepancies)
variance = statistics.variance(mmr_discrepancies) if count > 1 else 0
std_dev = statistics.stdev(mmr_discrepancies) if count > 1 else 0
min_discrepancy = min(mmr_discrepancies)
max_discrepancy = max(mmr_discrepancies)
median = statistics.median(mmr_discrepancies)
range_discrepancy = max_discrepancy - min_discrepancy

# Optional: coefficient of variation (CV = std_dev / mean)
cv = std_dev / average if average != 0 else 0

# Print results
print(f"Total matches: {count}")
print(f"Average MMR discrepancy: {average:.2f}")
print(f"Variance: {variance:.2f}")
print(f"Standard Deviation: {std_dev:.2f}")
print(f"Min discrepancy: {min_discrepancy}")
print(f"Max discrepancy: {max_discrepancy}")
print(f"Median: {median}")
print(f"Range: {range_discrepancy}")
print(f"Coefficient of Variation: {cv:.2f}")
