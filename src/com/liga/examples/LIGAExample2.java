/*
 * Title: LIGAExample2.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.examples;

import com.liga.DataLoader;
import com.liga.LIGA;
import com.liga.TextFileLoader;
import com.liga.Tokenizer;
import org.apache.commons.lang3.tuple.MutablePair;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Example of LIGA class usage
 */
public class LIGAExample2 {

    public static void main(String[] args) {

        long t0 = System.currentTimeMillis();

        //active learned
        LIGA ligaA = new LIGA.LIGABuilder(0.5).setLogLIGA(true).setMaxSearchDepth(1000).build();
        ligaA.loadModel("res" + File.separator + "model_active.liga");
        //original (human labeled)
        LIGA ligaO = new LIGA.LIGABuilder(0.5).setLogLIGA(true).setMaxSearchDepth(1000).build();
        ligaO.loadModel("res" + File.separator + "model_orig.liga");

        DataLoader dl = new DataLoader.DataLoaderBuilder().build();

        dl.loadFromFile("res\\data-train-new.txt", "\t", 1);

        long t1 = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (t1 - t0)/1000.0 + " sec");

        evaluate(ligaA, dl.dataset);

        long t2 = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (t2 - t1)/1000.0 + " sec");

        evaluate(ligaO, dl.dataset);

        long t3 = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (t3 - t2)/1000.0 + " sec");

        System.exit(0);

    }

    private static void evaluate(LIGA liga, List<MutablePair<String, String>> list) {
        int i = 0;
        int c = 0;
        int cTrue = 0;
        int cEn = 0;
        int cEnTrue = 0;
        int cDe = 0;
        int cDeTrue = 0;
        int cEs = 0;
        int cEsTrue = 0;
        int cIt = 0;
        int cItTrue = 0;
        int cFr = 0;
        int cFrTrue = 0;
        int cNl = 0;
        int cNlTrue = 0;
        for (MutablePair<String, String> p : list) {
            i++;
            if (i % 10000 == 0)
                System.out.println("Iteration " + i + " out of " + list.size());
            if (p.getLeft().equals("en") || p.getLeft().equals("de") || p.getLeft().equals("es")
                    || p.getLeft().equals("it") || p.getLeft().equals("fr") || p.getLeft().equals("nl")) {

                c++;
                String prep = Tokenizer.preprocess(p.getRight());
                String res = liga.classifyMostProbable(prep, 3);

                //if (!res.equals(p.getLeft())) System.out.println(res + " " + p.getLeft());

                switch (res) {
                    case "en":
                        if (res.equals(p.getLeft())){
                            cTrue++;
                            cEnTrue++;
                        }
                        cEn++;
                        break;
                    case "de":
                        if (res.equals(p.getLeft())){
                            cTrue++;
                            cDeTrue++;
                        }
                        cDe++;
                        break;
                    case "es":
                        if (res.equals(p.getLeft())){
                            cTrue++;
                            cEsTrue++;
                        }
                        cEs++;
                        break;
                    case "it":
                        if (res.equals(p.getLeft())){
                            cTrue++;
                            cItTrue++;
                        }
                        cIt++;
                        break;
                    case "fr":
                        if (res.equals(p.getLeft())){
                            cTrue++;
                            cFrTrue++;
                        }
                        cFr++;
                        break;
                    case "nl":
                        if (res.equals(p.getLeft())){
                            cTrue++;
                            cNlTrue++;
                        }
                        cNl++;
                        break;
                    default:
                        break;
                }

            }
        }
        System.out.println();
        System.out.println("Сука, блядь, пидор. Винишь ли ты меня в слепоте мысли, пидор, сука, блядь?\n" +
                "Я, блядь, вижу, сука, образ, блядь, пидорас, являющийся выражением, ёбаный твой рот, эмоций, блядь.\n" +
                "Понимаешь меня? Блядь, да проще тебе ебало набить.");
        System.out.println();
        System.out.println("Overall accuracy " + (double) cTrue/c + " (" + cTrue + "/"+ c +")");
        System.out.println("English accuracy " + (double) cEnTrue/cEn + " (" + cEnTrue + "/"+ cEn +")");
        System.out.println("German accuracy  " + (double) cDeTrue/cDe + " (" + cDeTrue + "/"+ cDe +")");
        System.out.println("Spanish accuracy " + (double) cEsTrue/cEs + " (" + cEsTrue + "/"+ cEs +")");
        System.out.println("Italian accuracy " + (double) cItTrue/cIt + " (" + cItTrue + "/"+ cIt +")");
        System.out.println("French accuracy  " + (double) cFrTrue/cFr + " (" + cFrTrue + "/"+ cFr +")");
        System.out.println("Dutch accuracy   " + (double) cNlTrue/cNl + " (" + cNlTrue + "/"+ cNl +")");
        System.out.println();
    }

}
