/**
 * An Individual DataItem
 * @author Michael Frederick (n00725913)
 */
public class DataItem {
    private String label;
    private boolean extended;
    private String mneumonic;
    private char operandFlag;    
    private String operand;
    private String comments;
    private String error;
    private int address;
    private int commandLength;
    private String indexEntry;    

    /**
     * For the Deleted Item
     * @param label 
     */
    public DataItem(String label) {
        this.label = label;
    }

    /**
     * For Real DataItems
     * @param label
     * @param extended
     * @param mneumonic
     * @param operandFlag
     * @param operand
     * @param comments 
     */
    public DataItem(String label, boolean extended, String mneumonic, char operandFlag, String operand, String comments) {
        this.label = label;
        this.extended = extended;
        this.mneumonic = mneumonic;
        this.operandFlag = operandFlag;
        this.operand = operand;
        this.comments = comments;
    } 
    
    // For Literals
    public DataItem(String label, String mneumonic, int commandLength) {
        this.label = label;
        this.mneumonic = mneumonic;
        this.commandLength = commandLength;
    }
    
    public String getIndexEntry() {
        return indexEntry;
    }

    public void setIndexEntry(String indexEntry) {
        this.indexEntry = indexEntry;
    }
    
    /**
     * Sets the value of Address
     * @param address
     */
    public void setAddress(int address) {
        this.address = address;
    }
    
    public int getAddress() {
        return this.address;
    }
    
    /**
     * Sets the value of commandLength
     * @param length
     */
    public void setCommandLength(int length) {
        this.commandLength = length;
    }
    
    /**
     * Gets the value of commandLength
     * @return the value of commandLength
     */
    public int getCommandLength() {
        return this.commandLength;
    }

    /**
     * Get the value of label
     * @return the value of label
     */
    public String getLabel() {
        return this.label;
    }
    
    /**
     * Get the value of extended
     * @return the value of extended
     */
    public boolean isExtended() {
        return extended;
    }
    
    /**
     * Get the value of mneumonic
     * @return the value of mneumonic
     */
    public String getMneumonic() {
        return mneumonic;
    }
    
    /**
     * Get the value of operandFlag
     * @return the value of operandFlag
     */
    public char getOperandFlag() {
        return operandFlag;
    }
    
    /**
     * Get the value of operand
     * @return the value of operand
     */
    public String getOperand() {
        return operand;
    }
    
    /**
     * Get the value of comments
     * @return the value of comments
     */
    public String getComments() {
        return comments;
    }
    /**
     * Get the value of error
     * @return the value of error
     */
    public String getError() {
        return error;
    }

    /**
     * Set the value of error
     * @param error new value of error
     */
    public void setError(String error) {
        this.error = error;
    }
    
    public void addError(String er) {
        if (this.error == null || "".equals(this.error)) {
            this.error = " " + er;
        }
        else {
            this.error += " " + er;
        }
    }

    /**
     * Returns True if o is a DataItem with the same label
     * @param o Object
     * @return boolean
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof DataItem) {
            DataItem x = (DataItem) o;
            if (x.getLabel() == null || x.getLabel().isEmpty()) {
                return false;
            }
            else {
                return x.getLabel().equals(this.label);
            }
        }
        else {
            return false;
        }
    }
}// end Class DataItem