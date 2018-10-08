/*
 * Title: ActiveLIGAExample.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.examples;

import com.liga.DataLoader;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.List;

public class ActiveLIGAExample {

    public static void main(String[] args) {
        String dir = "I:\\Work\\datasets\\liga_publication_dataset";
        DataLoader dataLoader = new DataLoader.DataLoaderBuilder(dir).setN(3).setSplit(0.9).build();
        dataLoader.readFilesUpper();

        dataLoader.shuffleData(0);

        List<MutablePair<String, String>> train = dataLoader.getTrainSet();
        List<MutablePair<String, String>> test = dataLoader.getTestSet();
    }

}
