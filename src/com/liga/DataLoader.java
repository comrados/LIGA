/*
 * Title: DataLoader.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga;

import org.apache.commons.lang3.tuple.MutablePair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DataLoader {

    //public Map<String, List<String>> data;
    public List<MutablePair<String, String>> dataset;
    public int n; // ngram length
    private File directory; //path

    private long seed; // shuffle seed
    private int shuffleN; //number of shufflings

    public List<MutablePair<String, String>> train = new ArrayList<>(); // training data
    public List<MutablePair<String, String>> test = new ArrayList<>(); // testing data
    public List<MutablePair<String, String>> init = new ArrayList<>(); // initial labeled data

    private double trainThresholdPart; // % of all data labeled before stop of active learning
    private int trainThreshold = 0; // training threshold as number
    private double testDataPart; // test data %
    private double initDataPart; // initial-labeled data, part of training one (rest is used in active learning)

    public double getTrainThresholdPart() {
        return trainThresholdPart;
    }

    public void setTrainThresholdPart(double trainThresholdPart) {
        this.trainThresholdPart = trainThresholdPart;
    }

    public double getTestDataPart() {
        return testDataPart;
    }

    public void setTestDataPart(double testDataPart) {
        this.testDataPart = testDataPart;
    }

    public double getInitDataPart() {
        return initDataPart;
    }

    public void setInitDataPart(double initDataPart) {
        this.initDataPart = initDataPart;
    }

    public DataLoader(DataLoaderBuilder builder){
        this.n = builder.n;
        this.directory = new File(builder.directory);
        //this.data = new HashMap<>();
        this.dataset = new ArrayList<>();
        this.seed = builder.seed;
        this.shuffleN = builder.shuffleN;
        this.trainThresholdPart = builder.trainThresholdPart;
        this.testDataPart = builder.testDataPart;
        this.initDataPart = builder.initDataPart;
    }

    /**
     * Reads files from directory to model. Works with main (Upper) folder. Ignores non-folders (files).
     * Structure: main folder contains subfolders (language labels: "en", "de", etc.), which contain files with text (one file - one text)
     */
    public void readFilesUpper() {
        if (directory.listFiles() != null){
            for (File fileEntry : directory.listFiles()) {
                if (fileEntry.isDirectory()) {
                    System.out.println(fileEntry.getAbsolutePath());
                    readFilesLower(fileEntry);
                }
            }
        } else {
            System.out.println("Directory " + directory.getAbsolutePath() + " is empty");
        }
    }

    /**
     * Reads files from directory to model. Works with language (Lower) folders. Ignores non-files (directories).
     * Structure: main folder contains subfolders (language labels: "en", "de", etc.), which contain files with text (one file - one text)
     *
     * @param directory subfolder (folder name = language label)
     */
    public void readFilesLower(File directory) {
        String lang = directory.getName();
        //List<String> texts = new ArrayList<>();
        if (directory.listFiles() != null) {
            for (File fileEntry : directory.listFiles()) {
                if (fileEntry.isFile()) {
                    String text = readTextFromFile(fileEntry);
                    if (!text.isEmpty()){
                        //texts.add(text);
                        dataset.add(new MutablePair<>(lang, text));
                    }
                }
            }
            //data.put(lang, texts);
        } else {
            System.out.println("Directory " + directory.getAbsolutePath() + " is empty");
        }

    }

    /**
     * reads text from the file
     *
     * @param file file
     */
    private String readTextFromFile(File file){
        StringBuilder text = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String textLine; (textLine = br.readLine()) != null;)
                if (!textLine.trim().isEmpty()) text.append(textLine.trim()).append(" ");
        } catch (IOException e){
            System.out.println("Can't read file " + file.getAbsolutePath());
        }
        return text.toString();
    }

    /**
     * Shuffle loaded data (with seed)
     *
     * @param seed seed
     * @param n number of shuffle iterations
     */
    public void shuffleData(long seed, int n){
        if (n < 1) n = 1;
        for (int i = 0; i < n; i++) Collections.shuffle(dataset, new Random(seed));
    }

    /**
     * Shuffle loaded data (with random seed)
     */
    public void shuffleData(){
        if (shuffleN < 0) shuffleN = 25;
        for (int i = 0; i < n; i++) Collections.shuffle(dataset, new Random(seed));
    }

    /**
     * Removes all loaded instances from dataset
     */
    public void dropDataset(){
        dataset.clear();
    }

    public boolean hasData(){
        return (dataset.size() > 0);
    }

    /**
     * Splits original dataset (testing method)
     */
    public void getTestData(){

        if (this.hasData()){
            this.shuffleData();

            // get train and test sets
            train = splitBot(this.dataset, testDataPart);
            test = splitTop(this.dataset, testDataPart);
            // calculate threshold, free the memory
            trainThreshold = train.size() * trainThreshold;
            //loader.dropDataset();
            // get sets for active learning
            init = splitTop(train, initDataPart);
            train = splitBot(train, initDataPart);
        } else {
            System.err.println("No data was loaded");
        }

    }

    /**
     * Returns top part of dataset
     *
     * @param dataset dataset to split
     * @param split split coefficient
     */
    private List<MutablePair<String, String>> splitTop(List<MutablePair<String, String>> dataset, double split){
        int splitId = (int) (dataset.size() * split);
        return dataset.subList(0, splitId);
    }

    /**
     * Returns bottom part of dataset
     *
     * @param dataset dataset to split
     * @param split split coefficient
     */
    private List<MutablePair<String, String>> splitBot(List<MutablePair<String, String>> dataset, double split){
        int splitId = (int) (dataset.size() * split);
        return dataset.subList(splitId, dataset.size());
    }

    public static class DataLoaderBuilder {

        private int n = 3; // ngram length
        private String directory; //path
        private long seed = 0; // shuffle seed
        private int shuffleN = 25; //number of shufflings

        private double trainThresholdPart = 0.5; // % of all data labeled before stop of active learning
        private double testDataPart = 0.1; // test data %
        private double initDataPart = 0.1; // initial-labeled data, part of training one (rest is used in active learning)

        public DataLoaderBuilder setN(int n) {
            this.n = n;
            return this;
        }

        public DataLoaderBuilder setSeed(long seed) {
            this.seed = seed;
            return this;
        }

        public DataLoaderBuilder setShuffleN(int shuffleN) {
            this.shuffleN = shuffleN;
            return this;
        }

        public DataLoaderBuilder setTrainThresholdPart(double trainThresholdPart) {
            this.trainThresholdPart = trainThresholdPart;
            return this;
        }

        public DataLoaderBuilder setTestDataPart(double testDataPart) {
            this.testDataPart = testDataPart;
            return this;
        }

        public DataLoaderBuilder setInitDataPart(double initDataPart) {
            this.initDataPart = initDataPart;
            return this;
        }

        /**
         * builder
         */
        public DataLoaderBuilder(String dir) {
            this.directory = dir;
        }

        public DataLoader build() {
            return new DataLoader(this);
        }
    }

}
