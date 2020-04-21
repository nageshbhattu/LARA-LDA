/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Preprocessing;

import java.io.Serializable;

/**
 *
 * @author nageshbhattu
 */
public class Token implements Serializable {

        private static final long serialVersionUID = 3788541728207309425L;
        public String m_word;
        public String m_lemma;
        public String m_pos;

        public Token(String word, String lemma, String pos) {
            m_word = word;
            m_lemma = lemma;
            m_pos = pos;
        }

        public String toString() {
            return m_word + "\t" + m_lemma + "\t" + m_pos;
        }
    }
