/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package slda;

import static org.knowceans.lda.Utils.*;
import static java.lang.Math.*;
import org.apache.commons.math3.linear.LUDecomposition;
import slda.SLDArModel;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
/**
 * performs optimisation tasks
 * <p>
 * lda-c reference: functions in lda-alpha.c.
 * 
 * @author heinrich
 */
public class SLDArMStep {

    public static double objective(double s, double suffStats, int numDocs,
        int numTopics) {
        double r = 0;
        assert numTopics > 0;
        assert numDocs > 0;
        r = ((s / numTopics) - 1) * suffStats / numDocs;
        r += lgamma(s) - numTopics * lgamma(s / numTopics);
        return (r);
    }

    public static double gradient(double s, double suffStats, int numDocs,
        int numTopics) {
        double r;
        assert numTopics > 0;
        r = numDocs * (digamma(s) - digamma(s / numTopics)) + suffStats
            / numTopics;
        return (r);
    }

    public static double gradientAscent(double s, double suffStats,
        int numDocs, int numTopics) {
        double f, stepSize = 0.1, oldS, oldF;
        // int i;

        // s = - suff_stats - log(num_topics);
        // s = (num_topics - 1) / s;
        s = 1;
        f = objective(s, suffStats, numDocs, numTopics);
        assert f != Double.NaN;
        oldF = f - 1;
        System.out.println("initial s : " + s + ";     initial f : " + f);
        assert oldF != 0;
        while ((f - oldF) / abs(oldF) > 0.0001) {
            oldS = s;
            oldF = f;
            s = s + stepSize * gradient(s, suffStats, numDocs, numTopics);
            f = objective(s, suffStats, numDocs, numTopics);
            assert f != Double.NaN;
            while ((s < 0) || f < oldF) {
                System.out.println("reducing step size to " + stepSize
                    + " because f=" + f + " or s=" + s);

                s = oldS;
                stepSize = stepSize / 2;
                s = s + stepSize * gradient(s, suffStats, numDocs, numTopics);
                f = objective(s, suffStats, numDocs, numTopics);
                assert f != Double.NaN;
            }
            System.out.println("s : " + s + ";     initial f : " + f);
            assert oldF != 0;
        }
        return s;
    }

    public static void maximizeAlpha(double[][] gamma, SLDArModel model,
        int numDocs) {
        int d, k;
        double suffStats = 0, gamma_sum, s;

        for (d = 0; d < numDocs; d++) {
            gamma_sum = 0;
            for (k = 0; k < model.getNumTopics(); k++) {
                gamma_sum += gamma[d][k];
                suffStats += digamma(gamma[d][k]);
            }
            suffStats -= model.getNumTopics() * digamma(gamma_sum);
        }
        System.out.println("suff stats : " + suffStats);
        s = gradientAscent(model.getAlpha() * model.getNumTopics(), suffStats,
            numDocs, model.getNumTopics());
        model.setAlpha(s / model.getNumTopics());
    }
    public static double[] estimateEta(double[][] covMatrix,double[] ezy, int numTopics){
        // Compute eta values
        
        RealMatrix  rmCovMatrix = MatrixUtils.createRealMatrix(covMatrix);
        System.out.println("covMatrix*************");
        for(int i=0; i<numTopics; i++){
            for(int j=0; j<numTopics; j++){
                System.out.print(covMatrix[i][j]+"  ");
            }
            System.out.println();
        }
        RealMatrix rmCovMatrixInverse = new LUDecomposition(rmCovMatrix).getSolver().getInverse();
        double[][] covMatrixInverse = rmCovMatrixInverse.getData();
        return matrixprod(covMatrixInverse,ezy,numTopics);
    }
    public static double estimateDeltaSq(double ysquared, double[] etaNew, double[] ezy,int numTopics,int numDocs){
        return (ysquared- dotprod(etaNew, ezy,numTopics))/numDocs;
    }
    
    
}
