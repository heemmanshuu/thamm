package types;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.Serializable;
import java.util.Arrays;

public class MMRBucketizer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int k;
    private final double[] boundaries;

    public MMRBucketizer(double mean, double stddev, int k) {
        this.k = k;
        this.boundaries = new double[k + 1];
        NormalDistribution normDist = new NormalDistribution();

        for (int i = 0; i <= k; i++) {
            double quantile = i / (double) k;
            boundaries[i] = mean + stddev * normDist.inverseCumulativeProbability(quantile);
        }
    }

    public int getBucket(double mmr) {
        int index = Arrays.binarySearch(boundaries, mmr);
        if (index < 0) {
            index = -index - 2;
        }
        if (index < 0) return 0;
        if (index >= k) return k - 1;
        return index;
    }
}