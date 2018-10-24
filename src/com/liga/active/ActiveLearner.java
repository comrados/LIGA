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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveLearner {

    private boolean debug = true;

    private Oracle oracle;
    private Sampler sampler;
    private LIGA liga;
    private int ngramLength = 3;

    public int getNgramLength() {
        return ngramLength;
    }

    public void setNgramLength(int ngramLength) {
        this.ngramLength = ngramLength;
    }

    List<MutablePair<String, String>> train; // training data
    List<MutablePair<String, String>> test; // testing data
    List<MutablePair<String, String>> init; // initial labeled data

    private double trainThreshold; // training threshold (part of init + train)

    private Map<String, Double> results = new HashMap<>(); // hashmap with results

    public Map<String, Double> getResults() {
        return results;
    }

    public ActiveLearner(ActiveLearnerBuilder builder){
        this.oracle = builder.oracle;
        this.sampler = builder.sampler;
        this.liga = builder.model;
        this.trainThreshold = builder.trainThreshold;
    }

    /**
     * active learning process
     */
    public void learn(){

        int iter = 0;

            if ((train.size() > 0) && (init.size() > 0)){

                if (debug) System.out.println("Starting training");

                liga.addDataset(init, ngramLength);

                // looping restrictions
                int count = init.size();
                int limit = (int) (trainThreshold * (train.size() + init.size()));

                if (debug) System.out.println("Training limit: " + limit + " samples");
                if (debug) System.out.println("Training samples left: " + train.size());
                if (debug) System.out.println("Training samples used: " + count);

                // scores
                List<Map<String, Double>> scores;

                do {

                    iter++;
                    if (debug) System.out.println("Iteration: " + iter);

                    scores = calcScores(train);

                    // samples to label
                    List<MutablePair<String, String>> samples = sampler.getSamples(train, scores);

                    if (debug) System.out.println(samples.size() + " samples selected");

                    //label the selected samples
                    samples = oracle.getLabels(samples);

                    //add samples to model, increase count
                    liga.addDataset(samples, ngramLength);
                    count += samples.size();

                    if (debug) System.out.println("Training samples used: " + count);

                } while (count < limit);

                // add meta parameters to results
                results.put("trainingThreshold", trainThreshold);
                results.put("trainingLimit", (double) limit);
                results.put("iterations", (double) iter);
                results.put("initialSize", (double) init.size());
                results.put("endSize", (double) count);
                results.put("ngramLength", (double) ngramLength);

                // relabel training data according to the model
                labelRemaining(train);

                // add the remaining relabeled samples to the model
                liga.addDataset(train, ngramLength);

                if (debug) System.out.println("Training complete");
                if (debug) System.out.println("Training samples used: " + count);
                if (debug) System.out.println("Training samples left: " + train.size());

                if (test == null || test.size() == 0){
                    System.err.println("No testing data");
                } else{
                    testModel(test);
                }

            } else{
                System.err.println("No training data or initial");
            }
    }

    /**
     * Sets datasets
     *
     * @param init initial set, labeled
     * @param train training set
     * @param test testing set
     */
    public void setDatasets(List<MutablePair<String, String>> init, List<MutablePair<String, String>> train,
                            List<MutablePair<String, String>> test){
        this.init = new ArrayList<>(init);
        this.train = new ArrayList<>(train);
        this.test = new ArrayList<>(test);
    }

    /**
     * Sets datasets (no test)
     *
     * @param init initial set, labeled
     * @param train training set
     */
    public void setDatasets(List<MutablePair<String, String>> init, List<MutablePair<String, String>> train){
        this.init = init;
        this.train = train;
        this.test = null;
    }

    public void initModel(List<MutablePair<String, String>> init, int n){
        liga.addDataset(init, n);
    }

    public List<Map<String, Double>> calcScores(List<MutablePair<String, String>> train){
        List<Map<String, Double>> scores = new ArrayList<>();
        for (MutablePair<String, String> p: train){
            scores.add(liga.classifyAll(p.getRight(), ngramLength));
        }
        return scores;
    }

    /**
     * labels remaining train set according to the model, counts the number of wrong relablings (if labels were known)
     *
     * @param data data to label
     */
    private void labelRemaining(List<MutablePair<String, String>> data){
        int count = 0;
        for (int i = 0; i < data.size(); i++){
            MutablePair<String, String> p = data.get(i);
            String lang = liga.classifyMostProbable(p.getRight(), ngramLength);
            if (!p.getLeft().equals(lang) && !p.getLeft().equals("")){
                count++;
            }
            p.setLeft(lang);
        }
        results.put("wrongLablings", (double) count);
        if (debug) System.out.println("Wrong relablings: " + count);
    }

    /**
     * compares labels assigned to the model with real labels
     *
     * @param testData
     */
    public Map<String, Double> testModel(List<MutablePair<String, String>> testData){

        if (results.containsKey("testSize")) results.remove("testSize");
        if (results.containsKey("testCorrect")) results.remove("testCorrect");
        if (results.containsKey("testWrong")) results.remove("testWrong");
        if (results.containsKey("testCorrectPart")) results.remove("testCorrectPart");
        if (results.containsKey("testWrongPart")) results.remove("testWrongPart");

        int count = 0;

        for (int i = 0; i < testData.size(); i++){
            MutablePair<String, String> p = testData.get(i);
            String lang = liga.classifyMostProbable(p.getRight(), ngramLength);
            if (p.getLeft().equals(lang)){
                count++;
            }
            p.setLeft(lang);
        }

        results.put("testSize", (double) testData.size());
        results.put("testCorrect", (double) count);
        results.put("testWrong", (double) (testData.size() - count));
        results.put("testCorrectPart", (double) count / testData.size());
        results.put("testWrongPart", (double) (testData.size() - count) / testData.size());

        return results;
    }

    public static class ActiveLearnerBuilder{

        private Oracle oracle;
        private Sampler sampler;
        private LIGA model;

        private double trainThreshold = 0.25; // training threshold (part of init + train)

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
