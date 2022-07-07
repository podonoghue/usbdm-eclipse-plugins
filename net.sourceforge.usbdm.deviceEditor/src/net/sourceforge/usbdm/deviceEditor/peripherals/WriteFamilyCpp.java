package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.PcrInitialiser;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public class WriteFamilyCpp {

   /** Device information */
   private DeviceInfo fDeviceInfo;

   /** Include directory in C Project */
   private final static String INCLUDE_DIRECTORY = "Project_Headers";

   /** Source directory ion C project */
//   private final static String SOURCE_DIRECTORY = "Sources";

   /** Base name for pin mapping file */
   private final static String PIN_MAPPING_BASEFILENAME = "pin_mapping";

   /** Base name for C++ files */
//   private final static String HARDWARE_BASEFILENAME = "hardware";

   /** Key for include files needed in hardware.h **/
   private final static String HARDWARE_FILE_INCLUDES_FILE_KEY = "/HARDWARE_H/IncludeFiles";
   
   /** Key for user object declarations needed in hardware.h **/
   private final static String HARDWARE_FILE_DECLARATIONS_KEY = "/HARDWARE_H/Declarations";
   
   /** Key for user object definitions needed in hardware.cpp **/
   private final static String HARDWARE_FILE_DEFINITIONS_KEY = "/HARDWARE_CPP/Definitions";

   /** Key for user object definitions needed in hardware.cpp **/
   private final static String HARDWARE_FILE_PORT_INIT_KEY = "/HARDWARE_CPP/PortInitialisations";
   
   /** Key for user object definitions needed in hardware.cpp **/
   private final static String HARDWARE_FILE_PORT_INIT_ERRORS_KEY = "/HARDWARE_CPP/PortInitialisationsErrors";
   /*
    * Macros
    * ==========================================================================
    * ===================
    */
//   HashSet<String> aliases = null;

//   /**
//    * Records aliases used
//    * 
//    * @param aliasName
//    *           Alias to record
//    * 
//    * @return true=> new (acceptable) alias
//    */
//   private boolean recordAlias(String aliasName) {
//      if (aliases == null) {
//         aliases = new HashSet<String>();
//      }
//      if (aliases.contains(aliasName)) {
//         return false;
//      }
//      aliases.add(aliasName);
//      return true;
//   }
//
   /**
    * Write Peripheral Information Class<br>
    * 
    * <pre>
    *  class Adc0Info {
    *   ...
    *  };
    *  class Adc1Info {
    *   ...
    *  };
    * </pre>
    * 
    * @param writer
    *           Where to write
    * 
    * @throws IOException
    */
   private void writePeripheralInformationClass(DocumentUtilities writer, DocumentationGroups groups, Peripheral peripheral) throws IOException {
      try {
         groups.openGroup(peripheral);
         peripheral.writeInfoClass(writer);
      } catch (Exception e) {
         System.err.println("Failed to write Info for peripheral " + peripheral);
         e.printStackTrace();
      }
      writer.flush();
   }

   /**
    * Write all Peripheral Information Classes<br>
    * 
    * <pre>
    *  class Adc0Info {
    *   ...
    *  };
    *  class Adc1Info {
    *   ...
    *  };
    * </pre>
    * 
    * @param writer
    *           Where to write
    * 
    * @throws IOException
    */
   private void writePeripheralInformationClasses(DocumentUtilities writer) throws IOException {
      writer.writeOpenUsbdmNamespace();

      writer.openUsbdmDocumentationGroup();

//      writePortInfo(writer);

      fDeviceInfo.writeNamespaceInfo(writer);

      writer.writeBanner("Peripheral Information Classes");

      DocumentationGroups groups = new DocumentationGroups(writer);

      Collection<Peripheral> allperipherals = fDeviceInfo.getPeripherals().values();
      
      Map<String,String> sharedTemplates = new HashMap<String,String>();
      for (Peripheral peripheral : allperipherals) {
         if (peripheral instanceof PeripheralWithState) {
            PeripheralWithState p = (PeripheralWithState) peripheral;
            p.updateSharedVariables(fDeviceInfo.getVariablesSymbolMap(), sharedTemplates);
         }
      }
      sharedTemplates.forEach(new BiConsumer<String, String>() {

         @Override
         public void accept(String key, String value) {
//            System.err.println("Key   = " + key);
//            System.err.println("Value = \n" + value);
            fDeviceInfo.addOrUpdateStringVariable(key, key, value, true);
         }
      });
      
      // Write these classes in order as they declare dependent information etc.
      ArrayList<Pattern> priorityClasses = new ArrayList<Pattern>();
      priorityClasses.add(Pattern.compile("GPIO.*"));
      priorityClasses.add(Pattern.compile("OSC.*"));
      priorityClasses.add(Pattern.compile("SCG"));
      priorityClasses.add(Pattern.compile("PCC"));
      priorityClasses.add(Pattern.compile("RTC"));
      priorityClasses.add(Pattern.compile("MCG.*"));
      priorityClasses.add(Pattern.compile("SIM"));
      priorityClasses.add(Pattern.compile("PMC"));
      
      for (Pattern pattern : priorityClasses) {
         for (Peripheral peripheral : allperipherals) {
            if (pattern.matcher(peripheral.getName()).matches()) {
               writePeripheralInformationClass(writer, groups, peripheral);
            }
         }
      }
      for (Peripheral peripheral : allperipherals) {
         boolean excluded = false;
         for (Pattern pattern : priorityClasses) {
            if (pattern.matcher(peripheral.getName()).matches()) {
               excluded = true;
               break;
            }
         }
         if (!excluded) {
            writePeripheralInformationClass(writer, groups, peripheral);
         }
      }
      groups.closeGroup();
      writer.closeDocumentationGroup();
      writer.writeCloseNamespace();
      writer.write("\n");
      writer.flush();     
   }

   /**
    * Get declarations for simple peripheral signals (e.g. GPIO,ADC,PWM) that
    * are mapped to pins e.g.
    * 
    * <pre>
    *    using adc_p53              = const USBDM::Adc1&lt;4&gt;;
    *    using adc_p54              = const USBDM::Adc1&lt;5&gt;;
    * </pre>
    * 
    * @param peripheral
    *           Peripheral information
    * @param mappedSignal
    *           Information about the mapped signal being declared
    * @param fnIndex
    *           Index into list of multiple functions mapped to pin
    * 
    * @throws IOException
    */
//   private String getSignalDeclaration(Peripheral peripheral, MappingInfo mappedSignal, int fnIndex) throws IOException {
//      StringBuffer sb = null;
//
//      if (!mappedSignal.isSelected()) {// &&
//                                       // (mappedFunction.getMux()!=MuxSelection.mux1))
//                                       // {
//         return null;
//      }
//      String definition = peripheral.getDefinition(mappedSignal, fnIndex);
//      if (definition == null) {
//         return null;
//      }
//      if (mappedSignal.getPin().isAvailableInPackage()) {
//         String aliasName = peripheral.getCodeIdentifier(mappedSignal);
//         if (aliasName != null) {
//            String declaration = peripheral.getAliasDeclaration(aliasName, mappedSignal, fnIndex);
//            if (declaration != null) {
//               if (sb == null) {
//                  sb = new StringBuffer();
//               }
//               if (!recordAlias(aliasName)) {
//                  // Comment out repeated aliases
//                  sb.append("//");
//               }
//               sb.append(declaration);
//            }
//         }
//      }
//      if (sb == null) {
//         return null;
//      }
//      return sb.toString();
//   }

   private class DocumentationGroups {
      DocumentUtilities fWriter;

      public DocumentationGroups(DocumentUtilities writer) {
         fWriter = writer;
      }

      String groupName = null;

      public void openGroup(Peripheral peripheral) throws IOException {
         if (!peripheral.getGroupName().equals(groupName)) {
            if (groupName != null) {
               // Terminate previous group
               fWriter.closeDocumentationGroup();
            }
            groupName = peripheral.getGroupName();
            fWriter.openDocumentationGroup(peripheral);
         }
      }

      public void closeGroup() throws IOException {
         if (groupName != null) {
            // Terminate last group
            fWriter.closeDocumentationGroup();
         }
      }
   }

   /**
    * Writes #includes for classes that have simple signal declarations e.g.
    * ADC, GPIO etc.<br>
    * 
    * Example:
    * 
    * <pre>
    *    #include "gpio.h"
    *    #include "adc.h"
    *    #include "ftm.h"
    * </pre>
    * 
    * @param writer
    * @throws IOException
    */
   private void writeIncludes(DocumentUtilities writer) throws IOException {
      writer.write("// GPIO definitions are needed generally\n");
      writer.writeHeaderFileInclude("gpio.h");
      writer.write("\n");
   }

   /**
    * Create variables for peripheral declarations e.g.
    * <pre>
    * extern const USBDM::Adc<b><i>0</b></i>::Channel&lt;<b><i>3</b></i>&gt;    myAdcChannel; // p9   
    * extern const USBDM::Gpio<b><i>B</b></i>&lt;<b><i>16</b></i>&gt;           myGpio;       // p39   
    * extern const USBDM::Gpio<b><i>D</b></i>Field&lt;<b><i>14</b></i>,<b><i>12</b></i>&gt;   myGpioField;  // p39   
    * extern const USBDM::Ftm<b><i>1</b></i>::Channel&lt;<b><i>3</b></i>&gt    myFtmChannel; // p34
    * extern const 
    * </pre>
    * These are included in the peripheral files.
    *  
    * @param writer
    *           Where to write
    * 
    * @throws Exception
    */
   private void createSignalDeclarationVariables() throws IOException {

      // Prevent repeated use of the same C identifier
      HashSet<String> usedIdentifiers = new HashSet<String>();

      // Contains peripheral include files needed to initialise the user objects in hardware.cpp
      HashSet<String> hardwareIncludeFiles = new HashSet<String>();

      // Contains the actual definitions for any user objects needed by peripherals in hardware.cpp
      StringBuilder hardwareDefinitions = new StringBuilder();
      
      // Contains the actual definitions for any user objects needed by peripherals in hardware.cpp
      StringBuilder hardwareDeclarations = new StringBuilder();
      
      for (String key : fDeviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(key);
         peripheral.createDeclarations(usedIdentifiers, hardwareIncludeFiles, hardwareDeclarations, hardwareDefinitions);
      }

      // Save #include files for any user objects needed by peripherals in hardware.h
      if (hardwareIncludeFiles.isEmpty()) {
         // None - delete variable
         fDeviceInfo.removeVariableIfExists(HARDWARE_FILE_INCLUDES_FILE_KEY);
      }
      else {
         // Append found #includes
         StringBuilder sb = new StringBuilder();
         Iterator<String> i = hardwareIncludeFiles.iterator();
         while(i.hasNext()) {
            sb.append("" + i.next() + "\n");
         }
         // Create or replace variable
         fDeviceInfo.addOrUpdateStringVariable("IncludeFiles", HARDWARE_FILE_INCLUDES_FILE_KEY, sb.toString(), true);
      }         

      // Save declarations for any user objects needed by peripherals in hardware.h
      if (hardwareDeclarations.toString().isBlank()) {
         // None - delete variable
         fDeviceInfo.removeVariableIfExists(HARDWARE_FILE_DECLARATIONS_KEY);
      }
      else {
         // Create or replace variable
         fDeviceInfo.addOrUpdateStringVariable("Definitions", HARDWARE_FILE_DECLARATIONS_KEY, hardwareDeclarations.toString(), true);
      }
      
      // Save actual definitions for any user objects needed by peripherals in hardware.cpp
      if (hardwareDefinitions.toString().isBlank()) {
         // None - delete variable
         fDeviceInfo.removeVariableIfExists(HARDWARE_FILE_DEFINITIONS_KEY);
      }
      else {
         // Create or replace variable
         fDeviceInfo.addOrUpdateStringVariable("Definitions", HARDWARE_FILE_DEFINITIONS_KEY, hardwareDefinitions.toString(), true);
      }
   }

   /**
    * Create central pin mapping statements in variable <b>/HARDWARE/portInit</b><br>
    * 
    * Adds two variables for code substitution:<br>
    *   <li>HARDWARE_FILE_PORT_INIT_KEY          Contains PCR initialisation statements
    *   <li>HARDWARE_FILE_PORT_INIT_ERRORS_KEY   Contains PCR initialisation statement errors or warnings
    *   
    * <pre>
    *    enablePortClocks(PORTA_CLOCK_MASK|...);
    * 
    *    PORTA->GPCHR = 0x0000UL|PORT_GPCHR_GPWE(0x000CUL);
    *    ...
    *    PORTE->GPCHR = 0x0500UL|PORT_GPCHR_GPWE(0x0300UL);
    * </pre>
    * 
    * @return StringVariable containing statements
    */
   private void writePinMappingStatements() {
      
      StringBuilder sb = new StringBuilder();

      PcrInitialiser pcrInitialiser = new PcrInitialiser();

      // Accumulate mappings in pcrInitialiser 
      for (String pinName : fDeviceInfo.getPins().keySet()) {
         Pin pin = fDeviceInfo.getPins().get(pinName);
         pcrInitialiser.addPin(pin); 
      }
      sb.append(pcrInitialiser.getEnablePortClocksStatement("   "));
      sb.append(pcrInitialiser.getGlobalPcrInitStatements("   "));
      sb.append("\n" + 
                "   if constexpr (ForceLockoutUnbondedPins) {\n");
      sb.append(pcrInitialiser.getGlobalPcrLockoutStatements("      "));
      sb.append("   }\n");
      
      // Create or replace variable describing init. sequence
      fDeviceInfo.addOrUpdateStringVariable("Port Initialisation", HARDWARE_FILE_PORT_INIT_KEY, sb.toString(), true);

      // Create or replace error messages as needed
      fDeviceInfo.addOrUpdateStringVariable("Port Initialisation Errors", HARDWARE_FILE_PORT_INIT_ERRORS_KEY, pcrInitialiser.getErrorMessages(), true);
   }

   private final String DOCUMENTATION_OPEN = "///\n" + "/// @page PinSummary Pin Mapping\n";

   private final String TABLE_OPEN = 
         "///\n" + 
         "/// @section %s %s\n" + "///\n" + 
         "///   Pin Name      | C Identifier                  |  Functions                                         |  Location                 |  Description\n" + 
         "///  -------------- | ------------------------------|--------------------------------------------------- | ------------------------- | ----------------------------------------------------\n";

   private final String DOCUMENTATION_TEMPLATE = "///  %-14s | %-30s| %-50s | %-25s | %s\n";

   private final String TABLE_CLOSE = "///\n";

   private final String DOCUMENTATION_CLOSE = "///\n";

   /**
    * Write documentation as previously sorted
    *  
    * @param writer           Where to write
    * @param allMappingInfo   Sorted data
    * 
    * @throws IOException     
    */
   private void writeDocumentation(DocumentUtilities writer, ArrayList<MappingInfo> allMappingInfo) throws IOException {
      
      for (MappingInfo mappingInfo : allMappingInfo) {
         
         String pinName        = mappingInfo.getPin().getName();
         String pinLocation    = mappingInfo.getPin().getLocation();

         // List user variables or types
         boolean userDeclaration = false;
         for (Signal signal:mappingInfo.getSignals()) {
            String cIdentifier = signal.getCodeIdentifier();
            if (cIdentifier == null) {
               cIdentifier = "-";
            }
            String userDescription = signal.getUserDescription();
            if (userDescription.isBlank()) {
               userDescription = "-";
            }
            writer.write(String.format(DOCUMENTATION_TEMPLATE, pinName, cIdentifier, signal.getName(), pinLocation, userDescription));
            userDeclaration = true;
         }
         if (userDeclaration) {
            // Skip documentation if there is already a user declaration
            continue;
         }
         String useDescription = mappingInfo.getMappedSignalsUserDescriptions();
         if (useDescription.isBlank()) {
            // Only document if there is a description
            continue;
         }
         String signalList = mappingInfo.getSignalNames();
         writer.write(String.format(DOCUMENTATION_TEMPLATE, pinName, "-", signalList, pinLocation, useDescription));
      }

   }
   
   /**
    * Write pin mapping documentation
    * 
    * @param writer
    *           Where to write
    * 
    * @throws IOException
    */
   private void writeDocumentation(DocumentUtilities writer) throws IOException {

      ArrayList<MappingInfo> allMappingInfo = new ArrayList<MappingInfo>();

      // Accumulate pin-mapping information to document
      for (String pinName : fDeviceInfo.getPins().keySet()) {

         Pin pin = fDeviceInfo.getPins().get(pinName);
         if (pin.getLocation() == null) {
            // Discard unmapped pins
            continue;
         }
         if (pin.getLocation().isBlank()) {
            // Discard unmapped pins
            continue;
         }
         Map<MuxSelection, MappingInfo> mappableSignals = pin.getMappableSignals();
         
         // Add enabled mappings that have description or C identifier
         for (MuxSelection muxSelection:mappableSignals.keySet()) {
            MappingInfo mappingInfo = mappableSignals.get(muxSelection);
            if (!mappingInfo.isSelected()) {
               continue;
            }
            // Expand pin mappings so only 1 signal->pin
            for (Signal signal:mappingInfo.getSignals()) {
               String cIdentifier     = signal.getCodeIdentifier();
               String userDescription = signal.getUserDescription();
               if (((cIdentifier == null) || cIdentifier.isBlank()) && userDescription.isBlank()) {
                  continue;
               }
               // Create dummy pin mapping information
               MappingInfo mi = new MappingInfo(pin, MuxSelection.unassigned);
               mi.addSignal(signal);
               allMappingInfo.add(mi);
            }
         }
      }
      
      writer.write(DOCUMENTATION_OPEN);

      /*
       *  Write documentation in pin order
       */
      // No need to sort as already in pin name order
      
      // Write documentation
      writer.write(String.format(TABLE_OPEN, "PinsByPinName", "Pins by Pin Name"));
      writeDocumentation(writer, allMappingInfo);
      writer.write(TABLE_CLOSE);

      /*
       * Write documentation in location order (pin number or grid location)
       */
      // Sort by pin number or grid location
      Collections.sort(allMappingInfo, new Comparator<MappingInfo>() {
         final Pattern pattern = Pattern.compile("^(\\D+)(\\d+)(.*)$"); 
         
         @Override
         public int compare(MappingInfo o1, MappingInfo o2) {
            String s1 = o1.getPin().getLocation();
            String s2 = o2.getPin().getLocation();
            Matcher matcher1 = pattern.matcher(s1);
            Matcher matcher2 = pattern.matcher(s2);
            if (!matcher1.matches() || !matcher2.matches()) {
               return s1.compareTo(s2);
            }
            String r1 = matcher1.group(1);
            String r2 = matcher2.group(1);
            int res = r1.compareTo(r2);
            if (res != 0) {
               return res;
            }
            int i1 = Integer.parseInt(matcher1.group(2));
            int i2 = Integer.parseInt(matcher2.group(2));
            if (i1 != i2) {
               return i1-i2;
            }
            return matcher1.group(3).compareTo(matcher2.group(3));
         }
      });
      
      // Write documentation
      writer.write(String.format(TABLE_OPEN, "PinsByLocation", "Pins by Location"));
      writeDocumentation(writer, allMappingInfo);
      writer.write(TABLE_CLOSE);
      
      /*
       * Write documentation ordered by function
       */
      // Sort by function
      Collections.sort(allMappingInfo, new Comparator<MappingInfo>() {
         final Pattern pattern = Pattern.compile("^(\\D+)(\\d+)(.*)$"); 
         
         @Override
         public int compare(MappingInfo o1, MappingInfo o2) {
            String s1 = o1.getSignalNames();
            String s2 = o2.getSignalNames();
            Matcher matcher1 = pattern.matcher(s1);
            Matcher matcher2 = pattern.matcher(s2);
            if (!matcher1.matches() || !matcher2.matches()) {
               return s1.compareTo(s2);
            }
            String r1 = matcher1.group(1);
            String r2 = matcher2.group(1);
            int res = r1.compareTo(r2);
            if (res != 0) {
               return res;
            }
            int i1 = Integer.parseInt(matcher1.group(2));
            int i2 = Integer.parseInt(matcher2.group(2));
            if (i1 != i2) {
               return i1-i2;
            }
            return matcher1.group(3).compareTo(matcher2.group(3));
         }
      });
      
      // Write documentation
      writer.write(String.format(TABLE_OPEN, "PinsByFunction", "Pins by Peripheral"));
      writeDocumentation(writer, allMappingInfo);
      writer.write(TABLE_CLOSE);

      writer.write(DOCUMENTATION_CLOSE);
      writer.flush();
   }

   /**
    * Writes pin mapping header file
    * 
    * @param filePath Header file to write to
    * 
    * @throws IOException
    */
   private void writePinMappingHeaderFile(Path filePath) throws IOException {

      BufferedWriter headerFile = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
      DocumentUtilities writer = new DocumentUtilities(headerFile);

      writer.writeHeaderFilePreamble(
            PIN_MAPPING_BASEFILENAME + ".h", 
            fDeviceInfo.getSourceFilename(), 
            DeviceInfo.VERSION, 
            "Peripheral declarations for " + fDeviceInfo.getVariantName());

      writer.writeSystemHeaderFileInclude("stdint.h");
      writer.writeSystemHeaderFileInclude("stddef.h");
      headerFile.write("\n");
      writer.writeHeaderFileInclude("derivative.h");
      writer.writeHeaderFileInclude("pcr.h");
      writer.writeHeaderFileInclude("error.h");
      headerFile.write("\n");

      createSignalDeclarationVariables();

      writePeripheralInformationClasses(writer);

      writeIncludes(writer);

      writeDocumentation(writer);

      writer.writeHeaderFilePostamble(PIN_MAPPING_BASEFILENAME + ".h");

      writer.close();
   }

   /**
    * Generate CPP files (pin_mapping.h)<br>
    * Used for testing
    * 
    * @param directory         Parent director
    * @param filename          Filename to use as base of files written
    * @param deviceInfo        Device information to print to CPP files
    * 
    * @throws Exception
    */
   public void writeCppFiles(Path directory, String filename, DeviceInfo deviceInfo) throws IOException {
      if (!filename.isEmpty()) {
         filename = "-" + filename;
      }
      fDeviceInfo = deviceInfo;
      writePinMappingHeaderFile(directory.resolve(INCLUDE_DIRECTORY).resolve(PIN_MAPPING_BASEFILENAME + filename + ".h"));
      writePinMappingStatements();
   }

   /**
    * Generate CPP files (pin_mapping.h, gpio.h) for all variants<br>
    * For testing
    * 
    * @param directory
    *           Parent director
    * @param deviceInfo
    *           Device information to print to CPP files
    * @throws Exception
    */
   public void writeCppFiles(Path directory, DeviceInfo deviceInfo) throws Exception {
      for (String key : deviceInfo.getDeviceVariants().keySet()) {
         deviceInfo.setVariantName(key);
         writeCppFiles(directory, deviceInfo.getVariantName(), deviceInfo);
      }
   }

   /**
    * Generate CPP files (pin_mapping.h) within an Eclipse C++ project
    * 
    * @param  project      Destination project 
    * @param  deviceInfo   Device information to print to CPP files  
    * @throws Exception 
    */
   public void writeCppFiles(IProject project, DeviceInfo deviceInfo, IProgressMonitor monitor) throws Exception {
      final String pinMappingFile = INCLUDE_DIRECTORY+"/"+PIN_MAPPING_BASEFILENAME+".h";
      
      SubMonitor subMonitor = SubMonitor.convert(monitor, 100); 
      subMonitor.subTask("Generating device header file");

      fDeviceInfo = deviceInfo;
      
      Path directory = Paths.get(project.getLocation().toPortableString());
      
      writePinMappingHeaderFile(directory.resolve(pinMappingFile));
      subMonitor.worked(40);

      writePinMappingStatements();
      subMonitor.worked(40);
      
      try {
         IFile file = project.getFile(pinMappingFile);
         file.refreshLocal(0, subMonitor.newChild(10));
         file.setDerived(true, subMonitor.newChild(10));
      } catch (Exception e) {
         // Ignore
         System.err.println("WARNING: Failed to set Derived on '" + pinMappingFile + "'");
      }
   }

}
