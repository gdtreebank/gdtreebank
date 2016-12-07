package grammar;

import util.Constant;
import maxent.Features;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dqwang on 2/19/15.
 */

public class Rule {
    public static double DEFAULT_RULE_WEIGHTS = -1.0;
    private String lhs;
    private String[] rhs;
    private double weight;

    public Rule(String lhs, String[] rhs, double weight) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.weight = weight;
    }

    public Rule(String lhs, String rhs, double weight) {
        this.lhs = lhs;
        this.rhs = rhs.split(" ");
        this.weight = weight;

    }

    public String getLhs() {
        return lhs;
    }

    public String[] getRhs() {
        return rhs.clone();
    }

    public double getWeight() {
        return weight;
    }

    public String getRhsAsStr() {
        StringBuffer sb = new StringBuffer();
        for (String nt : rhs)
            sb.append(nt + " ");
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return weight + " " + lhs + " --> " + getRhsAsStr();
    }

    public int size() {
        return getRhs().length;
    }

}

