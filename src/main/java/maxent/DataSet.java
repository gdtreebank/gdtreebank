package maxent;

import edu.jhu.prim.bimap.IntObjectBimap;

import java.util.ArrayList;
import java.util.Iterator;

public class DataSet implements Iterable<Instance> {

    private IntObjectBimap<String> featureAlphabet;
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

    public void setFeatureAlphabet(IntObjectBimap<String> featureAlphabet) {
        this.featureAlphabet = featureAlphabet;
    }

    public IntObjectBimap<String> getFeatureAlphabet() {
        return featureAlphabet;
    }

    @Override
    public Iterator<Instance> iterator() {
        return instances.iterator();
    }

}
