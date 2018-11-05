/*
 * Title: DataLoader.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga;

import org.apache.commons.lang3.tuple.MutablePair;

import java.io.*;
import java.util.*;

/**
 * Original LIGA publication data loader class
 * Can also create testing and training sets from this data
 */
public class DataLoader {

    private boolean debug = true;

    public List<MutablePair<String, String>> dataset;
    public int n; // ngram length
    private File directory; //path

    private long seed; // shuffle seed
    private int shuffleN; //number of shufflings

    public List<MutablePair<String, String>> train = new ArrayList<>(); // training data
    public List<MutablePair<String, String>> test = new ArrayList<>(); // testing data
    public List<MutablePair<String, String>> init = new ArrayList<>(); // initial labeled data

    private double testDataPart; // test data %
    private double initDataPart; // initial-labeled data, part of training one (rest is used in active learning)

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

    public DataLoader(DataLoaderBuilder builder) {
        this.n = builder.n;
        this.directory = new File(builder.directory);
        this.dataset = new ArrayList<>();
        this.seed = builder.seed;
        this.shuffleN = builder.shuffleN;
        this.testDataPart = builder.testDataPart;
        this.initDataPart = builder.initDataPart;
    }

    /**
     * Slow method, because of original data structure
     * Reads files from directory to model. Works with main (Upper) folder. Ignores non-folders (files).
     * Structure: main folder contains subfolders (language labels: "en", "de", etc.), which contain files with text (one file - one text)
     */
    public void loadFromDirectorySubdirectories() {
        if (directory.listFiles() != null) {
            for (File fileEntry : directory.listFiles()) {
                if (fileEntry.isDirectory()) {
                    if (debug) System.out.println(fileEntry.getAbsolutePath());
                    loadFromDirectoryFiles(fileEntry);
                }
            }
        } else {
            System.out.println("Directory " + directory.getAbsolutePath() + " is empty");
        }
    }

    /**
     * Slow method, because of original data structure
     * Reads files from directory to model. Works with language (Lower) folders. Ignores non-files (directories).
     * Structure: main folder contains subfolders (language labels: "en", "de", etc.), which contain files with text (one file - one text)
     *
     * @param directory subfolder (folder name = language label)
     */
    public void loadFromDirectoryFiles(File directory) {
        int count = 0;
        String lang = directory.getName();
        if (directory.listFiles() != null) {
            for (File fileEntry : directory.listFiles()) {
                if (fileEntry.isFile()) {
                    String text = readTextFromFile(fileEntry);
                    if (!text.isEmpty()) {
                        dataset.add(new MutablePair<>(lang, text));
                        count++;
                    }
                }
            }
            if (debug) System.out.println(count + " non-empty files read");
        } else {
            System.out.println("Directory " + directory.getAbsolutePath() + " is empty");
        }

    }

    /**
     * Load all entries from the csv-like file. Entries are: lang_code, text. Entries are divided with separator
     *
     * @param path path to save the file
     * @param sep  separator
     * @param headers number of header lines
     */
    public void loadFromFile(String path, String sep, int headers) {
        File file = new File(path);
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String textLine; (textLine = br.readLine()) != null; ){
                count++;
                if (!textLine.trim().isEmpty() && (headers < count)){
                    String[] strings = textLine.trim().split(sep);
                    String first = strings[0];
                    StringBuilder rest = new StringBuilder();
                    for (int i = 1; i < strings.length; i++) rest.append(strings[i]);
                    dataset.add(new MutablePair<>(first, rest.toString()));
                }
            }
            if (debug) System.out.println((count-headers) + " non-empty entries read");
        } catch (IOException e) {
            System.out.println("Can't read file " + file.getAbsolutePath());
        }
    }

    /**
     * Load all loaded entries to the csv-like file. Entries are: lang_code, text. Entries are divided with separator
     *
     * @param path path to save the file
     * @param sep  separator
     */
    public void saveToFile(String path, String sep) {
        File file =new File(path);
        if (checkFilePath(file)) {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
                //header
                bw.write("lang" + sep + "text");
                bw.newLine();
                // content
                for (MutablePair<String, String> pair : dataset) {
                    bw.write(pair.getLeft() + sep + pair.getRight());
                    bw.newLine();
                    bw.flush();
                }
                bw.close();
                System.out.println("Data successfully saved to the file: " + file);
            } catch (IOException e) {
                System.err.println("Unable to write " + file);
                System.err.println(e.getMessage());
            }
        } else {
            System.err.println("Unable to save file: " + file);
        }
    }

    /**
     * reads text from the file
     *
     * @param file file
     */
    private String readTextFromFile(File file) {
        StringBuilder text = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String textLine; (textLine = br.readLine()) != null; )
                if (!textLine.trim().isEmpty()) text.append(textLine.trim()).append(" ");
        } catch (IOException e) {
            System.out.println("Can't read file " + file.getAbsolutePath());
        }
        return text.toString();
    }

    /**
     * Shuffle loaded data (with seed)
     *
     * @param seed seed
     * @param n    number of shuffle iterations
     */
    public void shuffleData(long seed, int n) {
        if (n < 1) n = 1;
        for (int i = 0; i < n; i++) Collections.shuffle(dataset, new Random(seed));
    }

    /**
     * Shuffle loaded data (with random seed)
     */
    public void shuffleData() {
        if (shuffleN < 0) shuffleN = 25;
        for (int i = 0; i < n; i++) Collections.shuffle(dataset, new Random(seed));
    }

    /**
     * Removes all loaded instances from dataset
     */
    public void dropDataset() {
        dataset.clear();
    }

    public boolean hasData() {
        return (dataset.size() > 0);
    }

    /**
     * Splits original dataset (testing method)
     */
    public void getTestData() {

        if (this.hasData()) {
            this.shuffleData();

            // get train and test sets
            train = splitBot(this.dataset, testDataPart);
            test = splitTop(this.dataset, testDataPart);
            // calculate threshold, free the memory
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
     * @param split   split coefficient
     */
    private List<MutablePair<String, String>> splitTop(List<MutablePair<String, String>> dataset, double split) {
        int splitId = (int) (dataset.size() * split);
        return dataset.subList(0, splitId);
    }

    /**
     * Returns bottom part of dataset
     *
     * @param dataset dataset to split
     * @param split   split coefficient
     */
    private List<MutablePair<String, String>> splitBot(List<MutablePair<String, String>> dataset, double split) {
        int splitId = (int) (dataset.size() * split);
        return dataset.subList(splitId, dataset.size());
    }

    /**
     * Checks if the file and path to file exist. If not - creates them.
     *
     * @param file file to check
     */
    private boolean checkFilePath(File file) {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            file.createNewFile();
            return true;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public static class DataLoaderBuilder {

        private int n = 3; // ngram length
        private String directory = "res"; //path
        private long seed = 0; // shuffle seed
        private int shuffleN = 25; //number of shufflings

        private double testDataPart = 0.1; // test data %
        private double initDataPart = 0.1; // initial-labeled data, part of training one (rest is used in active learning)

        public DataLoaderBuilder setN(int n) {
            this.n = n;
            return this;
        }

        public DataLoaderBuilder setDirectory(String directory) {
            this.directory = directory;
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
        public DataLoaderBuilder() {
        }

        public DataLoader build() {
            return new DataLoader(this);
        }
    }

}
