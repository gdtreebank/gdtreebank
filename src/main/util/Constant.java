package util;

import edu.jhu.util.cli.Opt;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dqwang on 14/12/2.
 */
public class Constant {
    // Feature configuration
    @Opt(hasArg = true, description = "The number of sentences to read from a treebank (default=maxint)")
    public static int numSentencesPerTB = Integer.MAX_VALUE;
    @Opt(hasArg = true, description = "Max number of children limitation for a tree to be considered (default=7)")
    public static int ruleLength = 7;
    @Opt(hasArg = true, description = "Verbosity level (default=1)")
    public static int verbose = 1;

    public static int filterPuncts = 0;
    public static String srcName = "#";
    public static String PERM_TAG_DEL = "|";
    public static String CHILDREN_DEL = "_";
    public static String FEATURE_VAL_DEL = " ";
    public static String TAG_ARC_DEL = "&";
    public static double EPSILON = 1e-4;
    public static double LAMBDA_SMALL = 1e-20;
    public static double UNDEFINED = -1;
    public static String SUB_MARK = "SUB_";
    public static String HEADER_MARK = "@";
    public static String NT_MARK = "_";
    public static String FORM = "FORM";
    public static String LEMMA = "LEMMA";
    public static String UPOS = "UPOS";
    public static String POS = "POS";
    public static String BOS = "BOS";
    public static String EOS = "EOS";
    public static String FEATURE = "FEATURE";
    public static String DEPREL = "DEPREL";
    public static String DEP = "DEP";
    public static String MISSING = "_";
    public static String MISC = "MISC";
    public static String SRC_LINE = "SRC_LINE";
    public static String SRC_ID = "SRC_ID";
    public static String SRC_FORM = "SRC_FORM";

    public static String VERB = "VERB";
    public static String NOUN = "NOUN";
    public static String PROPN = "PROPN";
    public static String PRON = "PRON";
    public static String ADJ = "ADJ";
}
