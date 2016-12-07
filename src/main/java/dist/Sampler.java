package dist;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static util.Util.normalize;

/**
 * Created by dqwang on 2/5/15.
 */
public class Sampler {
    public static final double DEFAULT_ALPHA = 20;

    public static SimpleEntry<Map<Integer, Double>, ArrayList<ArrayList<SimpleEntry<Double, Double>>>> genMixtureGaussianParams(int ndist, int ndim, Random random) {
        Map<Integer, Double> prior = new HashMap<>();
        ArrayList<ArrayList<SimpleEntry<Double, Double>>> params = new ArrayList<>();
        for (int i = 0; i < ndist; ++i) {
            prior.put(i, random.nextDouble());
            ArrayList<SimpleEntry<Double, Double>> param = new ArrayList<>();
            for (int j = 0; j < ndim; ++j)
                param.add(new SimpleEntry<>(2 * random.nextDouble() - 1, random.nextDouble()));
            params.add(param);
        }
        normalize(prior);
        return new SimpleEntry<>(prior, params);
    }

    public interface DiscreteSampler<T> {
        T sampleOne();
    }

    public static <T> T sampleOne(Map<T, Double> distribution, Double total, Random random) {
        Double randDouble = random.nextDouble() * total;
        for (Map.Entry<T, Double> entry : distribution.entrySet()) {
            randDouble -= entry.getValue();
            if (randDouble < 0) return entry.getKey();
        }
        throw new IllegalStateException("Wrong Sampler!");
    }

    public static <T> T sampleOne(Map<T, Double> distribution, Random random) {
        Double total = 0.0;
        for (Double weight : distribution.values()) total += weight;
        return sampleOne(distribution, total, random);
    }

    public static class SimpleSampler<T> implements DiscreteSampler {

        private Map<T, Double> distribution;
        private Random random;

        public SimpleSampler(Map<T, Double> distribution, Random random) {
            this.distribution = distribution;
            this.random = random;
        }

        @Override
        public T sampleOne() {
            Double total = 0.0;
            for (Double weight : distribution.values()) total += weight;
            return Sampler.sampleOne(distribution, total, random);
        }
    }

    public static class MutableSampler<T> implements DiscreteSampler {

        private Map<T, Double> samples;
        private double total;
        private Random random;

        public double getTotal() {
            return total;
        }

        public MutableSampler(Random random) {
            this.samples = new HashMap<>();
            this.random = random;
            total = 0.0;
        }

        public void addExample(T example, double weight) {
            samples.put(example, samples.containsKey(example) ? samples.get(example) + weight : weight);
            total += weight;
        }

        public Map<T, Double> getSamples() {
            return samples;
        }

        @Override
        public T sampleOne() {
            return Sampler.sampleOne(samples, total, random);
        }
    }

    public static class DPSampler<T> implements DiscreteSampler {
        private DiscreteSampler<T> proposalSampler;
        private MutableSampler<T> mutableSampler;
        private Random random;
        private Double alpha;

        public DPSampler(DiscreteSampler<T> proposalSampler, Double alpha, Random random) {
            this.proposalSampler = proposalSampler;
            this.alpha = alpha;
            this.random = random;
            this.mutableSampler = new MutableSampler<>(random);
        }

        public Map<T, Double> getSamples() {
            return mutableSampler.getSamples();
        }

        @Override
        public T sampleOne() {
            double total = alpha + mutableSampler.getTotal();
            Double randDouble = random.nextDouble() * total;
            T sample;
            if (randDouble > alpha)
                sample = mutableSampler.sampleOne();
            else
                sample = proposalSampler.sampleOne();
            mutableSampler.addExample(sample, 1.0);
            return sample;
        }
    }
}
