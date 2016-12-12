package maxent;

import edu.jhu.pacaya.gm.data.FgExampleList;
import edu.jhu.pacaya.gm.data.FgExampleMemoryStore;
import edu.jhu.pacaya.gm.data.LFgExample;
import edu.jhu.pacaya.gm.data.LabeledFgExample;
import edu.jhu.pacaya.gm.decode.MbrDecoder;
import edu.jhu.pacaya.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.pacaya.gm.feat.FeatureVector;
import edu.jhu.pacaya.gm.feat.StringIterable;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.maxent.LogLinearXY.LogLinearXYPrm;
import edu.jhu.pacaya.gm.model.*;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.train.CrfTrainer;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.prim.bimap.IntObjectBimap;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Log-linear model trainer and decoder.
 *
 * @author mgormley
 */
public class LogLinearXY {

    private static final Logger log = Logger.getLogger(LogLinearXY.class);

    private LogLinearXYPrm prm;

    public LogLinearXY(LogLinearXYPrm prm) {
        this.prm = prm;
    }

    private IntObjectBimap<String> alphabet = null;

    private Map<Integer, List<String>> stateNamesMap;


    public FgModel load(IntObjectBimap<String> featureAlphabet, Map<String, Double> param2Weights) {
        alphabet = featureAlphabet;
        stateNamesMap = new HashMap<>();
        FgModel model = new FgModel(featureAlphabet.size(), new StringIterable(featureAlphabet.getObjects()));
        for (int i = 0; i < featureAlphabet.size(); ++i) {
            String featureName = featureAlphabet.lookupObject(i);
            if (param2Weights.containsKey(featureName))
                model.add(i, param2Weights.get(featureName));
        }
        return model;
    }

    /**
     * Trains a log-linear model.
     *
     * @param data The log-linear model training examples created by
     *             LogLinearData.
     * @return Trained model.
     */
    public FgModel train(LogLinearXYData data, FgModel model) {
        IntObjectBimap<String> alphabet = data.getFeatureAlphabet();
        if (this.alphabet == null) {
            this.alphabet = alphabet;
            this.stateNamesMap = new HashMap<>();
        }
        FgExampleList list = getData(data);
        log.info("Number of train instances: " + list.size());
        log.info("Number of model parameters: " + alphabet.size());
        if (model == null)
            model = new FgModel(alphabet.size(), new StringIterable(alphabet.getObjects()));
        CrfTrainer trainer = new CrfTrainer(prm.crfPrm);
        trainer.train(model, list, null);
        return model;
    }

    /**
     * Decodes a single example.
     *
     * @param model The log-linear model.
     * @param llex  The example to decode.
     * @return A pair containing the most likely label (i.e. value of y) and the
     * distribution over y values.
     */
    public VarTensor decode(FgModel model, LogLinearXYData.LogLinearExample llex) {
        LFgExample ex = getFgExample(llex);
        MbrDecoderPrm prm = new MbrDecoderPrm();
        prm.infFactory = getBpPrm();
        MbrDecoder decoder = new MbrDecoder(prm);
        decoder.decode(model, ex);
        List<VarTensor> marginals = decoder.getVarMarginals();
        if (marginals.size() != 1)
            throw new IllegalStateException("Example is not from a LogLinearData factory");
        return marginals.get(0);
    }

    /**
     * For testing only. Converts to the graphical model's representation of the data.
     */
    public FgExampleList getData(LogLinearXYData data) {
        List<LogLinearXYData.LogLinearExample> exList = data.getData();
        // Because we don't directly support weights in the CrfObjective,
        // we instead just add multiple examples.
        FgExampleMemoryStore store = new FgExampleMemoryStore();
        for (final LogLinearXYData.LogLinearExample desc : exList) {
            LFgExample ex = getFgExample(desc);
            store.add(ex);
        }
        return store;
    }

    private LFgExample getFgExample(final LogLinearXYData.LogLinearExample desc) {
        if (alphabet == null) {
            throw new IllegalStateException("decode can only be called after train");
        }
        Var v0 = getVar(desc.getNumYs());
        final VarConfig trainConfig = new VarConfig();
        trainConfig.put(v0, desc.getY());

        FactorGraph fg = new FactorGraph();
        VarSet vars = new VarSet(v0);
        ExpFamFactor f0 = new ExpFamFactor(vars) {

            @Override
            public FeatureVector getFeatures(int config) {
                return desc.getExample().getFeatures(config);
            }

        };
        fg.addFactor(f0);
        LabeledFgExample ex = new LabeledFgExample(fg, trainConfig);
        ex.setWeight(desc.getWeight());
        return ex;
    }

    private Var getVar(int numYs) {
        List<String> stateNames;
        if (stateNamesMap.containsKey(numYs)) {
            stateNames = stateNamesMap.get(numYs);
        } else {
            stateNames = new ArrayList<>();
            for (int y = 0; y < numYs; ++y)
                stateNames.add("" + y);
            stateNamesMap.put(numYs, stateNames);
        }
        return new Var(VarType.PREDICTED, stateNames.size(), "v0", stateNames);
    }

    private BeliefPropagationPrm getBpPrm() {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.s = LogSemiring.getInstance();
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        return bpPrm;
    }

}
