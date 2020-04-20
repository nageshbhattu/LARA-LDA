package Preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.knowceans.lda.Document;
import Preprocessing.Corpus;

public class LARAMAnalyzer {

    //this aspect set only exist in the old TripAdvisor reviews
    public static final String[] ASPECT_SET = {"Value", "Rooms", "Location", "Cleanliness", "Check in / front desk", "Service", "Business Service"};
    //this is the common aspect set in TripAdvisor reviews
    public static final String[] ASPECT_SET_NEW = {"Value", "Rooms", "Location", "Cleanliness", "Check in / front desk", "Service", "Business Service"};
    public static final int ASPECT_COUNT_CUT = 2;
    public static final int ASPECT_CONTENT_CUT = 5;
    public static final String PUNCT = ":;=+-()[],.\"'";

    class _Aspect {

        String m_name;
        HashSet<String> m_keywords;

        _Aspect(String name, HashSet<String> keywords) {
            m_name = name;
            m_keywords = keywords;
        }
    }

    //Vector<Hotel> m_hotelList;
    Vector<HotelDocument> m_hotelDocList;
    Vector<_Aspect> m_keywords;
    Hashtable<String, Integer> m_vocabulary;//indexed vocabulary
    Hashtable<String,Integer> idfTable;
    Vector<String> m_wordlist;//term list in the original order

    Corpus corpus;
    HashSet<String> m_stopwords;
    double[] m_wordCount;
    boolean m_isLoadCV; // if the vocabulary is fixed

    int minIDF = 10;
    SentenceDetectorME m_stnDector;
    TokenizerME m_tokenizer;
    POSTaggerME m_postagger;
    Stemmer m_stemmer;

    
    public LARAMAnalyzer(String seedwords, String stopwords, String stnSplModel, String tknModel, String posModel) {
        m_hotelDocList = new Vector<HotelDocument>();
        m_vocabulary = new Hashtable<String, Integer>();
        m_isLoadCV = false;
        if (seedwords != null && seedwords.isEmpty() == false) {
            LoadKeywords(seedwords);
        }
        LoadStopwords(stopwords);

        try {
            m_stnDector = new SentenceDetectorME(new SentenceModel(new FileInputStream(stnSplModel)));
            m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tknModel)));
            m_postagger = new POSTaggerME(new POSModel(new FileInputStream(posModel)));
            m_stemmer = new Stemmer();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        idfTable = new Hashtable<>();
        System.out.println("[Info]NLP modules initialized...");
    }

    public void LoadKeywords(String filename) {
        try {
            m_keywords = new Vector<_Aspect>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            String tmpTxt;
            String[] container;
            HashSet<String> keywords;
            while ((tmpTxt = reader.readLine()) != null) {
                container = tmpTxt.split("\t");
                keywords = new HashSet<String>(container.length - 1);
                for (int i = 1; i < container.length; i++) {
                    keywords.add(container[i]);
                }
                m_keywords.add(new _Aspect(container[0], keywords));
                System.out.println("Keywords for " + container[0] + ": " + keywords.size());
            }
            reader.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void LoadVocabulary(String filename) {
        try {
            m_vocabulary = new Hashtable<String, Integer>();
            m_wordlist = new Vector<String>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            String tmpTxt;
            String[] container;
            while ((tmpTxt = reader.readLine()) != null) {
                container = tmpTxt.split("\t");
                m_vocabulary.put(container[0], m_vocabulary.size());
                m_wordlist.add(tmpTxt.trim());
            }
            reader.close();
            m_isLoadCV = true;
            System.out.println("[Info]Load " + m_vocabulary.size() + " control terms...");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void LoadStopwords(String filename) {
        try {
            m_stopwords = new HashSet<String>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            String tmpTxt;
            while ((tmpTxt = reader.readLine()) != null) {
                m_stopwords.add(tmpTxt.toLowerCase());
            }
            reader.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] getLemma(String[] tokens) {
        String[] lemma = new String[tokens.length];
        String term;
        for (int i = 0; i < lemma.length; i++) {
            //lemma[i] = m_stemmer.stem(tokens[i].toLowerCase());//shall we stem it?
            term = tokens[i].toLowerCase();
            if (term.length() > 1 && PUNCT.indexOf(term.charAt(0)) != -1 && term.charAt(1) >= 'a' && term.charAt(1) <= 'z') {
                lemma[i] = term.substring(1);
            } else {
                lemma[i] = term;
            }
        }
        return lemma;
    }

    static public String getHotelID(String fname) {
        int start = fname.indexOf("hotel_"), end = fname.indexOf("_parsed.txt");
        if (start == -1) {
            return fname.substring(0, end);
        } else {
            return fname.substring(start + "hotel_".length(), end);
        }
    }

    private String cleanReview(String content) {
        String error_A = "showReview\\([\\d]+\\, [\\w]+\\);";//
        return content.replace(error_A, "");
    }
    
    
    private void expandVocabulary(HotelDocument hd) {
        Hashtable<String,Integer> ht = new Hashtable<>();
        for (Sentence stn : hd.m_stns) {
            for (Token t : stn.m_tokens) {
                if(!ht.containsKey(t.m_lemma)){
                    ht.put(t.m_lemma,1);
                }
            }
        }
        for(String key: ht.keySet()){
            if(idfTable.containsKey(key)){
                idfTable.put(key, idfTable.get(key) + 1);                
            }else{
                idfTable.put(key,1);
            }
        }
    }

    //the review format is fixed
    public void LoadReviews(String filename) {//load reviews for annotation purpose
        try {
            File f = new File(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String tmpTxt, fname = getHotelID(f.getName()), title = "", content = null;
            int nReviews = 0;
            String[] stns, tokens;
            Span[] stn_spans;
            int[] ratings = new int[1 + ASPECT_SET_NEW.length];
            boolean flag = false;
            //Hotel tHotel = new Hotel(fname);
            while ((tmpTxt = reader.readLine()) != null) {
                if(tmpTxt.startsWith("<Rating>")){
                    String[] ratingStrs = tmpTxt.substring("<Rating>".length()).split("\t");
                    
                    for(int ri = 0;ri<ratingStrs.length;ri++){
                        ratings[ri] = Integer.valueOf(ratingStrs[ri]);
                        if(ratings[ri]<0){
                            flag = true;
                        }
                    }
                    
                } else if (tmpTxt.startsWith("<Content>")) {
                    content = cleanReview(tmpTxt.substring("<Content>".length()));
                } else if (tmpTxt.isEmpty() && content != null && !flag) {
                    stn_spans = m_stnDector.sentPosDetect(content);//list of the sentence spans
                    if (stn_spans.length < 3) {
                        content = null;
                        Arrays.fill(ratings, 0);
                        continue;
                    }
                    stns = Span.spansToStrings(stn_spans, content);
                    HotelDocument hd = new HotelDocument(fname, ratings);
                    for (int i = 0; i < stns.length; i++) {
                        tokens = m_tokenizer.tokenize(stns[i]);
                        if (tokens != null && tokens.length > 2)//discard too short sentences
                        {
                            hd.addStn(stns[i], tokens, m_postagger.tag(tokens), getLemma(tokens), m_stopwords);
                        }
                    }

                    if ((hd.getNumSentences()> 2) && (hd.getReviewSize() >= 50)) {
                        if (title.isEmpty() == false) {//include the title as content
                            tokens = m_tokenizer.tokenize(title);
                            if (tokens != null && tokens.length > 2)//discard too short sentences
                            {
                                hd.addStn(title, tokens, m_postagger.tag(tokens), getLemma(tokens), m_stopwords);
                            }
                        }

                        if (m_isLoadCV == false)//didn't load the controlled vocabulary
                        {
                            expandVocabulary(hd);
                        }
                        nReviews++;
                    }

                    content = null;
                    Arrays.fill(ratings, 0);
                    m_hotelDocList.add(hd);
                }else if(flag){
                    flag = false;
                    continue;
                }
                
            }
            reader.close();

            if (m_hotelDocList.size() % 100 == 0) {
                System.out.print(".");
            }
            if (m_hotelDocList.size() % 10000 == 0) {
                System.out.println("-");
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void pruneVocabulary(){
        // This function prunes the vocabulary based on minIDF criteria
        int index = 0;
        for(String key: idfTable.keySet()){
            int idf = idfTable.get(key);
            if(idf>=10){
                m_vocabulary.put(key, index++);
            }
        }
    }
    public void prepareDocVectors(){
        Hashtable<Integer,Integer> ht = new Hashtable<>();
        for(HotelDocument hd:m_hotelDocList){
            for(Sentence st:hd.m_stns){
                for(Token token: st.m_tokens){
                    if(m_vocabulary.containsKey(token.m_lemma)){
                        int wordID = m_vocabulary.get(token.m_lemma);
                        if(ht.containsKey(wordID)){
                            ht.put(wordID, ht.get(wordID)+1);
                        }else{
                            ht.put(wordID, 1);
                        }
                    }
                }
            }
            int[] wordIndices = new int[ht.size()];
            int[] wordCounts = new int[ht.size()];
            int wi = 0;
            int total =0;
            for(Integer wordID:ht.keySet()){
                wordIndices[wi] = wordID;
                wordCounts[wi++] = ht.get(wordID);
                total += ht.get(wordID);
            }
            Document doc = new Document();
            doc.setWords(wordIndices);
            doc.setCounts(wordCounts);
            doc.setLength(wi);
            doc.setTotal(total);
            hd.doc = doc;
            ht.clear();
        }
    }
    
    public void LoadDirectory(String path, String suffix) {
        File dir = new File(path);
        int size = m_hotelDocList.size();
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(suffix)) {
                LoadReviews(f.getAbsolutePath());
            } else if (f.isDirectory()) {
                LoadDirectory(f.getAbsolutePath(), suffix);
            }
        }
        size = m_hotelDocList.size() - size;
        pruneVocabulary();
        prepareDocVectors();
        corpus = new Corpus(m_hotelDocList,m_vocabulary,m_vocabulary.size());
        System.out.println("Loading " + size + " hotels from " + path  + " Vocabulary Size:" + m_vocabulary.size());
    }

    //write Vector<HotelDocument> into a file using serailization
    public void SerializeHotelDocs(String fileName) {
        ObjectOutputStream o = null;
        try {
            o = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
            o.writeObject(corpus);
            o.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LARAMAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LARAMAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                o.close();
            } catch (IOException ex) {
                Logger.getLogger(LARAMAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    public Corpus UnSerializeHotelDocs(String fileName){
        ObjectInputStream oin = null;
        try {
            oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fileName)));
            return( (Corpus) oin.readObject());
        } catch (IOException ex) {
            Logger.getLogger(LARAMAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LARAMAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                oin.close();
            } catch (IOException ex) {
                Logger.getLogger(LARAMAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;       
    }

    


    
    
    private void getVocabularyStat() {
        if (m_wordCount == null) {
            m_wordCount = new double[m_vocabulary.size()];
        } else {
            Arrays.fill(m_wordCount, 0);
        }

        for (HotelDocument hotel : m_hotelDocList) {
            for (Sentence stn : hotel.m_stns) {
                for (Token t : stn.m_tokens) {
                    if (m_vocabulary.containsKey(t.m_lemma) == false) {
                        System.out.println("Missing:" + t);
                    } else {
                        int wordID = m_vocabulary.get(t.m_lemma);
                        m_wordCount[wordID]++;
                    }
                }
            }
        }
    }


    
    
    public void SaveVocabulary(String filename) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
            getVocabularyStat();

            Iterator<Map.Entry<String, Integer>> vIt = m_vocabulary.entrySet().iterator();
            while (vIt.hasNext()) {//iterate over all the words
                Map.Entry<String, Integer> entry = vIt.next();
                int wordID = entry.getValue();

                writer.write(entry.getKey() + "\t" + m_wordCount[wordID] + "\n");
            }
            writer.close();

            System.out.println("[Info]Vocabulary size: " + m_vocabulary.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //output word list with more statistic info: CHI, DF 
    
    
    static public void main(String[] args) {
        String prefix = "/home/nageshbhattu/BTechProjects/LARA/";
        LARAMAnalyzer analyzer = new LARAMAnalyzer(prefix + "Data/Seeds/hotel_bootstrapping.dat", prefix + "Data/Seeds/stopwords.dat",
                prefix + "Data/Model/NLP/en-sent.zip", prefix + "Data/Model/NLP/en-token.zip", prefix + "Data/Model/NLP/en-pos-maxent.bin");
        //analyzer.LoadVocabulary("Data/Seeds/hotel_vocabulary_CHI.dat");
        analyzer.LoadDirectory(prefix + "../Texts/", ".txt");
        //analyzer.LoadReviews("e:/Data/Reviews/Tmp/hotel_111849.dat");
        //analyzer.OutputWordListWithInfo("Data/Seeds/hotel_vocabulary_May10.dat");
        analyzer.SerializeHotelDocs("HotelData.ser");
        //analyzer.SaveVocabulary("Data/Seeds/hotel_vocabulary.dat");
    }
}
