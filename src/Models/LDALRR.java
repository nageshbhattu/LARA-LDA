/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Models;

import Preprocessing.Corpus;
import Preprocessing.HotelDocument;
import Preprocessing.LARAMAnalyzer;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import lara.LRR;
import org.knowceans.lda.Document;
import org.knowceans.lda.LdaEstimate;
import org.knowceans.lda.LdaInference;
import org.knowceans.lda.LdaModel;

/**
 *
 * @author nageshbhattu
 */
public class LDALRR {
    public static void main(String[] args){
        try {
            String prefix = "/home/nageshbhattu/BTechProjects/LARA/";
            LARAMAnalyzer analyzer = new LARAMAnalyzer(prefix + "Data/Seeds/hotel_bootstrapping.dat", prefix + "Data/Seeds/stopwords.dat",
                    prefix + "Data/Model/NLP/en-sent.zip", prefix + "Data/Model/NLP/en-token.zip", prefix + "Data/Model/NLP/en-pos-maxent.bin");
            Corpus corpus = analyzer.UnSerializeHotelDocs("HotelData.ser");
            org.knowceans.lda.Corpus ldaCorpus = new org.knowceans.lda.Corpus();
            int numAspects = 7;
            ldaCorpus.setNumDocs(corpus.hotelDocList.size());
            Vector<HotelDocument> hotelDocList = corpus.getHotelDocList();
            Vector<Document> docList = new Vector<>();
            for(int di = 0; di<hotelDocList.size();di++){
                docList.add(hotelDocList.get(di).doc);
            }
            
            
            ldaCorpus.setDocs((Document[]) docList.toArray(new Document[] {}));
            ldaCorpus.setNumTerms(corpus.numTerms+1);
            String dir = "LDA-LRR-3";
            File ldaDir = new File(dir);
            LdaModel model = null;
            if (!ldaDir.exists()) {
                LdaEstimate.INITIAL_ALPHA = 50.0;
                LdaEstimate.readSettings("settings.txt");
                boolean a = ldaDir.mkdir();
                
                System.out.println("LDA estimation. Settings:");
                System.out.println("\tvar max iter " + LdaInference.VAR_MAX_ITER);
                System.out.println("\tvar convergence "
                        + LdaInference.VAR_CONVERGED);
                System.out.println("\tem max iter " + LdaEstimate.EM_MAX_ITER);
                System.out.println("\tem convergence " + LdaEstimate.EM_CONVERGED);
                System.out.println("\testimate alpha " + LdaEstimate.ESTIMATE_ALPHA);
                model = LdaEstimate.runEm("seeded", dir, ldaCorpus, numAspects);
                
            } else {
                model = new LdaModel(dir + "/011");
            }
            for (int d = 0; d < hotelDocList.size(); d++) {
                HotelDocument hotel = hotelDocList.get(d);
                Document doc = hotel.doc;
                double[][] phi = new double[doc.getLength()][model.getNumTopics()];
                double[] gamma = new double[model.getNumTopics()];
                double likelihood = LdaInference.ldaInference(doc, model, gamma, phi);
                hotel.phi = phi;
            }
            LRR lrrModel = new LRR(500, 1e-2, 5000, 1e-2, 2.0);
            lrrModel.EM_est(hotelDocList, 10, 1e-4);
            lrrModel.SavePrediction("prediction.dat");
            lrrModel.SaveModel("model_hotel.dat");
        } catch (IOException ex) {
            Logger.getLogger(LDALRR.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
