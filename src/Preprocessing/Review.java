package Preprocessing;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class Review implements Comparable<Review>, Serializable {

    private static final long serialVersionUID = 1232912787144084751L;
    public final static String Aspect_tag = "<ASPECT_";
    public final static String Aspect_tag_end = "</ASPECT_";

    public boolean m_annotated;
    public Vector<Sentence> m_stns;

    //meta data for the reviews
    public String m_hotelID;
    public String m_reviewID;
    public String m_author;
    public String m_content;
    public String m_date;
    public int m_ratings[];

    //new data format
    public String m_author_location;
    public String m_title;
    public double m_overall_rating;
    public Map<String, String> m_rating_map;

    //for ranking purpose
    public double m_rScore;

    public Review(String hotelID, String reviewID, int[] ratings) {
        m_hotelID = hotelID;
        m_reviewID = reviewID;
        m_annotated = false;
        m_stns = new Vector<Sentence>();
        if (ratings != null) {
            m_ratings = new int[ratings.length];
            System.arraycopy(ratings, 0, m_ratings, 0, ratings.length);
        } else {
            m_ratings = null;
        }

        m_rScore = -1;
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

    public int getStnSize() {
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

    public String toString() {
        StringBuffer buffer = new StringBuffer(1024);
        buffer.append(m_hotelID + "\n" + m_reviewID + "\n" + m_author + "\n" + m_author_location + "\n" + m_date + "\n" + m_title + "\n" + m_content + "\n");
        for (int r : m_ratings) {
            buffer.append(r + "\t");
        }
        return buffer.toString();
    }

    public String toPrintString() {
        StringBuffer buffer = new StringBuffer(1024);

        buffer.append("<Review ID>" + m_reviewID + "\n");
        buffer.append("<Author>" + m_author + "\n");
        buffer.append("<Author Location>" + m_author_location + "\n");
        buffer.append("<Title>" + m_title + "\n");
        buffer.append("<Content>" + m_content + "\n");
        buffer.append("<Date>" + m_date + "\n");

        buffer.append("<Overall>" + m_overall_rating + "\n");
        if (m_rating_map != null) {
            for (Map.Entry<String, String> iter : m_rating_map.entrySet()) {
                buffer.append("<" + iter.getKey() + ">" + iter.getValue() + "\n");
            }
        }
        return buffer.toString();
    }

    @Override
    public int compareTo(Review o) {
        if (this.m_rScore > o.m_rScore) {
            return -1;
        } else if (this.m_rScore < o.m_rScore) {
            return 1;
        } else {
            return 0;
        }
    }
}
