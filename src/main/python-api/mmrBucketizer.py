from scipy.stats import norm
import bisect

class MMRBucketizer:
    def __init__(self, mean: float, stddev: float, k: int):
        self.k = k
        self.boundaries = [mean + stddev * norm.ppf(i / k) for i in range(k + 1)]

    def get_bucket(self, mmr: float) -> int:
        index = bisect.bisect_right(self.boundaries, mmr) - 1
        return max(0, min(index, self.k - 1))