package grammar;


import datagen.GalacticGen;
import dist.Sampler;
import edu.jhu.prim.util.Lambda;
import maxent.Features;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import util.Constant;
import util.Util;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * N-ary tree for a context free grammar.
 *
 * @author wdd
 */
public class TreeBank implements Iterable<NaryTree> {

    private static final Logger log = Logger.getLogger(TreeBank.class);
    private List<NaryTree> trees;
    private String rootSymbol;
    public static String DEFAULT_ROOT = "ROOT";

    public TreeBank subTb(int start, int end) {
        return new TreeBank(trees.subList(start, end), rootSymbol);
    }

    public List<TreeBank> split(double rate) {
        int pivot = (int) (trees.size() * rate);
        List<TreeBank> ltb = new ArrayList<>();
        ltb.add(new TreeBank(trees.subList(0, pivot), rootSymbol));
        ltb.add(new TreeBank(trees.subList(pivot, trees.size()), rootSymbol));
        return ltb;
    }

    public String getRootSymbol() {
        return rootSymbol;
    }

    public NaryTree get(int idx) {
        return trees.get(idx);
    }

    public int size() {
        return trees.size();
    }

    public TreeBank(List<NaryTree> trees, String rootSymbol) {
        this.trees = trees;
        this.rootSymbol = rootSymbol;
    }

    public TreeBank(String rootSymbol) {
        this.rootSymbol = rootSymbol;
        this.trees = new ArrayList<>();
    }

    public void readTreesInPtbFormat(File treeBankFile) throws IOException {
        FileReader reader = new FileReader(treeBankFile);
        trees = readTreesInPtbFormat(reader);
        reader.close();
    }

    public void cut(int numTrees) {
        if (numTrees < trees.size())
            trees = trees.subList(0, numTrees);
    }

    public void readTreesInUniversalConLLFormat(File treeBankFile) throws FileNotFoundException {
        readTreesInUniversalConLLFormat(Integer.MAX_VALUE, treeBankFile);
    }

    public void readTreesInUniversalConLLFormat(int maxsent, File treeBankFile) throws FileNotFoundException {
        log.info("Loading proposal grammar from Standard TreeBank:" + treeBankFile.getName());
        trees = new ArrayList<>();
        Scanner scanner = new Scanner(treeBankFile);
        ArrayList<String> lines = new ArrayList<>();
        Map<Integer, Integer> trmap = new TreeMap<>();
        int val = 0, total = 0, lineNumber = 0, startLineNumber = -1;
        NaryTree tree;
        StringBuffer comments = new StringBuffer();
        while (scanner.hasNext()) {
            lineNumber += 1;
            String line = scanner.nextLine().trim();
            if (line.startsWith("#")) {
                comments.append("# " + Constant.srcName + ":" + Integer.toString(lineNumber) + ":" + line + "\n");
                continue;
            }
            if (line.equals("")) {
                total += 1;
                int len = lines.size();
                trmap.put(len, trmap.containsKey(len) ? trmap.get(len) + 1 : 1);
                try {
                    tree = NaryTree.readTreeInUniversalConLLFormat(lines, startLineNumber);
                    tree.info = comments.toString() + tree.info;
                    val += 1;
                    trees.add(tree);
                    if (trees.size() == maxsent) {
                        log.info(Integer.toString(val) + "/" + Integer.toString(total) + " are valid trees");
                        return;
                    }

                } catch (InvalidTreeException e) {
                }
                startLineNumber = -1;
                lines.clear();
                comments = new StringBuffer();
            } else {
                if (startLineNumber == -1)
                    startLineNumber = lineNumber;
                lines.add(line);
            }
        }
        try {
            total += 1;
            tree = NaryTree.readTreeInUniversalConLLFormat(lines, startLineNumber);
            tree.info = comments.toString() + tree.info;
            val += 1;
            trees.add(tree);

        } catch (InvalidTreeException e) {
        }
        log.info(Integer.toString(val) + "/" + Integer.toString(total) + " are valid trees");
    }

    public TreeBank(Reader reader, String rootSymbol) throws IOException {
        this.rootSymbol = rootSymbol;
        trees = readTreesInPtbFormat(reader);
    }

    public void add(NaryTree tree) {
        trees.add(tree);
    }

    public Grammar genRuleCounts() {
        List<Rule> allRules = new ArrayList<>();
        for (NaryTree tree : trees) {
            allRules.add(new Rule(rootSymbol, tree.getSymbol(), 1.0));
            tree.preOrderTraversal(new CollectRules(allRules));
        }
        return new Grammar(allRules, rootSymbol);
    }

    private static ArrayList<NaryTree> readTreesInPtbFormat(Reader reader) throws IOException {
        ArrayList<NaryTree> trees = new ArrayList<>();
        while (true) {
            NaryTree tree = NaryTree.readTreeInPtbFormat(reader);
            if (tree != null)
                trees.add(tree);
            else
                break;
        }
        return trees;
    }

    public static class Permutation {
        int[] perm;
        Map<String, Double> statistics;

        public Permutation(int[] perm) {
            this.perm = perm;
            this.statistics = new HashMap<>();
        }

        @Override
        public int hashCode() {
            StringBuffer sb = new StringBuffer();
            for (int i : perm)
                sb.append(i + "_");
            return sb.toString().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj.hashCode() == hashCode();
        }

        public int[] getPerm() {
            return perm;
        }

    }


    public TreeBank permuteV5(Random random, Map<String, Double> permute_model) {
        log.info("Generate new TreeBank from Standard TreeBank by permutation");
        TreeBank permutedTreeBank = new TreeBank(DEFAULT_ROOT);
        double cur = 1., total = this.trees.size();
        for (NaryTree naryTree : this) {
            if (Constant.verbose == 1)
                Util.updateProgress(cur++ / total);
            NaryTree naryTreeCopy = naryTree.copy();
            naryTreeCopy.preOrderTraversal(new PermuteChildrenFast(permute_model, random));
            naryTreeCopy.updateStartEnd();
            permutedTreeBank.add(naryTreeCopy);
        }
        System.out.println();
        log.info(Integer.toString(permutedTreeBank.size()) + "/" + Integer.toString((int) total) + " are valid trees");
        return permutedTreeBank;
    }


    public static class InvalidTreeException extends Exception {

    }

    public double avgArcLength() {
        double num = 0., denum = 0.;
        for (NaryTree naryTree : this) {
            List<List<String>> fields = naryTree.toStringInConllFormat();
            for (List<String> field : fields) {
//            0:conllEnt.add(Integer.toString(i + 1)); //    ID: Word index, integer starting at 1 for each new sentence; may be a range for tokens with multiple words.
//            1:conllEnt.add(fields.get(4)); // FORM: Word form or punctuation symbol (Ciphered)
//            2:conllEnt.add(fields.get(4)); // LEMMA: Same as the word for now
//            3:conllEnt.add(fields.get(1)); // UPOS: universal part-of-speech
//            4:conllEnt.add(fields.get(2)); // POSTAG: Fine-grained part-of-speech tag
//            5:conllEnt.add(Integer.toString(leafEnt.getValue() + 1)); // HEAD: not available
//            6:conllEnt.add(fields.get(3)); // DEPREL: Universal Stanford dependency relation to the HEAD (root iff HEAD = 0) or a defined language-specific subtype of one.
//            7:conllEnt.add(fields.get(0));// MISC: Any other annotation. (original word form)
                int head = Integer.parseInt(field.get(6));
                String upos = field.get(3);
                if (head == 0 || upos.equals("PUNCT") || upos.equals(".")) continue;
                int idx = Integer.parseInt(field.get(0));
                num += Math.abs((double) head - idx);
                denum += 1.;
            }
        }
        return num / denum;
    }

    public double condEnt() {
        double ent = 0., perm = 0.;
        for (NaryTree naryTree : this) {
            Map<String, Double> statistics = naryTree.getStatistics();
            if (statistics.containsKey("ent")) {
                ent += statistics.get("ent");
                perm += statistics.get("ent");
            }
        }
        return -ent / perm;
    }

    public double orderingEnt() {
        Map<String, Double> num = new HashMap<>(), denum = new HashMap<>();
        double ent = 0.;
        CollectNumPermutation collectNumPermutation = new CollectNumPermutation(num, denum);
        for (NaryTree tree : this)
            tree.preOrderTraversal(collectNumPermutation);
        for (String orderedRule : num.keySet()) {
            String[] ruleStrList = orderedRule.split(">");
            String lhs = ruleStrList[0];
            String[] rhs = ruleStrList[1].split("_");
            Arrays.sort(rhs);
            String unorderedRule = lhs + ">" + Util.join("_", rhs);
            ent += Util.entropy(num.get(orderedRule), denum.get(unorderedRule));
        }
        double sum = 0.;
        for (String unorderRule : denum.keySet()) {
            String[] ruleStrList = unorderRule.split(">");
            int length = ruleStrList[1].split("_").length;
            sum += denum.get(unorderRule) * Util.logFact(length);
        }
        return -ent / sum;
    }

    private class ChildrenOrderSamplerFast implements Sampler.DiscreteSampler<Permutation> {
        private int[][] orders;
        private Map<Integer, Double> dist;
        private double total;
        private int totalPerm;
        Random random;

        public ChildrenOrderSamplerFast(String headerPos, String[] children, Random random, Map<String, Double> model) {
            this.random = random;
            int len = children.length;
            totalPerm = Util.factorial(len);
            double[] alphaRhs = new double[totalPerm];
            orders = new int[totalPerm][len];
            for (int i = 0; i < len; ++i) orders[0][i] = i;
            Map<String, Double> features = Features.extractNodeFeatures(children);
            alphaRhs[0] = Features.getScore(headerPos, features, model);
            int headerIdx = Util.getHeaderIdx(children);
            ArrayList<Entry<Integer, Integer>> swapList = Util.getSwapPos(len);
            assert swapList.size() == totalPerm;
            int idx = 0;
            for (Entry<Integer, Integer> ent : swapList) {
                if (++idx == totalPerm) break;
                int pos1 = ent.getKey(), pos2 = ent.getValue();
                features = new HashMap<>(features);
                Entry<Integer, Double> diffEntry = Features.updateAndGetScoreDiff(headerPos, children, features, model, pos1, pos2, headerIdx);
                headerIdx = diffEntry.getKey();
                orders[idx] = orders[idx - 1].clone();
                Util.swap(orders[idx], pos1, pos2);
                alphaRhs[idx] = alphaRhs[idx - 1] + diffEntry.getValue();
            }
            double mx = Double.MIN_VALUE;
            for (int i = 0; i < totalPerm; ++i)
                if (alphaRhs[i] > mx)
                    mx = alphaRhs[i];
            dist = new HashMap<>();
            total = 0.;
            for (int i = 0; i < totalPerm; ++i) {
                double factor = Math.exp(alphaRhs[i] - mx);
                dist.put(i, factor);
                total += factor;
            }
        }

        public double getLogProb() {
            return (Math.log(total) - Math.log(dist.get(0))) / Math.log(2.);
        }

        public double getLogUniformProb() {
            return Math.log((double) dist.size()) / Math.log(2.);
        }

        public Permutation sampleOne() {
            int selectedOrderIdx = Sampler.sampleOne(dist, total, random);
            int[] selectedOrder = orders[selectedOrderIdx];
            return new Permutation(selectedOrder);
        }

    }

    private class ModelEvaluator implements Lambda.FnO1ToVoid<NaryTree> {
        private Map<String, Double> model;
        private Map<String, ChildrenOrderSamplerFast> samplerMap;
        private double modelXent, uniformXent;
        private double total;

        public ModelEvaluator(Map<String, Double> model) {
            this.model = model;
            this.modelXent = 0.;
            this.uniformXent = 0.;
            this.samplerMap = new HashMap<>();
        }

        public double getRatio() {
            return modelXent / uniformXent;
        }

        public double getUniform() {
            return uniformXent / total;
        }

        @Override
        public void call(NaryTree node) {
            if (!node.isLeaf() && node.getChildren().size() > 1 &&
                    (node.isNoun() && GalacticGen.permutable.contains(Constant.NOUN)
                            || (node.isVerb() && GalacticGen.permutable.contains(Constant.VERB)))) {
                String[] children;
                if ("arc".equals(GalacticGen.feature))
                    children = node.getChildrenArcAsString();
                else if ("pos".equals(GalacticGen.feature))
                    children = node.getChildrenPOSAsString();
                else
                    children = node.getChildrenAsString();
                String headerPos = node.isVerb() ? Constant.VERB : Constant.NOUN;
                String childrenString = headerPos + Constant.PERM_TAG_DEL + Util.join(Constant.CHILDREN_DEL, children);
                ChildrenOrderSamplerFast sampler;
                if (!samplerMap.containsKey(childrenString)) {
                    sampler = new ChildrenOrderSamplerFast(headerPos, children, null, model);
                    samplerMap.put(childrenString, sampler);
                } else
                    sampler = samplerMap.get(childrenString);
                this.modelXent += sampler.getLogProb();
                this.uniformXent += sampler.getLogUniformProb();
                this.total += 1;
            }
        }

    }

    private class PermuteChildrenFast implements Lambda.FnO1ToVoid<NaryTree> {
        private Random random;
        private Map<String, ChildrenOrderSamplerFast> samplerMap;
        private Map<String, Double> model;

        public PermuteChildrenFast(Map<String, Double> model, Random random) {
            this.random = random;
            this.model = model;
            this.samplerMap = new HashMap<>();
        }

        @Override
        public void call(NaryTree node) {
            if (!node.isLeaf() && node.getChildren().size() > 1 &&
                    (node.isNoun() && GalacticGen.permutable.contains(Constant.NOUN)
                            || (node.isVerb() && GalacticGen.permutable.contains(Constant.VERB)))) {
                String[] children;
                if ("arc".equals(GalacticGen.feature))
                    children = node.getChildrenArcAsString();
                else if ("pos".equals(GalacticGen.feature))
                    children = node.getChildrenPOSAsString();
                else
                    children = node.getChildrenAsString();
                String headerPos = node.isVerb() ? Constant.VERB : Constant.NOUN;
                String childrenString = headerPos + Constant.PERM_TAG_DEL + Util.join(Constant.CHILDREN_DEL, children);
                ChildrenOrderSamplerFast sampler;
                if (!samplerMap.containsKey(childrenString)) {
                    sampler = new ChildrenOrderSamplerFast(headerPos, children, random, model);
                    samplerMap.put(childrenString, sampler);
                } else
                    sampler = samplerMap.get(childrenString);
                Permutation perm = sampler.sampleOne();
                int[] selectedOrder = perm.getPerm();
                List<NaryTree> childrenNodes = node.getChildren();
                ArrayList<NaryTree> permutedChildren = new ArrayList<>();
                for (int i = 0; i < selectedOrder.length; ++i)
                    permutedChildren.add(childrenNodes.get(selectedOrder[i]));
                node.setChildren(permutedChildren);
            }
        }

    }

    public static class CollectNumPermutation implements Lambda.FnO1ToVoid<NaryTree> {

        public Map<String, Double> num, denum;

        public CollectNumPermutation(Map<String, Double> num, Map<String, Double> denum) {
            this.num = num;
            this.denum = denum;
        }

        @Override
        public void call(NaryTree node) {
            if (!node.isLeaf()) {
                for (NaryTree child : node.getChildren())
                    if (child.isLeaf()) return;
                Rule rule = node.getRule();
                String[] childrenAsString = node.getChildrenArcAsString();
                String orderedKey = rule.getLhs() + ">" + Util.join("_", childrenAsString);
                Arrays.sort(childrenAsString);
                String unorderedKey = rule.getLhs() + ">" + Util.join("_", childrenAsString);
                num.put(orderedKey, num.containsKey(orderedKey) ? num.get(orderedKey) + 1. : 1.);
                denum.put(unorderedKey, denum.containsKey(unorderedKey) ? denum.get(unorderedKey) + 1. : 1.);
            }
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (NaryTree naryTree : this)
            sb.append(naryTree.getAsOneLineString() + "\n");
        return sb.toString().trim();
    }


//    ID: Word index, integer starting at 1 for each new sentence; may be a range for tokens with multiple words.
//    FORM: Word form or punctuation symbol (Ciphered)
//    LEMMA: Lemma or stem of word form.
//    UPOS: Universal part-of-speech tag drawn from our revised version of the Google universal POS tags.
//    POSTAG: Language-specific part-of-speech tag; underscore if not available.
//    FEATS: List of morphological features from the universal feature inventory or from a defined language-specific extension; underscore if not available.
//    HEAD: Head of the current token, which is either a value of ID or zero (0).
//    DEPREL: Universal Stanford dependency relation to the HEAD (root iff HEAD = 0) or a defined language-specific subtype of one.
//    DEPS: List of secondary dependencies (head-deprel pairs).
//    MISC: Any other annotation.

    public void writeTreesToFile(File file) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write(toString());
        bufferedWriter.close();
    }

    public void writeLabelled(File labelledFn) throws IOException {
        BufferedWriter wtrlabelledFn = new BufferedWriter(new FileWriter(labelledFn));
        for (NaryTree naryTree : this) {
            ArrayList<String> sentenceTokensSrc = new ArrayList<>();
            List<List<String>> fields = naryTree.toStringInConllFormat();
            for (List<String> field : fields)
                sentenceTokensSrc.add(field.get(1));
            naryTree.info += "# sentence-tokens: " + StringUtils.join(sentenceTokensSrc, " ") + "\n";
            wtrlabelledFn.write(naryTree.info);
            for (List<String> field : fields) {
//            0:conllEnt.add(Integer.toString(i + 1)); //    ID: Word index, integer starting at 1 for each new sentence; may be a range for tokens with multiple words.
//            1:conllEnt.add(fields.get(4)); // FORM: Word form or punctuation symbol (Ciphered)
//            2:conllEnt.add(fields.get(4)); // LEMMA: Same as the word for now
//            3:conllEnt.add(fields.get(1)); // UPOS: universal part-of-speech
//            4:conllEnt.add(fields.get(2)); // POSTAG: Fine-grained part-of-speech tag
//            5:conllEnt.add(Integer.toString(leafEnt.getValue() + 1)); // HEAD: not available
//            6:conllEnt.add(fields.get(3)); // DEPREL: Universal Stanford dependency relation to the HEAD (root iff HEAD = 0) or a defined language-specific subtype of one.
//            7:conllEnt.add(fields.get(0));// MISC: Any other annotation. (original word form)
                wtrlabelledFn.write(StringUtils.join(field, "\t") + "\n");
            }
            wtrlabelledFn.write("\n");
        }
        wtrlabelledFn.close();
    }

    @Override
    public Iterator<NaryTree> iterator() {
        return trees.iterator();
    }

    private class CollectRules implements Lambda.FnO1ToVoid<NaryTree> {

        List<Rule> allRules;

        public CollectRules(List<Rule> allRules) {
            this.allRules = allRules;
        }

        @Override
        public void call(NaryTree node) {
            if (!node.isLeaf()) {
                String lhs = node.getSymbol();
                List<NaryTree> children = node.getChildren();
                String[] rhs = new String[children.size()];
                int idx = 0;
                for (NaryTree child : children)
                    rhs[idx++] = child.getSymbol();
                allRules.add(new Rule(lhs, rhs, 1.0));
            }
        }

    }

}
