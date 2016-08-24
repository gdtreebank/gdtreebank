package grammar;

import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Created by dqwang on 2/3/15.
 */
public class TreeBankToGrammar {

    private static final Logger log = Logger.getLogger(TreeBankToGrammar.class);

    public static void genGrammarFromTreeBank(TreeBank treeBank, File grammarFile, File weightFile, File statFile) throws IOException {
        Grammar ruleCounts = treeBank.genRuleCounts();
        Grammar grammar = ruleCounts.copy();
        grammar.normOverLhs();
        grammar.writeToFile(grammarFile);
        double numRoots = 0.0;
        double loglikelihood = 0.0;
        for (NaryTree naryTree : treeBank) {
            numRoots += naryTree.getNumRoots();
            loglikelihood += grammar.getLoglikelihood(naryTree);
        }
        Grammar weights = ruleCounts.copy();
        weights.globalNorm(numRoots);
        weights.writeToFile(weightFile);
        if (statFile != null) {
            BufferedWriter bfwtr = new BufferedWriter(new FileWriter(statFile));
            bfwtr.write("numRoots:" + numRoots + "\n");
            bfwtr.write("loglikelihood:" + loglikelihood + "\n");
            bfwtr.write("condEnt:" + treeBank.condEnt() + "\n");
            Map<Integer, Integer> dist = grammar.getRuleLengthDist();
            Integer cnt = 0;
            for (Integer i : dist.keySet()) {
                cnt += dist.get(i);
                bfwtr.write("Length Dist:" + i.toString() + '\t' + cnt.toString() + "\n");
            }
            bfwtr.close();
        }
    }

    public static void genGrammarFromTreeBank(TreeBank treeBank, File grammarFile, File statFile) throws IOException {
        Grammar ruleCounts = treeBank.genRuleCounts();
        Grammar grammar = ruleCounts.copy();
        grammar.normOverLhs();
        grammar.writeToFile(grammarFile);
        double numRoots = 0.0;
        double loglikelihood = 0.0;
        for (NaryTree naryTree : treeBank) {
            numRoots += naryTree.getNumRoots();
            loglikelihood += grammar.getLoglikelihood(naryTree);
        }
        if (statFile != null) {
            BufferedWriter bfwtr = new BufferedWriter(new FileWriter(statFile));
            bfwtr.write("[numRoots]:" + numRoots + "\n");
            bfwtr.write("[loglikelihood]:" + loglikelihood + "\n");
            bfwtr.write("[condEnt]:" + treeBank.condEnt() + "\n");
            bfwtr.write("[ent]:" + treeBank.orderingEnt() + "\n");
            Map<Integer, Integer> dist = grammar.getRuleLengthDist();
            Integer cnt = 0;
            for (Integer i : dist.keySet()) {
                cnt += dist.get(i);
                bfwtr.write("[Length Dist]:" + i.toString() + '\t' + cnt.toString() + "\n");
            }
            bfwtr.close();
        }
    }

    public static void genGrammarFromTreeBank(TreeBank treeBank, File statFile) throws IOException {
        if (statFile != null) {
            BufferedWriter bfwtr = new BufferedWriter(new FileWriter(statFile));
            String size = "[size]:" + treeBank.size();
            String ent = "[ent]:" + treeBank.orderingEnt();
            String avgArc = "[avgArc]:" + treeBank.avgArcLength();
            bfwtr.write(size + "\n");
            bfwtr.write(ent + "\n");
            bfwtr.write(avgArc + "\n");
            bfwtr.close();
            log.info(size);
            log.info(ent);
            log.info(avgArc);
        }
    }

    public static void genGrammarFromTreeBank(File treeBankFile, File grammarFile, File weightFile, File statFile) throws IOException {
        TreeBank treeBank = new TreeBank(TreeBank.DEFAULT_ROOT);
        treeBank.readTreesInPtbFormat(treeBankFile);
        genGrammarFromTreeBank(treeBank, grammarFile, weightFile, statFile);
    }
}
