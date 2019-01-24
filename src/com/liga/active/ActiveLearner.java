/*
 * Title: ActiveLearner.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

/*
 * Title: ActiveLearner.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.active;

import com.liga.LIGA;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.*;

/**
 * implements active learning pipeline
 */
public class ActiveLearner {

    private boolean debug = true;

    private Oracle oracle;
    private Sampler sampler;
    private LIGA liga;
    private int ngramLength = 3;
    private double trainThreshold; // training threshold (part of init + train)
    private boolean remainingLabeling; // adds the instances from the pool via automatic labeling after training
    private Set<String> langsSet = new HashSet<>();

    public int getNgramLength() {
        return ngramLength;
    }

    public void setNgramLength(int ngramLength) {
        this.ngramLength = ngramLength;
    }

    public boolean isRemainingLabeling() {
        return remainingLabeling;
    }

    public void setRemainingLabeling(boolean remainingLabeling) {
        this.remainingLabeling = remainingLabeling;
    }

    public Set<String> getLangsSet() {
        return langsSet;
    }

    List<MutablePair<String, String>> train; // training data
    List<MutablePair<String, String>> test; // testing data
    List<MutablePair<String, String>> init; // initial labeled data

    private Map<String, Double> results = new TreeMap<>(); // hashmap with results

    private List<Map<String, Double>> intermediateResults = new ArrayList<>(); // hashmap with results

    private List<Map<String, Map<String, Double>>> intermediateResultsConfusion = new ArrayList<>(); // hashmap with confusion matrices

    public Map<String, Double> getResults() {
        return results;
    }

    public List<Map<String, Double>> getIntermediateResults() {
        return intermediateResults;
    }

    public List<Map<String, Map<String, Double>>> getIntermediateResultsConfusion() {
        return intermediateResultsConfusion;
    }

    public ActiveLearner(ActiveLearnerBuilder builder) {
        this.remainingLabeling = builder.remainingLabeling;
        this.oracle = builder.oracle;
        this.sampler = builder.sampler;
        this.liga = builder.model;
        this.trainThreshold = builder.trainThreshold;
    }

    /**
     * active learning process
     */
    public void learn() {

        int iter = 0;

        if ((train.size() > 0) && (init.size() > 0)) {

            if (debug) System.out.println("Starting training");

            liga.addDataset(init, ngramLength);

            // looping restrictions
            int count = init.size();
            int trainLimit = (int) (trainThreshold * (train.size() + init.size()));

            if (debug) System.out.println("Training limit: " + trainLimit + " samples");
            if (debug) System.out.println("Training samples left: " + train.size());
            if (debug) System.out.println("Training samples used: " + count);

            // scores
            List<Map<String, Double>> scores;

            langsSet = getLangsSet(test);

            //clear intermediate results and run initial one
            intermediateResults.clear();
            intermediateTest(iter, test, count);

            do {

                iter++;

                scores = calcScores(train);

                // samples to label
                List<MutablePair<String, String>> samples = sampler.getSamples(train, scores);

                //label the selected samples
                samples = oracle.getLabels(samples);

                //add samples to model, increase count
                liga.addDataset(samples, ngramLength);
                count += samples.size();

                if (debug) System.out.println("Iteration " + iter + ", Training samples used: " + count);

                intermediateTest(iter, test, count);

            } while (count < trainLimit);

            // add meta parameters to results
            results.put("trainingThreshold", trainThreshold);
            results.put("trainingLimit", (double) trainLimit);
            results.put("iterations", (double) iter);
            results.put("initialSize", (double) init.size());
            results.put("endSize", (double) count);
            results.put("ngramLength", (double) ngramLength);

            if (remainingLabeling) {
                // relabel training data according to the model
                labelRemaining(train);
                // add the remaining relabeled samples to the model
                liga.addDataset(train, ngramLength);
            }

            if (debug) System.out.println("Training complete");
            if (debug) System.out.println("Training samples used: " + count);
            if (debug) System.out.println("Training samples left: " + train.size());

            if (test == null || test.size() == 0) {
                System.err.println("No testing data");
            } else {
                testModel();
            }

        } else {
            System.err.println("No training data or initial");
        }
    }

    /**
     * Sets datasets
     *
     * @param init  initial set, labeled
     * @param train training set
     * @param test  testing set
     */
    public void setDatasets(List<MutablePair<String, String>> init, List<MutablePair<String, String>> train,
                            List<MutablePair<String, String>> test) {
        this.init = new ArrayList<>(init);
        this.train = new ArrayList<>(train);
        this.test = new ArrayList<>(test);
    }

    /**
     * Sets datasets (no test)
     *
     * @param init  initial set, labeled
     * @param train training set
     */
    public void setDatasets(List<MutablePair<String, String>> init, List<MutablePair<String, String>> train) {
        this.init = init;
        this.train = train;
        this.test = null;
    }

    public void initModel(List<MutablePair<String, String>> init, int n) {
        liga.addDataset(init, n);
    }

    public List<Map<String, Double>> calcScores(List<MutablePair<String, String>> train) {
        List<Map<String, Double>> scores = new ArrayList<>();
        for (MutablePair<String, String> p : train) {
            scores.add(liga.classifyAll(p.getRight(), ngramLength));
        }
        return scores;
    }

    /**
     * labels remaining train set according to the model, counts the number of wrong relablings (if labels were known)
     *
     * @param data data to label
     */
    private void labelRemaining(List<MutablePair<String, String>> data) {
        int count = 0;
        for (int i = 0; i < data.size(); i++) {
            MutablePair<String, String> p = data.get(i);
            String lang = liga.classifyMostProbable(p.getRight(), ngramLength);
            if (!p.getLeft().equals(lang) && !p.getLeft().equals("")) {
                count++;
            }
            p.setLeft(lang);
        }
        results.put("wrongRemainingLablings", (double) count);
        if (debug) System.out.println("Wrong relablings: " + count);
    }

    /**
     * compares labels assigned to the model with real labels
     */
    private void testModel() {

        if (results.containsKey("testSize")) results.remove("testSize");
        if (results.containsKey("testCorrect")) results.remove("testCorrect");
        if (results.containsKey("testWrong")) results.remove("testWrong");
        if (results.containsKey("testCorrectPart")) results.remove("testCorrectPart");
        if (results.containsKey("testWrongPart")) results.remove("testWrongPart");

        int count = 0;

        for (int i = 0; i < test.size(); i++) {
            MutablePair<String, String> p = test.get(i);
            String lang = liga.classifyMostProbable(p.getRight(), ngramLength);
            if (p.getLeft().equals(lang)) {
                count++;
            }
            p.setLeft(lang);
        }

        results.put("testSize", (double) test.size());
        results.put("testCorrect", (double) count);
        results.put("testWrong", (double) (test.size() - count));
        results.put("testCorrectPart", (double) count / test.size());
        results.put("testWrongPart", (double) (test.size() - count) / test.size());
    }

    /**
     * intermediate test
     *
     * @param iter iteration number
     * @param test test set
     */
    private void intermediateTest(int iter, List<MutablePair<String, String>> test, int instCount) {
        Map<String, Double> results = new TreeMap<>();

        int count = 0;
        Map<String, Map<String, Double>> cms = createEmptyCMs(langsSet);

        for (int i = 0; i < test.size(); i++) {
            MutablePair<String, String> p = test.get(i);
            String langModel = liga.classifyMostProbable(p.getRight(), ngramLength);
            for (String lang : langsSet) {
                confusionMatrixUpdate(cms, lang, p.getLeft(), langModel);
            }
            if (p.getLeft().equals(langModel)) {
                count++;
            }
            p.setLeft(langModel);
        }

        // calculate and add to list
        calcPrecRecF1(cms);
        intermediateResultsConfusion.add(cms);

        results.put("iter", (double) iter);
        results.put("samplesUsed", (double) instCount);
        results.put("testSize", (double) test.size());
        results.put("testCorrect", (double) count);
        results.put("testWrong", (double) (test.size() - count));
        results.put("testCorrectPart", (double) count / test.size());
        results.put("testWrongPart", (double) (test.size() - count) / test.size());

        intermediateResults.add(results);
    }

    /**
     * Returns list of languages, presented in the test set
     *
     * @param test test set
     */
    private Set<String> getLangsSet(List<MutablePair<String, String>> test) {
        Set<String> langs = new HashSet<>();
        for (MutablePair<String, String> p : test) {
            if (!p.getLeft().equals("UNKNOWN"))
                langs.add(p.getLeft());
        }
        return langs;
    }

    /**
     * updates confusion matrices for all languages
     *
     * @param matrix    matrix to update
     * @param curLang   current language
     * @param realLang  real language label
     * @param modelLang language label, detected by model
     */
    protected static void confusionMatrixUpdate(Map<String, Map<String, Double>> matrix, String curLang, String realLang, String modelLang) {
        //TP
        if (curLang.equals(realLang) && realLang.equals(modelLang) && curLang.equals(modelLang)) {
            Map<String, Double> temp = matrix.get(curLang);
            temp.put("TP", temp.get("TP") + 1);
            matrix.put(curLang, temp);
            return;
        }
        //TN
        if (!curLang.equals(realLang) && realLang.equals(modelLang) && !curLang.equals(modelLang)) {
            Map<String, Double> temp = matrix.get(curLang);
            temp.put("TN", temp.get("TN") + 1);
            matrix.put(curLang, temp);
            return;
        }
        //FN
        if (!curLang.equals(modelLang)) {
            Map<String, Double> temp = matrix.get(curLang);
            temp.put("FN", temp.get("FN") + 1);
            matrix.put(curLang, temp);
            return;
        }
        //FP
        if (!curLang.equals(realLang)) { // else
            Map<String, Double> temp = matrix.get(curLang);
            temp.put("FP", temp.get("FP") + 1);
            matrix.put(curLang, temp);
            return;
        }
    }

    /**
     * initializes confusion matrices for given languages
     *
     * @param langs
     * @return
     */
    protected static Map<String, Map<String, Double>> createEmptyCMs(Set<String> langs) {
        Map<String, Map<String, Double>> matrices = new HashMap<>();
        for (String lang : langs)
            matrices.put(lang, createEmptyCM());
        return matrices;
    }

    /**
     * creates empty confusion martix for one language
     */
    protected static Map<String, Double> createEmptyCM() {
        Map<String, Double> matrix = new HashMap<>();
        matrix.put("TP", 0.0);
        matrix.put("TN", 0.0);
        matrix.put("FN", 0.0);
        matrix.put("FP", 0.0);
        matrix.put("prec", 0.0);
        matrix.put("rec", 0.0);
        matrix.put("f", 0.0);
        return matrix;
    }

    /**
     * Calculates precision, recall and F1-score for matrices
     *
     * @param matrix matrices
     */
    protected static void calcPrecRecF1(Map<String, Map<String, Double>> matrix) {
        for (Map.Entry<String, Map<String, Double>> entry : matrix.entrySet()) {
            Map<String, Double> t = entry.getValue();
            Double prec = t.get("TP") / (t.get("TP") + t.get("FP"));
            Double rec = t.get("TP") / (t.get("TP") + t.get("FN"));
            Double f = 2.0 * prec * rec / (prec + rec);
            t.put("prec", prec);
            t.put("rec", rec);
            t.put("f", f);
            matrix.put(entry.getKey(), t);
        }
    }


    /**
     * resets learning and resulting data (not the model)
     * to reset model use LIGA instances methods
     */
    public void resetLearner() {
        results.clear();
        train.clear();
        init.clear();
        test.clear();
    }

    public static class ActiveLearnerBuilder {

        private Oracle oracle;
        private Sampler sampler;
        private LIGA model;

        private boolean remainingLabeling = false;
        private double trainThreshold = 0.25; // training threshold (part of init + train)

        public ActiveLearnerBuilder setRemainingLabeling(boolean remainingLabeling) {
            this.remainingLabeling = remainingLabeling;
            return this;
        }

        public ActiveLearnerBuilder setTrainThreshold(double trainThreshold) {
            this.trainThreshold = trainThreshold;
            return this;
        }

        /**
         * builder
         */
        public ActiveLearnerBuilder(Oracle oracle, Sampler sampler, LIGA model) {
            this.oracle = oracle;
            this.sampler = sampler;
            this.model = model;
        }

        public ActiveLearner build() {
            return new ActiveLearner(this);
        }
    }

}
