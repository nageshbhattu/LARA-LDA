/*


 */

package slda;

import org.knowceans.lda.*;
import java.io.Serializable;

/**
 * wrapper for a document in SLDA
 * <p>
 * lda-c reference: struct document in lda.h. TODO automatic length tracking
 * 
 * @author nagesh bhattu
 */

public class SLDArDocument implements Serializable{

    private int[] words;

    private int[] counts;

    private int length;

    private int total;
    
    private double rating;

    /**
     * 
     */
    public SLDArDocument() {
        length = 0;
        words = new int[0];
        counts = new int[0];
        rating = 0.0;
    }

    /**
     * 
     */
    public SLDArDocument(int length) {
        words = new int[length];
        counts = new int[length];
        this.length = length;
        this.rating = 0.0;
    }
    
    public SLDArDocument(Document doc, double rating){
        this.words = doc.getWords();
        this.counts = doc.getCounts();
        this.length = doc.getLength();
        this.total = doc.getTotal();
        this.rating = rating;
    }

    public void compile() {
        try {
            if (counts.length != words.length)
                throw new Exception("Document inconsistent.");
            length = counts.length;
            for (int c : counts)
                total += c;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @return
     */
    public int[] getCounts() {
        return counts;
    }

    /**
     * @param index
     * @return
     */
    public int getCount(int index) {
        return counts[index];
    }

    /**
     * @param count
     * @param index
     */
    public void setCount(int index, int count) {
        counts[index] = count;
    }

    /**
     * @return
     */
    public int getLength() {
        return length;
    }

    /**
     * @return
     */
    public int getTotal() {
        return total;
    }

    /**
     * @return
     */
    public int[] getWords() {
        return words;
    }

    /**
     * @param index
     * @return
     */
    public int getWord(int index) {
        return words[index];
    }

    /**
     * @param word
     * @param index
     */
    public void setWord(int index, int word) {
        words[index] = word;
    }

    /**
     * @param is
     */
    public void setCounts(int[] is) {
        counts = is;
    }

    /**
     * @param i
     */
    public void setLength(int i) {
        length = i;
    }

    /**
     * @param i
     */
    public void setTotal(int i) {
        total = i;
    }

    /**
     * @param is
     */
    public void setWords(int[] is) {
        words = is;
    }
    
    public void setRating(double rating){
        this.rating = rating;
    }
    
    public double getRating(){
        return this.rating;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("Document {length=" + length + " total=" + total + "}");
        return b.toString();
    }
    
    public void sortCounts(){
        
    }

}
