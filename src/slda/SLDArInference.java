/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package slda;

import org.knowceans.lda.Document;
import org.knowceans.lda.LdaModel;
import static org.knowceans.lda.Utils.*;
import static java.lang.Math.*;
import static java.lang.Double.*;
import slda.SLDArDocument;
import slda.SLDArModel;

/**
 * lda inference functions
 * <p>
 * lda-c reference: functions in lda-inference.c. TODO: merge with model?
 * 
 * @author nageshbhattu
 */
public class SLDArInference {

    public double VAR_CONVERGED;

    public int VAR_MAX_ITER;
    

    /*
     * variational inference
     */
    public SLDArInference(){
        
    }
    public SLDArInference(double VAR_CONVERGED, int VAR_MAX_ITER){
        this.VAR_CONVERGED = VAR_CONVERGED;
        this.VAR_MAX_ITER = VAR_MAX_ITER;
    }
    
    public double sldaInference(SLDArDocument doc, SLDArModel model,
        double[] varGamma, double[][] phi, double[][] a) {
        
        double converged = 1;
        double phisum = 0, likelihood = 0, likelihoodOld = Double.NEGATIVE_INFINITY;
        double[] oldphi = new double[model.getNumTopics()];
        int k, n, varIter;
        
        double[] phiSummary = new double[model.getNumTopics()];
        double[] phiNotN = new double[model.getNumTopics()];
        
        
        assert model.getNumTopics() > 0;
        for (k = 0; k < model.getNumTopics(); k++) {
            varGamma[k] = model.getAlpha() + doc.getTotal()
                / (double) model.getNumTopics();
            
            for (n = 0; n < doc.getLength(); n++){
                phi[n][k] = 1.0 / model.getNumTopics();
                phiSummary[k] += phi[n][k] * doc.getCount(n);
            }
        }
        varIter = 0;
        while ((converged > VAR_CONVERGED) && (varIter < VAR_MAX_ITER)) {
            varIter++;
            for (n = 0; n < doc.getLength(); n++) {
                phisum = 0;
                
                for(k = 0;k<model.getNumTopics();k++){
                    phiNotN[k] = phiSummary[k] - phi[n][k]*doc.getCount(n);
                }
                
                double dProd = dotprod(model.getEta(),phiNotN, model.getNumTopics());
                double dNDeltaSq = model.getDetlaSq()* doc.getTotal();
                
                for (k = 0; k < model.getNumTopics(); k++) {
                    oldphi[k] = phi[n][k];
                    assert varGamma[k] != 0;
                    double dVal = doc.getCount(n)*model.eta[k]*(2*dProd + model.eta[k]*doc.getCount(n)) 
					/ (2*dNDeltaSq*doc.getTotal());
                    if (model.getClassWord(k, doc.getWord(n)) > 0) {
                        assert model.getClassTotal(k) != 0;
                        phi[n][k] = digamma(varGamma[k])
                            + log(model.getClassWord(k, doc.getWord(n)))
                            - log(model.getClassTotal(k))
                            + model.eta[k]*doc.getRating()* doc.getCount(n)/dNDeltaSq - dVal;
                    } else {
                        phi[n][k] = digamma(varGamma[k]) - 100;
                    }
                    if (k > 0) {
                        phisum = logSum(phisum, phi[n][k]);
                    } else {
                        phisum = phi[n][k];
                    }
                }
                for (k = 0; k < model.getNumTopics(); k++) {
                    phi[n][k] = exp(phi[n][k] - phisum);
                    varGamma[k] = varGamma[k] + doc.getCount(n)
                        * (phi[n][k] - oldphi[k]);
                    phiSummary[k] = phiNotN[k] + phi[n][k] * doc.getCount(n);
                }
            }
            amatrix(doc, model, phi, a);
           
            likelihood = computeLikelihood(doc, model, phi,a, varGamma,true);
            assert likelihoodOld != 0;
            converged = (likelihoodOld - likelihood) / likelihoodOld;
            likelihoodOld = likelihood;
        }
        return likelihood;
    }
    
    
    /* 
* Given the model and w, compute the E[Z] for prediction
*/
double sldaInferencePrediction(SLDArDocument doc, SLDArModel model, double[] varGamma, double[][] phi, double [][] a){
    int numTopics = model.getNumTopics();
    int docLength = doc.getLength();
    double converged = 1;
    double phisum = 0, likelihood = 0;
    double likelihood_old = 0;
    double[] oldphi = new double[numTopics];//(double*)malloc(sizeof(double)*model->num_topics);
    int k, n, var_iter;
    
    // compute posterior dirichlet
    for (k = 0; k < numTopics; k++) {
        varGamma[k] = model.getAlpha() + (doc.getTotal() / ((double) numTopics));
        for (n = 0; n < docLength; n++) {
            phi[n][k] = 1.0 / numTopics;
        }
    }
    var_iter = 0;

    int ncount, nwrd;
    while ((converged > VAR_CONVERGED) && ((var_iter < VAR_MAX_ITER) || (VAR_MAX_ITER == -1))) {
        var_iter++;
        for (n = 0; n < docLength; n++) {
            nwrd = doc.getWord(n);
            ncount = doc.getCount(n);
            phisum = 0;
            for (k = 0; k < numTopics; k++) {
                oldphi[k] = phi[n][k];
                phi[n][k] = digamma(varGamma[k]) + log(model.getClassWord(k, doc.getWord(n))) - log(model.getClassTotal(k));
		if (k > 0) {
                    phisum = logSum(phisum, phi[n][k]);
                } else {
                    phisum = phi[n][k]; // note, phi is in log space
                }
            }

            // update gamma and normalize phi
            for (k = 0; k < numTopics; k++) {
                phi[n][k] = exp(phi[n][k] - phisum);
                varGamma[k] = varGamma[k] + ncount * (phi[n][k] - oldphi[k]);
            }
        }
        amatrix(doc, model, phi, a);
        likelihood = computeLikelihood(doc, model, phi, a, varGamma, false);
        converged = (likelihood_old - likelihood) / likelihood_old;
        likelihood_old = likelihood;
    }
    return (likelihood);
}


    /*
     * compute likelihood bound
     */
    public static double computeLikelihood(SLDArDocument doc, SLDArModel model, double[][] phi, double[][]a, double[] varGamma,boolean bTrain ) {
        double likelihood = 0, digsum = 0, varGammaSum = 0;// , x;
        double[] dig = new double[model.getNumTopics()];
        int k, n, message = 0;

        for (k = 0; k < model.getNumTopics(); k++) {
            dig[k] = digamma(varGamma[k]);
            varGammaSum += varGamma[k];
        }
        digsum = digamma(varGammaSum);
        likelihood = lgamma(model.getAlpha() * model.getNumTopics())
            - model.getNumTopics() * lgamma(model.getAlpha())
            - (lgamma(varGammaSum));
        assert likelihood != Double.NaN;
        if ((isNaN(likelihood)) && (message == 0)) {
            // printf("(1) : %5.5f %5.5f\n", var_gamma_sum, likelihood);
            System.out.println("(1) : " + varGammaSum + " " + likelihood);
            message = 1;
        }
        for (k = 0; k < model.getNumTopics(); k++) {
            likelihood += (model.getAlpha() - 1) * (dig[k] - digsum)
                + lgamma(varGamma[k]) - (varGamma[k] - 1) * (dig[k] - digsum);
            assert likelihood != Double.NaN;
            if ((isNaN(likelihood)) && (message == 0)) {
                // printf("(2 %d) : %5.5f\n", k, likelihood);
                System.out.println("(2 " + k + ") : " + likelihood);
                message = 1;
            }
            double dVal = 0.0;
            for (n = 0; n < doc.getLength(); n++) {
                if (model.getClassWord(k, doc.getWord(n)) > 0) {
                    if (phi[n][k] > 0) {
                        // likelihood += doc->counts[n]*
                        // (phi[n][k]*((dig[k] - digsum) - log(phi[n][k])
                        // + log(model->class_word[k][doc->words[n]])
                        // - log(model->class_total[k])));
                        assert phi[n][k] > 0;
                        assert model.getClassTotal(k) > 0;
                        likelihood += doc.getCount(n)
                            * (phi[n][k] * ((dig[k] - digsum) - log(phi[n][k])
                                + log(model.getClassWord(k, doc.getWord(n))) - log(model
                                .getClassTotal(k))));
                        assert likelihood != Double.NaN;
                        if ((isNaN(likelihood)) && (message == 0)) {
                            // printf("(2 %d) : %5.5f\n", k, likelihood);
                            // printf("(3 %d %d) : %5.5f\n", k, n, likelihood);
                            System.out.println("(2 " + k + ") : " + likelihood);
                            System.out.println("(3 " + k + " " + n + ") : "
                                + likelihood);
                        }
                    }
                }
                dVal += phi[n][k] * doc.getCount(n) / doc.getTotal();
            }
            if(bTrain){
                likelihood += doc.getRating()* model.eta[k] * dVal/model.getDetlaSq();
                likelihood -= 0.5 * log( model.getDetlaSq() * 2 * 3.14159265);
		likelihood -= (doc.getRating() * doc.getRating()) / ( 2 * model.getDetlaSq() );

		double[] arry = matrixprod(model.eta, a,  model.getNumTopics());
		double dVal2 = dotprod(arry, model.eta, model.getNumTopics());

		likelihood -= dVal2 / ( 2 * model.getDetlaSq() );
            }
        }
        return likelihood;
    }
    /*
    * Compute the matrix E[zz^t]
    */
    void amatrix(SLDArDocument doc, SLDArModel model, double[][] phi, double[][] a){
        for(int k = 0; k<model.getNumTopics();k++){
            for(int i = 0; i< model.getNumTopics();i++){
                a[k][i]  = 0;
            }
        }
        double dnorm = doc.getTotal() * doc.getTotal();
        for(int n = 0;n<doc.getLength();n++){
            for(int k = 0;k<model.getNumTopics();k++){
                a[k][k] += phi[n][k]*doc.getCount(n)*doc.getCount(n)/dnorm;
            }
            for(int m = n+1;m<doc.getLength();m++){
                double dfactor = doc.getCount(n)*doc.getCount(m)/dnorm;
                addmatrix2(a, phi[n], phi[m], model.getNumTopics(), dfactor);
            }
        }
    }
}
