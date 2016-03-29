package net.sourceforge.usbdm.deviceEditor.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DevicePackage;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralTemplateInformation;
import net.sourceforge.usbdm.deviceEditor.information.PinInformation;

public class ParseFamilyCSV {
   
   /** The parsed information */
   private DeviceInfo deviceInfo;
   /** Index of Pin name column in CSV file */
   private int pinIndex       = 1;
   /** List of all indices of package columns in CSV file */
   private ArrayList<PackageColumnInfo> packageIndexes = new ArrayList<PackageColumnInfo>();
   /** Index of reset function column in CSV file */
   private int resetIndex     = 3;
   /** Index of default function column in CSV file */
   private int defaultIndex   = 4;
   /** Start index of multiplexor function columns in CSV file */
   private int altStartIndex  = 5;
   /** Last index of multiplexor function columns in CSV file */
   private int altEndIndex    = altStartIndex+7;
   
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
         return PeripheralFunction.comparator.compare(arg0[1], arg1[1]);
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
   private ArrayList<PeripheralFunction> createFunctionsFromString(String pinText, Boolean convert) {
      ArrayList<PeripheralFunction> peripheralFunctionList = new ArrayList<PeripheralFunction>();
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
         PeripheralFunction peripheralFunction = deviceInfo.findOrCreatePeripheralFunction(function);
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
      pinIndex       = 1;
      defaultIndex   = 4;
      altStartIndex  = 5;
      altEndIndex    = altStartIndex+7;
      packageIndexes = new ArrayList<PackageColumnInfo>();

      // Add base device without aliases

      final Pattern packagePattern  = Pattern.compile("[P|p]kg\\s*(.*)\\s*");

      for (int col=0; col<line.length; col++) {
         if (line[col].equalsIgnoreCase("Pin")) {
            pinIndex = col;
         }
         Matcher packageMatcher = packagePattern.matcher(line[col]);
         if (packageMatcher.matches()) {
            packageIndexes.add(new PackageColumnInfo(packageMatcher.group(1), col));
         }
         if (line[col].equalsIgnoreCase("Reset")) {
            resetIndex = col;
         }
         if (line[col].equalsIgnoreCase("Default")) {
            defaultIndex = col;
         }
         if (line[col].equalsIgnoreCase("ALT0")) {
            altStartIndex = col;
            altEndIndex = col;
         }
         if (line[col].toUpperCase().startsWith("ALT")) {
            if (altEndIndex<col) {
               altEndIndex = col;
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
      String pinName  = line[pinIndex];
      if ((pinName == null) || (pinName.isEmpty())) {
         throw new RuntimeException("No pin name");
      }
      // Use first name on pin as Pin name e.g. PTC4/LLWU_P8 => PTC4
      Pattern p = Pattern.compile("(.+?)/.*");
      Matcher m = p.matcher(pinName);
      if (m.matches()) {
         pinName = m.group(1);
      }

      final PinInformation pinInformation = deviceInfo.createPin(pinName);
//      if (pinName.startsWith("PTA18")) {
//         System.err.println("parsePinLine("+pinInformation+")");
//      }
      
      sb.append(String.format("%-10s => ", pinInformation.getName()));
      
      boolean pinIsMapped = false;
      for (int col=altStartIndex; col<=altEndIndex; col++) {
         if (col>=line.length) {
            break;
         }
         ArrayList<PeripheralFunction> peripheralFunctions = createFunctionsFromString(line[col], true);
         for (PeripheralFunction peripheralFunction:peripheralFunctions) {
            sb.append(peripheralFunction.getName()+", ");
            if ((peripheralFunction != null)) {
               MuxSelection functionSelector = MuxSelection.valueOf(col-altStartIndex);
               deviceInfo.createMapping(peripheralFunction, pinInformation, functionSelector);
               pinIsMapped = true;
            }
         }
      }

//      if (pinName.startsWith("PTA18")) {
//         System.err.println("parsePinLine("+pinInformation+")");
//      }
      if ((line.length>resetIndex) && (line[resetIndex] != null) && (!line[resetIndex].isEmpty())) {
         String resetName  = line[resetIndex];
         ArrayList<PeripheralFunction> resetFunctions = createFunctionsFromString(resetName, true);
         for (PeripheralFunction peripheralFunction:resetFunctions) {
            sb.append("R:" + peripheralFunction.getName() + ", ");
            // Pin is not mapped to this function in the ALT columns - must be a non-mappable pin
            MappingInfo mapping = deviceInfo.createMapping(peripheralFunction, pinInformation, pinIsMapped?MuxSelection.reset:MuxSelection.fixed);
            for (PeripheralFunction function:mapping.getFunctions()) {
               function.setResetPin(mapping);
            }
         }
         pinInformation.setResetPeripheralFunctions(deviceInfo, resetName);
      }
      else {
         sb.append("R:" + PeripheralFunction.DISABLED.getName() + ", ");
         deviceInfo.createMapping(PeripheralFunction.DISABLED, pinInformation, MuxSelection.reset);
         pinInformation.setResetPeripheralFunctions(deviceInfo, PeripheralFunction.DISABLED.getName());
      }
      if (line.length>defaultIndex) {
         String defaultName  = convertName(line[defaultIndex]);
         if ((defaultName != null) && (!defaultName.isEmpty())) {
            pinInformation.setDefaultPeripheralFunctions(deviceInfo, defaultName);
            sb.append("D:" + pinInformation.getDefaultValue());
         }
      }
      if (pinInformation.getDefaultValue() == MuxSelection.unused) {
         // If no default set then set the default to reset value
         pinInformation.setDefaultValue(MuxSelection.reset);
      }
      for (PackageColumnInfo pkgIndex:packageIndexes){
         String pinNum = line[pkgIndex.index];
         if ((pinNum == null) || pinNum.isEmpty()) {
            pinNum = pinName;
         }
         if (pinNum.equals("*")) {
            continue;
         }
         DevicePackage devicePackage = deviceInfo.findDevicePackage(pkgIndex.name);

         if (devicePackage == null) {
            throw new RuntimeException("Failed to find package " + pkgIndex.name);
         }
         devicePackage.addPin(pinInformation, pinNum);
         sb.append("(" + pkgIndex.name + ":" + pinNum + ") ");
      }
//      System.err.println(sb.toString());
      if (pinName.startsWith("PTA0")) {
         System.err.println("parsePinLine("+pinInformation+")");
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
      for(PeripheralTemplateInformation template:deviceInfo.getTemplateList()) {
         if (template.getPeripheralName().equalsIgnoreCase(peripheralName)) {
            template.setClockInfo(peripheralClockReg, peripheralClockMask);
            for (int index=0; index<irqNums.length; index++) {
               if (irqNums[index] != null) {
                  template.addIrqNum(irqNums[index]);
               }
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
     deviceInfo.createDmaInfo(Integer.parseInt(line[1]), Integer.parseInt(line[2]), line[3]);
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
      pinIndex          = 1;
      resetIndex        = 3;
      defaultIndex      = 4;
      altStartIndex     = 5;
      altEndIndex       = altStartIndex+7;
      packageIndexes    = new ArrayList<PackageColumnInfo>();
            
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
         if (line[0].equalsIgnoreCase("Device")) {
            deviceInfo.createDeviceInformation(line[1], line[2], line[3]);
         }
      }
      if (packageIndexes.size() == 0) {
         throw new RuntimeException("No packages provided");
      }
      if (deviceInfo.getDevices().size() == 0) {
         throw new RuntimeException("No Devices found in file");
      }
   }

   /**
    * Process pins
    */
   private void processPins() {
      for (PeripheralTemplateInformation pinTemplate:deviceInfo.getTemplateList()) {
         for (String pinName:deviceInfo.getPins().keySet()) {
            PinInformation pinInfo = deviceInfo.findPin(pinName);
            Map<MuxSelection, MappingInfo> mappedFunctions = pinInfo.getMappedFunctions();
            if (mappedFunctions == null) {
               continue;
            }
            for (MuxSelection index:mappedFunctions.keySet()) {
               if (index == MuxSelection.reset) {
                  continue;
               }
               MappingInfo mappedFunction = mappedFunctions.get(index);
               for (PeripheralFunction function:mappedFunction.getFunctions()) {
                  if (pinTemplate.matches(function)) {
                     deviceInfo.addFunctionType(pinTemplate.getPeripheralName(), pinInfo);
                  }
               }
            }
         }
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

      String sourceName = filePath.getFileName().toString();
      String deviceName = sourceName.replaceAll("\\.csv", "");
      
      deviceInfo = new DeviceInfo(sourceName, deviceName);
      
      // Open source file
      BufferedReader sourceFile = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
      parsePreliminaryInformation(sourceFile);
      sourceFile.close();

      deviceInfo.initialiseTemplates();

      // Re-open source file
      sourceFile = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
      parseFile(sourceFile);
      processPins();
      sourceFile.close();
      
      deviceInfo.consistencyCheck();
      
      return deviceInfo;
   }

}
