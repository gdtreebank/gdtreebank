package grammar;

import edu.jhu.prim.util.Lambda;
import edu.jhu.pacaya.util.files.QFiles;
import org.apache.commons.lang3.StringUtils;
import util.Constant;

import java.io.IOException;
import java.io.Reader;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;

/**
 * N-ary tree for a context free grammar.
 *
 * @author wdd
 */
public class NaryTree {

    private static final int NOT_INITIALIZED = -1;
    private String symbol;
    public String info;
    private int start;
    private int end;
    private int numRoots;
    private Map<String, String> fields;
    private Map<String, Double> statistics;
    public TreeMap<Integer, NaryTree> punctPos;
    /**
     * Children of this node, ordered left-to-right
     */
    private List<NaryTree> children;
    private boolean isLexical;

    public NaryTree(String symbol, int start, int end, List<NaryTree> children, boolean isLexical) {
        this.symbol = symbol;
        this.start = start;
        this.end = end;
        this.children = children;
        this.isLexical = isLexical;
        this.fields = new HashMap<>();
        this.statistics = new HashMap<>();
        this.numRoots = -1;
        this.punctPos = new TreeMap<>();
    }

    public boolean isNoun() {
        Map<String, String> fields = getFields();
        String pos = fields.get(Constant.UPOS);
        return Constant.NOUN.equals(pos) || Constant.PROPN.equals(pos) || Constant.PRON.equals(pos);
    }

    public boolean isVerb() {
        return Constant.VERB.equals(getFields().get(Constant.UPOS));
    }

    public boolean isPunct() {
        String upos = getFields().get(Constant.UPOS);
        return upos.equals("PUNCT") || upos.equals(".");
    }

    public boolean isPOS_PROPN() {
        String upos = getFields().get(Constant.UPOS);
        return upos.equals(Constant.PROPN);
    }

    public boolean isDEP_CONJ() {
        String upos = getFields().get(Constant.DEPREL);
        return upos.equals("conj");
    }

    public boolean isDEP_CC() {
        String upos = getFields().get(Constant.DEPREL);
        return upos.equals("cc");
    }

    public boolean isADJ() {
        String upos = getFields().get(Constant.UPOS);
        return upos.equals(Constant.ADJ);
    }

    public boolean isDEP_MWE() {
        String upos = getFields().get(Constant.DEPREL);
        return upos.equals("mwe");
    }

    public NaryTree(String symbol, int start, int end, List<NaryTree> children, Map<String, String> fields, boolean isLexical) {
        this.symbol = symbol;
        this.start = start;
        this.end = end;
        this.children = children;
        this.isLexical = isLexical;
        this.fields = fields;
        this.statistics = new HashMap<>();
        this.numRoots = -1;
    }

    public NaryTree(NaryTree naryTree) {
        this.symbol = naryTree.symbol;
        this.start = naryTree.start;
        this.end = naryTree.end;
        this.children = naryTree.children;
        this.isLexical = naryTree.isLexical;
        this.fields = naryTree.fields;
        this.numRoots = naryTree.numRoots;
        this.statistics = naryTree.statistics;
        this.punctPos = naryTree.punctPos;
        this.info = naryTree.info;
    }

    public Map<String, String> getFields() {
        if (isLeaf()) return fields;
        if (children.size() > 1)
            for (NaryTree child : children)
                if (child.getSymbol().startsWith(Constant.HEADER_MARK))
                    return child.getFields();
        return children.get(0).getFields();
    }

    public boolean isHeader() {
        return this.symbol.startsWith(Constant.HEADER_MARK);
    }

    public Map<String, Double> getStatistics() {
        return this.statistics;
    }

    public void appendField(String key, String val) {
        if (this.fields.containsKey(key)) System.out.println("Overwritting fields");
        this.fields.put(key, val);
    }

    public int getNumRoots() {
        if (numRoots != -1) return numRoots;
        if (isLexical) return 1;
        numRoots = 1;
        for (NaryTree naryTree : children)
            numRoots += naryTree.getNumRoots();
        return numRoots;
    }

    private static String canonicalizeTreeString(String newTreeStr) {
        return newTreeStr.trim().replaceAll("\\s+\\)", ")").replaceAll("\\s+", " ");
    }

    public String getAsOneLineString() {
        return canonicalizeTreeString(getAsPennTreebankString());
    }

    /**
     * Gets a string representation of this parse that looks like the typical
     * Penn Treebank style parse.
     * <p>
     * Example:
     * ((ROOT (S (NP (NN time))
     * (VP (VBZ flies)
     * (PP (IN like)
     * (NP (DT an)
     * (NN arrow)))))))
     *
     * @return A string representing this parse.
     */
    public String getAsPennTreebankString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        getAsPennTreebankString(1, 1, sb);
        sb.append(")");
        return sb.toString();
    }

    public String getSentence() {
        ArrayList<NaryTree> leaves = getLexicalLeaves();
        StringBuffer sb = new StringBuffer();
        for (NaryTree naryTree : leaves)
            sb.append(naryTree.symbol + " ");
        return sb.toString().trim();
    }

    public Rule getRule() {
        if (isLeaf() && isLexical()) return null;
        String lhs = symbol;
        String[] rhs = new String[children.size()];
        int idx = 0;
        for (NaryTree naryTree : children)
            rhs[idx++] = naryTree.getSymbol();
        return new Rule(lhs, rhs, Constant.UNDEFINED);
    }

    public String[] getChildrenPOSAsString() {
        if (isLeaf() && isLexical()) return null;
        String[] res = new String[children.size()];
        int idx = 0;
        for (NaryTree naryTree : children)
            res[idx++] = naryTree.getSymbol();
        return res;
    }

    public String[] getChildrenArcAsString() {
        if (isLeaf() && isLexical()) return null;
        String[] res = new String[children.size()];
        int idx = 0;
        for (NaryTree naryTree : children) {
            Map<String, String> fields = naryTree.getFields();
            String depRel = fields.get(Constant.DEPREL);
            res[idx++] = (naryTree.getSymbol().startsWith(Constant.HEADER_MARK) ? Constant.HEADER_MARK : depRel);
        }
        return res;
    }

    public String[] getChildrenAsString() {
        if (isLeaf() && isLexical()) return null;
        String[] res = new String[children.size()];
        int idx = 0;
        for (NaryTree naryTree : children) {
            Map<String, String> fields = naryTree.getFields();
            String depRel = fields.get(Constant.DEPREL);
            res[idx++] = naryTree.getSymbol() + Constant.TAG_ARC_DEL + (naryTree.getSymbol().startsWith(Constant.HEADER_MARK) ? Constant.HEADER_MARK : depRel);
        }
        return res;
    }

    private SimpleEntry<NaryTree, Integer> genDependency(List<SimpleEntry<NaryTree, Integer>> leaves) {
        if (isLeaf()) {
            leaves.set(getStart(), new SimpleEntry<>(this, getStart()));
            return leaves.get(getStart());
        }
        int header = -1;
        List<Integer> headerList = new ArrayList<>();
        for (NaryTree child : getChildren()) {
            SimpleEntry<NaryTree, Integer> curEntry = child.genDependency(leaves);
            if (child.getSymbol().startsWith(Constant.HEADER_MARK) || header == -1)
                header = curEntry.getValue();
            headerList.add(curEntry.getValue());
        }
        for (Integer childHeader : headerList)
            leaves.get(childHeader).setValue(header);
        return leaves.get(header);
    }

    public List<List<String>> toStringInConllFormat() {
        List<List<String>> ret = new ArrayList<>();
        List<SimpleEntry<NaryTree, Integer>> leaves = new ArrayList<>();
        while (leaves.size() < getSentLength()) leaves.add(null);
        genDependency(leaves).setValue(-1);
        Map<String, String> posMap = new HashMap<>();
        posMap.put("0", "0"); //    ID: Word index, integer starting at 1 for each new sentence; may be a range for tokens with multiple words.
        for (int i = 0; i < leaves.size(); ++i) {
            SimpleEntry<NaryTree, Integer> leafEnt = leaves.get(i);
            NaryTree leaf = leafEnt.getKey();
            assert leaf.isLeaf() : "Entry is not leaf!";
            posMap.put(fields.get(Constant.SRC_ID), Integer.toString(i + 1)); //    ID: Word index, integer starting at 1 for each new sentence; may be a range for tokens with multiple words.
        }
        for (int i = 0; i < leaves.size(); ++i) {
            List<String> conllEnt = new ArrayList<>();
            SimpleEntry<NaryTree, Integer> leafEnt = leaves.get(i);
            NaryTree leaf = leafEnt.getKey();
            assert leaf.isLeaf() : "Entry is not leaf!";
            conllEnt.add(Integer.toString(i + 1)); //    ID: Word index, integer starting at 1 for each new sentence; may be a range for tokens with multiple words.
            Map<String, String> fields = leaf.getFields();
            conllEnt.add(fields.get(Constant.FORM)); // FORM: Word form or punctuation symbol (Ciphered)
            conllEnt.add(fields.get(Constant.LEMMA)); // LEMMA: Same as the word for now
            conllEnt.add(fields.get(Constant.UPOS)); // UPOS: universal part-of-speech
            conllEnt.add(fields.get(Constant.POS)); // POSTAG: Fine-grained part-of-speech tag
            conllEnt.add(fields.get(Constant.FEATURE)); // FEATURE: Fine-grained part-of-speech tag
            conllEnt.add(Integer.toString(leafEnt.getValue() + 1)); // HEAD: not available
            conllEnt.add(fields.get(Constant.DEPREL)); // DEPREL: Universal Stanford dependency relation to the HEAD (root iff HEAD = 0) or a defined language-specific subtype of one.
            String depString = fields.get(Constant.DEP);
            if (!Constant.MISSING.equals(depString)) {
                StringBuffer sb = new StringBuffer();
                String[] depList = depString.split("\\|");
                for (String depItem : depList) {
                    String[] item = depItem.split(":");
                    sb.append("|" + posMap.get(item[0]) + ":" + item[1]);
                }
                depString = sb.toString().substring(1);
            }
            conllEnt.add(depString); // DEP: Universal Stanford dependency relation to the HEAD (root iff HEAD = 0) or a defined language-specific subtype of one.
            conllEnt.add(fields.get(Constant.MISC));// MISC: Any other annotation. (original word form)
            conllEnt.add(fields.get(Constant.SRC_LINE)); // DEPREL: Universal Stanford dependency relation to the HEAD (root iff HEAD = 0) or a defined language-specific subtype of one.
            conllEnt.add(fields.get(Constant.SRC_ID)); // DEPREL: Universal Stanford dependency relation to the HEAD (root iff HEAD = 0) or a defined language-specific subtype of one.
            conllEnt.add(fields.get(Constant.SRC_FORM)); // DEPREL: Universal Stanford dependency relation to the HEAD (root iff HEAD = 0) or a defined language-specific subtype of one.
            ret.add(conllEnt);
        }
        return ret;
    }

    private void getAsPennTreebankString(int indent, int numOnLine, StringBuilder sb) {
        int numSpaces = indent - numOnLine;
        for (int i = 0; i < numSpaces; i++) {
            sb.append(" ");
        }
        if (isLexical) {
            sb.append(getSymbol());
        } else {
            sb.append("(");
            sb.append(getSymbol());

            // If this is a constant instead, then we have each depth in one column.
            int numNewChars = 1 + getSymbol().length();

            for (int i = 0; i < children.size(); i++) {
                NaryTree child = children.get(i);
                if (i == 0) {
                    // First child.
                    child.getAsPennTreebankString(indent + numNewChars + 1, indent + numNewChars, sb);
                } else {
                    // Other children.
                    sb.append("\n");
                    child.getAsPennTreebankString(indent + numNewChars + 1, 0, sb);
                }
            }
            sb.append(")");
        }
    }

    public enum ReaderState {
        START, LEXICAL, NONTERMINAL, DONE,
    }

    /**
     * Reads a full tree in Penn Treebank format. Such a tree should include an
     * outer set of parentheses. The returned tree will have initialized the
     * start/end fields.
     */
    public static NaryTree readTreeInPtbFormat(Reader reader) throws IOException {
        QFiles.readUntilCharacter(reader, '(');
        NaryTree root = readSubtreeInPtbFormat(reader);
        QFiles.readUntilCharacter(reader, ')');
        if (root == null) return null;
        root.updateStartEnd();
        return root;
    }

    public static NaryTree readTreeInUniversalConLLFormat(ArrayList<String> wordLines, int lineNumber) throws TreeBank.InvalidTreeException {
        ArrayList<NaryTree> naryTreeArray = new ArrayList<>();
        ArrayList<Integer> headArray = new ArrayList<>();
        ArrayList<String> sentenceTokensSrc = new ArrayList<>();
        for (int i = 0; i < wordLines.size(); ++i) {
            String line = wordLines.get(i);
            String[] items = line.split("\t");
            int idx = -1;
            try {
                idx = Integer.parseInt(items[0]);
            } catch (NumberFormatException e) {
                continue;
            }
            String form = items[1];
            sentenceTokensSrc.add(form);
            String lemma = items[2];
            String upos = items[3];
            String pos = items[4];
            String feat = items[5];
            headArray.add(Integer.parseInt(items[6]) - 1);
            if (headArray.size() != idx) throw new AssertionError("Wrong idx");
            String udep = items[7];
            String dep = items[8];
            String misc = items[9];
            NaryTree posNode = new NaryTree(form, idx - 1, idx, null, true);
            posNode.appendField(Constant.FORM, form);
            posNode.appendField(Constant.LEMMA, lemma);
            posNode.appendField(Constant.UPOS, upos);
            posNode.appendField(Constant.POS, pos);
            posNode.appendField(Constant.FEATURE, feat);
            posNode.appendField(Constant.DEPREL, udep);
            posNode.appendField(Constant.DEP, dep);
            posNode.appendField(Constant.MISC, misc);
            posNode.appendField(Constant.SRC_LINE, Constant.srcName + ":" + Integer.toString(lineNumber + i));
            posNode.appendField(Constant.SRC_ID, items[0]);
            posNode.appendField(Constant.SRC_FORM, form);
            ArrayList<NaryTree> tmpChild = new ArrayList<>();
            tmpChild.add(posNode);
            naryTreeArray.add(new NaryTree(upos, idx - 1, idx, tmpChild, false));
        }
        //Check Nonprojectivity
        for (int dep1 = 0; dep1 < headArray.size(); ++dep1) {
            int head1 = headArray.get(dep1);
            for (int dep2 = 0; dep2 < headArray.size(); ++dep2) {
                int head2 = headArray.get(dep2);
                if (head1 < 0 || head2 < 0)
                    continue;
                if (dep1 > head1 && head1 != head2)
                    if ((dep1 > head2 && dep1 < dep2 && head1 < head2) || (dep1 < head2 && dep1 > dep2 && head1 < dep2))
                        throw new TreeBank.InvalidTreeException();
                if (dep1 < head1 && head1 != head2)
                    if ((head1 > head2 && head1 < dep2 && dep1 < head2) || (head1 < head2 && head1 > dep2 && dep1 < dep2))
                        throw new TreeBank.InvalidTreeException();
            }
        }
        NaryTree root = null;
        boolean punctHead = false;
        for (int i = 0; i < naryTreeArray.size(); ++i) {
            NaryTree me = naryTreeArray.get(i);
            if (headArray.get(i) == -1) {
                root = me;
                if (me.isPunct()) punctHead = true;
                continue;
            }
            NaryTree header = naryTreeArray.get(headArray.get(i));
            if (header.isPunct()) punctHead = true;
            if (i < headArray.get(i))
                header.children.add(header.children.size() - 1, me);
            else
                header.children.add(me);
        }

        if (Constant.filterPuncts > 0 && punctHead)
            throw new TreeBank.InvalidTreeException();
        root.preOrderTraversal(new PushLeavesDown());
        if (Constant.filterPuncts == 1) root.postOrderFilterNodes(new FilterPuncts());
        else if (Constant.filterPuncts == 2) root.preOrderTraversal(new StorePuncts());
        root.updateStartEnd();
        NaryTree.CountMaxChildren maxChildren = new NaryTree.CountMaxChildren();
        root.postOrderTraversal(maxChildren);
        if (maxChildren.count > Constant.ruleLength)
            throw new TreeBank.InvalidTreeException();
        root.info = "# sentence-tokens-src: " + StringUtils.join(sentenceTokensSrc.toArray(), " ") + "\n";
        return root;
    }

    public static class CountMaxChildren implements Lambda.FnO1ToVoid<NaryTree> {
        public int count;

        public CountMaxChildren() {
            this.count = 0;
        }

        public void call(NaryTree node) {
            if (node.getChildren() != null && node.getChildren().size() > count)
                count = node.getChildren().size();
        }

    }

    private static class PushLeavesDown implements Lambda.FnO1ToVoid<NaryTree> {
        @Override
        public void call(NaryTree node) {
            if (!node.isLeaf() && node.getChildren().size() > 1) {
                List<NaryTree> children = node.getChildren();
                int lexIdx = 0;
                for (int i = 0; i < children.size(); ++i) {
                    if (children.get(i).isLexical()) {
                        lexIdx = i;
                        break;
                    }
                }
                NaryTree oldChild = children.get(lexIdx);
                ArrayList<NaryTree> childrenLst = new ArrayList<>();
                childrenLst.add(oldChild);
                NaryTree newChild = new NaryTree(Constant.HEADER_MARK + node.symbol, node.getStart(), node.getEnd(), childrenLst, false);
                children.set(lexIdx, newChild);
                node.setSymbol(node.getSymbol());
            }
        }
    }

    /**
     * Reads an NaryTreeNode from a string.
     * <p>
     * Example:
     * (NP (DT the) (NN board) )
     * <p>
     * Note that the resulting tree will NOT have the start/end fields initialized.
     *
     * @param reader
     * @return
     * @throws IOException
     */
    private static NaryTree readSubtreeInPtbFormat(Reader reader) throws IOException {
        ReaderState state = ReaderState.START;
        StringBuilder symbolSb = new StringBuilder();
        ArrayList<NaryTree> children = null;
        boolean isLexical = false;

        char[] cbuf = new char[1];
        while (reader.read(cbuf) != -1) {
            char c = cbuf[0];
            if (state == ReaderState.START) {
                if (c == '(') {
                    state = ReaderState.NONTERMINAL;
                } else if (c == ')') {
                    // This was the tail end of a tree.
                    break;
                } else if (!isWhitespace(c)) {
                    symbolSb.append(c);
                    state = ReaderState.LEXICAL;
                    isLexical = true;
                }
            } else if (state == ReaderState.LEXICAL) {
                if (isWhitespace(c) || c == ')') {
                    state = ReaderState.DONE;
                    symbolSb.append(c);
                    break;
                } else {
                    symbolSb.append(c);
                }
            } else if (state == ReaderState.NONTERMINAL) {
                if (isWhitespace(c)) {
                    children = readSubTreesInPtbFormat(reader);
                    state = ReaderState.DONE;
                    break;
                } else {
                    symbolSb.append(c);
                }
            } else {
                throw new IllegalStateException("Invalid state: " + state);
            }
        }
        if (state != ReaderState.DONE) {
            // This reader did not start with a valid PTB style tree.
            return null;
        }

        int start = NOT_INITIALIZED;
        int end = NOT_INITIALIZED;
        String symbol = symbolSb.toString();
        NaryTree root = new NaryTree(symbol, start, end, children, isLexical);
        return root;
    }

    private static ArrayList<NaryTree> readSubTreesInPtbFormat(Reader reader) throws IOException {
        ArrayList<NaryTree> trees = new ArrayList<>();
        while (true) {
            NaryTree tree = readSubtreeInPtbFormat(reader);
            if (tree != null) {
                if (tree.isLexical) {
                    String rootSymbol = tree.getSymbol();
                    char lastChar = rootSymbol.charAt(rootSymbol.length() - 1);
                    rootSymbol = rootSymbol.substring(0, rootSymbol.length() - 1);
                    tree.setSymbol(rootSymbol);
                    trees.add(tree);
                    if (lastChar == ')')
                        break;
                } else
                    trees.add(tree);
            } else
                break;
        }
        return trees;
    }

    public NaryTree copy() {
        NaryTree naryTree = new NaryTree(this);
        if (isLeaf())
            return naryTree;
        naryTree.children = new ArrayList<>();
        for (NaryTree child : children)
            naryTree.children.add(child.copy());
        return naryTree;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\t';
    }

    public void preOrderTraversal(Lambda.FnO1ToVoid<NaryTree> function) {
        // Visit this node.
        function.call(this);
        // Pre-order traversal of each child.
        if (children != null) {
            for (NaryTree child : children) {
                child.preOrderTraversal(function);
            }
        }
    }

    public void postOrderTraversal(Lambda.FnO1ToVoid<NaryTree> function) {
        if (children != null)
            for (NaryTree child : children)
                child.postOrderTraversal(function);
        function.call(this);
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public List<NaryTree> getChildren() {
        return children;
    }

    public boolean isLeaf() {
        return children == null || children.size() == 0;
    }

    /**
     * Updates all the start end fields, treating the current node as the root.
     */
    public void updateStartEnd() {
        ArrayList<NaryTree> leaves = getLeaves();
        for (int i = 0; i < leaves.size(); i++) {
            NaryTree leaf = leaves.get(i);
            leaf.start = i;
            leaf.end = i + 1;
        }
        postOrderTraversal(new UpdateStartEnd());
    }

    public interface NaryTreeNodeFilter {
        boolean accept(NaryTree node);
    }


    public static class FilterPuncts implements NaryTreeNodeFilter {
        public boolean accept(NaryTree node) {
            return !node.isPunct();
        }
    }

    public static class StorePuncts implements Lambda.FnO1ToVoid<NaryTree> {
        private FilterPuncts filterPuncts;

        public StorePuncts() {
            this.filterPuncts = new FilterPuncts();
        }

        @Override
        public void call(NaryTree node) {
            if (!node.isLeaf()) {
                ArrayList<NaryTree> filtChildren = new ArrayList<>();
                for (int i = 0; i < node.children.size(); ++i) {
                    NaryTree child = node.children.get(i);
                    if (this.filterPuncts.accept(child))
                        filtChildren.add(child);
                    else
                        node.punctPos.put(i, child);
                }
                node.children = filtChildren;
            }
        }
    }

    /**
     * Keep only those nodes which the filter accepts.
     */
    public void postOrderFilterNodes(final NaryTreeNodeFilter filter) {
        postOrderTraversal(new Lambda.FnO1ToVoid<NaryTree>() {
            @Override
            public void call(NaryTree node) {
                if (!node.isLeaf()) {
                    ArrayList<NaryTree> filtChildren = new ArrayList<NaryTree>();
                    for (NaryTree child : node.children) {
                        if (filter.accept(child)) {
                            filtChildren.add(child);
                        }
                    }
                    node.children = filtChildren.size() > 0 ? filtChildren : node.children;
                }
            }
        });
        updateStartEnd();
    }

    /**
     * Gets the leaves of this tree in left-to-right order.
     */
    public ArrayList<NaryTree> getLeaves() {
        LeafCollector leafCollector = new LeafCollector();
        postOrderTraversal(leafCollector);
        return leafCollector.leaves;
    }

    /**
     * Gets the lexical leaves of this tree in left-to-right order.
     */
    public ArrayList<NaryTree> getLexicalLeaves() {
        LexicalLeafCollector leafCollector = new LexicalLeafCollector();
        postOrderTraversal(leafCollector);
        return leafCollector.leaves;
    }

    public List<String> getWords() {
        ArrayList<NaryTree> leaves = getLexicalLeaves();
        ArrayList<String> words = new ArrayList<>(leaves.size());
        for (int i = 0; i < leaves.size(); i++) {
            words.add(leaves.get(i).symbol);
        }
        return words;
    }

    public int getSentLength() {
        return getWords().size();
    }

    private class LeafCollector implements Lambda.FnO1ToVoid<NaryTree> {

        public ArrayList<NaryTree> leaves = new ArrayList<NaryTree>();

        @Override
        public void call(NaryTree node) {
            if (node.isLeaf()) {
                leaves.add(node);
            }
        }

    }

    private class LexicalLeafCollector implements Lambda.FnO1ToVoid<NaryTree> {

        public ArrayList<NaryTree> leaves = new ArrayList<>();

        @Override
        public void call(NaryTree node) {
            if (node.isLeaf() && node.isLexical()) {
                leaves.add(node);
            }
        }

    }


    private class UpdateStartEnd implements Lambda.FnO1ToVoid<NaryTree> {

        @Override
        public void call(NaryTree node) {
            if (!node.isLeaf()) {
                node.start = node.children.get(0).start;
                node.end = node.children.get(node.children.size() - 1).end;
            }
        }

    }

    public boolean isLexical() {
        return isLexical;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return "NaryTreeNode [symbol=" + getSymbol() + "_{" + start + ", "
                + end + "}, children=" + children + "]";
    }

    public void setChildren(List<NaryTree> children) {
        this.children = children;
    }
}

