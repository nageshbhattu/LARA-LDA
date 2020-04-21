/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package slda;

import Preprocessing.Corpus;
import Preprocessing.HotelDocument;
import Preprocessing.LARAMAnalyzer;
import java.io.File;
import java.util.Vector;
import org.knowceans.lda.Document;
import org.knowceans.lda.LdaEstimate;
import org.knowceans.lda.LdaInference;
import org.knowceans.lda.LdaModel;

/**
 *
 * @author nageshbhattu
 */
public class SLDArDriver {

    public static void main(String[] args){
        String prefix = "/home/nageshbhattu/BTechProjects/LARA/";
            LARAMAnalyzer analyzer = new LARAMAnalyzer(prefix + "Data/Seeds/hotel_bootstrapping.dat", prefix + "Data/Seeds/stopwords.dat",
                    prefix + "Data/Model/NLP/en-sent.zip", prefix + "Data/Model/NLP/en-token.zip", prefix + "Data/Model/NLP/en-pos-maxent.bin");
            Corpus corpus = analyzer.UnSerializeHotelDocs("HotelData.ser");
            slda.SLDArCorpus sldarCorpus = new slda.SLDArCorpus();
            int numAspects = 7;
            sldarCorpus.setNumDocs(corpus.hotelDocList.size());
            
            Vector<HotelDocument> hotelDocList = corpus.getHotelDocList();
            Vector<SLDArDocument> docList = new Vector<>();
            for(int di = 0; di<hotelDocList.size();di++){
                docList.add(new SLDArDocument(hotelDocList.get(di).doc,hotelDocList.get(di).ratings[0]));
            }
            sldarCorpus.setDocs((SLDArDocument[]) docList.toArray(new SLDArDocument[] {}));
            sldarCorpus.setNumTerms(corpus.numTerms+1);
            
            SLDArEstimate sldarEstimate = new SLDArEstimate();
            sldarEstimate.INITIAL_ALPHA = 50.0;
            sldarEstimate.readSettings("settings.txt");
            String dir = "SLDAR-1";
            boolean a = new File(dir).mkdir();

            System.out.println("SLDAr estimation. Settings:");
            System.out.println("\tvar max iter " + sldarEstimate.sldarInference.VAR_MAX_ITER);
            System.out.println("\tvar convergence "+ sldarEstimate.sldarInference.VAR_CONVERGED);
            System.out.println("\tem max iter " + sldarEstimate.EM_MAX_ITER);
            System.out.println("\tem convergence " + sldarEstimate.EM_CONVERGED);
            System.out.println("\testimate alpha " + sldarEstimate.ESTIMATE_ALPHA);

            SLDArModel model = sldarEstimate.runEm("seeded", dir, sldarCorpus, numAspects);
    }
}
