package util;

import edu.jhu.hlt.optimize.*;
import edu.jhu.hlt.optimize.AdaDelta.AdaDeltaPrm;
import edu.jhu.hlt.optimize.BottouSchedule.BottouSchedulePrm;
import edu.jhu.hlt.optimize.SGD.SGDPrm;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.functions.L2;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.pacaya.gm.train.CrfTrainer.CrfTrainerPrm;
import edu.jhu.pacaya.gm.train.CrfTrainer.Trainer;
import edu.jhu.pacaya.util.cli.Opt;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import org.apache.commons.cli.ParseException;

import java.util.Date;

/**
 * Created by dqwang on 12/14/14.
 */
public class OptWrapper {

    public enum Optimizer {LBFGS, QN, SGD, ADAGRAD, ADAGRAD_COMID, ADADELTA, FOBOS, ASGD}

    public static enum RegularizerType {L2, NONE}

    @Opt(hasArg = true, description = "Number of threads for training")
    public static int threads = 10;

    // Options for optimization.
    @Opt(hasArg = true, description = "The optimization method to use for training.")
    public static Optimizer optimizer = Optimizer.ADAGRAD_COMID;
    @Opt(hasArg = true, description = "The variance for the L2 regularizer.")
    public static double l2variance = Double.MAX_VALUE;
    @Opt(hasArg = true, description = "The type of regularizer.")
    public static RegularizerType regularizer = RegularizerType.NONE;
    @Opt(hasArg = true, description = "Number of effective passes over the dataset for SGD.")
    public static int sgdNumPasses = 10;
    @Opt(hasArg = true, description = "The batch size to use at each step of SGD.")
    public static int sgdBatchSize = 20;
    @Opt(hasArg = true, description = "The initial learning rate for SGD.")
    public static double sgdInitialLr = 0.1;
    @Opt(hasArg = true, description = "Whether to sample with replacement for SGD.")
    public static boolean sgdWithRepl = false;
    @Opt(hasArg = true, description = "Whether to automatically select the learning rate.")
    public static boolean sgdAutoSelectLr = true;
    @Opt(hasArg = true, description = "How many epochs between auto-select runs.")
    public static int sgdAutoSelecFreq = 2;
    @Opt(hasArg = true, description = "Whether to compute the function value on iterations other than the last.")
    public static boolean sgdComputeValueOnNonFinalIter = true;
    @Opt(hasArg = true, description = "Whether to do parameter averaging.")
    public static boolean sgdAveraging = false;
    @Opt(hasArg = true, description = "Whether to do early stopping.")
    public static boolean sgdEarlyStopping = true;
    @Opt(hasArg = true, description = "The AdaGrad parameter for scaling the learning rate.")
    public static double adaGradEta = 0.1;
    @Opt(hasArg = true, description = "The constant addend for AdaGrad.")
    public static double adaGradConstantAddend = 1e-9;
    @Opt(hasArg = true, description = "The decay rate for AdaDelta.")
    public static double adaDeltaDecayRate = 0.95;
    @Opt(hasArg = true, description = "The constant addend for AdaDelta.")
    public static double adaDeltaConstantAddend = Math.pow(Math.E, -6.);
    @Opt(hasArg = true, description = "Stop training by this date/time.")
    public static Date stopTrainingBy = null;

    public static CrfTrainerPrm getCrfTrainerPrm() throws ParseException {
        CrfTrainerPrm prm = new CrfTrainerPrm();
        prm.infFactory = new BruteForceInferencerPrm(LogSemiring.getInstance());

        if (optimizer == Optimizer.LBFGS) {
            prm.optimizer = getMalletLbfgs();
            prm.batchOptimizer = null;
        } else if (optimizer == Optimizer.QN) {
            prm.optimizer = getStanfordLbfgs();
            prm.batchOptimizer = null;
        } else {
            BottouSchedulePrm boPrm;
            if (optimizer != Optimizer.SGD && optimizer != Optimizer.ASGD && optimizer != Optimizer.ADAGRAD && optimizer != Optimizer.ADADELTA) {
                if (optimizer == Optimizer.ADAGRAD_COMID) {
                    AdaGradComidL2.AdaGradComidL2Prm lossPrm1 = new AdaGradComidL2.AdaGradComidL2Prm();
                    setSgdPrm(lossPrm1);
                    lossPrm1.l2Lambda = 1.0D / l2variance;
                    lossPrm1.eta = adaGradEta;
                    lossPrm1.constantAddend = adaDeltaConstantAddend;
                    lossPrm1.sched = null;
                    prm.optimizer = null;
                    prm.batchOptimizer = new AdaGradComidL2(lossPrm1);
                } else {
                    if (optimizer != Optimizer.FOBOS) {
                        throw new RuntimeException("Optimizer not supported: " + optimizer);
                    }

                    SGDFobos.SGDFobosPrm lossPrm2 = new SGDFobos.SGDFobosPrm();
                    setSgdPrm(lossPrm2);
                    lossPrm2.l2Lambda = 1.0D / l2variance;
                    boPrm = new BottouSchedulePrm();
                    boPrm.initialLr = sgdInitialLr;
                    boPrm.lambda = 1.0D / l2variance;
                    lossPrm2.sched = new BottouSchedule(boPrm);
                    prm.optimizer = null;
                    prm.batchOptimizer = new SGDFobos(lossPrm2);
                }
            } else {
                prm.optimizer = null;
                SGDPrm lossPrm = getSgdPrm();
                if (optimizer == Optimizer.SGD) {
                    boPrm = new BottouSchedulePrm();
                    boPrm.initialLr = sgdInitialLr;
                    boPrm.lambda = 1.0D / l2variance;
                    lossPrm.sched = new BottouSchedule(boPrm);
                } else if (optimizer == Optimizer.ASGD) {
                    boPrm = new BottouSchedulePrm();
                    boPrm.initialLr = sgdInitialLr;
                    boPrm.lambda = 1.0D / l2variance;
                    boPrm.power = 0.75D;
                    lossPrm.sched = new BottouSchedule(boPrm);
                    lossPrm.averaging = true;
                } else if (optimizer == Optimizer.ADAGRAD) {
                    AdaGradSchedule.AdaGradSchedulePrm boPrm1 = new AdaGradSchedule.AdaGradSchedulePrm();
                    boPrm1.eta = adaGradEta;
                    boPrm1.constantAddend = adaDeltaConstantAddend;
                    lossPrm.sched = new AdaGradSchedule(boPrm1);
                } else if (optimizer == Optimizer.ADADELTA) {
                    AdaDeltaPrm boPrm2 = new AdaDeltaPrm();
                    boPrm2.decayRate = adaDeltaDecayRate;
                    boPrm2.constantAddend = adaDeltaConstantAddend;
                    lossPrm.sched = new AdaDelta(boPrm2);
                    lossPrm.autoSelectLr = false;
                }

                prm.batchOptimizer = new SGD(lossPrm);
            }
        }

        if (regularizer == RegularizerType.L2) {
            prm.regularizer = new L2(l2variance);
        } else {
            if (regularizer != RegularizerType.NONE) {
                throw new ParseException("Unsupported regularizer: " + regularizer);
            }

            prm.regularizer = null;
        }

        prm.trainer = Trainer.CLL;

        return prm;
    }

    private static edu.jhu.hlt.optimize.Optimizer<DifferentiableFunction> getMalletLbfgs() {
        throw new RuntimeException("Stanford LBFGS requires optimize-wrappers, an optional dependency.");
    }

    private static edu.jhu.hlt.optimize.Optimizer<DifferentiableFunction> getStanfordLbfgs() {
        throw new RuntimeException("Stanford LBFGS requires optimize-wrappers, an optional dependency.");
    }

    private static SGDPrm getSgdPrm() {
        SGDPrm prm = new SGDPrm();
        setSgdPrm(prm);
        return prm;
    }

    private static void setSgdPrm(SGDPrm prm) {
        prm.numPasses = sgdNumPasses;
        prm.batchSize = sgdBatchSize;
        prm.withReplacement = sgdWithRepl;
        prm.stopBy = stopTrainingBy;
        prm.autoSelectLr = sgdAutoSelectLr;
        prm.autoSelectFreq = sgdAutoSelecFreq;
        prm.computeValueOnNonFinalIter = sgdComputeValueOnNonFinalIter;
        prm.averaging = sgdAveraging;
        prm.earlyStopping = sgdEarlyStopping;
        prm.sched = null;
    }

}
