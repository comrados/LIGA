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

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Example of active learning LIGA usage
 */
public class ActiveLIGAExample {

    private static String savePath = "results";

    public static void main(String[] args) {

        int batch = 10;
        double test = 0.1;
        double init = 0.01;

        String params = batch + "_" + init + "_" + test;

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
        Date d = new Date();
        String date = df.format(d);

        // data loader instance
        String dir = "I:\\Work\\datasets\\liga_publication_dataset";
        DataLoader dl = new DataLoader.DataLoaderBuilder().setN(3).setShuffleN(25).setSeed(0).setDirectory("res")
                .setTestDataPart(test).setInitDataPart(init).build();

        // obtain datasets for training and testing
        //dl.loadFromDirectorySubdirectories();

        dl.loadFromFile("res\\original_data.csv", "\t", 1);

        //dl.saveToFile("res\\original_data.csv", "\t");

        dl.getTestData();

        // oracle instance
        Oracle ora = new AutoOracle();

        // sampler instance
        Sampler samM = new MarginSampler.MarginSamplerBuilder(batch).build();

        // testing sampler instance
        Sampler samR = new RandomSampler.RandomSamplerBuilder(batch).setSeed(0).build();

        // LIGA instance
        LIGA liga = new LIGA.LIGABuilder(0.5).setLogLIGA(true).setMaxSearchDepth(1000).build();

        // margin sampler
        ActiveLearner learnerM = new ActiveLearner.ActiveLearnerBuilder(ora, samM, liga)
                .setTrainThreshold(0.10).build();
        learnerM.setDatasets(dl.init, dl.train, dl.test);
        learnerM.learn();
        Map<String, Double> resultsM = learnerM.getResults();
        List<Map<String, Double>> intermediateResultsM = learnerM.getIntermediateResults();
        saveResults(samM, date, params, resultsM, intermediateResultsM);
        liga.saveModel("res" + File.separator + "model_active.liga");

        //clean model, start new learning
        liga.dropModel();
        ActiveLearner learnerR = new ActiveLearner.ActiveLearnerBuilder(ora, samR, liga)
                .setTrainThreshold(0.10).build();
        learnerR.setDatasets(dl.init, dl.train, dl.test);
        learnerR.learn();
        Map<String, Double> resultsR = learnerR.getResults();
        List<Map<String, Double>> intermediateResultsR = learnerR.getIntermediateResults();
        saveResults(samR, date, params, resultsR, intermediateResultsR);
        liga.saveModel("res" + File.separator + "model_random.liga");

        System.exit(0);
    }

    private static String saveResults(Sampler sam, String date, String params, Map<String, Double> results, List<Map<String, Double>> intermediateResults){
        String sampler = "";
        if (sam instanceof MarginSampler){
            sampler = "margin";
        } else if (sam instanceof RandomSampler){
            sampler = "random-" + ((RandomSampler) sam).getSeed();
        }
        String name = sampler + "_" + params + "_" + date + ".txt";
        String fullName = savePath + File.separator + name;
        try {
            FileOutputStream fos = new FileOutputStream(new File(fullName));

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            for (Map.Entry<String, Double> e: results.entrySet()){
                bw.write(e.getKey() + " " + e.getValue());
                bw.newLine();
            }

            bw.newLine();

            for (Map<String, Double> map: intermediateResults){
                for (Map.Entry<String, Double> e: map.entrySet()){
                    bw.write(e.getKey() + " " + e.getValue());
                    bw.newLine();
                }
                bw.newLine();
            }

            bw.close();

        } catch (IOException e) {
            System.err.println("Unable to write " + fullName);
            System.err.println(e.getMessage());
        }
        return name;
    }

}
