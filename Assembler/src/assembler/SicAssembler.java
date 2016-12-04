package assembler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author Michael Frederick (n00725913)
 */
public class SicAssembler {

    private String listFile;
    private String objectFile;
    private String intermediateFile;
    private HashTable symbols;
    private OPHashTable opcodes;
    private String[] assemblerDirectives = {"BASE", "LTORG", "START", "END"};
    // Build something for literals
    private ArrayList<String> literals = new ArrayList<String>();
    
    /**
     * @param args the command line arguments
     */
    public SicAssembler(String[] args) {
        int temp;
        int lineCount;
        String opCodeList = "SICOPS.txt";
        File file;

        if (args[0] != null || !args[0].isEmpty()) {
            file = new File(args[0]);
            if (!file.isDirectory() && file.exists()) {
                temp = file.getName().lastIndexOf(".");
                this.listFile = file.getName().substring(0, temp) + ".lst";
                this.objectFile = file.getName().substring(0, temp) + ".obj";
                this.intermediateFile = file.getName().substring(0, temp) + ".imd";
                try {
                    lineCount = getLineCount(file);
                    symbols = new HashTable(lineCount);
                    lineCount = getLineCount(new File(opCodeList));
                    opcodes = buildOPTable(lineCount, opCodeList);
                    temp = passOneAssemble(file, symbols, opcodes);
                    passTwoAssemble(temp, symbols, opcodes);
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

    private int passOneAssemble(File file, HashTable symbols, OPHashTable opcodes) throws FileNotFoundException {
        int initialAddress;
        int address;
        String temp;
        DataItem item;
        String programLine;
        Scanner programScanner = new Scanner(file);
        
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
        initialAddress = address;
        writeToFile(String.format("%6s %s", Integer.toHexString(address).toUpperCase(), programLine), this.intermediateFile);
        while ((programLine = programScanner.nextLine()) != null) {
            if (isNullOrEmpty(programLine)) {
                        continue;
            }
            else if (programLine.charAt(0) == '.') {
                // Comment Line
                writeToFile(programLine, this.intermediateFile);
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
                writeToFile(String.format("%6s %s", Integer.toHexString(address).toUpperCase(), programLine), this.intermediateFile);
                if (!isNullOrEmpty(item.getError())) {
                    writeToFile(".----- ERROR:" + item.getError() + "-----", this.intermediateFile);
                }
                address += item.getCommandLength();
                if ("LTORG".equals(item.getMneumonic())) {
                    address = this.printLiterals(address);
                }
            }
        }
        writeToFile(String.format("%6s %s", Integer.toHexString(address).toUpperCase(), programLine), this.intermediateFile);
        
        return address - initialAddress;
    }// end passOneAssemble()
    
    private void passTwoAssemble(int programLength, HashTable symbols, OPHashTable opcodes) throws FileNotFoundException, Exception {
        int index;
        int address;
        int baseAddress;
        int displacement;
        boolean isPC = true;
        String textRecord;
        String programLine;
        String temp;
        DataItem dataItem;
        OPCode opcode;
        File file;
        Scanner fileScanner;
        StringTokenizer tokenMaker;
        
        try {
            file = new File(this.intermediateFile);
            fileScanner = new Scanner(file);
            //Pull first line
            temp = fileScanner.nextLine();
            tokenMaker = new StringTokenizer(temp);
            
            //Get address
            address = Integer.parseInt(tokenMaker.nextToken().trim());
            //Create Header record
            writeToFile(temp, this.listFile);
            // Check if program has label
            temp = tokenMaker.nextToken();
            index = symbols.searchForData(temp);
            if (index >= 0) {
                dataItem = symbols.getData(index);
                textRecord = String.format("H %6s %08d %08d", dataItem.getLabel(), address, programLength);
            }
            else {
                textRecord = String.format("H %6s %08d %08d", "", address, programLength);
            }
            writeToFile(textRecord, this.objectFile);
            //Start loop
            while ((programLine = fileScanner.nextLine()) != null) {
                isPC = true;
                displacement = 0;
                textRecord = "";
                tokenMaker = new StringTokenizer(programLine);
                temp = tokenMaker.nextToken();
                if (temp.charAt(0) == '.') {
                    writeToFile(programLine, this.listFile);
                    continue;
                }
                else {
                    dataItem = buildItem(programLine, opcodes);
                }
                //  Get OPcode
                index = opcodes.searchForData(dataItem.getMneumonic());
                
                if (index >= 0) {
                    opcode = opcodes.getOPCode(index);

                    //  Get operand and Calculate displacement
                    index = symbols.searchForData(dataItem.getOperand());
                    if (index >= 0) {
                        displacement = symbols.getData(index).getAddress() - (dataItem.getAddress() + dataItem.getCommandLength());
                        // Check for PC relative
                        if (displacement < -2048 || displacement > 2047) {
                            // outside pc range
                            isPC = false;
                            throw new Exception("Figure out what to do with base relative addressing");
                        }
                    }
                    else {
                        // Does the operand have an immediate addressing flag (is it a number?)
                        throw new Exception("Does is this operand a number? " + dataItem.getOperand());
                    }

                    
                    if (opcode.getFormat() == 2) {
                        textRecord = opcode.getOpcode();
                        index = programLine.indexOf(',');
                        temp = Character.toString(programLine.charAt(index - 1));
                        index = opcodes.searchForData(temp);
                        if (index >= 0) {
                            textRecord += opcodes.getOPCode(index).getOpcode();
                            index = programLine.indexOf(',');
                            temp = Character.toString(programLine.charAt(index + 1));
                            index = opcodes.searchForData(temp);
                            if (index > 0) {
                                textRecord += opcodes.getOPCode(index).getOpcode();
                            }
                            else {
                                writeToFile("---ERROR: Unknown Register ---", this.listFile);
                            }
                        }
                        else {
                            writeToFile("---ERROR: Unknown Register ---", this.listFile);
                        }
                    }
                    else if (opcode.getFormat() == 3) {
                        // Standard instruction
                        if (opcode.getLabel().charAt(0) == '*') {
                            textRecord = opcode.getOpcode();
                        }
                        else if (dataItem.getOperandFlag() == '#') {
                            textRecord = Integer.toHexString(Integer.parseInt(opcode.getOpcode(), 16) + 1);
                        }
                        else if (dataItem.getOperandFlag() == '@') {
                            textRecord = Integer.toHexString(Integer.parseInt(opcode.getOpcode(), 16) + 2);
                        }
                        else {
                            textRecord = Integer.toHexString(Integer.parseInt(opcode.getOpcode(), 16) + 3);
                        }
                        
                        if (isPC) {
                            if (isNullOrEmpty(dataItem.getIndexEntry())) {
                                textRecord += Integer.toHexString(2);
                            }
                            else {
                                textRecord += Integer.toHexString(10);
                            }
                        }
                        else {
                            if (isNullOrEmpty(dataItem.getIndexEntry())) {
                                textRecord += Integer.toHexString(4);
                            }
                            else {
                                textRecord += Integer.toHexString(12);
                            }
                        }
                        
                        temp = Integer.toHexString(displacement);
                        if (temp.length() < 3) {
                            while (temp.length() < 3) {
                                temp = "0" + temp;
                            }
                        }
                        textRecord += temp;
                    }
                    else {
                        if (opcode.getLabel().charAt(0) == '*') {
                            textRecord = opcode.getOpcode();
                        }
                        else if (dataItem.getOperandFlag() == '#') {
                            textRecord = Integer.toHexString(Integer.parseInt(opcode.getOpcode(), 16) + 1);
                        }
                        else if (dataItem.getOperandFlag() == '@') {
                            textRecord = Integer.toHexString(Integer.parseInt(opcode.getOpcode(), 16) + 2);
                        }
                        else {
                            textRecord = Integer.toHexString(Integer.parseInt(opcode.getOpcode(), 16) + 3);
                        }
                        
                        textRecord += Integer.toHexString(1);
                        temp = Integer.toHexString(dataItem.getAddress());
                        if (temp.length() < 5) {
                            while (temp.length() < 5) {
                                temp = "0" + temp;
                            }
                        }
                        textRecord += temp;
                    }// end if/else for opcode format
                    
                    // WRITE THE RECORD
                    writeToFile(programLine + " " + textRecord, this.listFile);
                    index = (textRecord.length() / 2);
                    if (index == 0) {
                        index = 1;
                    }
                    writeToFile(String.format("T %06d %01d %s", dataItem.getAddress(), index, textRecord), this.objectFile);
                }
                else {
                    // Do I have an assembler directive?
                    if (searchArray(this.assemblerDirectives, dataItem.getMneumonic())) {
                        // check if base
                        if (dataItem.getMneumonic().equalsIgnoreCase("BASE")) {
                            index = symbols.searchForData(dataItem.getOperand());
                            if (index >= 0) {
                                baseAddress = symbols.getData(index).getAddress();
                            }
                            else {
                                writeToFile("---Error: Unknown Symbol used for BASE ---", this.listFile);
                            }
                        }
                        writeToFile(programLine, this.listFile);
                        continue;
                    }
                    else if ("WORD".equalsIgnoreCase(dataItem.getMneumonic())) {
                        if (dataItem.getOperand().contains("C")) {
                            temp = dataItem.getOperand().substring(dataItem.getMneumonic().indexOf("\'") + 1, dataItem.getMneumonic().length() - 1);
                            char[] array = temp.toCharArray();
                            for(char a: array) {
                                textRecord = Integer.toHexString((int) a);
                            }
                        }
                        else {
                            index = dataItem.getOperand().indexOf("\'");
                            if (index >= 0) {
                                temp = dataItem.getOperand().substring(index, dataItem.getOperand().length() - 1);
                                textRecord = Integer.toHexString(Integer.parseInt(temp, 16));
                            }
                            else {
                                textRecord = Integer.toHexString(Integer.parseInt(dataItem.getOperand(), 16));
                            }
                        }
                    }
                    else if ("BYTE".equalsIgnoreCase(dataItem.getMneumonic())) {
                        textRecord = Integer.toHexString(Integer.parseInt(dataItem.getOperand(), 16));
                    }
                    writeToFile(programLine + " " + textRecord, this.listFile);
                    index = (textRecord.length() / 2);
                    if (index == 0) {
                        index = 1;
                    }
                    writeToFile(String.format("T %06d %01d %s", dataItem.getAddress(), index, textRecord), this.objectFile);
                }
                
            }

            //Write last text record
        }
        catch (FileNotFoundException ex) {
            System.out.println("I don't know man");
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
        if (isNullOrEmpty(label)) {
            error = " No Label Found ";
        }
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
    
    private DataItem buildItem(String line, OPHashTable opTable) {
        String temp;
        String label = null;
        String mneumonic = null;
        String operand = null;
        String comments = null;
        String error = "";
        String indexEntry = null;
        int index;
        int commandLength;
        int address = 0;
        char operandFlag;
        boolean extended;
        boolean addressFound = false;
        DataItem item;
        OPCode opc;
        StringTokenizer tokenMaker = new StringTokenizer(line);
        
        while (tokenMaker.hasMoreTokens()) {
            
            temp = tokenMaker.nextToken();
            if (addressFound) {
                index = line.indexOf(temp, 7);
            }
            else {
                index = line.indexOf(temp);
            }
            if (index >= 0 && index < 7) {
                address = Integer.parseInt(temp, 16);
                addressFound = true;
            }
            else if (index >= 7 && index < 14) {
                label = temp;
            }
            else if (14 <= index && index < 23) {
                mneumonic = temp;
            }
            else if (23 <= index && index < 34) {
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
            else if (34 <= index && index < line.length()) {
                comments = temp;
            }
            else {
                error = temp + " not in extpected index range";
            }
        }
        
        // Validate Strings
        if (isNullOrEmpty(label)) {
            error = " No Label Found ";
        }
        if (isNullOrEmpty(operand)) {
            error += " No Operand ";
        }

        if (line.length() >= 17) {
            extended = (line.charAt(16) == '+');
        }
        else {
            extended = false;
        }
        if (line.length() >= 26) {
            operandFlag = line.charAt(25);
        }
        else {
            operandFlag = ' ';
        }

        item = new DataItem(label, extended, mneumonic, operandFlag, operand, comments);
        item.setAddress(address);
        
        
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
     * @param filename 
     */
    public void writeToFile(String message, String filename) {
        try (   
                FileWriter fileWriter = new FileWriter(filename, true);
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
