 
/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package slda;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

/**
 * Represents a corpus of documents.
 * <p>
 * slda-c reference: struct corpus in lda.h and function in lda-data.c.
 * 
 * @author nagesh bhattu
 */
public class SLDArCorpus {

    private SLDArDocument[] docs;

    private int numTerms;

    private int numDocs;

    public SLDArCorpus(String dataFilename) {
        read(dataFilename);
    }
    public SLDArCorpus(){
        
    }
    
    static int OFFSET = 0; // offset for reading data

    /**
     * read a file in "pseudo-SVMlight" format. TODO: make robust against
     * irregular whitespace (duplicate spaces)
     * 
     * @param dataFilename
     */
    public void read(String dataFilename) {
        int length, count, word, n, nd, nw;

        System.out.println("reading data from " + dataFilename);

        try {
            Vector cdocs = new Vector<SLDArDocument>();
            BufferedReader br = new BufferedReader(new FileReader(dataFilename));
            nd = 0;
            nw = 0;
            String line = "";
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(" ");
                double rating =  0.0;
                if (fields[0].equals("") || fields[0].equals(""))
                    continue;
                SLDArDocument d = new SLDArDocument();
                cdocs.add(d);
                rating = Double.parseDouble(fields[0]);
                d.setRating(rating);
                length = Integer.parseInt(fields[1]);
                d.setLength(length);
                d.setTotal(0);
                d.setWords(new int[length]);
                d.setCounts(new int[length]);

                for (n = 0; n < length; n++) {
                    // fscanf(fileptr, "%10d:%10d", &word, &count);
                    String[] numbers = fields[n + 2].split(":");
                    if (numbers[0].equals("") || numbers[0].equals(""))
                        continue;
                    word = Integer.parseInt(numbers[0]);
                    count = (int) Float.parseFloat(numbers[1]);
                    word = word - OFFSET;
                    d.setWord(n, word);
                    d.setCount(n, count);
                    d.setTotal(d.getTotal() + count);
                    if (word >= nw) {
                        nw = word + 1;
                    }
                }

                nd++;
            }
            numDocs = nd;
            numTerms = nw;
            docs = (SLDArDocument[]) cdocs.toArray(new SLDArDocument[] {});
            System.out.println("number of docs    : " + nd);
            System.out.println("number of terms   : " + nw);
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @return
     */
    public SLDArDocument[] getDocs() {
        return docs;
    }

    /**
     * @param index
     * @return
     */
    public SLDArDocument getDoc(int index) {
        return docs[index];
    }

    /**
     * @param index
     * @param doc
     */
    public void setDoc(int index, SLDArDocument doc) {
        docs[index] = doc;
    }

    /**
     * @return
     */
    public int getNumDocs() {
        return numDocs;
    }

    /**
     * @return
     */
    public int getNumTerms() {
        return numTerms;
    }

    /**
     * @param documents
     */
    public void setDocs(SLDArDocument[] documents) {
        docs = documents;
    }

    /**
     * @param i
     */
    public void setNumDocs(int i) {
        numDocs = i;
    }

    /**
     * @param i
     */
    public void setNumTerms(int i) {
        numTerms = i;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("Corpus {numDocs=" + numDocs + " numTerms=" + numTerms + "}");
        return b.toString();
    }
}
