/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Models;

import Preprocessing.Corpus;
import Preprocessing.HotelDocument;
import Preprocessing.LARAMAnalyzer;
import java.util.Vector;
import org.knowceans.lda.Document;
import slda.SLDArDocument;

/**
 *
 * @author nageshbhattu
 */
public class SLDArLRR {
    public static void main(String[] args){
        String prefix = "/home/nageshbhattu/BTechProjects/LARA/";
        LARAMAnalyzer analyzer = new LARAMAnalyzer(prefix + "Data/Seeds/hotel_bootstrapping.dat", prefix + "Data/Seeds/stopwords.dat",
                prefix + "Data/Model/NLP/en-sent.zip", prefix + "Data/Model/NLP/en-token.zip", prefix + "Data/Model/NLP/en-pos-maxent.bin");
        Corpus corpus = analyzer.UnSerializeHotelDocs("HotelData.ser");
        slda.SLDArCorpus sldarCorpus = new slda.SLDArCorpus();
        sldarCorpus.setNumDocs(corpus.hotelDocList.size());
        Vector<HotelDocument> hotelDocList = corpus.getHotelDocList();
        Vector<SLDArDocument> docList = new Vector<>();
        for (int di = 0; di < hotelDocList.size(); di++) {
            HotelDocument hd = hotelDocList.get(di);
            SLDArDocument rdoc = new SLDArDocument(hd.doc, hd.ratings[0]);
            docList.add(rdoc);
        }
        sldarCorpus.setDocs((SLDArDocument[]) docList.toArray(new SLDArDocument[]{}));
        sldarCorpus.setNumTerms(corpus.numTerms + 1);
    }
}
