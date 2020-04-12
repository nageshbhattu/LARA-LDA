/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Preprocessing;

import org.knowceans.lda.Document;

/**
 *
 * @author nageshbhattu
 */
public class HotelDocument {
    public Document doc;
    public double[][] phi; // topic distributions num of unique words X numAspects 
    public String m_ID;
    public double[] ratings; // 0th element is overall rating and remaining are aspect ratings
    public int numAspects;
    public HotelDocument(Document doc, String m_ID, double[] ratings){
        this.m_ID = m_ID;
        this.doc = doc;
        this.ratings = ratings;
    }
}
