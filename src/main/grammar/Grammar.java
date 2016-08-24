package grammar;

import edu.jhu.prim.util.Lambda;
import util.Constant;

import java.io.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;


/**
 * Grammar representation for fast lookups via LHS.
 * <p/>
 * A grammar may consist of either rule weights or normalized rule
 * probabilities.
 *
 * @author wdd
 */

public class Grammar implements Iterable<Rule> {

    @Override
    public Iterator<Rule> iterator() {
        return new RuleIterator();
    }

    private class RuleIterator implements Iterator<Rule> {
        private Iterator<String> lhsItr;
        private Iterator<Map.Entry<String, Double>> rhsItr;
        private String lhs;

        public RuleIterator() {
            lhsItr = Grammar.this.lhs2RhsWeightsMap.keySet().iterator();
        }

        @Override
        public boolean hasNext() {
            while ((rhsItr == null || !rhsItr.hasNext()) && lhsItr.hasNext()) {
                lhs = lhsItr.next();
                rhsItr = Grammar.this.lhs2RhsWeightsMap.get(lhs).entrySet().iterator();
            }
            return rhsItr != null && rhsItr.hasNext();
        }

        @Override
        public Rule next() {
            if (hasNext()) {
                Map.Entry<String, Double> entry = rhsItr.next();
                return new Rule(lhs, entry.getKey(), entry.getValue());
            }
            return null;
        }
    }

    private String rootSymbol;
    private int size;
    public static final String DEFAULT_ROOT = "ROOT";
    private Map<String, Map<String, Double>> lhs2RhsWeightsMap;

    public Grammar(String rootSymbol) {
        this.rootSymbol = rootSymbol;
        lhs2RhsWeightsMap = new HashMap<>();
        size = -1;
    }

    public Grammar(List<Rule> allRules, String rootSymbol) {
        this.rootSymbol = rootSymbol;
        lhs2RhsWeightsMap = new HashMap<>();
        size = allRules.size();
        for (Rule rule : allRules) addRule(rule);
    }

    public Map<Integer, Integer> getRuleLengthDist() {
        Map<Integer, Integer> dist = new HashMap<>();
        for (Rule rule : this) {
            int len = rule.size();
            dist.put(len, dist.containsKey(len) ? dist.get(len) + 1 : 1);
        }
        return dist;

    }

    public double getWeight(Rule rule) {
        String lhs = rule.getLhs();
        Map<String, Double> rhs2Weights = getRhs2Weight(lhs);
        if (rhs2Weights == null) return 0.0;
        String rhs = rule.getRhsAsStr();
        Double weight = rhs2Weights.get(rhs);
        if (weight == null) return 0.0;
        return weight;
    }

    public void addRule(String lhs, String rhs, Double weights) {
        size = -1;
        Map<String, Double> rhs2Weights;
        if (lhs2RhsWeightsMap.containsKey(lhs))
            rhs2Weights = lhs2RhsWeightsMap.get(lhs);
        else
            rhs2Weights = new HashMap<>();
        rhs2Weights.put(rhs, rhs2Weights.containsKey(rhs) ? rhs2Weights.get(rhs) + weights : weights);
        lhs2RhsWeightsMap.put(lhs, rhs2Weights);
    }

    public void addRule(Rule rule) {
        addRule(rule.getLhs(), rule.getRhsAsStr(), rule.getWeight());
    }

    public Grammar simplify() {
        Grammar grammar = new Grammar(rootSymbol);
        for (Rule rule : this) {
            String lhs = rule.getLhs();
            Double weight = rule.getWeight();
            if (weight > (Constant.EPSILON < 1.0 / lhs2RhsWeightsMap.get(lhs).size() ? Constant.EPSILON : 0.0))
                grammar.addRule(rule);
        }
        grammar.normOverLhs();
        return grammar;
    }

    public Collection<String> getLhsSet() {
        return lhs2RhsWeightsMap.keySet();
    }

    public Map<String, Double> getRhs2Weight(String lhs) {
        return lhs2RhsWeightsMap.get(lhs);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(rootSymbol + "\n");
        for (Rule rule : this)
            if (rule.getLhs().equals(rootSymbol))
                sb.append(rule + "\n");
        for (Rule rule : this)
            if (!rule.getLhs().equals(rootSymbol))
                sb.append(rule + "\n");
        return sb.toString().trim();
    }

    public Grammar getRandomCopy(Random random) {
        Grammar grammar = new Grammar(rootSymbol);
        for (Rule rule : this) {
            String lhs = rule.getLhs();
            String rhs = rule.getRhsAsStr();
            grammar.addRule(lhs, rhs, random.nextDouble());
        }
        grammar.normOverLhs();
        return grammar;
    }

    public String getRootSymbol() {
        return rootSymbol;
    }

    public Grammar getUniformCopy() {
        Grammar grammar = new Grammar(rootSymbol);
        for (Rule rule : this) {
            String lhs = rule.getLhs();
            String rhs = rule.getRhsAsStr();
            grammar.addRule(lhs, rhs, 1.0);
        }
        grammar.normOverLhs();
        return grammar;
    }

    public Grammar getZeroCopy() {
        Grammar grammar = new Grammar(rootSymbol);
        for (Rule rule : this) {
            String lhs = rule.getLhs();
            String rhs = rule.getRhsAsStr();
            grammar.addRule(lhs, rhs, 0.0);
        }
        return grammar;
    }

    public static String toString(Grammar gTrue, Grammar gPred) {
        StringBuilder sb = new StringBuilder();
        sb.append("CnfGrammar [rootSymbol=");
        sb.append(gTrue.getRootSymbol());
        sb.append(", allRules=\n");
        for (String lhs : gTrue.getLhsSet()) {
            Map<String, Double> rhsToWeightsGTrue = gTrue.getRhs2Weight(lhs);
            Map<String, Double> rhsToWeightsGPred = gPred.getRhs2Weight(lhs);
            for (String rhs : rhsToWeightsGTrue.keySet()) {
                sb.append("\t" + lhs + "-->" + rhs + ":");
                sb.append("wTrue=" + String.format("%.2f", rhsToWeightsGTrue.get(rhs)));
                sb.append(", wPred=" + String.format("%.2f", rhsToWeightsGPred.containsKey(rhs) ?
                        rhsToWeightsGPred.get(rhs) : 0.0));
                sb.append("]\n");
            }
        }
        return sb.toString();
    }

    public Grammar copy() {
        Grammar grammar = new Grammar(rootSymbol);
        for (Rule rule : this)
            grammar.addRule(rule);
        return grammar;
    }

    public void normOverLhs() {
        for (String lhs : getLhsSet()) {
            Map<String, Double> oriRhsMap = getRhs2Weight(lhs);
            double norm = 0;
            for (String rhs : oriRhsMap.keySet()) {
                double weight = oriRhsMap.get(rhs);
                norm += weight;
            }
            for (String rhs : oriRhsMap.keySet())
                oriRhsMap.put(rhs, oriRhsMap.get(rhs) / norm);
        }
    }

    public void globalNorm(double norm) {
        for (String lhs : getLhsSet()) {
            Map<String, Double> oriRhsMap = getRhs2Weight(lhs);
            for (String rhs : oriRhsMap.keySet())
                oriRhsMap.put(rhs, oriRhsMap.get(rhs) / norm);
        }
    }

    public void writeToFile(File file) throws IOException {
        BufferedWriter bwtr = new BufferedWriter(new FileWriter(file));
        bwtr.write(toString());
        bwtr.close();
    }

    public double getLoglikelihood(NaryTree naryTree) {
        LoglikelihoodCounter loglikelihoodCounter = new LoglikelihoodCounter();
        naryTree.preOrderTraversal(loglikelihoodCounter);
        return loglikelihoodCounter.getLoglikelihood();
    }


    private class LoglikelihoodCounter implements Lambda.FnO1ToVoid<NaryTree> {

        public double loglikelihood;

        public LoglikelihoodCounter() {
            loglikelihood = 0.0;
        }

        public double getLoglikelihood() {
            return loglikelihood;
        }

        @Override
        public void call(NaryTree node) {
            if (!node.isLexical()) {
                double weight = getWeight(node.getRule());
                loglikelihood += Math.log(weight);
            }
        }

    }

}