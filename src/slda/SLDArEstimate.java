
/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package slda;

import org.knowceans.lda.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Math.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Random;
import static org.knowceans.lda.Utils.digamma;
import static slda.SLDArMStep.estimateDeltaSq;
import static slda.SLDArMStep.estimateEta;

/**
 * lda parameter estimation
 * <p>
 * lda-c reference: functions in lda-estimate.c.
 * 
 * @author nageshbhattu
 */
public class SLDArEstimate {

    int LAG = 10;

    int NUM_INIT = 1;

    public float EM_CONVERGED;

    public int EM_MAX_ITER;

    public int ESTIMATE_ALPHA;

    public double INITIAL_ALPHA;

    public double numTopics;

    public Random rand = new Random(4357);
    
    
    public SLDArInference sldarInference; 
    
    //public double[][] covMatrix;
    //public double alphaSuffStats;
    //public double[] ezy;
        //CokusNative.cokusseed(4357);
    public SLDArEstimate(){
        sldarInference = new SLDArInference();
    }
    public SLDArEstimate(double VAR_CONVERGED, int VAR_MAX_ITER){
        sldarInference = new SLDArInference(VAR_CONVERGED, VAR_MAX_ITER);
    }

    double myrand() {
        return rand.nextDouble();
    }

    /**
     * initializes class_word and class_total to reasonable beginnings.
     */
    public SLDArModel initialModel(String start, SLDArCorpus corpus,
        int numTopics, double alpha) {
        int k, d, i, n;
        SLDArModel model;
        SLDArDocument doc;

        if (start.equals("seeded")) {
            model = new SLDArModel(corpus.getNumTerms(), numTopics);
            model.setAlpha(alpha);  //we can set this in above constructor
            // foreach topic
            for (k = 0; k < numTopics; k++) {
                // sample NUM_INIT documents and add their term counts to the
                // class-word table
                for (i = 0; i < NUM_INIT; i++) {
                    d = (int) floor(myrand() * corpus.getNumDocs());
                    System.out.println("initialized with document " + d);
                    doc = corpus.getDoc(d);
                    for (n = 0; n < doc.getLength(); n++) {
                        model.addClassWord(k, doc.getWord(n), doc.getCount(n));
                    }
                }
                // add to all terms in class-word table 1/nTerms; update class
                // total accordingly
                assert model.getNumTerms() > 0;
                for (n = 0; n < model.getNumTerms(); n++) {
                    model.addClassWord(k, n, 1.0 / model.getNumTerms());
                    model.addClassTotal(k, model.getClassWord(k, n));
                }
                model.ezy[k] = -1 + 2 * k /model.getNumTopics();
                for(int l = 0;l<model.getNumTopics();l++){
                    model.covMatrix[k][l] = 0.0;
                }
            }
            for (d = 0;d<corpus.getNumDocs();d++){
                model.ysquared += corpus.getDoc(d).getRating() * corpus.getDoc(d).getRating();
            }
        } else if (start.equals("random")) {
            model = new SLDArModel(corpus.getNumTerms(), numTopics);
            model.setAlpha(alpha);
            // for each topic
            for (k = 0; k < numTopics; k++) {
                // add to all terms in class-word table a random sample \in
                // (0,1) plus 1/nTerms;
                // update class total accordingly
                for (n = 0; n < model.getNumTerms(); n++) {
                    model.addClassWord(k, n, 1.0 / model.getNumTerms()
                        + myrand());
                    model.addClassTotal(k, model.getClassWord(k, n));
                }
                // SLDA sufficient stats must be initialized
                model.ezy[k] = -1 + 2 * k /model.getNumTopics();
                for(int l = 0;l<model.getNumTopics();l++){
                    model.covMatrix[k][l] = 0.0;
                }
            }
            for (d = 0;d<corpus.getNumDocs();d++){
                model.ysquared += corpus.getDoc(d).getRating() * corpus.getDoc(d).getRating();
            }
            double[] eta = rand.doubles().toArray();
            model.setEta(eta);
            
        } else {
            // load model from file (.beta and .other data)
            model = new SLDArModel(start);
        }
        
        model.alphaSuffStats = 0.0;
        model.alpha = INITIAL_ALPHA/(double)model.getNumTopics();
        return model;
    }

    /**
     * iterate_document
     */
    public double docEm(SLDArDocument doc, double[] gamma, SLDArModel model,
        SLDArModel nextModel, double[][]a) {
        double likelihood;
        double[][] phi;
        int n, k;

        phi = new double[doc.getLength()][model.getNumTopics()];
        likelihood = sldarInference.sldaInference(doc, model, gamma, phi, a);
        double gammaSum = 0.0;
        for (k = 0; k < model.getNumTopics(); k++) {
            double dVal = 0;
            gammaSum +=gamma[k];
            model.alphaSuffStats += digamma(gamma[k]);
            for (n = 0; n < doc.getLength(); n++) {
                double ePhi = doc.getCount(n) * phi[n][k];
                nextModel.addClassWord(k, doc.getWord(n), ePhi);
                nextModel.addClassTotal(k, ePhi);
                dVal+= ePhi/doc.getTotal();
            }
            for(int m = 0;m<model.getNumTopics();m++){
                model.covMatrix[k][m] += a[k][m];
            }
            model.ezy[k] += dVal*doc.getRating();
        }
        model.alphaSuffStats -= model.getNumTopics() * digamma(gammaSum);
        return likelihood;
    }

    /**
     * saves the gamma parameters of the current dataset
     */
    static void saveGamma(String filename, double[][] gamma, int numDocs,
        int numTopics) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            int d, k;
            for (d = 0; d < numDocs; d++) {
                for (k = 0; k < numTopics; k++) {
                    if (k > 0)
                        bw.write(' ');
                    bw.write(Utils.formatDouble(gamma[d][k]));
                }
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * run_em
     */
    public SLDArModel runEm(String start, String directory, SLDArCorpus corpus, int numTopics) {
        try {
            BufferedWriter likelihoodFile;
            String filename;
            int i, d;
            double likelihood, likelihoodOld = Double.NEGATIVE_INFINITY, converged = 1;
            SLDArModel model, nextModel;
            double[][] varGamma;
            filename = directory + "/" + "likelihood.dat";
            likelihoodFile = new BufferedWriter(new FileWriter(filename));
            varGamma = new double[corpus.getNumDocs()][numTopics];
            model = initialModel(start, corpus, numTopics, INITIAL_ALPHA);
            filename = directory + "/000";
            model.save(filename);
            double dmean = 0;
            for (d = 0; d < corpus.getNumDocs(); d++)
                dmean += corpus.getDoc(d).getRating() / corpus.getNumDocs();
            model.deltasq = 0;
           for (d = 0; d < corpus.getNumDocs(); d++)
                model.deltasq += (corpus.getDoc(d).getRating() - dmean) * (corpus.getDoc(d).getRating() - dmean)
                / corpus.getNumDocs();
            
            double[][] a = new double[numTopics][numTopics];
            i = 0;
            NumberFormat nf = new DecimalFormat("000");
            String itername = "";
            while (((converged > EM_CONVERGED) || (i <= 2))
                && (i <= EM_MAX_ITER)) {
                i++;
                System.out.println("**** em iteration " + i + " ****");
                likelihood = 0;
                model.zeroInitializeSS();
                nextModel = new SLDArModel(model.getNumTerms(), model.getNumTopics());
                nextModel.setAlpha(INITIAL_ALPHA);
                for (d = 0; d < corpus.getNumDocs(); d++) {
                    if ((d % 10000) == 0)
                        System.out.println("document " + d);
                    likelihood += docEm(corpus.getDoc(d), varGamma[d], model, nextModel,a);
                }
                if (ESTIMATE_ALPHA == 1)
                    SLDArMStep.maximizeAlpha(varGamma, nextModel, corpus.getNumDocs());
                    nextModel.eta = estimateEta(model.covMatrix,model.ezy,model.getNumTopics());
                    nextModel.deltasq = estimateDeltaSq(model.ysquared,nextModel.eta, model.ezy, model.getNumTopics(), corpus.getNumDocs());
                nextModel.ysquared = model.ysquared;
                model.free();
                model = nextModel;
                assert likelihoodOld != 0;
                converged = (likelihoodOld - likelihood) / likelihoodOld;
                likelihoodOld = likelihood;
                // fprintf(likelihood_file, "%10.10f\t%5.5e\n", likelihood,
                // converged);
                likelihoodFile.write(likelihood + "\t" + converged + "\n");
                likelihoodFile.flush();

                if ((i % LAG) == 0) {
                    // sprintf(filename,"%s/%03d",directory, i);
                    itername = nf.format(i);
                    filename = directory + "/" + itername;
                    model.save(filename);

                    filename = directory + "/" + itername + ".gamma";
                    saveGamma(filename, varGamma, corpus.getNumDocs(), model
                        .getNumTopics());
                }
            }
            itername = nf.format(i);
            filename = directory + "/" + itername;
            model.save(filename);
            filename = directory + "/" + itername + ".gamma";
            saveGamma(filename, varGamma, corpus.getNumDocs(), model
                .getNumTopics());
            likelihoodFile.close();
            return model;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * read settings.
     */
    public void readSettings(String filename) {
        String alphaAction = "";
        int VAR_MAX_ITER = 0;
        double VAR_CONVERGED = 0.0;
        
        

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {

                if (line.startsWith("var max iter ")) {
                    sldarInference.VAR_MAX_ITER = Integer.parseInt(line
                        .substring(13).trim());
                } else if (line.startsWith("var convergence ")) {
                    sldarInference.VAR_CONVERGED = Float.parseFloat(line
                        .substring(16).trim());
                } else if (line.startsWith("em max iter ")) {
                    this.EM_MAX_ITER = Integer.parseInt(line.substring(12).trim());
                } else if (line.startsWith("em convergence ")) {
                    this.EM_CONVERGED = Float.parseFloat(line.substring(15).trim());
                } else if (line.startsWith("alpha ")) {
                    alphaAction = line.substring(6).trim();
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

        if (alphaAction.equals("fixed")) {
            ESTIMATE_ALPHA = 0;
        } else {
            ESTIMATE_ALPHA = 1;
        }
    }

    /**
     * inference only
     */
    public void infer(String modelRoot, String save, SLDArCorpus corpus) {
        String filename;
        int i, d, n;
        SLDArModel model;
        double[][] varGamma, phi;
        double likelihood;
        SLDArDocument doc;

        model = new SLDArModel(modelRoot);
        varGamma = new double[corpus.getNumDocs()][model.getNumTopics()];
        filename = save + ".likelihoods";
        double[][] a = new double[model.getNumTopics()][model.getNumTopics()];

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            for (d = 0; d < corpus.getNumDocs(); d++) {
                if ((d % 10000) == 0)
                    System.out.println("document " + d);

                doc = corpus.getDoc(d);
                phi = new double[doc.getLength()][model.getNumTopics()];
                likelihood = sldarInference.sldaInference(doc, model, varGamma[d], phi, a);

                bw.write(Utils.formatDouble(likelihood));
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        filename = save + ".gamma";
        saveGamma(filename, varGamma, corpus.getNumDocs(), model.getNumTopics());
    }

    /**
     * main
     */
    public static void main(String[] args) {

        int i = 0;
        double y = 0;
        double x, z, d;
        SLDArCorpus corpus;
        SLDArEstimate sldarEstimate = new SLDArEstimate();
        // command: lda est 0.10 16 settings.txt berry/berry.dat seeded
        // berry.model
        if (args[0].equals("est")) {
            if (args.length < 7) {
                System.out
                    .println("usage\n: slda est <initial alpha> <k> <settings> <data> <random/seeded/*> <directory>");
                System.out
                    .println("example\n: slda est 10 100 settings.txt ..\\ap\\ap.dat seeded ..\\ap.model");
                return;
            }

            sldarEstimate.INITIAL_ALPHA = Float.parseFloat(args[1]);
            int K = Integer.parseInt(args[2]);
            sldarEstimate.readSettings(args[3]);
            corpus = new SLDArCorpus(args[4]);
            boolean a = new File(args[6]).mkdir();

            System.out.println("LDA estimation. Settings:");
            System.out.println("\tvar max iter " + sldarEstimate.sldarInference.VAR_MAX_ITER);
            System.out.println("\tvar convergence "+ sldarEstimate.sldarInference.VAR_CONVERGED);
            System.out.println("\tem max iter " + sldarEstimate.EM_MAX_ITER);
            System.out.println("\tem convergence " + sldarEstimate.EM_CONVERGED);
            System.out.println("\testimate alpha " + sldarEstimate.ESTIMATE_ALPHA);

            SLDArModel model = sldarEstimate.runEm(args[5], args[6], corpus, K);
            // model.getClassWord();

        } else {
            // command: lda inf settings.txt berry.model berry/berry.dat
            // berry.inf
            if (args.length < 5) {
                System.out
                    .println("usage\n: slda inf <settings> <model> <data> <name>\n");
                System.out
                    .println("example\n: slda inf settings.txt ..\\ap.model ..\\aptest ..\\aptest.inf\n");
                return;
            }
            sldarEstimate.readSettings(args[1]);

            System.out.println("LDA inference. Settings:");
            System.out.println("\tvar max iter " + sldarEstimate.sldarInference.VAR_MAX_ITER);
            System.out.println("\tvar convergence "
                + sldarEstimate.sldarInference.VAR_CONVERGED);
            System.out.println("\tem max iter " + sldarEstimate.EM_MAX_ITER);
            System.out.println("\tem convergence " + sldarEstimate.EM_CONVERGED);
            System.out.println("\testimate alpha " + sldarEstimate.ESTIMATE_ALPHA);
            corpus = new SLDArCorpus(args[3]);
            sldarEstimate.infer(args[2], args[4], corpus);
        }
    }

    
}
