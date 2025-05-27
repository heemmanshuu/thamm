import org.apache.commons.math3.distribution.NormalDistribution;


// this class helps determine the "buckets" that make matchmaking fair. It uses the mean and standard deviation to determine skill buckets for the playerbase.
public class MMRBucketizer implements java.io.Serializable{
    private final double[] boundaries;  // size = k+1
    private final int k;

    public MMRBucketizer(double mean, double stddev, int k) {
        this.k = k;
        this.boundaries = new double[k+1];
        NormalDistribution nd = new NormalDistribution();

        for (int i = 0; i <= k; i++) {
            double quantile = (double) i / k;
            double z = nd.inverseCumulativeProbability(quantile);
            boundaries[i] = mean + stddev * z;
        }
    }

    public int getBucket(double mmr) {
        // Binary search boundaries to find bucket index
        int low = 0, high = k;
        while (low < high) {
            int mid = (low + high) / 2;
            if (mmr < boundaries[mid]) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return Math.max(0, low - 1);
    }
}