/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Preprocessing;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author nageshbhattu
 */
public class Corpus implements Serializable{
    public Vector<HotelDocument> hotelDocList;
    public Hashtable<String,Integer> dictionary;
    public int numTerms;
    public Corpus(Vector<HotelDocument> hotelDocList, Hashtable<String,Integer> dictionary, int numTerms){
        this.dictionary = dictionary;
        this.hotelDocList = hotelDocList;
        this.numTerms = numTerms;
    }
    public int getNumTerms(){
        return numTerms;
    }
    public Hashtable<String,Integer> getDictionary(){
        return dictionary;
    }
    public Vector<HotelDocument> getHotelDocList(){
        return hotelDocList;
    }
}
