/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Preprocessing;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 *
 * @author nageshbhattu
 */
public class Sentence implements Comparable<Sentence>, Serializable {

        private static final long serialVersionUID = -8979851850686318513L;
        public Vector<Token> m_tokens;
        public int m_aspectID;
        public double m_sentiScore;
        public String m_content;

        public Sentence(int tSize, String content) {
            m_content = content;
            m_tokens = new Vector<Token>(tSize);
            m_aspectID = -1;
            m_sentiScore = 0;
        }

        public Sentence(String content, int aspectID, double sentiScore) {
            m_content = content;
            m_aspectID = aspectID;
            m_sentiScore = sentiScore;
            m_tokens = null;
        }

        public void addToken(String word, String lemma, String pos) {
            m_tokens.add(new Token(word, lemma, pos));
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer(1024);
            for (Token t : m_tokens) {
                buffer.append(t.toString() + "\n");
            }
            return buffer.toString();
        }

        //annotate by all the words
        public int AnnotateByKeyword(Set<String> keywords) {
            int count = 0;
            for (Token t : m_tokens) {
                if (keywords.contains(t.m_lemma)) {
                    count++;
                }
            }
            return count;
        }

        public void setSentiScore(Map<String, Double> lexicon) {
            m_sentiScore = 0;
            double w = 0;
            for (Token t : m_tokens) {
                if (lexicon.containsKey(t.m_lemma)) {
                    m_sentiScore += lexicon.get(t.m_lemma);
                    w++;
                }
            }
            if (w > 0) {
                if (m_tokens.size() > 4) {
                    m_sentiScore /= w;
                } else {
                    m_sentiScore = 0;
                }
            }
        }

        public void setAspectID(int aID) {
            m_aspectID = aID;
        }

        public int getAspectID() {
            return m_aspectID;
        }

        public double getAspectSentiScore() {
            return m_sentiScore;
        }

        public int getLength() {
            return m_tokens.size();
        }

        @Override
        public int compareTo(Sentence o) {
            if (this.m_sentiScore > o.m_sentiScore) {
                return 1;
            } else if (this.m_sentiScore < o.m_sentiScore) {
                return -1;
            } else {
                return 0;
            }
        }
    }