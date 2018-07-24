/*
 * Title: TextFileLoader.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TextFileLoader {

    /**
     * Reads files from directory to model. Works with main (Upper) folder. Ignores non-folders (files).
     * Structure: main folder contains subfolders (language labels: "en", "de", etc.), which contain files with text (one file - one text)
     *
     * @param directory main folder
     * @param liga model
     * @param ngramLength ngram length
     */
    public static void readFilesToModelUpper(File directory, LIGA liga, int ngramLength) {
        if (directory.listFiles() != null){
            for (File fileEntry : directory.listFiles()) {
                if (fileEntry.isDirectory()) {
                    System.out.println(fileEntry.getAbsolutePath());
                    readFilesToModelLower(fileEntry, liga, ngramLength);
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
     * @param liga model
     * @param ngramLength ngram length
     */
    public static void readFilesToModelLower(File directory, LIGA liga, int ngramLength) {
        String lang = directory.getName();
        if (directory.listFiles() != null) {
            for (File fileEntry : directory.listFiles()) {
                if (fileEntry.isFile()) {
                    String text = readTextFromFile(fileEntry);
                    if (!text.isEmpty()) liga.addDocument(text, lang, ngramLength);
                }
            }
        } else {
            System.out.println("Directory " + directory.getAbsolutePath() + " is empty");
        }

    }

    /**
     * reads text from the file
     *
     * @param file file
     */
    private static String readTextFromFile(File file){
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
     * reads texts delimited with newline from file to model
     *
     * @param file file
     * @param liga model
     * @param lang language
     * @param ngramLength ngram length
     */
    public static void readTextsFromFile(File file, LIGA liga, String lang, int ngramLength){
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String textLine; (textLine = br.readLine()) != null;)
                if (!textLine.trim().isEmpty()) liga.addDocument(textLine.trim(), lang, ngramLength);
        } catch (IOException e){
            System.out.println("Can't read file " + file.getAbsolutePath());
        }
    }

}
