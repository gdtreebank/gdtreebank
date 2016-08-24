package maxent;

import edu.jhu.gm.feat.FeatureVector;

/**
 * A single labeled data instance for a classification or regression problem.
 *
 * @author wdd
 */
public class Instance {

    private FeatureVector[] features;

    private double[] weights;

    private String[] yString;

    private String lhs;

    public Instance(FeatureVector[] features, double[] weights, String lhs, String[] yString) {
        this.features = features;
        this.weights = weights;
        this.lhs = lhs;
        this.yString = yString;
    }
    public Instance(FeatureVector[] features, double[] weights, String[] yString) {
        this.features = features;
        this.weights = weights;
        this.yString = yString;
    }

    public double[] getWeights() {
        return weights;
    }

    public FeatureVector[] getFeatures() {
        return features;
    }

    public String[] getyString() {
        return yString;
    }

    public String getLhs() {

        return lhs;
    }

}
