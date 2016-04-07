package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.Pin;

public class WriteFamilyCpp {

   private DeviceInfo fDeviceInfo;

   /** Include directory in C Project */
   private final String INCLUDE_DIRECTORY = "Project_Headers";
   
   /** Source directory ion C project */
   private final String SOURCE_DIRECTORY  = "Sources";
   
   /** Base name for pin mapping file */
   private final static String pinMappingBaseFileName   = "pin_mapping";

   /** Base name for C++ files */
   private final static String gpioBaseFileName         = "gpio";

   /** Fixed GPIO mux function */
   private int      gpioFunctionMuxValue          = 1; 

   /** GPIO mux function varies with port */
   private boolean  gpioFunctionMuxValueChanged   = false;

   /** Fixed ADC mux function - default to mux setting 0*/
   private int      adcFunctionMuxValue           = 0;

   /** GPIO ADC function varies with port */
   private boolean  adcFunctionMuxValueChanged    = false;

   /** Fixed PORT clock enable register */
   private String   portClockRegisterValue        = "SCGC5";

   /** PORT clock enable register varies with port */
   private boolean  portClockRegisterChanged      = false;

   /*
    * Macros =============================================================================================
    */
   HashSet<String> aliases = null;
   
   /**
    * Records aliases used
    * 
    * @param aliasName Alias to record
    * 
    * @return true=> new (acceptable) alias
    */
   private boolean recordAlias(String aliasName) {
      if (aliases == null) {
         aliases = new HashSet<String>();
      }
      if (aliases.contains(aliasName)) {
         return false;
      }
      aliases.add(aliasName);
      return true;
   }

   /**
    * Writes macros describing common pin functions for all pins
    * e.g.<pre>
    * #undef FIXED_ADC_FN
    * #undef FIXED_GPIO_FN
    * #undef FIXED_PORT_CLOCK_REG
    * 
    * #define FIXED_ADC_FN         0                    // Fixed ADC Multiplexing value
    * #define FIXED_GPIO_FN        1                    // Fixed GPIO Multiplexing value
    * #define FIXED_PORT_CLOCK_REG SIM->SCGC5           // Fixed PORT Clock
    * </pre>
    * 
    * @param writer Where to write
    * 
    * @throws IOException 
    * 
    * @throws Exception 
    */
   private void writePinDefines(DocumentUtilities writer) throws IOException {
      writer.writeBanner("Common Mux settings for PCR");
      writer.writeMacroUnDefinition("FIXED_ADC_FN");
      writer.writeMacroUnDefinition("FIXED_GPIO_FN");
      writer.writeMacroUnDefinition("FIXED_PORT_CLOCK_REG");
      if (adcFunctionMuxValueChanged) {
         writer.writeMacroDefinition("ADC_FN_CHANGES", "", " Indicates ADC Multiplexing varies with pin");
      }
      else {
         writer.writeMacroDefinition("FIXED_ADC_FN", Integer.toString(adcFunctionMuxValue), " Fixed ADC Multiplexing value");
      }
      if (gpioFunctionMuxValueChanged) {
         writer.writeMacroDefinition("GPIO_FN_CHANGES", "", " Indicates GPIO Multiplexing varies with pin");
      }
      else {
         writer.writeMacroDefinition("FIXED_GPIO_FN", Integer.toString(gpioFunctionMuxValue), " Fixed GPIO Multiplexing value");
      }
      if (portClockRegisterChanged) {
         writer.writeMacroDefinition("PORT_CLOCK_REG_CHANGES", "", " Indicates PORT Clock varies with pin");
      }
      else {
         writer.writeMacroDefinition("FIXED_PORT_CLOCK_REG", portClockRegisterValue, " Fixed PORT Clock");
      }
      writer.write("\n");
   }

   /**
    * Gets pin name with appended location<br>
    * If no location a null is returned
    * 
    * @param pinInformation
    * @return name with aliases e.g. <b><i>PTE0 (Alias:D14)</b></i>
    */
   @SuppressWarnings("unused")
   private String getPinNameWithLocation(Pin pinInformation) {
      String pinName = pinInformation.getName();

      String location = fDeviceInfo.getDeviceVariant().getPackage().getLocation(pinInformation.getName());
      if (location == null) {
         return null;
      }
      if (!location.equalsIgnoreCase(pinInformation.getName())) {
         location = " (Alias:"+location.replaceAll("/", ", ")+")";
      }
      else {
         location = "";
      }
      return pinName+location;

   }

   /**
    * Writes all clock macros e.g.
    * <pre>
    * #define ADC0_CLOCK_REG       SIM->SCGC6          
    * #define ADC0_CLOCK_MASK      SIM_SCGC6_ADC0_MASK 
    * </pre>
    * 
    * @param writer  Where to write
    * 
    * @throws IOException 
    */
   private void writeClockMacros(DocumentUtilities writer) throws IOException {
      writer.writeBanner("Peripheral clock macros");
      writer.writeMacroDefinition("PORT_CLOCK_REG", portClockRegisterValue);
      writer.write("\n");
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
    * @param writer Where to write
    * 
    * @throws IOException 
    */
   private void writePeripheralInformationClasses(DocumentUtilities writer) throws IOException {
      writer.writeOpenNamespace(DeviceInfo.NAME_SPACE);
      writer.writeBanner("Peripheral Information Classes");

      String groupName = null;
      
      for (String key:fDeviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(key);
         if (!peripheral.getGroupName().equals(groupName)) {
            if (groupName != null) {
               writer.writeCloseGroup();
            }
            writer.writeStartGroup(peripheral);
            groupName = peripheral.getGroupName();
         }
         peripheral.writeInfoClass(writer);
      }
      writer.writeCloseGroup();
      writer.writeCloseNamespace();
      writer.write("\n");
   }

   /**
    * Write templates for simple peripheral functions (e.g. GPIO,ADC,PWM) mapped to locations  e.g.
    * 
    * <pre>
    *    using adc_p53              = const USBDM::Adc1&lt;4&gt;;
    *    using adc_p54              = const USBDM::Adc1&lt;5&gt;;
    * </pre>
    * 
    * @param peripheral       Peripheral information
    * @param mappedFunction   Information about the pin and function being declared
    * @param fnIndex          Index into list of functions mapped to pin
    * @param writer           Where to write
    * 
    * @throws IOException 
    */
   private void writeFunctionCTemplates(Peripheral peripheral, MappingInfo mappedFunction, int fnIndex, DocumentUtilities writer) throws IOException {
      String definition = peripheral.getDefinition(mappedFunction, fnIndex);
      if (definition == null) {
         return;
      }
      String signalName = peripheral.getInstanceName(mappedFunction, fnIndex);
      String locations = fDeviceInfo.getDeviceVariant().getPackage().getLocation(mappedFunction.getPin());
      if ((locations != null) && (!locations.isEmpty())) {
         for (String location:locations.split("/")) {
            if (!location.equalsIgnoreCase(mappedFunction.getPin().getName())) {
               String aliasName = peripheral.getAliasName(signalName, location);
               if (aliasName!= null) {
                  String declaration = peripheral.getAliasDeclaration(aliasName, mappedFunction, fnIndex);
                  if ((declaration != null) && isFunctionMappedToPin(peripheral, mappedFunction)) {
                     if (!recordAlias(aliasName)) {
                        // Comment out repeated aliases
                        writer.write("//");
                     }
                     writer.write(declaration);
                  }
               }
            }
         }
      }
   }

   /**
    * Write C templates for peripherals and peripheral functions
    * 
    * <pre>
    *    /**
    *     * Convenience template class representing an ADC
    *     *
    *     * Example
    *     * @code
    *     *  // Instantiate ADC0 single-ended channel #8
    *     *  const adc0&lt;8&gt; adc0_se8;
    *     *
    *     *  // Initialise ADC
    *     *  adc0_se8.initialiseADC(USBDM::resolution_12bit_se);
    *     *
    *     *  // Set as analogue input
    *     *  adc0_se8.setAnalogueInput();
    *     *
    *     *  // Read input
    *     *  uint16_t value = adc0_se8.readAnalogue();
    *     *  @endcode
    *     *
    *     * @tparam adcChannel    ADC channel
    *     * /
    *    template&lt;uint8_t channel&gt; using Adc1 = Adc_T&lt;Adc1Info, channel&gt;;
    *    
    *    using adc_p53              = const USBDM::Adc1&lt;4&gt;;
    *    using adc_p54              = const USBDM::Adc1&lt;5&gt;;
    * </pre>
    * 
    * @param writer Where to write
    * 
    * @throws Exception 
    */
   private void writePeripheralCTemplates(DocumentUtilities writer) throws IOException {

      writer.write("\n");
      writer.writeOpenNamespace(DeviceInfo.NAME_SPACE);

      String groupName = null;

      for (String key:fDeviceInfo.getPeripherals().keySet()) {

         Peripheral peripheral = fDeviceInfo.getPeripherals().get(key);
         String declaration = peripheral.getCTemplate();
         if (declaration == null) {
            continue;
         }
         if (!peripheral.getGroupName().equals(groupName)) {
            if (groupName != null) {
               // Terminate previous group
               writer.writeCloseGroup();
            }
            groupName = peripheral.getGroupName();
            writer.writeStartGroup(peripheral);
         }

         writer.write(declaration);

         for (String pinName:fDeviceInfo.getPins().keySet()) {

            Pin pin = fDeviceInfo.getPins().get(pinName);
            Map<MuxSelection, MappingInfo> mappedFunctions = pin.getMappedSignals();
            if (mappedFunctions == null) {
               continue;
            }
            for (MuxSelection index:mappedFunctions.keySet()) {
               if (index == MuxSelection.reset) {
                  continue;
               }
               MappingInfo mappedFunction = mappedFunctions.get(index);
               for (int fnIndex=0; fnIndex<mappedFunction.getSignals().size(); fnIndex++) {
                  Signal function = mappedFunction.getSignals().get(fnIndex);
                  if (function.getPeripheral() == peripheral) {
                     writeFunctionCTemplates(peripheral, mappedFunction, fnIndex, writer);
                  }
               }
            }
         }
      }
      if (groupName != null) {
         // Terminate last group
         writer.writeCloseGroup();
      }
      writer.writeDocBanner("Used to configure pin-mapping before 1st use of peripherals");
      writer.write("extern void usbdm_PinMapping();\n");
      writer.writeCloseNamespace();
   }

   /**
    * Write Pin Mapping function to CPP file
    * 
    * @param writer Where to write
    * 
    * @throws IOException
    */
   private void writePinMappingFunction(DocumentUtilities writer) throws IOException {

      writer.write(
            "struct PinInit {\n"+
                  "   uint32_t pcrValue;\n"+
                  "   uint32_t volatile *pcr;\n"+
                  "};\n\n"+
                  "static constexpr PinInit pinInit[] = {\n"
            );

      TreeSet<String> usedPcrs = new TreeSet<String>();
      for (String pinName:fDeviceInfo.getPins().keySet()) {
         if (fDeviceInfo.getDeviceVariant().getPackage().getLocation(pinName) == null) {
            // Discard pin that is not available on this package
            continue;
         }
         Pattern p = Pattern.compile("PT([A-Z]+)([0-9]+)");
         Matcher m = p.matcher(pinName);
         if (m.matches()) {
            Pin pin = fDeviceInfo.getPins().get(pinName);
            MuxSelection mux = pin.getMuxValue();
            if (!mux.isMappedValue()) {
               // Skip unused pin
               continue;
            }
            String instance = m.replaceAll("$1");
            String signal   = m.replaceAll("$2");
            usedPcrs.add(instance);
            writer.write(String.format(
                  "   { PORT_PCR_MUX(%d)|%s::DEFAULT_PCR, &PORT%s->PCR[%s]},\n",
                  mux.value, 
                  DeviceInfo.NAME_SPACE, 
                  instance, 
                  signal));
         }
      }
      writer.write("};\n\n");

      writer.write(
            "/**\n" + 
                  " * Used to configure pin-mapping before 1st use of peripherals\n" + 
                  " */\n" + 
                  "void usbdm_PinMapping() {\n"
            );

      boolean maskWritten = false;
      for (String pcr:usedPcrs) {
         if (!maskWritten) {
            writer.write(String.format("\n   SIM->FIXED_PORT_CLOCK_REG |= PORT%s_CLOCK_MASK", pcr));
         }
         else {
            writer.write(String.format("|PORT%s_CLOCK_MASK", pcr));
         }
         maskWritten = true;
      }
      if (maskWritten) {
         writer.write(String.format(";\n"));
      }
      
      writer.write(
            "\n"+
                  "   for (const PinInit *p=pinInit; p<(pinInit+(sizeof(pinInit)/sizeof(pinInit[0]))); p++) {\n"+   
                  "      *(p->pcr) = p->pcrValue;\n"+ 
                  "   }\n"
            );
      writer.write("}\n");
   }

   /**
    * Checks if a function is mapped to a pin
    * 
    * @param peripheral
    * @param mappedFunction
    * 
    * @return True if mapped.
    */
   private boolean isFunctionMappedToPin(Peripheral peripheral, MappingInfo mappedFunction) {
      if (mappedFunction.getMux() == MuxSelection.fixed) {
         // Fixed mapping are always available
         return true;
      }
      Pin pin = mappedFunction.getPin();
      return (pin.getMuxValue() == mappedFunction.getMux());
   }

   
   private final String DOCUMENTATION_OPEN = 
         "/**\n"+
         " *\n"+
         " * @mainpage Summary\n";
      
   private final String TABLE_OPEN = 
         " *\n"+
         " * @section %s %s\n"+
         " *\n"+
         " *    Pin Name               |   Functions                                 |  Location           |  Description  \n"+
         " *  ------------------------ | --------------------------------------------|---------------------| ------------- \n";
      
   private final String DOCUMENTATION_TEMPLATE = 
      " *  %-20s     | %-40s    | %-15s     | %s       \n";
   
   private final String TABLE_CLOSE = 
         " *\n";
      
   private final String DOCUMENTATION_CLOSE = 
         " */\n";
      
   /**
    * Write pin mapping documentation 
    * 
    * @param writer Where to write
    * 
    * @throws IOException
    */
   private void writeDocumentation(DocumentUtilities writer) throws IOException {

      Map<String, Pin> pinsByLocation = new TreeMap<String, Pin>(Signal.comparator);
      Map<String, Pin> pinsByFunction = new TreeMap<String, Pin>(Signal.comparator);

      writer.write(DOCUMENTATION_OPEN);
      
      writer.write(String.format(TABLE_OPEN, "PinsByPinName", "Pins by Pin Name"));
      for (String pinName:fDeviceInfo.getPins().keySet()) {

         Pin pin = fDeviceInfo.getPins().get(pinName);

         if (!pin.isAvailableInPackage()) {
            // Discard pins without package location
            continue;
         }
         String useDescription = pin.getPinUseDescription();
         if (useDescription.isEmpty()) {
            useDescription = "-";
         }
         String function = pin.getMappedSignals().get(pin.getMuxValue()).getSignalList();
         writer.write(String.format(DOCUMENTATION_TEMPLATE,
               pin.getName(), 
               function,
               pin.getLocation(), 
               useDescription));
         if ((pin.getLocation() != null) && !pin.getLocation().isEmpty()) {
            pinsByLocation.put(pin.getLocation(), pin);
         }
         pinsByFunction.put(function, pin);
      }
      writer.write(TABLE_CLOSE);
      writer.write(String.format(TABLE_OPEN, "PinsByLocation", "Pins by Location"));
      for (String pinName:pinsByLocation.keySet()) {
         
         Pin pin = pinsByLocation.get(pinName);
         
         String useDescription = pin.getPinUseDescription();
         if (useDescription.isEmpty()) {
            useDescription = "-";
         }
         String function = pin.getMappedSignals().get(pin.getMuxValue()).getSignalList();
         writer.write(String.format(DOCUMENTATION_TEMPLATE,
               pin.getName(), 
               function,
               pin.getLocation(), 
               useDescription));
         
      }
      writer.write(TABLE_CLOSE);
      writer.write(String.format(TABLE_OPEN, "PinsByFunction", "Pins by Function"));
      for (String pinName:pinsByFunction.keySet()) {
         
         Pin pin = pinsByFunction.get(pinName);
         
         String useDescription = pin.getPinUseDescription();
         if (useDescription.isEmpty()) {
            useDescription = "-";
         }
         writer.write(String.format(DOCUMENTATION_TEMPLATE,
               pin.getName(), 
               pin.getMappedSignals().get(pin.getMuxValue()).getSignalList(),
               pin.getLocation(), 
               useDescription));
      }
      writer.write(TABLE_CLOSE);
      writer.write(DOCUMENTATION_CLOSE);
   }
   
   /**
    * Writes pin mapping header file
    * 
    * @param filePath Header file to write to
    * 
    * @throws IOException 
    */
   private void writePinMappingHeaderFile(Path filePath) throws IOException {
      
      aliases = null;

      BufferedWriter headerFile = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
      DocumentUtilities writer = new DocumentUtilities(headerFile);
      
      writer.writeHeaderFilePreamble(
            pinMappingBaseFileName+".h", fDeviceInfo.getSourceFilename(),
            DeviceInfo.VERSION, 
            "Pin declarations for "+fDeviceInfo.getDeviceVariantName());

      writer.writeSystemHeaderFileInclude("stddef.h");
      writer.writeHeaderFileInclude("derivative.h");
      headerFile.write("\n");

      writePinDefines(writer);
      writeClockMacros(writer);
      writePeripheralInformationClasses(writer);

      writer.writeHeaderFileInclude("gpio_defs.h");

      writePeripheralCTemplates(writer);

      writeDocumentation(writer);
      
      writer.writeHeaderFilePostamble(pinMappingBaseFileName+".h");

      writer.close();
   }

   /**                    
    * Write CPP file      
    *                     
    * @param filePath      Path to file for writing
    * 
    * @throws IOException 
    */                    
   private void writePinMappingCppFile(Path filePath) throws IOException {
      BufferedWriter cppFile = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
      DocumentUtilities writer = new DocumentUtilities(cppFile);
      
      writer.writeCppFilePreamble(
            gpioBaseFileName+".cpp", fDeviceInfo.getSourceFilename(),
            DeviceInfo.VERSION, 
            "Pin declarations for "+fDeviceInfo.getDeviceVariantName());


      writer.writeHeaderFileInclude("gpio.h");
      writer.write("\n");

      writer.writeOpenNamespace(DeviceInfo.NAME_SPACE);
      writePinMappingFunction(writer);
      writer.writeCppFilePostAmble();
      writer.writeCloseNamespace();
      writer.close();
   }

   /**
    * Generate CPP files (pin_mapping.h, gpio.h)<br>
    * Used for testing
    * 
    * @param  directory    Parent director
    * @param  filename     Filename to use as base of files written
    * @param  deviceInfo   Device information to print to CPP files  
    * 
    * @throws IOException 
    */
   public void writeCppFiles(Path directory, String filename, DeviceInfo deviceInfo) throws IOException {
      if (!filename.isEmpty()) {
         filename = "-"+filename;
      }
      fDeviceInfo = deviceInfo;
      writePinMappingHeaderFile(directory.resolve("Project_Headers").resolve(pinMappingBaseFileName+filename+".h"));
      writePinMappingCppFile(directory.resolve("Sources").resolve(gpioBaseFileName+filename+".cpp"));
   }
   
   /**
    * Generate CPP files (pin_mapping.h, gpio.h) for all variants<br>
    * For testing 
    * 
    * @param  directory    Parent director
    * @param  deviceInfo   Device information to print to CPP files  
    * 
    * @throws IOException 
    */
   public void writeCppFiles(Path directory, DeviceInfo deviceInfo) throws IOException {
      for (String key:deviceInfo.getDeviceVariants().keySet()) {
         deviceInfo.setDeviceVariant(key);
         writeCppFiles(directory, deviceInfo.getDeviceVariantName(), deviceInfo);
      }
   }

   /**
    * Generate CPP files (pin_mapping.h, gpio.h) within an Eclipse C++ project
    * 
    * @param project       Destination project 
    * @param  deviceInfo   Device information to print to CPP files  
    * 
    * @throws IOException
    * @throws CoreException 
    */
   public void writeCppFiles(IProject project, DeviceInfo deviceInfo, IProgressMonitor mon) throws IOException, CoreException {
      
      fDeviceInfo = deviceInfo;

      mon.beginTask("Generating files", IProgressMonitor.UNKNOWN);
      
      Path directory = Paths.get(project.getLocation().toPortableString());
      
      writePinMappingHeaderFile(directory.resolve(INCLUDE_DIRECTORY).resolve(pinMappingBaseFileName+".h"));
      writePinMappingCppFile(directory.resolve(SOURCE_DIRECTORY).resolve(gpioBaseFileName+".cpp"));
      
      IFile file;
      
      file = project.getFile(INCLUDE_DIRECTORY+"/"+pinMappingBaseFileName+".h");
      file.refreshLocal(IResource.DEPTH_ONE, mon);
      file.setDerived(true, mon);
      
      file = project.getFile(SOURCE_DIRECTORY+"/"+gpioBaseFileName+".cpp");
      file.refreshLocal(IResource.DEPTH_ONE, mon);
      file.setDerived(true, mon);
   }
   

}
