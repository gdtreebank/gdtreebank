package maxent;

import edu.jhu.util.Alphabet;

import java.util.ArrayList;
import java.util.Iterator;

public class DataSet implements Iterable<Instance> {

    private Alphabet<String> featureAlphabet;
    private ArrayList<Instance> instances;

    public DataSet() {
        this.instances = new ArrayList<>();
    }

    public void add(Instance instance) {
        instances.add(instance);
    }

    public int size() {
        return instances.size();
    }

    public void setFeatureAlphabet(Alphabet<String> featureAlphabet) {
        this.featureAlphabet = featureAlphabet;
    }

    public Alphabet<String> getFeatureAlphabet() {
        return featureAlphabet;
    }

    @Override
    public Iterator<Instance> iterator() {
        return instances.iterator();
    }

}
