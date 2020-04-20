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
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;
import Preprocessing.Sentence;
import Preprocessing.Token;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.knowceans.lda.Corpus;
import org.knowceans.lda.Document;
import org.knowceans.lda.LdaEstimate;
import static org.knowceans.lda.LdaEstimate.runEm;
import org.knowceans.lda.LdaInference;
import org.knowceans.lda.LdaModel;

public class LDAAnalyzer {
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

    Vector<Hotel> m_hotelList;
    Vector<_Aspect> m_keywords;
    Hashtable<String, Integer> m_vocabulary;//indexed vocabulary
    Vector<String> m_wordlist;//term list in the original order

    HashSet<String> m_stopwords;
    double[][] m_chi_table;
    double[] m_wordCount;
    boolean m_isLoadCV; // if the vocabulary is fixed

    //specific parameter to be tuned for bootstrapping aspect segmentation
    static public double chi_ratio = 4.0;
    static public int chi_size = 35;
    static public int chi_iter = 10;
    static public int tf_cut = 10;

    //NLP modules
    SentenceDetectorME m_stnDector;
    TokenizerME m_tokenizer;
    POSTaggerME m_postagger;
    Stemmer m_stemmer;
    int numAspects;
    public LDAAnalyzer(String seedwords, String stopwords, String stnSplModel, String tknModel, String posModel) {
        m_hotelList = new Vector<Hotel>();
        m_vocabulary = new Hashtable<String, Integer>();
        m_chi_table = null;
        m_isLoadCV = false;
        numAspects = ASPECT_SET.length;
        //if (seedwords != null && seedwords.isEmpty()==false)
        //	LoadKeywords(seedwords);
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
        System.out.println("[Info]NLP modules initialized...");
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
        int start = fname.indexOf("hotel_"), end = fname.indexOf(".dat");
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

    //the review format is fixed
    public void LoadReviews(String filename) {//load reviews for annotation purpose
        try {
            File f = new File(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String tmpTxt, fname = getHotelID(f.getName()), title = "", content = null;
            int review_size = 0;

            Review review = null;
            String[] stns, tokens;
            Span[] stn_spans;
            int[] ratings = new int[1 + numAspects];
            Hotel tHotel = new Hotel(fname);
            HashMap<Integer,Integer> hmap = new HashMap<>();
            while ((tmpTxt = reader.readLine()) != null) {
                if (tmpTxt.startsWith("<Title>")) {
                    title = tmpTxt.substring("<Title>".length() + 1, tmpTxt.length() - 1);
                } else if (tmpTxt.startsWith("<Overall>")) {//only read those aspects
                    try {
                        double r = Double.valueOf(tmpTxt.substring("<Overall>".length()));
                        ratings[0] = (int) r;
                    } catch (Exception e) {
                        System.err.println("Error format: " + fname);
                        reader.close();
                        return;
                    }
                } else if (tmpTxt.startsWith("<Value>")) {
                    ratings[1] = Integer.valueOf(tmpTxt.substring("<Value>".length()));
                } else if (tmpTxt.startsWith("<Rooms>")) {
                    ratings[2] = Integer.valueOf(tmpTxt.substring("<Rooms>".length()));
                } else if (tmpTxt.startsWith("<Location>")) {
                    ratings[3] = Integer.valueOf(tmpTxt.substring("<Location>".length()));
                } else if (tmpTxt.startsWith("<Cleanliness>")) {
                    ratings[4] = Integer.valueOf(tmpTxt.substring("<Cleanliness>".length()));
                } else if (tmpTxt.startsWith("<Service>")) {
                    ratings[5] = Integer.valueOf(tmpTxt.substring("<Service>".length()));
                } else if (tmpTxt.startsWith("<Check in / front desk>")) {
                    ratings[6] = Integer.valueOf(tmpTxt.substring("<Check in / front desk>".length()));
                }else if (tmpTxt.startsWith("<Business service>")) {
                    ratings[7] = Integer.valueOf(tmpTxt.substring("<Business service>".length()));
                } else if (tmpTxt.startsWith("<Content>")) {
                    content = cleanReview(tmpTxt.substring("<Content>".length()));
                } else if (tmpTxt.isEmpty() && content != null) {
                    stn_spans = m_stnDector.sentPosDetect(content);//list of the sentence spans
                    if (stn_spans.length < 3) {
                        content = null;
                        Arrays.fill(ratings, 0);
                        continue;
                    }

                    stns = Span.spansToStrings(stn_spans, content);
                    review = new Review(fname, Integer.toString(review_size), ratings);
                    for (int i = 0; i < stns.length; i++) {
                        tokens = m_tokenizer.tokenize(stns[i]);
                        if (tokens != null && tokens.length > 2)//discard too short sentences
                        {
                            review.addStn(stns[i], tokens, m_postagger.tag(tokens), getLemma(tokens), m_stopwords);
                        }
                    }

                    if (review.getStnSize() > 2) {
                        if (title.isEmpty() == false) {//include the title as content
                            tokens = m_tokenizer.tokenize(title);
                            if (tokens != null && tokens.length > 2)//discard too short sentences
                            {
                                review.addStn(title, tokens, m_postagger.tag(tokens), getLemma(tokens), m_stopwords);
                            }
                        }

                        if (m_isLoadCV == false)//didn't load the controlled vocabulary
                        {
                            expendVocabular(review);
                        }
                        tHotel.addReview(review);
                        review_size++;
                        
                        for(Sentence s:review.m_stns){
                            for(Token t:s.m_tokens){
                                Integer wordID = m_vocabulary.get(t.m_lemma);
                                if(hmap.containsKey(wordID)){
                                    hmap.put(wordID, hmap.get(wordID)+1);
                                }else{
                                    hmap.put(wordID,1);
                                }
                            }
                        }
                    }

                    content = null;
                    Arrays.fill(ratings, 0);
                }
            }
            reader.close();
            tHotel.wcDict = hmap;
            if (tHotel.getReviewSize() > 1) {
                m_hotelList.add(tHotel);
                if (m_hotelList.size() % 100 == 0) {
                    System.out.print(".");
                }
                if (m_hotelList.size() % 10000 == 0) {
                    System.out.println(".");
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void LoadDirectory(String path, String suffix) {
        File dir = new File(path);
        int size = m_hotelList.size();
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(suffix)) {
                LoadReviews(f.getAbsolutePath());
            } else if (f.isDirectory()) {
                LoadDirectory(f.getAbsolutePath(), suffix);
            }
        }
        size = m_hotelList.size() - size;
        System.out.println("Loading " + size + " hotels from " + path);
    }



    private void clearVector(double[] ratings, double[] counts, int[][] vectors) {
        Arrays.fill(ratings, 0);
        Arrays.fill(counts, 0);
        for (int aspectID = 0; aspectID < vectors.length; aspectID++) {
            Arrays.fill(vectors[aspectID], 0);
        }
    }

    //more strategies can be derived for selecting the reviews
    private boolean ready4output(int[][] vectors, double[] counts) {
        for (int aspectID = 0; aspectID < vectors.length; aspectID++) {
            if (counts[aspectID] <= ASPECT_COUNT_CUT) {
                return false;//at least have these amount of user ratings
            }
            double sum = 0;
            for (int wordID = 0; wordID < vectors[aspectID].length; wordID++) {
                sum += vectors[aspectID][wordID];
            }
            if (sum <= ASPECT_CONTENT_CUT) {
                return false;//at least have these amount of words in content
            }
        }
        return true;
    }

    private void expendVocabular(Review tReview) {
        for (Sentence stn : tReview.m_stns) {
            for (Token t : stn.m_tokens) {
                if (m_vocabulary.containsKey(t.m_lemma) == false) {
                    m_vocabulary.put(t.m_lemma, m_vocabulary.size());
                }
            }
        }
    }


    private void getVocabularyStat() {
        if (m_wordCount == null) {
            m_wordCount = new double[m_vocabulary.size()];
        } else {
            Arrays.fill(m_wordCount, 0);
        }

        for (Hotel hotel : m_hotelList) {
            for (Review tReview : hotel.m_reviews) {
                for (Sentence stn : tReview.m_stns) {
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
    public void getLDAAspectVectors(String filename) {
        BufferedWriter writer = null;
        try {
            System.out.println("Vocabulary size: " + m_vocabulary.size());
            LdaEstimate.INITIAL_ALPHA = 50.0;
            LdaEstimate.readSettings("settings.txt");
            Corpus corpus = new Corpus(m_hotelList, m_vocabulary,numAspects);
            boolean a = new File("Final").mkdir();
            LdaModel model = LdaEstimate.runEm("seeded", "Final", corpus, numAspects);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
            for(int h=0;h<m_hotelList.size();h++){
                Hotel hotel = m_hotelList.get(h);
                Document doc = hotel.doc;
                double[][] phi = new double[doc.getLength()][model.getNumTopics()];
                double [] gamma = new double[model.getNumTopics()];
                double likelihood = LdaInference.ldaInference(doc, model, gamma, phi);
                writer.write(hotel.m_ID +" " + hotel.m_rating );
                for(int aspectID = 0;aspectID<numAspects;aspectID++){
                    writer.write(" "+hotel.ratings[aspectID]/hotel.counts[aspectID]);
                }
                writer.write("\n");
                for(int aspectID = 0;aspectID<numAspects;aspectID++){
                    writer.write(doc.getLength());
                    for(int wordID=0;wordID<doc.getLength();wordID++){
                        writer.write(" "+ doc.getWord(wordID)+":"+doc.getCount(wordID)*phi[wordID][aspectID]);
                    }
                    writer.write("\n");
                }
            }
            writer.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LDAAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LDAAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LDAAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(LDAAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    public void writeHotelWordVectors(String fileName){
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
            HashMap<Integer, Integer> hmap = new HashMap<>();
            for(Hotel hotel: m_hotelList){
                try {
                    
                    double[] ratings = new double[numAspects+1];
                    double[] counts = new double[numAspects+1];
                    for (Review r : hotel.m_reviews) {
                        /*for (Review.Sentence s : r.m_stns) {
                            for (Review.Token t : s.m_tokens) {
                                if (m_vocabulary.contains(t.m_lemma)) {
                                    if (hmap.containsKey(m_vocabulary.get(t.m_lemma))) {
                                        hmap.put(m_vocabulary.get(t.m_lemma), hmap.get(m_vocabulary.get(t.m_lemma)) + 1);
                                    } else {
                                        hmap.put(m_vocabulary.get(t.m_lemma), 1);
                                    }
                                }
                            }
                        }*/
                        for(int aspectID =0;aspectID<r.m_ratings.length;aspectID++){
                            if(r.m_ratings[aspectID]>0){
                                ratings[aspectID]+=r.m_ratings[aspectID];
                                counts[aspectID]++;
                            }
                        }
                    }
                    hmap = hotel.wcDict;
                    System.out.println(hotel.m_ID + "::" + hmap.size() + "::");
                    writer.write(hotel.m_ID+"");
                    for(int aspectID=0;aspectID<numAspects+1;aspectID++){
                        writer.write(" " + ratings[aspectID]/counts[aspectID]);
                    }
                    for(Integer word:hmap.keySet()){
                        writer.write(" "+ word+":"+hmap.get(word) );
                    }
                    writer.write("\n");
                    hmap.clear();
                } catch (IOException ex) {
                    Logger.getLogger(LDAAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(LDAAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    static public void main(String[] args) {
        String prefix = "/home/nageshbhattu/BTechProjects/LARA/";
        LDAAnalyzer analyzer = new LDAAnalyzer(prefix + "Data/Seeds/hotel_bootstrapping.dat", prefix +"Data/Seeds/stopwords.dat",
                prefix + "Data/Model/NLP/en-sent.zip", prefix + "Data/Model/NLP/en-token.zip", prefix + "Data/Model/NLP/en-pos-maxent.bin");
        //analyzer.LoadVocabulary("Data/Seeds/hotel_vocabulary_CHI.dat");
        analyzer.LoadDirectory(prefix + "../LDA/Review_Texts/", ".dat");
        //analyzer.LoadReviews("e:/Data/Reviews/Tmp/hotel_111849.dat");
        analyzer.writeHotelWordVectors("output.txt");
        //analyzer.getLDAAspectVectors("output.txt");
        //analyzer.OutputWordListWithInfo("Data/Seeds/hotel_vocabulary_May10.dat");
        //analyzer.Save2Vectors("Data/Vectors/vector_CHI_4000.dat");
        //analyzer.SaveVocabulary("Data/Seeds/hotel_vocabulary.dat");
    }
}
