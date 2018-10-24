/*
 * Title: MarginSampler.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.active;

import org.apache.commons.lang3.tuple.MutablePair;

import java.util.*;

public class MarginSampler implements Sampler {

    private int samplesBatch; //number of samples to select

    public int getSamplesBatch() {
        return samplesBatch;
    }

    public void setSamplesBatch(int samplesBatch) {
        this.samplesBatch = samplesBatch;
    }

    public MarginSampler(MarginSamplerBuilder builder){
        this.samplesBatch = builder.samplesBatch;
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
        List<MutablePair<String, String>> samples = new ArrayList<>();
        Map<Double, List<Integer>> margins = getMargins(scores);
        Map<Double, List<Integer>> margins2 = getNoneOnes(margins);
        List<MutablePair<String, String>> chosen = chooseSamples(margins, train);
        return chosen;
    }

    /**
     * calculates margins for each sample
     *
     * @param scores
     * @return
     */
    private Map<Double, List<Integer>> getMargins(List<Map<String, Double>> scores){
        Map<Double, List<Integer>> margins = new TreeMap<>();

        for (int i = 0; i < scores.size(); i++) {
            Map<String, Double> entries = scores.get(i);
            // if entries is empty or small - just set large margin
            if ((entries == null) || (entries.size() < 2)) {
                // check if exists and add large value
                if (margins.containsKey(1000d)) {
                    margins.get(1000d).add(i);
                } else {
                    List<Integer> ints = new ArrayList<>();
                    ints.add(i);
                    margins.put(1000d, ints);
                }
            } else {
                double margin = getMargin(entries);
                // check if exists and add margin
                if (margins.containsKey(margin)) {
                    margins.get(margin).add(i);
                } else {
                    List<Integer> ints = new ArrayList<>();
                    ints.add(i);
                    margins.put(margin, ints);
                }
            }
        }

        return margins;
    }

    /**
     * calculates margin for given sample
     *
     * @param entries scores
     */
    private Double getMargin(Map<String, Double> entries){

        double margin = 1000d;

        double first = -1;
        double second = -1;

        // Get the best score or return unknown
        for (Map.Entry<String, Double> entry : entries.entrySet()) {
            if (entry.getValue() > first) {
                second = first;
                first = entry.getValue();
            } else {
                if (entry.getValue() > second){
                    second = entry.getValue();
                }
            }
        }

        if ((first > second) && (second > 0)){
            margin = first - second;
        }

        return margin;
    }

    /**
     * selects n samples according to calculated margins
     *
     * @param margins margins
     * @param train all samples
     */
    private List<MutablePair<String, String>> chooseSamples(Map<Double, List<Integer>> margins,
                                                            List<MutablePair<String, String>> train){
        List<Integer> excl = new ArrayList<>();
        List<MutablePair<String, String>> chosen = new ArrayList<>();
        int count = 0;
        while (count < this.samplesBatch){
            for (Map.Entry<Double, List<Integer>> entry : margins.entrySet()) {
                for (Integer num: entry.getValue()){
                    if (count < this.samplesBatch){
                        chosen.add(train.get(num));
                        count++;
                        excl.add(num);
                    }
                }
            }
        }
        removeChosen(excl, train);
        return chosen;
    }

    private Map<Double, List<Integer>> getNoneOnes(Map<Double, List<Integer>> margs){
        Map<Double, List<Integer>> res = new TreeMap<>();
        for (Map.Entry<Double, List<Integer>> entry : margs.entrySet()) {
            if (entry.getValue().size() != 1) res.put(entry.getKey(), entry.getValue());
        }
        return res;
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

    public static class MarginSamplerBuilder{

        int samplesBatch; //number of samples to select

        public MarginSamplerBuilder(int samplesBatch){
            this.samplesBatch = samplesBatch;
        }

        public MarginSampler build() {
            return new MarginSampler(this);
        }

    }

}
