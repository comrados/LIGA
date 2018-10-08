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
    private double split; // split (specify the size of training data from 0 to 1)

    public DataLoader(DataLoaderBuilder builder){
        this.n = builder.n;
        this.directory = new File(builder.directory);
        this.split = builder.split;
        //this.data = new HashMap<>();
        this.dataset = new ArrayList<>();
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
     * @param seed seed for shuffling
     */
    public void shuffleData(int seed){
        //for (Map.Entry<String, List<String>> entry: data.entrySet())
            //Collections.shuffle(entry.getValue(), new Random(seed));
        Collections.shuffle(dataset, new Random(seed));
    }

    /**
     * Shuffle loaded data (random seed)
     */
    public void shuffleData(){
        //for (Map.Entry<String, List<String>> entry: data.entrySet())
            //Collections.shuffle(entry.getValue());
        Collections.shuffle(dataset);
    }

    /**
     * Returns training set
     */
    public List<MutablePair<String, String>> getTrainSet(){
        int splitId = (int) (dataset.size() * split);
        return dataset.subList(0, splitId);
    }

    /**
     * Returns test set
     */
    public List<MutablePair<String, String>> getTestSet(){
        int splitId = (int) (dataset.size() * split);
        return dataset.subList(splitId, dataset.size());
    }

    /**
     * Removes all loaded instances from dataset
     */
    public void dropDataset(){
        dataset.clear();
    }

    public static class DataLoaderBuilder {

        private int n = 3; // ngram length
        private String directory; //path
        private double split = 0.9; // split (specify the size of training data from 0 to 1)

        public DataLoaderBuilder setN(int n) {
            this.n = n;
            return this;
        }

        public DataLoaderBuilder setSplit(double split) {
            this.split = split;
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
