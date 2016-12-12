package datagen;

import edu.jhu.pacaya.gm.maxent.LogLinearXY.LogLinearXYPrm;
import edu.jhu.pacaya.util.Threads;
import edu.jhu.pacaya.util.cli.ArgParser;
import edu.jhu.pacaya.util.cli.Opt;
import edu.jhu.pacaya.util.report.ReporterManager;
import edu.jhu.prim.bimap.IntObjectBimap;
import edu.jhu.prim.util.random.Prng;
import grammar.LanguageToDataSet;
import grammar.TreeBank;
import maxent.DataSet;
import maxent.Model;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import util.Constant;
import util.OptWrapper;
import util.Util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by dqwang on 12/14/14.
 */
public class GalacticGen {

    private static final Logger log = Logger.getLogger(GalacticGen.class);
    @Opt(name = "seed", hasArg = true, description = "Pseudo random number generator seed")
    public static long seed = 0;
    @Opt(hasArg = true, description = "Task to run:\n" +
            "train (default): train a permutation model from an input treebank\n" +
            "test: permute a treebank given trained models")
    public static String task = "train";
    @Opt(hasArg = true, description = "Input treebank:\n" +
            "if task == train: It is the treebank for training the permutation model,\n" +
            "if task == test: It is the treeband for permutation")
    public static File inputTB = new File("your/input/treebank");
    @Opt(hasArg = true, description = "The ouput model path")
    public static File modelPath = new File("your_trained_model.orm");
    @Opt(hasArg = true, description = "Output treebank after permutation")
    public static File outputTB = new File("your_output_treebank.conllu");

    @Opt(hasArg = true, description = "Feature to be considered into the model:\n" +
            "posArc (default): consider both POS-tag and arc label as feature\n" +
            "pos: only POS-tag\n" +
            "arc: only arc label\n" +
            "WARNING:At test time, this value should set to be the same as training")
    public static String feature = "posArc";
    @Opt(hasArg = true, description = "The lower bound of the high order feature (default=3)\n" +
            "WARNING:At test time, this value should set to be the same as training")
    public static int higOrderLow = 3;
    @Opt(hasArg = true, description = "The upper bound of the high order feature (default=5)\n" +
            "WARNING:At test time, this value should set to be the same as training")
    public static int higOrderUpper = 5;
    @Opt(hasArg = true, description = "Taget node to be trained on:\n" +
            "NOUN (default): noun (common noun), propn (proper noun) and pron (pronoun)" +
            "VERB: verb")
    public static String targetNode = Constant.NOUN;
    @Opt(hasArg = true, description = "Maximum children number a node to be considered")
    public static int maxChildrenNum = 6;

    @Opt(hasArg = true, description = "Input NOUN-permutation model trained from superstrate\n" +
            "WARNING:NOUN won't be permuted if the file doesn't exist")
    public static File supStrateModelNOUN = new File("superstrate@N.orm");
    @Opt(hasArg = true, description = "Input VERB-permutation model trained from superstrate\n" +
            "WARNING:VERB won't be permuted if the file doesn't exist")
    public static File supStrateModelVERB = new File("superstrate@V.orm");
    @Opt(hasArg = true, description = "Input NOUN-permutation model trained from substrate\n" +
            "WARNING:NOUN won't be permuted if the file doesn't exist")
    public static File subStrateModelNOUN = new File("substrate@N.orm");
    @Opt(hasArg = true, description = "Input VERB-permutation model trained from substrate\n" +
            "WARNING:VERB won't be permuted if the file doesn't exist")
    public static File subStrateModelVERB = new File("substrate@V.orm");
    @Opt(hasArg = true, description = "Combination rate for substrate and superstrate\n" +
            "param = (1-lambda)*param_superstrate + lambda*param_substrate")
    public static double lambda = 0.05;

    public static Set<String> permutable = new HashSet<>();

    public void train() throws Exception {
        TreeBank treeBank = new TreeBank(TreeBank.DEFAULT_ROOT);
        Constant.ruleLength = Integer.MAX_VALUE;
        treeBank.readTreesInUniversalConLLFormat(inputTB);
        treeBank.cut(Constant.numSentencesPerTB);
        LogLinearXYPrm prm = new LogLinearXYPrm();
        prm.crfPrm = OptWrapper.getCrfTrainerPrm();
        Model model = new Model(prm);
        IntObjectBimap<String> featAlphabet = model.getFeatureAlphabet();
        log.info("Loading data....");
        DataSet trainData = LanguageToDataSet.languagesToDataSetFast(treeBank, featAlphabet);
        log.info("Training");
        model.train(trainData);
        model.saveText(modelPath);

    }

    Map<String, Double> loadMode() throws ClassNotFoundException {
        Map<String, Double> ret = new HashMap<>();
        try {
            Util.update(Util.loadText(supStrateModelNOUN), ret, Constant.NOUN + Constant.PERM_TAG_DEL, 1 - lambda);
            Util.update(Util.loadText(subStrateModelNOUN), ret, Constant.NOUN + Constant.PERM_TAG_DEL, lambda);
            permutable.add(Constant.NOUN);
        } catch (IOException e) {
        }
        try {
            Util.update(Util.loadText(supStrateModelVERB), ret, Constant.VERB + Constant.PERM_TAG_DEL, 1 - lambda);
            Util.update(Util.loadText(subStrateModelVERB), ret, Constant.VERB + Constant.PERM_TAG_DEL, lambda);
            permutable.add(Constant.VERB);
        } catch (IOException e) {
        }

        log.info("permutable Nodes:" + StringUtils.join(permutable, " "));
        return ret;
    }

    /**
     * Permute a treebank given a model.
     */
    public void test() throws Exception {
        // Load model
        Map<String, Double> permute_model = loadMode();
        // Load Training data
        TreeBank inputTreeBank = new TreeBank(TreeBank.DEFAULT_ROOT);
        Constant.srcName = inputTB.getName();
        inputTreeBank.readTreesInUniversalConLLFormat(Constant.numSentencesPerTB, inputTB);
        // Permute Training data
        TreeBank permutedTreeBank = permute_model.size() > 0 ? inputTreeBank.permuteV5(Prng.getRandom(), permute_model) : inputTreeBank;
        // Save to train file
        permutedTreeBank.writeLabelled(outputTB);
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        log.info("Running with args: " + StringUtils.join(args, " "));
        int exitCode = 0;
        ArgParser parser = null;
        try {
            parser = new ArgParser(GalacticGen.class);
            parser.registerClass(GalacticGen.class);
            parser.registerClass(OptWrapper.class);
            parser.registerClass(Constant.class);
            parser.parseArgs(args);
            ReporterManager.init(ReporterManager.reportOut, true);
            Prng.seed(seed);
            Threads.initDefaultPool(OptWrapper.threads);
            GalacticGen galacticGen = new GalacticGen();
            if ("train".equals(task))
                galacticGen.train();
            else
                galacticGen.test();
        } catch (ParseException e1) {
            log.error(e1.getMessage());
            if (parser != null)
                parser.printUsage();
            exitCode = 1;
        } catch (Throwable t) {
            t.printStackTrace();
            exitCode = 1;
        } finally {
            Threads.shutdownDefaultPool();
            ReporterManager.close();
        }
        System.exit(exitCode);
    }

}
