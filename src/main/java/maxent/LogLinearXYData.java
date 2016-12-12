package maxent;

import edu.jhu.pacaya.gm.feat.FeatureVector;
import edu.jhu.prim.bimap.IntObjectBimap;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for log-linear model instances, specifying features of the
 * observed variable, x, and a label, y.
 *
 * @author wdd
 */
public class LogLinearXYData {

    public static class LogLinearExample {
        private edu.jhu.pacaya.gm.maxent.LogLinearXYData.LogLinearExample example;
        private int numYs;

        public LogLinearExample(double weight, int x, int y, FeatureVector[] fvs) {
            example = new edu.jhu.pacaya.gm.maxent.LogLinearXYData.LogLinearExample(weight, x, y, fvs);
            numYs = fvs.length;
        }

        public double getWeight() {
            return example.getWeight();
        }

        public int getX() {
            return example.getX();
        }

        public int getY() {
            return example.getY();
        }

        public edu.jhu.pacaya.gm.maxent.LogLinearXYData.LogLinearExample getExample() {
            return example;

        }

        public int getNumYs() {
            return numYs;
        }

    }

    private List<LogLinearExample> exList;
    private IntObjectBimap<String> featureAlphabet;


    public LogLinearXYData(IntObjectBimap<String> featureAlphabet) {
        this.featureAlphabet = featureAlphabet;
        this.exList = new ArrayList<>();
    }

    /**
     * Adds a new log-linear model instance.
     *
     * @param weight The weight of this example.
     * @param x      The observation, x.
     * @param y      The prediction, y.
     * @param fvs    The binary features on the observations, x, for all possible labels, y'. Indexed by y'.
     */
    public void addEx(double weight, int x, int y, FeatureVector[] fvs) {
        LogLinearExample ex = new LogLinearExample(weight, x, y, fvs);
        exList.add(ex);
    }

    public int size() {
        return exList.size();
    }

    public LogLinearExample get(int i) {
        return exList.get(i);
    }

    public IntObjectBimap<String> getFeatureAlphabet() {
        return featureAlphabet;
    }

    public List<LogLinearExample> getData() {
        return exList;
    }
}