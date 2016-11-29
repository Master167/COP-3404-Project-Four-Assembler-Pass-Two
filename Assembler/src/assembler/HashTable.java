package assembler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Builds and contains a DataItem table based on the input file given
 * @author Michael Frederick (n00725913)
 */
public class HashTable { 
    
    private DataItem[] data;
    private int size;
    private DataItem deletedRecord;
    
    /**
     * Generates a HashTable
     * @param size 
     */
    public HashTable(int size) {
        this.size = this.getNextPrime(size);
        this.deletedRecord = new DataItem("deleted");
        this.data = new DataItem[this.size];
    }
    
    /**
     * Inserts the DataItem into the array, if item with the same label does not exist
     * @param item 
     * @return  
     */
    public String insertData(DataItem item) {
        String error = "";
        if (!isNullOrEmpty(item.getLabel())) {
            int insertionIndex = this.hashFunction(item.getLabel(), this.size);
            int collisions = 0;
            boolean searching = true;
            
            while (searching) {
                if (this.data[insertionIndex] == null) {
                    this.data[insertionIndex] = item;
                    searching = false;
                }
                else if (item.equals(this.data[insertionIndex])) {
                    error = "Duplicate Label";
                    //searching = false;
                    collisions++;
                    insertionIndex = this.collisionResolver(insertionIndex, collisions);
                }
                else if (collisions > this.size) {
                    // Switching to Linear Probing
                    insertionIndex = (insertionIndex + 1) % this.size;
                }
                else {
                    // Going to next cell
                    collisions++;
                    insertionIndex = this.collisionResolver(insertionIndex, collisions);
                }
            }
        }
        return error;
    }

    /**
     * Searches the Data array for the DataItem with the same string
     * @param key 
     * @return index
     */
    public int searchForData(String key) {
        int insertionIndex = this.hashFunction(key, this.size);
        int collisions = 0;
        boolean searching = true;
        
        while (searching) {
            if (this.data[insertionIndex] == null || this.data[insertionIndex].equals(deletedRecord)) {
                //System.out.println("ERROR: '" + key + "' not found");
                searching = false;
                insertionIndex = -1;
            }
            else if (this.data[insertionIndex].getLabel().equals(key)) {
                //System.out.println("'" + key + "' with value: " + this.data[insertionIndex].getValue() + " found at index: " + insertionIndex);
                searching = false;
            }
            else if (collisions > this.size) {
                // Switching to Linear Probing
                insertionIndex = (insertionIndex + 1) % this.size;
            }
            else {
                collisions++;
                insertionIndex = this.collisionResolver(insertionIndex, collisions);
            }
        }
        
        return insertionIndex;
    }
    
    public DataItem getDeletedItem() {
        return this.deletedRecord;
    }
    
    public void printTable() {
        ArrayList<DataItem> list = new ArrayList<DataItem>(Arrays.asList(data));
        Iterator iterator = list.iterator();
        DataItem item;
        Object temp;
        int index;
        //System.out.printf("Location\tLabel  \tAddress\t%n");
        SicAssembler.writeToFile(String.format("Location\tLabel  \tAddress\t"));
        while (iterator.hasNext()) {
            temp = iterator.next();
            if ((temp != null ) && (temp instanceof DataItem)) {
                index = list.indexOf(temp);
                item = (DataItem)temp;
                //System.out.printf("%-8d\t%s\t%7s%n", index, item.getLabel(), Integer.toHexString(item.getAddress()));
                SicAssembler.writeToFile(String.format("%-8d\t%s\t%7s", index, item.getLabel(), Integer.toHexString(item.getAddress())));
            }
        }
    }
    
    /**
     * Hashes the string by each character value and Horner's Polynomial
     * @param str
     * @param maxSize
     * @return int Index
     */
    private int hashFunction(String str, int maxSize) {
        int value;
        char[] charArray = str.toCharArray();
        int temp;
        value = charArray[0];
        for (int i = 1; i < charArray.length; i++) {
            temp = charArray[i];
            value = (value * 26 + temp) % maxSize;
        }
        return value;
    }
    
    /**
     * How I resolve a data collision
     * @param index
     * @param collisions
     * @return int Next Index to be checked
     */
    private int collisionResolver(int index, int collisions) {
        return ((index * index) + collisions) % this.size;
    }
    
    
    // Method from pg. 541 of Data Structures and Algorithms in Java by Robert Lafore
    private int getNextPrime(int min){
        for(int i = min + 1; i < Integer.MAX_VALUE - 1; i++){
            if(isPrime(i) && (i / 2) > min){
                return i;
            }
        }
        return -1;
    }
    
    // Method from pg. 541 of Data Structures and Algorithms in Java by Robert Lafore
    private boolean isPrime(int p){
        for(int i = 2; (i * i) <= p; i++){
            if( (p % i) == 0){
                return false;
            }
        }
        return true;
    }
    
    private boolean isNullOrEmpty(String str) {
        boolean a = false;
        if ("".equals(str) || str == null) {
            a = true;
        }
        return a;
    }

}//end Class HashTable