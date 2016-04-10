package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DevicePackage;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.DeviceFamily;

public class ParseFamilyCSV {
   
   /** The parsed information */
   private DeviceInfo fDeviceInfo;
   
   /** Index of Pin name column in CSV file */
   private int fPinIndex       = 1;
   
   /** List of all indices of package columns in CSV file */
   private ArrayList<PackageColumnInfo> fPackageIndexes = new ArrayList<PackageColumnInfo>();
   
   /** Index of reset function column in CSV file */
   private int fResetIndex     = 3;
   
   /** Index of default function column in CSV file */
   private int fDefaultIndex   = 4;
   
   /** Start index of multiplexor function columns in CSV file */
   private int fAltStartIndex  = 5;
   
   /** Last index of multiplexor function columns in CSV file */
   private int fAltEndIndex    = fAltStartIndex+7;
   
   /**
    * Compares two lines based upon the Port name in line[0]
    */
   private static Comparator<String[]> LineComparitor = new Comparator<String[]>() {
      @Override
      public int compare(String[] arg0, String[] arg1) {
         if (arg0.length < 2) {
            return (arg1.length<2)?0:-1;
         }
         if (arg1.length < 2) {
            return 1;
         }
         return Signal.comparator.compare(arg0[1], arg1[1]);
      }
   };

   /**
    * Convert some common names
    * 
    * @param pinText
    * @return
    */
   private static String convertName(String pinText) {
      pinText = pinText.replaceAll("PTA", "GPIOA_");
      pinText = pinText.replaceAll("PTB", "GPIOB_");
      pinText = pinText.replaceAll("PTC", "GPIOC_");
      pinText = pinText.replaceAll("PTD", "GPIOD_");
      pinText = pinText.replaceAll("PTE", "GPIOE_");
      return pinText;
   }
   
   /**
    * Create a list of peripheral functions described by a string
    * 
    * @param pinText Text of function names e.g. <b><i>PTA4/LLWU_P3</b></i>
    * 
    * @return List of functions created
    * 
    * @throws Exception
    */
   private ArrayList<Signal> createFunctionsFromString(String pinText, Boolean convert) {
      ArrayList<Signal> peripheralFunctionList = new ArrayList<Signal>();
      pinText = pinText.trim();
      if (pinText.isEmpty()) {
         return peripheralFunctionList;
      }
      if (convert) {
         pinText = convertName(pinText);
      }
      String[] functions = pinText.split("\\s*/\\s*");
      for (String function:functions) {
         function = function.trim();
         if (function.isEmpty()) {
            continue;
         }
         Signal peripheralFunction = fDeviceInfo.findOrCreatePeripheralFunction(function);
         if (peripheralFunction != null) {
            peripheralFunctionList.add(peripheralFunction);
         }
      }
      return peripheralFunctionList;
   }
   
   private class PackageColumnInfo {
      final public String  name;
      final public int     index;
      
      public PackageColumnInfo(String name, int index) {
         this.name         = name;
         this.index        = index;
      }

      @Override
      public String toString() {
         return "("+name+", "+index+")";
      }
   }
   /**
    * Parse line containing Key information
    *  
    * @param line
    * 
    * @return true - line is valid
    * 
    * @throws Exception
    */
   private boolean parseKeyLine(String[] line) {
      
      // Set default values for column indices
      fPinIndex       = 1;
      fDefaultIndex   = 4;
      fAltStartIndex  = 5;
      fAltEndIndex    = fAltStartIndex+7;
      fPackageIndexes = new ArrayList<PackageColumnInfo>();

      // Add base device without aliases

      final Pattern packagePattern  = Pattern.compile("[P|p]kg\\s*(.*)\\s*");

      for (int col=0; col<line.length; col++) {
         if (line[col].equalsIgnoreCase("Pin")) {
            fPinIndex = col;
         }
         Matcher packageMatcher = packagePattern.matcher(line[col]);
         if (packageMatcher.matches()) {
            fPackageIndexes.add(new PackageColumnInfo(packageMatcher.group(1), col));
         }
         if (line[col].equalsIgnoreCase("Reset")) {
            fResetIndex = col;
         }
         if (line[col].equalsIgnoreCase("Default")) {
            fDefaultIndex = col;
         }
         if (line[col].equalsIgnoreCase("ALT0")) {
            fAltStartIndex = col;
            fAltEndIndex = col;
         }
         if (line[col].toUpperCase().startsWith("ALT")) {
            if (fAltEndIndex<col) {
               fAltEndIndex = col;
            }
         }
      }
      return true;
   }
   
   /**
    * Parse line containing Pin information
    *  
    * @param line
    * @throws Exception
    */
   private void parsePinLine(String[] line) {

      StringBuffer sb = new StringBuffer();
      
      if (!line[0].equals("Pin")) {
         return;
      }
      String pinName  = line[fPinIndex];
      if ((pinName == null) || (pinName.isEmpty())) {
         throw new RuntimeException("No pin name");
      }
      // Use first name on pin as Pin name e.g. PTC4/LLWU_P8 => PTC4
      Pattern p = Pattern.compile("(.+?)/.*");
      Matcher m = p.matcher(pinName);
      if (m.matches()) {
         pinName = m.group(1);
      }

      final Pin pinInformation = fDeviceInfo.createPin(pinName);
      
      sb.append(String.format("%-10s => ", pinInformation.getName()));
      
      boolean pinIsMapped = false;
      for (int col=fAltStartIndex; col<=fAltEndIndex; col++) {
         if (col>=line.length) {
            break;
         }
         ArrayList<Signal> peripheralFunctions = createFunctionsFromString(line[col], true);
         for (Signal peripheralFunction:peripheralFunctions) {
            sb.append(peripheralFunction.getName()+", ");
            if ((peripheralFunction != null)) {
               MuxSelection functionSelector = MuxSelection.valueOf(col-fAltStartIndex);
               fDeviceInfo.createMapping(peripheralFunction, pinInformation, functionSelector);
               pinIsMapped = true;
            }
         }
      }

      if ((line.length>fResetIndex) && (line[fResetIndex] != null) && (!line[fResetIndex].isEmpty())) {
         String resetName  = line[fResetIndex];
         ArrayList<Signal> resetFunctions = createFunctionsFromString(resetName, true);
         for (Signal peripheralFunction:resetFunctions) {
            sb.append("R:" + peripheralFunction.getName() + ", ");
            // Pin is not mapped to this function in the ALT columns - must be a non-mappable pin
            MappingInfo mapping = fDeviceInfo.createMapping(peripheralFunction, pinInformation, pinIsMapped?MuxSelection.reset:MuxSelection.fixed);
            for (Signal function:mapping.getSignals()) {
               function.setResetPin(mapping);
            }
         }
         pinInformation.setResetSignals(fDeviceInfo, resetName);
      }
      else {
         sb.append("R:" + Signal.DISABLED_SIGNAL.getName() + ", ");
         fDeviceInfo.createMapping(Signal.DISABLED_SIGNAL, pinInformation, MuxSelection.reset);
         pinInformation.setResetSignals(fDeviceInfo, Signal.DISABLED_SIGNAL.getName());
      }
      if (line.length>fDefaultIndex) {
         String defaultName  = convertName(line[fDefaultIndex]);
         if ((defaultName != null) && (!defaultName.isEmpty())) {
            pinInformation.setDefaultSignals(fDeviceInfo, defaultName);
            sb.append("D:" + pinInformation.getDefaultValue());
         }
      }
      if (pinInformation.getDefaultValue() == MuxSelection.unused) {
         // If no default set then set the default to reset value
         pinInformation.setDefaultValue(MuxSelection.reset);
      }
      for (PackageColumnInfo pkgIndex:fPackageIndexes){
         String pinNum = line[pkgIndex.index];
         if (pinNum.equals("*")) {
            continue;
         }
         DevicePackage devicePackage = fDeviceInfo.findDevicePackage(pkgIndex.name);

         if (devicePackage == null) {
            throw new RuntimeException("Failed to find package " + pkgIndex.name + ", for "+pinName);
         }
         devicePackage.addPin(pinInformation, pinNum);
         sb.append("(" + pkgIndex.name + ":" + pinNum + ") ");
      }
   }

   /**
    * Parse line containing Peripheral information
    * 
    * @param line
    */
   private void parsePeripheralInfoLine(String[] line) {
      if (!line[0].equals("Peripheral")) {
         return;
      }
      if (line.length < 3) {
         throw new RuntimeException("Illegal ClockInfo Mapping line");
      }
      String peripheralName       = line[1];
      String peripheralClockReg   = line[2];
      
      String peripheralClockMask = null;
      if (line.length >= 4) {
         peripheralClockMask = line[3];
      }
      if ((peripheralClockMask==null) || (peripheralClockMask.isEmpty())) {
         peripheralClockMask = peripheralClockReg.replace("->", "_")+"_"+peripheralName+"_MASK";
      }
      String[] irqNums = new String[10]; 
      for (int index=0; index<irqNums.length; index++) {
         if (line.length >= index+5) {
            irqNums[index] = line[index+4];
         }
      }
      
      Pattern pattern = Pattern.compile("SIM->(SCGC\\d?)");
      Matcher matcher = pattern.matcher(peripheralClockReg);
      if (!matcher.matches()) {
         throw new RuntimeException("Unexpected Peripheral Clock Register " + peripheralClockReg + " for " + peripheralName);
      }
      peripheralClockReg = matcher.group(1);
      if (!peripheralClockMask.contains(peripheralClockReg)) {
         throw new RuntimeException("Clock Mask "+peripheralClockMask+" doesn't match Clock Register " + peripheralClockReg);
      }
      Peripheral peripheral = fDeviceInfo.findOrCreatePeripheral(peripheralName);
      if (peripheral == null) {
         throw new RuntimeException("Unable to find peripheral "+peripheralName);
      }
      if (peripheral != null) {
         peripheral.setClockInfo(peripheralClockReg, peripheralClockMask);
         for (int index=0; index<irqNums.length; index++) {
            if (irqNums[index] != null) {
               peripheral.addIrqNum(irqNums[index]);
            }
         }
      }
   }

   /**
    * Parse DMA info line
    * 
    * @param line
    */
   private void parseDmaMuxInfoLine(String[] line)  {
      if (!line[0].equals("DmaMux")) {
         return;
      }
      if (line.length < 4) {
         throw new RuntimeException("Illegal DmaMux Mapping line");
      }
      fDeviceInfo.createDmaInfo(line[1], Integer.parseInt(line[2]), line[3]);
   }
 
   /**
    * Parse file
    * 
    * @param reader
    * @throws IOException 
    * 
    * @throws Exception
    */
   private void parseFile(BufferedReader reader) throws IOException {
      
      ArrayList<String[]> grid = new ArrayList<String[]>();
      do {
         String line = reader.readLine();
         if (line == null) {
            break;
         }
         grid.add(line.split(","));
//         System.err.println(line);
      } while (true);
      
      Collections.sort(grid, LineComparitor);
      for(String[] line:grid) {
         for (int index=0; index<line.length; index++) {
            line[index] = line[index].trim();
         }
      }
      for(String[] line:grid) {
         if (line.length < 2) {
            continue;
         }
         try {
            parsePinLine(line);
         } catch (Exception e) {
            System.err.println("Exception @line " + line[1].toString());
            throw new RuntimeException(e);
         }
      }
      for(String[] line:grid) {
         if (line.length < 2) {
            continue;
         }
         parsePeripheralInfoLine(line);
      }
      for(String[] line:grid) {
         if (line.length < 2) {
            continue;
         }
         parseDmaMuxInfoLine(line);
      }
   }

   /**
    * Parse preliminary information from file
    * 
    * @param reader
    * @throws IOException 
    * 
    * @throws Exception
    */
   private void parsePreliminaryInformation(BufferedReader reader) throws IOException {
      
      // Set default values for column indices
      fPinIndex          = 1;
      fResetIndex        = 3;
      fDefaultIndex      = 4;
      fAltStartIndex     = 5;
      fAltEndIndex       = fAltStartIndex+7;
      fPackageIndexes    = new ArrayList<PackageColumnInfo>();
            
      ArrayList<String[]> grid = new ArrayList<String[]>();
      do {
         String line = reader.readLine();
         if (line == null) {
            break;
         }
         grid.add(line.split(","));
//         System.err.println(line);
      } while (true);
      for(String[] line:grid) {
         if (line.length<1) {
            continue;
         }
         for (int index=0; index<line.length; index++) {
            line[index] = line[index].trim();
         }
         if (line[0].equalsIgnoreCase("Key")) {
            // Process title line
            parseKeyLine(line);
         }
         else if (line[0].equalsIgnoreCase("Device")) {
            fDeviceInfo.createDeviceInformation(line[1], line[2], line[3]);
         }
         else if (line[0].equalsIgnoreCase("Family")) {
            fDeviceInfo.setFamilyName(line[1]);
            fDeviceInfo.setFamily(DeviceFamily.valueOf(line[2]));
         }
      }
      if (fPackageIndexes.size() == 0) {
         throw new RuntimeException("No packages provided");
      }
      if (fDeviceInfo.getDeviceVariants().size() == 0) {
         throw new RuntimeException("No Devices found in file");
      }
   }

   /**
    * Process file
    * 
    * @param filePath   File to process
    * 
    * @return Class containing information from file
    * 
    * @throws IOException 
    */
   public DeviceInfo parseFile(Path filePath) throws Exception {

      fDeviceInfo = new DeviceInfo(filePath);
      
      // Open source file
      BufferedReader sourceFile = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
      parsePreliminaryInformation(sourceFile);
      sourceFile.close();

      fDeviceInfo.initialiseTemplates();

      // Re-open source file
      sourceFile = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
      parseFile(sourceFile);
      sourceFile.close();
      
      fDeviceInfo.consistencyCheck();
      
      return fDeviceInfo;
   }

}
