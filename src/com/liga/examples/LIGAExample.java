/*
 * Title: LIGAExample.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.examples;

import com.liga.LIGA;
import com.liga.TextFileLoader;
import com.liga.Tokenizer;

import java.io.File;
import java.util.Map;

/**
 * Example of LIGA class usage
 */
public class LIGAExample {

    public static void main(String[] args) {

        // Create LIGA instance
        LIGA liga = new LIGA.LIGABuilder(0.5).setLogLIGA(true).setMaxSearchDepth(1000).build();

        liga.loadModel("res" + File.separator + "model_orig.liga");

        //TextFileLoader.readFilesToModelUpper(new File("I:\\Work\\datasets\\liga_publication_dataset"), liga, 3);

        //liga.saveModel("res" + File.separator + "model_orig.liga");

        Map<String, Double> s1 = liga.classifyAll(Tokenizer.preprocess("absolutamente asqueroso"), 3); //es
        Map<String, Double> s2 =  liga.classifyAll(Tokenizer.preprocess("absolutely disgusting"), 3); //en
        Map<String, Double> s3 = liga.classifyAll(Tokenizer.preprocess("absolut eklig"), 3); //de
        String s4 = liga.classifyMostProbable(Tokenizer.preprocess("absolument dégoûtant"), 3); //fr
        String s5 = liga.classifyMostProbable(Tokenizer.preprocess("absoluut walgelijk"), 3); //nl

        liga.dropModel();

        // learn test model from following entries
        liga.addDocument("Ich gehe nach Hause.", "de", 3);
        liga.addDocument("I'm going home.", "en", 3);
        liga.addDocument("Я иду домой.", "ru", 3);
        liga.addDocument("Me voy a casa.", "es", 3);
        liga.addDocument("Je rentre à la maiso.", "fr", 3);
        liga.classifyMostProbable(Tokenizer.preprocess("my home, mi casa, мой дом, mein haus"), 3);
        liga.saveModel("res" + File.separator + "test.liga");
        liga.dropModel();

        // load test model from file
        liga.loadModel("res" + File.separator + "test.liga");
        liga.classifyMostProbable(Tokenizer.preprocess("my home, mi casa, мой дом, mein haus"), 3);

        // Load LIGA model
        liga.loadModel("res" + File.separator + "model.liga");

        // Classify some messages
        liga.classifyMostProbable(Tokenizer.preprocess("Dit is ook een test. This is another test."), 3); // nl_NL / en_UK
        liga.classifyMostProbable(Tokenizer.preprocess("Bankers are the assassins of hope."), 3); // en_UK
        liga.classifyMostProbable(Tokenizer.preprocess("Dit is ook een test"), 3); // nl_NL
        liga.classifyMostProbable(Tokenizer.preprocess("Und diese war auch eine Teste"), 3); // de_DE
        liga.classifyMostProbable(Tokenizer.preprocess("Wir haben wegen # prism Anzeige gegen Unbekannt gestellt , damit mal nach unseren Grundrechten gefahndet wird :"), 3); // de_DE
        liga.classifyMostProbable(Tokenizer.preprocess("Cezaevinde anneleriyle kalan 0-6 yaş TUTUKSUZ TUTUKLU 35 BEBEK için pabuç ve biberon gerekiyor, destek olmak isteyen?"), 3); // tr_TR
        liga.classifyMostProbable(Tokenizer.preprocess("Ganu arep di bojo ora gelem yemm RT FitRia_Maniez : Masi berkutat dgn pekerjaan . . workaholic kpn mbojone . . ! !"), 3); // tl_TL
        liga.classifyMostProbable(Tokenizer.preprocess("Hehehe , , , iyhaaa , wah kpn ktmu kwe mneh yo lin , sue tenan og : ( LiendaSulisty : holohh koe ki iyo ajek ngunukui te pangling"), 3); // tl_TL
    }
}
