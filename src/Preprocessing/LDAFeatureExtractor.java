/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.knowceans.lda.Document;

/**
 *
 * @author NIT-ANDHRA
 */
public class LDAFeatureExtractor {
    public static void main(String[] args){
        BufferedReader br = null;
        try {
            int numAspects = 7;
            String fileName = "output.txt";
            br = new BufferedReader(new FileReader(new File(fileName)));
            String line = null;
            while((line=br.readLine())!=null){
                String[] words = line.split(" ");
                double[] ratings = new double[numAspects+1];
                for(int aspect = 0;aspect<numAspects+1;aspect++){
                    ratings[aspect] = Double.parseDouble(words[aspect+1]);
                }
                int numWords = words.length - (7 + 1 + 1);
                Document doc = new Document();
                for(int wi = 0;wi<numWords;wi++){
                    int ind = numAspects + 1+ wi;
                    String wordCount = words[ind];
                    String[] wcStrs = wordCount.split(":");
                    doc.setWord(wi, Integer.parseInt(wcStrs[0]));
                    doc.setCount(wi, Integer.parseInt(wcStrs[1]));
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LDAFeatureExtractor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LDAFeatureExtractor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(LDAFeatureExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
