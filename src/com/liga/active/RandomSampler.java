/*
 * Title: RandomSampler.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.active;

import org.apache.commons.lang3.tuple.MutablePair;

import java.util.*;

/**
 * Randomly samples from the pool. Effective, when initial labeled data part is very small
 */
public class RandomSampler implements Sampler {

    private int samplesBatch; // number of samples to select
    Integer seed; // seed
    Random random; // random generator

    public Integer getSeed() {
        return seed;
    }

    public int getSamplesBatch() {
        return samplesBatch;
    }

    public void setSamplesBatch(int samplesBatch) {
        this.samplesBatch = samplesBatch;
    }

    public RandomSampler(RandomSamplerBuilder builder){
        this.samplesBatch = builder.samplesBatch;
        this.seed = builder.seed;
        // random seed or not random seed
        if (this.seed != null){
            this.random = new Random(this.seed);
        } else {
            this.random = new Random();
        }
    }

    /**
     * get n samples from train data, prefers samples with lowest margin between two best scores
     *
     * @param train training data
     * @param scores scores
     */
    @Override
    public List<MutablePair<String, String>> getSamples(List<MutablePair<String, String>> train,
                                                        List<Map<String, Double>> scores) {
        return chooseSamples(train);
    }

    /**
     * selects n samples according to calculated margins
     *
     * @param train all samples
     */
    private List<MutablePair<String, String>> chooseSamples(List<MutablePair<String, String>> train){
        List<Integer> excl = new ArrayList<>();
        List<MutablePair<String, String>> chosen = new ArrayList<>();
        int count = 0;
        int limit = this.samplesBatch;
        if (this.samplesBatch > train.size()){
            limit = train.size();
        }
        while (count < limit){
            Integer randInt = random.nextInt(train.size());
            if (!excl.contains(randInt)){
                chosen.add(train.get(randInt));
                excl.add(randInt);
                count++;
            }
        }
        removeChosen(excl, train);
        return chosen;
    }

    /**
     * removes selected samples from original list
     *
     * @param excl numbers of samples to remove
     * @param train samples list
     */
    private void removeChosen(List<Integer> excl, List<MutablePair<String, String>> train){
        excl.sort(Collections.reverseOrder());
        for (Integer i: excl){
            MutablePair<String, String> p = train.get(i);
            train.remove(p);
        }
    }

    public static class RandomSamplerBuilder {

        int samplesBatch; // number of samples to select
        Integer seed = null; // seed

        public RandomSamplerBuilder setSeed(int seed) {
            this.seed = seed;
            return this;
        }

        public RandomSamplerBuilder(int samplesBatch){
            this.samplesBatch = samplesBatch;
        }

        public RandomSampler build() {
            return new RandomSampler(this);
        }

    }

}
