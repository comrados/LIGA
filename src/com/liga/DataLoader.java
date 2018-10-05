/*
 * Title: DataLoader.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DataLoader {

    public Map<String, List<String>> data;
    public int n; // ngram length
    private File directory; //path

    public DataLoader(DataLoaderBuilder builder){
        this.data = builder.data;
        this.n = builder.n;
        this.directory = new File(builder.directory);
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
        List<String> texts = new ArrayList<>();
        if (directory.listFiles() != null) {
            for (File fileEntry : directory.listFiles()) {
                if (fileEntry.isFile()) {
                    String text = readTextFromFile(fileEntry);
                    if (!text.isEmpty())
                        texts.add(text);
                }
            }
            data.put(lang, texts);
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
        for (Map.Entry<String, List<String>> entry: data.entrySet()){
            Collections.shuffle(entry.getValue(), new Random(seed));
        }
    }

    /**
     * Shuffle loaded data (random seed)
     */
    public void shuffleData(){
        for (Map.Entry<String, List<String>> entry: data.entrySet()){
            Collections.shuffle(entry.getValue());
        }
    }

    public static class DataLoaderBuilder {

        private Map<String, List<String>> data = new HashMap<>();
        private int n = 3; // ngram length
        private String directory; //path

        public DataLoaderBuilder setData(Map<String, List<String>> data) {
            this.data = data;
            return this;
        }

        public DataLoaderBuilder setN(int n) {
            this.n = n;
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
