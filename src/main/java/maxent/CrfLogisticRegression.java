package maxent;

import edu.jhu.pacaya.gm.feat.FeatureVector;
import edu.jhu.pacaya.gm.maxent.LogLinearXY.LogLinearXYPrm;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.prim.bimap.IntObjectBimap;
import edu.jhu.prim.vector.IntDoubleVector;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Wrapper of logistic regression from edu.jhu.gm.maxent.
 *
 * @author wdd
 */
public class CrfLogisticRegression {

    private static final Logger log = Logger.getLogger(CrfLogisticRegression.class);

    private static final double DEFAULT_WEIGHT = 1.0;
    private FgModel model;
    private LogLinearXY maxent;

    private final static int IGNORED_Y = 0;
    private final static int IGNORED_X = 0;

    public CrfLogisticRegression(LogLinearXYPrm prm) {
        if (prm == null) prm = new LogLinearXYPrm();
        this.maxent = new LogLinearXY(prm);
    }

    public void train(DataSet trainData) {
        log.info("Number of weighted train instances: " + trainData.size());
        LogLinearXYData data = new LogLinearXYData(trainData.getFeatureAlphabet());
        for (Instance inst : trainData) {
            FeatureVector[] fvs = inst.getFeatures();
            double[] weights = inst.getWeights();
            for (int y = 0; y < fvs.length; ++y) {
                double weight = weights[y];
                if (weight > 0.)
                    data.addEx(weight, IGNORED_X, y, fvs);
            }
        }
        log.info("Training log-linear model.");
        model = maxent.train(data, model);
    }

    public IntDoubleVector getParams() {
        return model.getParams();
    }

    public void initModel(IntObjectBimap<String> featureAlphabet, Map<String, Double> loadedModelParams) {
        model = maxent.load(featureAlphabet, loadedModelParams);
    }

}
