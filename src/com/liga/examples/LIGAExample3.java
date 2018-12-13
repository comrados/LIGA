/*
 * Title: LIGAExample3.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.examples;

import com.liga.DataLoader;
import com.liga.LIGA;
import com.liga.Tokenizer;
import org.apache.commons.lang3.tuple.MutablePair;

import java.io.File;
import java.util.List;

public class LIGAExample3 {

    public static void main(String[] args) {

        long t0 = System.currentTimeMillis();

        //active learned
        LIGA ligaA = new LIGA.LIGABuilder(0.5).setLogLIGA(true).setMaxSearchDepth(1000).build();
        ligaA.loadModel("res" + File.separator + "model_active.liga");
        //random learned
        LIGA ligaR = new LIGA.LIGABuilder(0.5).setLogLIGA(true).setMaxSearchDepth(1000).build();
        ligaR.loadModel("res" + File.separator + "model_random.liga");
        //original (human labeled)
        LIGA ligaO = new LIGA.LIGABuilder(0.5).setLogLIGA(true).setMaxSearchDepth(1000).build();
        ligaO.loadModel("res" + File.separator + "model_orig.liga");

        DataLoader dl = new DataLoader.DataLoaderBuilder().build();

        dl.loadFromFile("D:\\ground-truth-out.csv", "\t", 1);

        long t1 = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (t1 - t0)/1000.0 + " sec");

        evaluate(ligaA, dl.dataset, "active");

        long t2 = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (t2 - t1)/1000.0 + " sec");

        evaluate(ligaO, dl.dataset, "original");

        long t3 = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (t3 - t2)/1000.0 + " sec");

        evaluate(ligaR, dl.dataset, "random");

        long t4 = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (t4 - t3)/1000.0 + " sec");

        System.exit(0);

    }

    private static void evaluate(LIGA liga, List<MutablePair<String, String>> list, String name) {
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
                String prep = Tokenizer.preprocess(p.getRight());
                if ((prep.trim() != null) && (!prep.trim().equals(""))){
                    c++;

                    String res = liga.classifyMostProbable(prep, 3);

                    if (!res.equals(p.getLeft())) System.out.println(res + " " + p.getLeft() + " " + p.getRight() + " ----> " + prep);

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
        }
        System.out.println();
        System.out.println("Model: " + name);
        System.out.println("Overall accuracy " + numToProc((double) cTrue/c) + " (" + cTrue + "/"+ c +")");
        System.out.println("English accuracy " + numToProc((double) cEnTrue/cEn) + " (" + cEnTrue + "/"+ cEn +")");
        System.out.println("German accuracy  " + numToProc((double) cDeTrue/cDe) + " (" + cDeTrue + "/"+ cDe +")");
        System.out.println("Spanish accuracy " + numToProc((double) cEsTrue/cEs) + " (" + cEsTrue + "/"+ cEs +")");
        System.out.println("Italian accuracy " + numToProc((double) cItTrue/cIt) + " (" + cItTrue + "/"+ cIt +")");
        System.out.println("French accuracy  " + numToProc((double) cFrTrue/cFr) + " (" + cFrTrue + "/"+ cFr +")");
        System.out.println("Dutch accuracy   " + numToProc((double) cNlTrue/cNl) + " (" + cNlTrue + "/"+ cNl +")");
        System.out.println();
    }

    private static String numToProc(double d){
        return String.format("%.3f", d*100) + "%";
    }

}
