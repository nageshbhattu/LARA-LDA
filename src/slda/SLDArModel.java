/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package slda;

import org.knowceans.lda.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * wrapper for an LDA model.
 * <p>
 * lda-c reference: Combines the struct lda_model in lda.h and the code in
 * lda-model.h
 * 
 * @author nageshbhattu
 */
public class SLDArModel {

    public double alpha;

    private double[][] classWord;

    private double[] classTotal;

    public int numTopics;

    private int numTerms;
    
    public double[] eta;
    
    public double deltasq;
    
    // These variables are used as summary statistics
    public double[] phiSum;

    public double ysquared;
    
    public double[] ezy;
    
    public double[][] covMatrix;
    
    public double alphaSuffStats;
    /**
     * create an empty lda model with parameters:
     * 
     * @param numTerms
     *            number of terms in dictionary
     * @param numTopics
     *            number of topics
     */
    public SLDArModel(int numTerms, int numTopics) {
        int i, j;
        this.numTopics = numTopics;
        this.numTerms = numTerms;
        this.alpha = 1;

        initArrays(numTerms, numTopics);
    }

    /**
     * initialise data array in the model.
     * 
     * @param numTerms
     * @param numTopics
     */
    private void initArrays(int numTerms, int numTopics) {

        classTotal = new double[numTopics];
        classWord = new double[numTopics][numTerms];
        eta = new double[numTopics];
        phiSum = new double[numTopics];
        for (int i = 0; i < numTopics; i++) {
            this.classTotal[i] = 0;
            for (int j = 0; j < numTerms; j++) {
                this.classWord[i][j] = 0;
            }
        }
        covMatrix = new double[numTopics][numTopics];
        ezy = new double[numTopics];
    }
    
    public void zeroInitializeSS() {
        this.alphaSuffStats = 0.0;
        for(int i  =0; i<covMatrix.length;i++){
            Arrays.fill(covMatrix[i], 0.0);
        }
        Arrays.fill(ezy, 0.0);
    }

    /**
     * create an lda model from information read from the files below modelRoot,
     * i.e. {root}.beta and {root}.other.
     * 
     * @param modelRoot
     */
    public SLDArModel(String modelRoot) {
        String filename;
        int i, j;
        double x, alpha = 0;
        filename = modelRoot + ".other";
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {

                if (line.startsWith("num_topics ")) {
                    numTopics = Integer.parseInt(line.substring(11).trim());
                } else if (line.startsWith("num_terms ")) {
                    numTerms = Integer.parseInt(line.substring(10).trim());
                } else if (line.startsWith("alpha ")) {
                    alpha = Double.parseDouble(line.substring(6).trim());
                } else if(line.startsWith("eta ")){
                    String etaStr = line.substring("eta ".length()).trim();
                    String[] etaStrs = etaStr.split(" ");
                    for(int topic = 0;topic<numTopics;topic++){
                        eta[topic] = Double.parseDouble(etaStrs[topic]);
                    }
                } else if(line.startsWith("deltasq ")){
                    deltasq = Double.parseDouble(line.substring("deltasq ".length()).trim());
                }
            }
            br.close();
            initArrays(numTerms, numTopics);
            this.alpha = alpha;
            filename = modelRoot + ".beta";
            System.out.println("loading " + filename);
            br = new BufferedReader(new FileReader(filename));
            for (i = 0; i < numTopics; i++) {
                this.classTotal[i] = 0;
                line = br.readLine();
                String[] fields = line.trim().split(" ");
                for (j = 0; j < numTerms; j++) {
                    // fscanf(fileptr, "%f", &x);
                    x = Double.parseDouble(fields[j]);
                    this.classWord[i][j] = x;
                    this.classTotal[i] += this.classWord[i][j];
                }
            }
            br.close();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * deallocate lda model (dummy)
     */
    public void free() {
        // nothing to do in Java
    }

    /**
     * save an lda model
     * 
     * @param modelRoot
     */
    public void save(String modelRoot) {
        int i, j;

        String filename = modelRoot + ".beta";

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            for (i = 0; i < this.numTopics; i++) {
                for (j = 0; j < this.numTerms; j++) {
                    if (j > 0)
                        bw.write(' ');
                    bw.write(Utils.formatDouble(this.classWord[i][j]
                        / this.classTotal[i]));
                }
                bw.newLine();
            }
            bw.newLine();
            bw.close();
            filename = modelRoot + ".other";
            bw = new BufferedWriter(new FileWriter(filename));
            bw.write("num_topics " + numTopics + "\n");
            bw.write("num_terms " + numTerms + "\n");
            bw.write("alpha " + alpha + "\n");
            bw.write("eta");
            for(int topic = 0;topic<numTopics; topic++){
                bw.write(" "+ eta[topic]);
            }
            bw.write("\n");
            bw.write("deltasq " + deltasq + "\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * @return
     */
    public double[] getClassTotal() {
        return classTotal;
    }

    /**
     * @param cls
     * @return
     */
    public double getClassTotal(int cls) {
        return classTotal[cls];
    }

    /**
     * @param cls
     * @param total
     */
    public void setClassTotal(int cls, double total) {
        classTotal[cls] = total;
    }

    /**
     * @param cls
     * @param total
     */
    public void addClassTotal(int cls, double total) {
        classTotal[cls] += total;
    }

    /**
     * @return
     */
    public double[][] getClassWord() {
        return classWord;
    }

    /**
     * @param cls
     * @param word
     * @return
     */
    public double getClassWord(int cls, int word) {
        return classWord[cls][word];
    }

    /**
     * @param cls
     * @param word
     * @param value
     */
    public void setClassWord(int cls, int word, double value) {
        classWord[cls][word] = value;
    }

    /**
     * @param cls
     * @param word
     * @param value
     */
    public void addClassWord(int cls, int word, double value) {
        classWord[cls][word] += value;
    }

    /**
     * @return
     */
    public int getNumTerms() {
        return numTerms;
    }

    /**
     * @return
     */
    public int getNumTopics() {
        return numTopics;
    }

    /**
     * @param d
     */
    public void setAlpha(double d) {
        alpha = d;
    }

    /**
     * @param ds
     */
    public void setClassTotal(double[] ds) {
        classTotal = ds;
    }

    /**
     * @param ds
     */
    public void setClassWord(double[][] ds) {
        classWord = ds;
    }

    /**
     * @param i
     */
    public void setNumTerms(int i) {
        numTerms = i;
    }

    /**
     * @param i
     */
    public void setNumTopics(int i) {
        numTopics = i;
    }
    
    
    public void setEta(double[] eta){
        this.eta = eta;
    }
    
    
    public double[] getEta(){
        return eta;
    }
    
    public void setDeltaSq(double deltaSq){
        this.deltasq = deltaSq;
    }
    
    public double getDetlaSq(){
        return this.deltasq;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("Model {numTerms=" + numTerms + " numTopics=" + numTopics
            + " alpha=" + alpha + "}");
        return b.toString();
    }

}
