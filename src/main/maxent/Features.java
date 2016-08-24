package maxent;

import datagen.GalacticGen;
import util.Constant;
import util.Util;

import java.util.*;
import java.util.Map.Entry;

/**
 * Find surface features of sentences
 * -Normalized counts of unigrams, bigrams, trigrams of tags
 * -conditional counts of bigrams
 *
 * @author Sharon
 * @author wdd
 */
public class Features {
    public static double getScore(String headerPos, Map<String, Double> feature, Map<String, Double> model) {
        double alpha = 0.;
        for (String feat : feature.keySet()) {
            String k = headerPos + Constant.PERM_TAG_DEL + feat;
            if (model.containsKey(k))
                alpha += model.get(k) * feature.get(feat);
        }
        return alpha;
    }

    private static void addBigramFeature(String name, String c1, String c2, Map<String, Double> featMap) {
        String key = name + Constant.CHILDREN_DEL + c1 + Constant.CHILDREN_DEL + c2;
        featMap.put(key, featMap.containsKey(key) ? featMap.get(key) + 1. : 1.);
    }

    private static void delBigramFeature(String name, String c1, String c2, Map<String, Double> featMap) {
        String key = name + Constant.CHILDREN_DEL + c1 + Constant.CHILDREN_DEL + c2;
        assert featMap.containsKey(key);
        featMap.put(key, featMap.get(key) - 1.);
        if (featMap.get(key) == 0.)
            featMap.remove(key);
    }

    private static double addBigramFeature(String headerPos, String name, String c1, String c2, Map<String, Double> featMap, Map<String, Double> model) {
        String key = name + Constant.CHILDREN_DEL + c1 + Constant.CHILDREN_DEL + c2;
        featMap.put(key, featMap.containsKey(key) ? featMap.get(key) + 1. : 1.);
        String featureKey = headerPos + Constant.PERM_TAG_DEL + key;
        double diff = 0.;
        if (model.containsKey(featureKey))
            diff += model.get(featureKey);
        return diff;
    }

    private static void delBigramFeature(String name, Map<String, Double> featureCount, Map<String, Double> featMap, Set<String> highOrderFeature) {
        for (String k : featureCount.keySet()) {
            String key = name + Constant.CHILDREN_DEL + k;
            if (highOrderFeature.contains(k))
                featMap.remove(key);
        }
    }

    private static void addBigramFeature(String name, Map<String, Double> featureCount, Map<String, Double> featMap, Set<String> highOrderFeature) {
        for (String k : featureCount.keySet()) {
            String key = name + Constant.CHILDREN_DEL + k;
            if (highOrderFeature.contains(k))
                featMap.put(key, featureCount.get(k));
        }
    }

    private static double delBigramFeature(String headerPos, String name, Map<String, Double> featureCount, Map<String, Double> featMap, Map<String, Double> model) {
        double diff = 0.;
        for (String k : featureCount.keySet()) {
            String key = name + Constant.CHILDREN_DEL + k;
            String featureKey = headerPos + Constant.PERM_TAG_DEL + key;
            if (model.containsKey(featureKey))
                diff -= model.get(featureKey) * featureCount.get(k);
            featMap.remove(key);
        }
        return diff;
    }

    private static double addBigramFeature(String headerPos, String name, Map<String, Double> featureCount, Map<String, Double> featMap, Map<String, Double> model) {
        double diff = 0.;
        for (String k : featureCount.keySet()) {
            String key = name + Constant.CHILDREN_DEL + k;
            String featureKey = headerPos + Constant.PERM_TAG_DEL + key;
            if (model.containsKey(featureKey))
                diff += model.get(featureKey) * featureCount.get(k);
            featMap.put(key, featureCount.get(k));
        }
        return diff;
    }

    private static double delBigramFeature(String headerPos, String name, String c1, String c2, Map<String, Double> featMap, Map<String, Double> model) {
        String key = name + Constant.CHILDREN_DEL + c1 + Constant.CHILDREN_DEL + c2;
        assert featMap.containsKey(key);
        featMap.put(key, featMap.get(key) - 1.);
        if (featMap.get(key) == 0.)
            featMap.remove(key);
        String featureKey = headerPos + Constant.PERM_TAG_DEL + key;
        double diff = 0.;
        if (model.containsKey(featureKey))
            diff -= model.get(featureKey);
        return diff;
    }


    public static Entry<Integer, Double> updateListAndGetScoreDiff(String headerPos, String[] children, Map<String, Double> feature, Map<String, Double> model, int pos1, int pos2, boolean allHighOrder, int header) {
        String tk1 = children[pos1], tk2 = children[pos2];
        int len = children.length;
        String prev = pos1 == 0 ? Constant.BOS : children[pos1 - 1];
        String aftr = pos2 == len - 1 ? Constant.EOS : children[pos2 + 1];
        // update bigram feature
        double diff = 0.;
        diff += delBigramFeature(headerPos, "BI", prev, tk1, feature, model);
        diff += delBigramFeature(headerPos, "BI", tk1, tk2, feature, model);
        diff += delBigramFeature(headerPos, "BI", tk2, aftr, feature, model);
        diff += addBigramFeature(headerPos, "BI", prev, tk2, feature, model);
        diff += addBigramFeature(headerPos, "BI", tk2, tk1, feature, model);
        diff += addBigramFeature(headerPos, "BI", tk1, aftr, feature, model);
        // update precedence feature
        diff += delBigramFeature(headerPos, "PRE", tk1, tk2, feature, model);
        diff += addBigramFeature(headerPos, "PRE", tk2, tk1, feature, model);
        // update the head feature
        if (pos2 < header) {
            diff += delBigramFeature(headerPos, "BFR", tk1, tk2, feature, model);
            diff += addBigramFeature(headerPos, "BFR", tk2, tk1, feature, model);
        } else if (pos2 == header) {
            for (int i = pos2 + 1; i < len; ++i) {
                diff += delBigramFeature(headerPos, "BTW", tk1, children[i], feature, model);
                diff += addBigramFeature(headerPos, "AFT", tk1, children[i], feature, model);
            }
            for (int i = 0; i < pos1; ++i) {
                diff += delBigramFeature(headerPos, "BFR", children[i], tk1, feature, model);
                diff += addBigramFeature(headerPos, "BTW", children[i], tk1, feature, model);
            }
            header = pos1;
        } else if (pos1 > header) {
            diff += delBigramFeature(headerPos, "AFT", tk1, tk2, feature, model);
            diff += addBigramFeature(headerPos, "AFT", tk2, tk1, feature, model);
        } else if (pos1 == header) {
            for (int i = pos2 + 1; i < len; ++i) {
                diff += delBigramFeature(headerPos, "AFT", tk2, children[i], feature, model);
                diff += addBigramFeature(headerPos, "BTW", tk2, children[i], feature, model);
            }
            for (int i = 0; i < pos1; ++i) {
                diff += delBigramFeature(headerPos, "BTW", children[i], tk2, feature, model);
                diff += addBigramFeature(headerPos, "BFR", children[i], tk2, feature, model);
            }
            header = pos2;
        }
        Map<String, Double> ngramFeatMap = new HashMap<>();
        if (allHighOrder) {
            Util.collectNGram(children, GalacticGen.higOrderLow, GalacticGen.higOrderUpper, ngramFeatMap); //High order feature
            diff += delBigramFeature(headerPos, "HI", ngramFeatMap, feature, model);
        }
        Util.swap(children, pos1, pos2);
        ngramFeatMap = new HashMap<>();
        if (allHighOrder) {
            Util.collectNGram(children, GalacticGen.higOrderLow, GalacticGen.higOrderUpper, ngramFeatMap); //High order feature
            diff += addBigramFeature(headerPos, "HI", ngramFeatMap, feature, model);
        }
        return new AbstractMap.SimpleEntry<>(header, diff);
    }

    public static int updateList(String[] children, Map<String, Double> feature, int pos1, int pos2, Set<String> highOrderFeature, int header) {
        String tk1 = children[pos1], tk2 = children[pos2];
        int len = children.length;
        String prev = pos1 == 0 ? Constant.BOS : children[pos1 - 1];
        String aftr = pos2 == len - 1 ? Constant.EOS : children[pos2 + 1];
        // update bigram feature
        delBigramFeature("BI", prev, tk1, feature);
        delBigramFeature("BI", tk1, tk2, feature);
        delBigramFeature("BI", tk2, aftr, feature);
        addBigramFeature("BI", prev, tk2, feature);
        addBigramFeature("BI", tk2, tk1, feature);
        addBigramFeature("BI", tk1, aftr, feature);
        // update precedence feature
        delBigramFeature("PRE", tk1, tk2, feature);
        addBigramFeature("PRE", tk2, tk1, feature);
        // update the head feature
        if (pos2 < header) {
            delBigramFeature("BFR", tk1, tk2, feature);
            addBigramFeature("BFR", tk2, tk1, feature);
        } else if (pos2 == header) {
            for (int i = pos2 + 1; i < len; ++i) {
                delBigramFeature("BTW", tk1, children[i], feature);
                addBigramFeature("AFT", tk1, children[i], feature);
            }
            for (int i = 0; i < pos1; ++i) {
                delBigramFeature("BFR", children[i], tk1, feature);
                addBigramFeature("BTW", children[i], tk1, feature);
            }
            header = pos1;
        } else if (pos1 > header) {
            delBigramFeature("AFT", tk1, tk2, feature);
            addBigramFeature("AFT", tk2, tk1, feature);
        } else if (pos1 == header) {
            for (int i = pos2 + 1; i < len; ++i) {
                delBigramFeature("AFT", tk2, children[i], feature);
                addBigramFeature("BTW", tk2, children[i], feature);
            }
            for (int i = 0; i < pos1; ++i) {
                delBigramFeature("BTW", children[i], tk2, feature);
                addBigramFeature("BFR", children[i], tk2, feature);
            }
            header = pos2;
        }
        Map<String, Double> ngramFeatMap = new HashMap<>();
        if (highOrderFeature != null) {
            Util.collectNGram(children, GalacticGen.higOrderLow, GalacticGen.higOrderUpper, ngramFeatMap); //High order feature
            delBigramFeature("HI", ngramFeatMap, feature, highOrderFeature);
        }
        Util.swap(children, pos1, pos2);
        ngramFeatMap = new HashMap<>();
        if (highOrderFeature != null) {
            Util.collectNGram(children, GalacticGen.higOrderLow, GalacticGen.higOrderUpper, ngramFeatMap); //High order feature
            addBigramFeature("HI", ngramFeatMap, feature, highOrderFeature);
        }
        return header;
    }

    public static Entry<Integer, Double> updateAndGetScoreDiff(String headerPos, String[] children, Map<String, Double> feature, Map<String, Double> model, int pos1, int pos2, int header) {
        double diff = 0.;
        if (GalacticGen.feature.equals("posArc")) {
            int len = children.length;
            String[] posList = new String[len], arcList = new String[len];
            for (int i = 0; i < len; ++i) {
                String[] entry = children[i].split(Constant.TAG_ARC_DEL);
                posList[i] = entry[0];
                arcList[i] = entry[1];
            }
            diff += updateListAndGetScoreDiff(headerPos, posList, feature, model, pos1, pos2, false, header).getValue();
            diff += updateListAndGetScoreDiff(headerPos, arcList, feature, model, pos1, pos2, false, header).getValue();
        }
        Entry<Integer, Double> ent = updateListAndGetScoreDiff(headerPos, children, feature, model, pos1, pos2, true, header);
        return new AbstractMap.SimpleEntry<>(ent.getKey(), diff + ent.getValue());
    }

    public static int update(String[] children, Map<String, Double> feature, int pos1, int pos2, Set<String> highOrderFeature, int header) {
        if (GalacticGen.feature.equals("posArc")) {
            int len = children.length;
            String[] posList = new String[len], arcList = new String[len];
            for (int i = 0; i < len; ++i) {
                String[] entry = children[i].split(Constant.TAG_ARC_DEL);
                posList[i] = entry[0];
                arcList[i] = entry[1];
            }
            updateList(posList, feature, pos1, pos2, null, header);
            updateList(arcList, feature, pos1, pos2, null, header);
        }
        return updateList(children, feature, pos1, pos2, highOrderFeature, header);
    }

    public static Map<String, Double> extractNodeFeatures(String[] children) {
        Map<String, Double> featMap = new HashMap<>();
        featMap.putAll(extractNodeListFeatures(children));
        if (GalacticGen.feature.equals("posArc")) {
            int len = children.length;
            String[] posList = new String[len], arcList = new String[len];
            for (int i = 0; i < len; ++i) {
                String[] entry = children[i].split(Constant.TAG_ARC_DEL);
                posList[i] = entry[0];
                arcList[i] = entry[1];
            }
            featMap.putAll(extractNodeListFeatures(posList, null));
            featMap.putAll(extractNodeListFeatures(arcList, null));
        }
        return featMap;
    }

    public static Map<String, Double> extractNodeFeatures(String[] children, Set<String> highOrderFeature) {
        Map<String, Double> featMap = new HashMap<>();
        featMap.putAll(extractNodeListFeatures(children, highOrderFeature));
        if (GalacticGen.feature.equals("posArc")) {
            int len = children.length;
            String[] posList = new String[len], arcList = new String[len];
            for (int i = 0; i < len; ++i) {
                String[] entry = children[i].split(Constant.TAG_ARC_DEL);
                posList[i] = entry[0];
                arcList[i] = entry[1];
            }
            featMap.putAll(extractNodeListFeatures(posList, null));
            featMap.putAll(extractNodeListFeatures(arcList, null));
        }
        return featMap;
    }

    public static Map<String, Double> extractNodeListFeatures(String[] children) {
        Map<String, Double> featMap = new TreeMap<>();
        Map<String, Double> bigramFeatMap = new HashMap<>();
        int headerIdx = 0;
        for (; headerIdx < children.length; ++headerIdx)
            if (children[headerIdx].startsWith(Constant.HEADER_MARK))
                break;
        Util.collectNGram(children, 2, 2, bigramFeatMap);//Bigram feature
        for (String k : bigramFeatMap.keySet())
            featMap.put("BI_" + k, bigramFeatMap.get(k));
        Map<String, Double> ngramFeatMap = new HashMap<>();
        Util.collectNGram(children, GalacticGen.higOrderLow, GalacticGen.higOrderUpper, ngramFeatMap); //High order feature
        for (String k : ngramFeatMap.keySet())
            featMap.put("HI_" + k, ngramFeatMap.get(k));
        for (int i = 0; i < children.length; ++i) //precedence feature
            for (int j = i + 1; j < children.length; ++j)
                addBigramFeature("PRE", children[i], children[j], featMap);
        for (int i = 0; i < headerIdx; ++i) //precedence feature
            for (int j = i + 1; j < headerIdx; ++j)
                addBigramFeature("BFR", children[i], children[j], featMap);
        for (int i = 0; i < headerIdx; ++i) //precedence feature
            for (int j = headerIdx + 1; j < children.length; ++j)
                addBigramFeature("BTW", children[i], children[j], featMap);
        for (int i = headerIdx + 1; i < children.length; ++i) //precedence feature
            for (int j = i + 1; j < children.length; ++j)
                addBigramFeature("AFT", children[i], children[j], featMap);
        return featMap;
    }

    public static Map<String, Double> extractNodeListFeatures(String[] children, Set<String> highOrderFeature) {
        Map<String, Double> featMap = new TreeMap<>();
        Map<String, Double> bigramFeatMap = new HashMap<>();
        int headerIdx = 0;
        for (; headerIdx < children.length; ++headerIdx)
            if (children[headerIdx].startsWith(Constant.HEADER_MARK))
                break;
        Util.collectNGram(children, 2, 2, bigramFeatMap);//Bigram feature
        for (String k : bigramFeatMap.keySet())
            featMap.put("BI_" + k, bigramFeatMap.get(k));
        Map<String, Double> ngramFeatMap = new HashMap<>();
        if (highOrderFeature != null) {
            Util.collectNGram(children, GalacticGen.higOrderLow, GalacticGen.higOrderUpper, ngramFeatMap); //High order feature
            for (String k : ngramFeatMap.keySet())
                if (highOrderFeature.contains(k))
                    featMap.put("HI_" + k, ngramFeatMap.get(k));
        }
        for (int i = 0; i < children.length; ++i) //precedence feature
            for (int j = i + 1; j < children.length; ++j)
                addBigramFeature("PRE", children[i], children[j], featMap);
        for (int i = 0; i < headerIdx; ++i) //precedence feature
            for (int j = i + 1; j < headerIdx; ++j)
                addBigramFeature("BFR", children[i], children[j], featMap);
        for (int i = 0; i < headerIdx; ++i) //precedence feature
            for (int j = headerIdx + 1; j < children.length; ++j)
                addBigramFeature("BTW", children[i], children[j], featMap);
        for (int i = headerIdx + 1; i < children.length; ++i) //precedence feature
            for (int j = i + 1; j < children.length; ++j)
                addBigramFeature("AFT", children[i], children[j], featMap);
        return featMap;
    }

    public static Map<String, Double> marginProbGrams(String[] tkList, int n) {
        Map<String, Double> ret = new HashMap<>();
        double norm = 0.0;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < tkList.length - n + 1; ++i) {
            for (int j = i; j < i + n; ++j)
                sb.append(" " + tkList[j]);
            String gram = sb.toString().trim();
            norm += 1.0;
            ret.put(gram, ret.containsKey(gram) ? ret.get(gram) + 1.0 : 1.0);
        }
        for (String gram : ret.keySet())
            ret.put(gram, ret.get(gram) / norm);
        return ret;
    }


}
