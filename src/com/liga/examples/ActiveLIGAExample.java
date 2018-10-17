/*
 * Title: ActiveLIGAExample.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.examples;

import com.liga.DataLoader;
import com.liga.LIGA;
import com.liga.active.*;

public class ActiveLIGAExample {

    public static void main(String[] args) {

        // data loader instance
        String dir = "I:\\Work\\datasets\\liga_publication_dataset";
        DataLoader dl = new DataLoader.DataLoaderBuilder(dir).setN(3).setShuffleN(25).setSeed(0).
                setTrainThresholdPart(0.25).setTestDataPart(0.1).setInitDataPart(0.05).build();

        // obtain datasets for training and testing
        dl.readFilesUpper();
        dl.getTestData();

        // oracle instance
        Oracle ora = new AutomatedOracle();

        // sampler instance
        Sampler sam = new MarginSampler();

        // LIGA instance
        LIGA liga = new LIGA.LIGABuilder(0.5).setLogLIGA(true).setMaxSearchDepth(1000).build();

        ActiveLearner learner = new ActiveLearner.ActiveLearnerBuilder(ora, sam, liga).build();

        learner.setDatasets(dl.init, dl.train, dl.test);

        learner.learn();

        System.exit(0);
    }

}