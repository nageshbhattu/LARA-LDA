/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Preprocessing;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import java.util.Vector;
import org.knowceans.lda.Document;

/**
 *
 * @author nageshbhattu
 */
public class HotelDocument implements Serializable{
    public Document doc;
    public double[][] phi; // topic distributions num of unique words X numAspects 
    public String m_ID;
    public double[] ratings; // 0th element is overall rating and remaining are aspect ratings
    public Vector<Sentence> m_stns = new Vector<Sentence>();
    public HotelDocument(Document doc, String m_ID, double[] ratings){
        this.m_ID = m_ID;
        this.doc = doc;
        this.ratings = ratings;
        this.m_stns = new Vector<Sentence>();
    }
    public HotelDocument(String m_ID, int[] ratings ){
        this.ratings = Arrays.stream(ratings).asDoubleStream().toArray();
        this.m_ID = m_ID;
        this.m_stns = new Vector<Sentence>();
    }
    public void setPhi(double[][] phi){
        this.phi = phi;
    }
    public double[] getPhiCounts(int aspect){
        double[] phiCounts = new double[doc.getLength()];
        int[] counts = doc.getCounts();
        for(int wi = 0;wi<doc.getLength();wi++){
            phiCounts[wi] = counts[wi]*phi[wi][aspect];
        }
        return phiCounts;
    }
    
    public int getNumSentences() {
        return m_stns.size();
    }
    
    public int getReviewSize(){
        int size = 0;
        for(int sent = 0;sent<m_stns.size();sent++){
            Sentence sentence = m_stns.get(sent);
            size = size + sentence.m_tokens.size();
        }
        return size;
    }
    
    public void addStn(String content, String[] tokens, String[] pos, String[] lemma, Set<String> stopwords) {
        if (m_stns == null) {
            m_stns = new Vector<Sentence>();
        }

        Sentence stn = new Sentence(tokens.length, content);
        for (int i = 0; i < tokens.length; i++) {
            if (!pos[i].equals("DT") && !pos[i].equals("CD") && !pos[i].equals("IN") && !stopwords.contains(lemma[i])) {
                stn.addToken(tokens[i], lemma[i], pos[i]);
            }
        }
        if (stn.getLength() > 0) {
            m_stns.add(stn);
        }
    }
}
