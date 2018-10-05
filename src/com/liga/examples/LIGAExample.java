/*
 * Title: LIGAExample.java
 * Project: LIGA
 * Creator: Georgii Mikriukov
 * 2018
 */

package com.liga.examples;

import com.liga.DataLoader;
import com.liga.LIGA;
import com.liga.Tokenizer;

import java.io.File;

public class LIGAExample {

    public static void main(String[] args) {

        String dir = "I:\\Work\\datasets\\liga_publication_dataset";
        DataLoader dataLoader = new DataLoader.DataLoaderBuilder(dir).build();
        dataLoader.readFilesUpper();

        dataLoader.shuffleData(0);

        // Create LIGA instance
        LIGA liga = new LIGA().setThreshold(0.5).setLogLIGA(true).setMaxSearchDepth(1000);

        //TextFileLoader.readFilesToModelUpper(new File("I:\\Work\\datasets\\liga_publication_dataset"), liga, 3);

        //liga.saveModel("res" + File.separator + "model_orig.liga");

        liga.classify(Tokenizer.preprocess("absolutamente asqueroso"), 3); //es
        liga.classify(Tokenizer.preprocess("absolutely disgusting"), 3); //en
        liga.classify(Tokenizer.preprocess("absolut widerlich"), 3); //de
        liga.classify(Tokenizer.preprocess("absolument dégoûtant"), 3); //fr
        liga.classify(Tokenizer.preprocess("absoluut walgelijk"), 3); //nl

        liga.dropModel();

        // learn test model from following entries
        liga.addDocument("Ich gehe nach Hause.", "de", 3);
        liga.addDocument("I'm going home.", "en", 3);
        liga.addDocument("Я иду домой.", "ru", 3);
        liga.addDocument("Me voy a casa.", "es", 3);
        liga.addDocument("Je rentre à la maiso.", "fr", 3);
        liga.classify(Tokenizer.preprocess("my home, mi casa, мой дом, mein haus"), 3);
        liga.saveModel("res" + File.separator + "test.liga");
        liga.dropModel();

        // load test model from file
        liga.loadModel("res" + File.separator + "test.liga");
        liga.classify(Tokenizer.preprocess("my home, mi casa, мой дом, mein haus"), 3);

        // Load LIGA model
        liga.loadModel("res" + File.separator + "model.liga");

        // Classify some messages
        liga.classify(Tokenizer.preprocess("Dit is ook een test. This is another test."), 3); // nl_NL / en_UK
        liga.classify(Tokenizer.preprocess("Bankers are the assassins of hope."), 3); // en_UK
        liga.classify(Tokenizer.preprocess("Dit is ook een test"), 3); // nl_NL
        liga.classify(Tokenizer.preprocess("Und diese war auch eine Teste"), 3); // de_DE
        liga.classify(Tokenizer.preprocess("Wir haben wegen # prism Anzeige gegen Unbekannt gestellt , damit mal nach unseren Grundrechten gefahndet wird :"), 3); // de_DE
        liga.classify(Tokenizer.preprocess("Cezaevinde anneleriyle kalan 0-6 yaş TUTUKSUZ TUTUKLU 35 BEBEK için pabuç ve biberon gerekiyor, destek olmak isteyen?"), 3); // tr_TR
        liga.classify(Tokenizer.preprocess("Ganu arep di bojo ora gelem yemm RT FitRia_Maniez : Masi berkutat dgn pekerjaan . . workaholic kpn mbojone . . ! !"), 3); // tl_TL
        liga.classify(Tokenizer.preprocess("Hehehe , , , iyhaaa , wah kpn ktmu kwe mneh yo lin , sue tenan og : ( LiendaSulisty : holohh koe ki iyo ajek ngunukui te pangling"), 3); // tl_TL
    }
}
