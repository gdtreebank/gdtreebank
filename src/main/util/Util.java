package util;

import edu.jhu.util.Alphabet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by dqwang on 14/11/21.
 */
public class Util {

    public static double logFact(int num) {
        double ret = 0.;
        for (int i = 1; i <= num; ++i)
            ret += Math.log(i);
        return ret;
    }

    public static void collectNGram(String[] array, int min, int max, Map<String, Double> freq) {
        if (min > array.length) return;
        max = Math.min(array.length, max);
        String[] newArray = new String[array.length + 2 * max - 2];
        Arrays.fill(newArray, 0, max - 1, Constant.BOS);
        Arrays.fill(newArray, max - 1 + array.length, newArray.length, Constant.EOS);
        List<String> arr = Arrays.asList(newArray);
        for (int i = 0; i < array.length; ++i)
            newArray[i + max - 1] = array[i];
        for (int i = max - 1; i < arr.size(); ++i) {
            for (int j = min; j <= max; ++j) {
                String key = StringUtils.join(arr.subList(i - j + 1, i + 1), Constant.CHILDREN_DEL);
                freq.put(key, freq.containsKey(key) ? freq.get(key) + 1. : 1.);
            }
        }
    }

    public static <K, V extends Comparable<? super V>>
    List<Entry<K, V>> entriesSortedByValues(Map<K, V> map) {

        List<Entry<K, V>> sortedEntries = new ArrayList<Entry<K, V>>(map.entrySet());

        Collections.sort(sortedEntries,
                new Comparator<Entry<K, V>>() {
                    @Override
                    public int compare(Entry<K, V> e1, Entry<K, V> e2) {
                        return e2.getValue().compareTo(e1.getValue());
                    }
                }
        );

        return sortedEntries;
    }

    public static <T> void swap(T[] arr, int i, int j) {
        T a = arr[i];
        arr[i] = arr[j];
        arr[j] = a;
    }

    public static void swap(int[] arr, int i, int j) {
        int a = arr[i];
        arr[i] = arr[j];
        arr[j] = a;
    }


    public static void updateProgress(double progressPercentage) {
        final int width = 50; // progress bar width in chars

        System.out.print("\r[");
        int i = 0;
        for (; i <= (int) (progressPercentage * width); i++) {
            System.out.print("|");
        }
        for (; i < width; i++) {
            System.out.print(" ");
        }
        System.out.print("]");
    }

    /**
     * Calculate n!
     *
     * @param n
     * @return
     */
    public static int factorial(int n) {
        //init to 1
        int factorial = 1;
        //multiply by n..1
        for (int i = n; i > 0; i--) {
            factorial = factorial * i;
        }
        //return result
        return factorial;
    }

    public static String join(String del, String[] strArray) {
        StringBuffer res = new StringBuffer(strArray[0]);
        for (int i = 1; i < strArray.length; ++i)
            res.append(del + strArray[i]);
        return res.toString();
    }


    public static Map<Integer, Double> toIntegerMap(Map<String, Double> stringMap, Alphabet<String> alphabet) {
        Map<Integer, Double> integerMap = new HashMap<>();
        for (Entry<String, Double> entry : stringMap.entrySet())
            integerMap.put(alphabet.lookupIndex(entry.getKey()), entry.getValue());
        return integerMap;

    }

    public static void loadAlphabet(File file, Alphabet<String> alphabet) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        while (scanner.hasNext())
            alphabet.lookupIndex(scanner.nextLine().trim());
        scanner.close();
    }

    public static void saveAlphabet(File file, Alphabet<String> alphabet) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        for (String feat : alphabet.getObjects())
            bufferedWriter.write(feat + "\n");
        bufferedWriter.close();
    }

    public static <T> void normalize(Map<T, Double> dist) {
        double sum = .0;
        for (double val : dist.values())
            sum += val;
        for (T key : dist.keySet())
            dist.put(key, dist.get(key) / sum);
    }

    public static double entropy(double num, double total) {
        if (num == 0.) return 0.;
        return (num / total) * (Math.log(num) - Math.log(total));

    }

    public static Map<String, Double> loadText(File fn) throws IOException, ClassNotFoundException {
        Map<String, Double> model = new HashMap<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fn));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] ent = line.split(Constant.FEATURE_VAL_DEL);
            model.put(ent[0], Double.parseDouble(ent[1]));
        }
        return model;
    }

    public static void update(Map<String, Double> from, Map<String, Double> to, String prfx, double discount) {
        for (String k : from.keySet()) {
            update(to, prfx + k, discount * from.get(k));
        }
    }

    public static void update(Map<String, Double> map, String key, double val) {
        map.put(key, map.containsKey(key) ? map.get(key) + val : val);
    }


    public static ArrayList<Entry<Integer, Integer>> getSwapPos(int N) {
        ArrayList<Entry<Integer, Integer>> swapPosList = new ArrayList<>();
        int[] p = new int[N];     // permutation
        int[] pi = new int[N];     // inverse permutation
        int[] dir = new int[N];     // direction = +1 or -1
        for (int i = 0; i < N; i++) {
            dir[i] = -1;
            p[i] = i;
            pi[i] = i;
        }
        perm(0, p, pi, dir, swapPosList);
        swapPosList.add(new AbstractMap.SimpleEntry<>(0, 1));
        return swapPosList;
    }

    private static void perm(int n, int[] p, int[] pi, int[] dir, ArrayList<Entry<Integer, Integer>> swapPosList) {

        // base case - print out permutation
        if (n >= p.length) {
            return;
        }

        perm(n + 1, p, pi, dir, swapPosList);
        for (int i = 0; i <= n - 1; i++) {
            // swap

            int min = Math.min(pi[n], pi[n] + dir[n]), max = Math.max(pi[n], pi[n] + dir[n]);
            swapPosList.add(new AbstractMap.SimpleEntry<>(min, max));
            int z = p[pi[n] + dir[n]];
            p[pi[n]] = z;
            p[pi[n] + dir[n]] = n;
            pi[z] = pi[n];
            pi[n] = pi[n] + dir[n];

            perm(n + 1, p, pi, dir, swapPosList);
        }
        dir[n] = -dir[n];
    }

    public static int getHeaderIdx(String[] nodeList) {
        int ret = -1;
        for (int i = 0; i < nodeList.length; ++i)
            if (nodeList[i].startsWith(Constant.HEADER_MARK))
                return i;
        return ret;
    }

}
