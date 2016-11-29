package assembler;

/**
 *
 * @author Michael Frederick (n00725913)
 */
public class Assembler {
    
    private static final String OUTPUTFILE = "output.txt";
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SicAssembler passOne = new SicAssembler(args);
    }
    
}
