import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * @author Michael Frederick (n00725913)
 */
public class SicAssembler {

    private static final String OUTPUTFILE = "output.txt";
    private HashTable symbols;
    private OPHashTable opcodes;
    private String[] assemblerDirectives = {"BASE", "LTORG", "START", "END"};
    // Build something for literals
    private ArrayList<String> literals = new ArrayList<String>();
    
    /**
     * @param args the command line arguments
     */
    public SicAssembler(String[] args) {
        int lineCount;
        String opCodeList = "SICOPS.txt";
        File file;

        if (args[0] != null || !args[0].isEmpty()) {
            file = new File(args[0]);
            if (!file.isDirectory() && file.exists()) {
                try {
                    lineCount = getLineCount(file);
                    symbols = new HashTable(lineCount);
                    lineCount = getLineCount(new File(opCodeList));
                    opcodes = buildOPTable(lineCount, opCodeList);
                    programAssemble(file, symbols, opcodes);
                    // Throw some blank lines down
                    writeToFile("");
                    writeToFile("");
                    symbols.printTable();
                }
                catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
            else {
                System.out.println("Invalid Filename");
            }
        }
        else {
            System.out.println("No file to Assemble");
        }
    }
        
    public int getLineCount(File file) throws FileNotFoundException {
        int lineCount = 0;
        Scanner fileScanner = new Scanner(file);
            while (fileScanner.hasNext()){
                lineCount++;
                //I'm not inserting the values yet
                fileScanner.nextLine();
            }
        
        return lineCount;
    }
    
    private OPHashTable buildOPTable(int size, String opCodeListFilename) throws FileNotFoundException {
        OPHashTable table = new OPHashTable(size);
        File file = new File(opCodeListFilename);
        Scanner fileScanner = new Scanner(file);

        while (fileScanner.hasNext()) {
            StringTokenizer tokenMaker = new StringTokenizer(fileScanner.nextLine());
            if (tokenMaker.countTokens() == 4) {
                // OPCODE
                table.insertData(new OPCode(tokenMaker.nextToken(),tokenMaker.nextToken(), Integer.parseInt(tokenMaker.nextToken()), tokenMaker.nextToken()));
            }
            else if (tokenMaker.countTokens() == 2) {
                // Register Symbol
                table.insertData(new OPCode(tokenMaker.nextToken(), tokenMaker.nextToken(), -1, null));
            }
        }
        
        return table;
    }   

    private void programAssemble(File file, HashTable symbols, OPHashTable opcodes) throws FileNotFoundException {
        int address;
        String temp;
        DataItem item;
        String programLine;
        Scanner programScanner = new Scanner(file);
        
        // To write to file
        try {
            FileWriter fileWriter = new FileWriter(OUTPUTFILE, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter outputWriter = new PrintWriter(bufferedWriter);

            //Read First line
            programLine = programScanner.nextLine();
            if ("START".equals(programLine.substring(10, 16).trim().toUpperCase())) {
                address = Integer.parseInt(programLine.substring(19, 28).trim(), 16);
                // Because it's the first line, I don't care
                symbols.insertData(buildCommand(programLine, opcodes));
            }
            else {
                address = 0;
            }
            writeToFile(Integer.toHexString(address)+ "\t" + programLine);
            while ((programLine = programScanner.nextLine()) != null) {
                if (isNullOrEmpty(programLine.trim())) {
                    continue;
                }
                else if (programLine.charAt(0) == '.') {
                    // Comment Line
                    writeToFile(programLine);
                }
                else {
                    item = buildCommand(programLine, opcodes);
                    item.setAddress(address);
                    if (!isNullOrEmpty(temp = symbols.insertData(item))) {
                        item.addError(temp);
                    }
                    if ("END".equals(item.getMneumonic())) {
                        break;
                    }
                    if (item.getOperandFlag() == '=') {
                        // It's a operand is a literal
                        literals.add(item.getOperand());
                    }
                    writeToFile(Integer.toHexString(address) + "\t" + programLine);
                    if (!isNullOrEmpty(item.getError())) {
                        writeToFile("----- ERROR:" + item.getError() + "-----");
                    }
                    address += item.getCommandLength();
                    if ("LTORG".equals(item.getMneumonic())) {
                        address = this.printLiterals(address);
                    }
                }
            }
            writeToFile(Integer.toHexString(address) + "\t" + programLine);
        }
        catch (IOException ex) {
            System.out.printf("%s%n", ex.getMessage());
        }
    }
    
    private DataItem buildCommand(String line, OPHashTable opTable) {
        String temp;
        String label = null;
        String mneumonic = null;
        String operand = null;
        String comments = null;
        String error = "";
        String indexEntry = null;
        int index;
        int commandLength;
        char operandFlag;
        boolean extended;
        DataItem item;
        OPCode opc;
        StringTokenizer tokenMaker = new StringTokenizer(line);
        
        while (tokenMaker.hasMoreTokens()) {
            temp = tokenMaker.nextToken();
            index = line.indexOf(temp);
            if (index == 0 && index < 7) {
                label = temp;
            }
            else if (7 <= index && index < 16) {
                mneumonic = temp;
            }
            else if (16 <= index && index < 27) {
                if (temp.charAt(0) == '#' || temp.charAt(0) == '@') {
                    // Operand has a flag
                    temp = temp.substring(1);
                    if (temp.contains(",")) {
                        index = temp.indexOf(",");
                        operand = temp.substring(0, index);
                        indexEntry = temp.substring(index + 1).trim();
                    }
                    else {
                        operand = temp;
                    }
                }
                else if (temp.charAt(0) == '=') {
                    literals.add(temp);
                    operand = temp;
                }
                else {
                    operand = temp;
                }
            }
            else if (27 <= index && index < line.length()) {
                comments = temp;
            }
            else {
                error = temp + " not in extpected index range";
            }
        }
        
        // Validate Strings
        if (isNullOrEmpty(operand)) {
            error += " No Operand ";
        }

        if (line.length() >= 10) {
            extended = (line.charAt(9) == '+');
        }
        else {
            extended = false;
        }
        if (line.length() >= 19) {
            operandFlag = line.charAt(18);
        }
        else {
            operandFlag = ' ';
        }

        item = new DataItem(label, extended, mneumonic, operandFlag, operand, comments);
        
        if (isNullOrEmpty(mneumonic)) {
            error += " Invalid mneomonic ";
            commandLength = 0;
        }
        else {
            index = opTable.searchForData(mneumonic);
            if (index == -1) {
                // It's not an opcode
                // Do stuff for assembler directives and reserved words
                if (searchArray(this.assemblerDirectives, mneumonic)) {
                    // ASSEMBLER DIRECTIVE
                    commandLength = 0;
                }
                else if ("RESW".equalsIgnoreCase(mneumonic)){
                    commandLength = 3 * Integer.parseInt(operand);
                }
                else if ("RESB".equalsIgnoreCase(mneumonic)){
                    commandLength = Integer.parseInt(operand);
                }
                else if ("WORD".equalsIgnoreCase(mneumonic)) {
                    commandLength = 3;
                }
                else if ("BYTE".equalsIgnoreCase(mneumonic)) {
                    commandLength = Integer.toHexString((Integer.parseInt(operand))).length();
                }
                else {
                    error += " Invalid Mneumonic ";
                    commandLength = 0;
                }
            }
            else {
                // Calculate the commandLength
                opc = opTable.getOPCode(index);
                commandLength = opc.getFormat();
            }
        }
        // Set commandLength
        item.setCommandLength(commandLength);
        
        
        if (!isNullOrEmpty(indexEntry)) {
            item.setIndexEntry(indexEntry);
        }

        if (!isNullOrEmpty(error)) {
            item.setError(error);
        }
        return item;
    }
    
    private boolean isNullOrEmpty(String str) {
        boolean a = false;
        if ("".equals(str) || str == null) {
            a = true;
        }
        return a;
    }
    
    private boolean searchArray(String[] array, String str) {
        boolean found = false;
        for(String temp : array) {
            if (temp.equals(str)) {
                found = true;
            }
        }
        
        return found;
    }
    
    private int printLiterals(int currentAddress) {
        int number = 0;
        if (literals.size() > 0) {
            for (String lit : literals) {
                
            }
        }
        return currentAddress;
    }
    
    private int getLengthOfLiteral(String literal) {
        return 0;
    }
    
    private String getValueOfLiteral(String literal) {
        return "";
    }
    
    /**
     * Outputs the message to the Output file
     * @param message 
     */
    public static void writeToFile(String message) {
        try (   
                FileWriter fileWriter = new FileWriter(OUTPUTFILE, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                PrintWriter outputWriter = new PrintWriter(bufferedWriter);
            ) {
            outputWriter.println(message); 
            //System.out.println(message);
        }
        catch (IOException ex) {
            System.out.printf("%s%n", ex.getMessage());
        }
    }
    
}
