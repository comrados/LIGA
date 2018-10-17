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

import java.util.List;

public class ActiveLearner {

    private Oracle oracle;
    private Sampler sampler;
    private LIGA model;

    List<MutablePair<String, String>> train; // training data
    List<MutablePair<String, String>> test; // testing data
    List<MutablePair<String, String>> init; // initial labeled data

    public ActiveLearner(ActiveLearnerBuilder builder){
        this.oracle = builder.oracle;
        this.sampler = builder.sampler;
        this.model = builder.model;
    }

    /**
     * active learning process
     */
    public void learn(){

            if (train.size() > 0){
                model.addDataset(init, 3);

                if (init == null || init.size() == 0){
                    System.err.println("No initial data");
                    System.err.println("Doing initial samplig");
                    //TODO initial sampling
                }

                // TODO get scores

                // TODO get samples to label

                // TODO add samples to model

                // TODO add samples to modelcheck thresholds

                if (test == null || test.size() == 0){
                    System.err.println("No testing data");
                } else{
                    //TODO run test
                }

            } else{
                System.err.println("No training data");
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
        this.init = init;
        this.train = train;
        this.test = test;
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

    /**
     * Sets datasets (no test, no init)
     *
     * @param train training set
     */
    public void setDatasets(List<MutablePair<String, String>> train){
        this.init = null;
        this.train = train;
        this.test = null;
    }

    public void initModel(List<MutablePair<String, String>> init, int n){
        model.addDataset(init, n);
    }



    public static class ActiveLearnerBuilder{

        private Oracle oracle;
        private Sampler sampler;
        private LIGA model;

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
