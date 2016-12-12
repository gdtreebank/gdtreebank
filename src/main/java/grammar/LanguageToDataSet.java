package grammar;

import datagen.GalacticGen;
import edu.jhu.pacaya.gm.feat.FeatureVector;
import edu.jhu.prim.bimap.IntObjectBimap;
import edu.jhu.prim.util.Lambda;
import maxent.DataSet;
import maxent.Features;
import maxent.Instance;
import org.apache.log4j.Logger;
import util.Constant;
import util.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Generate a dataset of training or test instances from a language, which
 * consists of a corpus of sentences and the PCFG used to generate the corpus.
 *
 * @author mgormley
 * @author Sharon
 * @author wdd
 */
public class LanguageToDataSet {

    private static final Logger log = Logger.getLogger(LanguageToDataSet.class);

    public static class CollectPermutation implements Lambda.FnO1ToVoid<NaryTree> {

        public Map<String, Map<String, Double>> getData() {
            return data;
        }

        public Map<String, Double> getFreq() {
            return freq;
        }

        Map<String, Map<String, Double>> data;
        Map<String, Double> freq;

        public CollectPermutation() {
            this.data = new HashMap<>();
            this.freq = new HashMap<>();
        }

        @Override
        public void call(NaryTree node) {
            if (!node.isLeaf() && (node.isNoun() && GalacticGen.targetNode.equals(Constant.NOUN)
                    || (node.isVerb() && GalacticGen.targetNode.equals(Constant.VERB)))) {
                String[] childrenAsString;
                if ("arc".equals(GalacticGen.feature))
                    childrenAsString = node.getChildrenArcAsString();
                else if ("pos".equals(GalacticGen.feature))
                    childrenAsString = node.getChildrenPOSAsString();
                else
                    childrenAsString = node.getChildrenAsString();
                if (childrenAsString.length < 2 || childrenAsString.length > GalacticGen.maxChildrenNum)
                    return;
                node.getChildrenAsString();
                Util.collectNGram(childrenAsString, GalacticGen.higOrderLow, GalacticGen.higOrderUpper, freq);
                String orderedKey = Util.join(Constant.CHILDREN_DEL, childrenAsString);
                Arrays.sort(childrenAsString);
                String unorderedKey = Util.join(Constant.CHILDREN_DEL, childrenAsString);
                Map<String, Double> orderedMap = data.containsKey(unorderedKey) ? data.get(unorderedKey) : new HashMap<String, Double>();
                orderedMap.put(orderedKey, orderedMap.containsKey(orderedKey) ? orderedMap.get(orderedKey) + 1. : 1.);
                data.put(unorderedKey, orderedMap);
            }
        }
    }

    public static DataSet languagesToDataSetFast(TreeBank treeBank, IntObjectBimap<String> featAlphabet) throws IOException {
        DataSet dataSet = new DataSet();
        CollectPermutation collectPermutation = new CollectPermutation();
        for (NaryTree tree : treeBank)
            tree.preOrderTraversal(collectPermutation);
        Map<String, Map<String, Double>> data = collectPermutation.getData();
        Map<String, Double> freq = collectPermutation.getFreq();
        List<Entry<String, Double>> sorted = Util.entriesSortedByValues(freq);
        Set<String> highOrderFeature = new HashSet<>();
        for (int i = 0; i < sorted.size() * 0.1; ++i)
            highOrderFeature.add(sorted.get(i).getKey());
        for (String unordered : data.keySet()) {
            String[] children = unordered.split(Constant.CHILDREN_DEL);
            Map<String, Double> features = Features.extractNodeFeatures(children, highOrderFeature);
            Map<String, Double> order2Weight = data.get(unordered);
            int totalLength = children.length;
            int factNumber = Util.factorial(totalLength);
            int headerIdx = Util.getHeaderIdx(children);
            ArrayList<Entry<Integer, Integer>> swapList = Util.getSwapPos(totalLength);
            assert swapList.size() == factNumber;
            FeatureVector[] fvs = new FeatureVector[factNumber];
            double[] weights = new double[factNumber];
            String[] rhss = new String[factNumber];
            int yIdx = 0;
            for (Entry<Integer, Integer> ent : swapList) {
                String orderedStr = Util.join(Constant.CHILDREN_DEL, children);
                Double weight = order2Weight.containsKey(orderedStr) ? order2Weight.get(orderedStr) : 0.;
                Map<Integer, Double> nodeFeature = Util.toIntegerMap(features, featAlphabet);
                FeatureVector fv = new FeatureVector();
                addFeatures(fv, nodeFeature);
                fvs[yIdx] = fv;
                weights[yIdx] = weight;
                rhss[yIdx++] = orderedStr;
                dataSet.add(new Instance(fvs, weights, rhss));
                if (yIdx == factNumber) break;
                int pos1 = ent.getKey(), pos2 = ent.getValue();
                features = new HashMap<>(features);
                headerIdx = Features.update(children, features, pos1, pos2, highOrderFeature, headerIdx);
            }
            assert yIdx == factNumber;
        }
        dataSet.setFeatureAlphabet(featAlphabet);
        return dataSet;
    }

    private static void addFeatures(FeatureVector fv, Map<Integer, Double> featureMap) throws FileNotFoundException {
        for (Map.Entry<Integer, Double> entry : featureMap.entrySet()) {
            addFeat(fv, entry.getKey(), entry.getValue());
        }
    }

    private static void addFeat(FeatureVector fv, int fid, double value) {
        // Don't add features not in the alphabet.
        if (fid != -1) {
            assert !Double.isNaN(value) : "value=" + value;
            assert !Double.isInfinite(value) : "value=" + value;
            fv.add(fid, value);
        }
    }


}
