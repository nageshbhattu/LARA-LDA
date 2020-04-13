/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import lara.LRR;
import org.knowceans.lda.Corpus;
import org.knowceans.lda.Document;
import org.knowceans.lda.LdaEstimate;
import static org.knowceans.lda.LdaEstimate.INITIAL_ALPHA;
import static org.knowceans.lda.LdaEstimate.readSettings;
import static org.knowceans.lda.LdaEstimate.runEm;
import org.knowceans.lda.LdaInference;
import org.knowceans.lda.LdaModel;

/**
 *
 * @author NIT-ANDHRA
 */
public class LDAFeatureExtractor {
    public static void main(String[] args){
        BufferedReader br = null;
        boolean directComputation = true;
        try {
            int numAspects = 7;
            String fileName = "output.txt";
            br = new BufferedReader(new FileReader(new File(fileName)));
            String line = null;
            Corpus corpus = new Corpus();
            int numDocs = 0;
            Vector<Document> cdocs = new Vector<>();
            Vector<HotelDocument> vHotels = new Vector<>();
            int max = 0;
            while((line=br.readLine())!=null){
                String[] words = line.split(" ");
                double[] ratings = new double[numAspects+1];
                for(int aspect = 0;aspect<numAspects+1;aspect++){
                    ratings[aspect] = Double.parseDouble(words[aspect+1]);
                }
                int numWords = words.length - (7 + 1 + 1);
                Document doc = new Document(numWords);
                doc.setLength(numWords);
                
                for(int wi = 0;wi<numWords;wi++){
                    int ind = numAspects + 1+1+ wi;
                    String wordCount = words[ind];
                    String[] wcStrs = wordCount.split(":");
                    int wordID = Integer.parseInt(wcStrs[0]);
                    int wordCnt = Integer.parseInt(wcStrs[1]);
                    doc.setWord(wi, wordID);
                    doc.setCount(wi, wordCnt);
                    doc.setTotal(doc.getTotal() + wordCnt);
                    if(max<wordID){
                        max = wordID;
                    }
                }
                HotelDocument hd = new HotelDocument(doc, words[0], ratings);
                numDocs++;
                cdocs.add(doc);
                vHotels.add(hd);
            }
            corpus.setNumDocs(numDocs);
            corpus.setDocs((Document[]) cdocs.toArray(new Document[] {}));
            corpus.setNumTerms(max+1);
            String dir = "LARA-LDA-OUT";
            File ldaDir = new File(dir);
            LdaModel model = null;
            if(!ldaDir.exists()){
                LdaEstimate.INITIAL_ALPHA = 50.0;
                LdaEstimate.readSettings("settings.txt");
                boolean a = new File("LARA-LDA-OUT").mkdir();

                System.out.println("LDA estimation. Settings:");
                System.out.println("\tvar max iter " + LdaInference.VAR_MAX_ITER);
                System.out.println("\tvar convergence "
                    + LdaInference.VAR_CONVERGED);
                System.out.println("\tem max iter " + LdaEstimate.EM_MAX_ITER);
                System.out.println("\tem convergence " + LdaEstimate.EM_CONVERGED);
                System.out.println("\testimate alpha " + LdaEstimate.ESTIMATE_ALPHA);

                System.out.println("\tem max iter " + LdaEstimate.EM_MAX_ITER);
                System.out.println("\tem convergence " + LdaEstimate.EM_CONVERGED);
                System.out.println("\testimate alpha " + LdaEstimate.ESTIMATE_ALPHA);

                model = LdaEstimate.runEm("seeded", "LARA-LDA-OUT", corpus, numAspects);
                
            }else{
                model = new LdaModel(dir+"/147");
            }
            if(!directComputation){
                String laraInput = "LARAInput.txt";
                BufferedWriter bw = new BufferedWriter(new FileWriter(laraInput));
                for (int d = 0; d < vHotels.size(); d++) {
                    HotelDocument hotel = vHotels.get(d);
                    Document doc = hotel.doc;
                    double[][] phi = new double[doc.getLength()][model.getNumTopics()];
                    double[] gamma = new double[model.getNumTopics()];
                    double likelihood = LdaInference.ldaInference(doc, model, gamma, phi);
                    bw.write(hotel.m_ID);
                    for (int aspectID = 0; aspectID < numAspects + 1; aspectID++) {
                        bw.write("\t" + hotel.ratings[aspectID]);
                    }
                    bw.write("\n");
                    for (int aspectID = 0; aspectID < numAspects; aspectID++) {
                        //bw.write(doc.getLength());
                        for (int wordID = 0; wordID < doc.getLength(); wordID++) {
                            if (wordID != 0) {
                                bw.write(" ");
                            }
                            bw.write(doc.getWord(wordID) + ":" + doc.getCount(wordID) * phi[wordID][aspectID]);
                        }
                        bw.write("\n");
                    }
                }
                bw.close();
            }else{
                LRR lrrModel = new LRR(500, 1e-2, 5000, 1e-2, 2.0);
                lrrModel.EM_est("LARAInput.txt", 10, 1e-4);
                lrrModel.SavePrediction("prediction.dat");
                lrrModel.SaveModel("model_hotel.dat");
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
