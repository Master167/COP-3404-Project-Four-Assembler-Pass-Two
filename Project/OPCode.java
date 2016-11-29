/**
 * @author Michael Frederick (n00725913)
 */
public class OPCode {
    
    private String label;
    private String opcode;
    private int format;
    private String unknown;

    public OPCode(String label, String opcode, int format, String unknown) {
        this.label = label;
        this.opcode = opcode;
        this.format = format;
        this.unknown = unknown;
    }

    /**
     * Get the value of unknown
     * @return the value of unknown
     */
    public String getUnknown() {
        return unknown;
    }    

    /**
     * Get the value of format
     * @return the value of format
     */
    public int getFormat() {
        return format;
    }

    /**
     * Get the value of opcode
     * @return the value of opcode
     */
    public String getOpcode() {
        return opcode;
    }

    /**
     * Get the value of label
     * @return the value of label
     */
    public String getLabel() {
        return label;
    }
    
    public boolean equals(Object o) {
        if (o instanceof OPCode) {
            OPCode x = (OPCode) o;
            return x.getLabel().equals(this.getLabel());
        }
        return false;
    }

}
