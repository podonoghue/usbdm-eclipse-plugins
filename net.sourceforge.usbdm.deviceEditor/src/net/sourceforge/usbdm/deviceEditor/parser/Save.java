package net.sourceforge.usbdm.deviceEditor.parser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.configEditor.information.DeviceInfo;
import net.sourceforge.usbdm.configEditor.information.DeviceInfo.DeviceFamily;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;
import net.sourceforge.usbdm.configEditor.information.DeviceInformation;
import net.sourceforge.usbdm.configEditor.information.DevicePackage;
import net.sourceforge.usbdm.configEditor.information.MappingInfo;
import net.sourceforge.usbdm.configEditor.information.MuxSelection;
import net.sourceforge.usbdm.configEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.configEditor.information.PeripheralTemplateInformation;
import net.sourceforge.usbdm.configEditor.information.PinInformation;

public class Save {
   
   String     fXmlFilename;
   DeviceInfo fDeviceInfo;
   
   /** Base name for pin mapping file */
   private final static String pinMappingBaseFileName   = "pin_mapping";
   
   /** Name of the source file e.g. MKL25Z4.csv */
   private String sourceName;
   
   /** Name of pin-mapping-XX.h header file */
   private String xmlFileName;

   /** Path to 'XML' output directory */
   private Path xmlDirectory;
   
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

   /** Name for namespace to use */
   public static final String NAME_SPACE = "USBDM";

//   private HashSet<String> macroAliases;
   private static ArrayList<DmaInfo> dmaInfoList;
   
   class DmaInfo {
      public final int    dmaInstance;
      public final int    dmaChannelNumber;
      public final String dmaSource;
      public DmaInfo(int dmaInstance, int dmaChannelNumber, String dmaSource) {
         this.dmaInstance      = dmaInstance;
         this.dmaChannelNumber = dmaChannelNumber;
         this.dmaSource        = dmaSource;
      }
   };
   
   public static class NameAttribute implements WizardAttribute {
      private String fName;
      
      NameAttribute(String name) {
         fName = name;
      }
      
      @Override
      public String getAttributeString() {
         return "<name=" + fName + ">";
      }
   }

   public static class ValidatorAttribute implements WizardAttribute {
      private String fValidatorId;
      
      ValidatorAttribute(String validatorId) {
         fValidatorId = validatorId;
      }
      
      @Override
      public String getAttributeString() {
         return "<validate=" + fValidatorId + ">";
      }
   }

   public static class SelectionAttribute implements WizardAttribute {
      private String fName;
      private String fSelection;
      
      SelectionAttribute(String name, String selection) {
         fName  = name;
         fSelection = selection;
      }
      
      @Override
      public String getAttributeString() {
         return "<selection=" + fName + "," + fSelection + ">";
      }
   }

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
         return portNameComparator.compare(arg0[1], arg1[1]);
      }
   };

   /**
    * Convert some common names
    * 
    * @param pinText
    * @return
    */
   String convertName(String pinText) {
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
   ArrayList<PeripheralFunction> createFunctionsFromString(String pinText, Boolean convert) throws Exception {
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
         PeripheralFunction peripheralFunction = fDeviceInfo.findOrCreatePeripheralFunction(function);
         if (peripheralFunction != null) {
            peripheralFunctionList.add(peripheralFunction);
         }
      }
      return peripheralFunctionList;
   }


   /**
    * Parse DMA info line
    * 
    * @param line
    * @throws Exception
    */
   private void parseDmaMuxInfoLine(String[] line) throws Exception {
      if (!line[0].equals("DmaMux")) {
         return;
      }
      if (line.length < 4) {
         throw new Exception("Illegal DmaMux Mapping line");
      }
      DmaInfo dmaInfo = new DmaInfo(Integer.parseInt(line[1]), Integer.parseInt(line[2]), line[3]);
      dmaInfoList.add(dmaInfo);
   }

//   /**
//    * Writes enumeration describing DMA slot use
//    * 
//    * e.g.<pre>
//    * 
//    * </pre>
//    * @param writer
//    * @throws IOException
//    */
//   private void writeDmaMuxInfo(BufferedWriter writer) throws IOException {
//      if (dmaInfoList.size() == 0) {
//         return;
//      }
//      writer.write("\n");
//      writeOpenNamespace(writer, NAME_SPACE);
//      writeStartGroup(writer, "DMA_Group", "Direct Memory Access (DMA)", "Support for DMA operations");
//      for (int instance=0; instance<4; instance++) {
//         boolean noneWritten = true;
//         for (DmaInfo item:dmaInfoList) {
//            if (item.dmaInstance == instance) {
//               if (noneWritten) {
//                  writer.write("enum {\n");
//                  noneWritten = false;
//               }
//               writer.write(String.format("   %-35s  = %d,\n", "DMA"+item.dmaInstance+"_SLOT_"+item.dmaSource, item.dmaChannelNumber));
//            }
//         }
//         if (!noneWritten) {
//            writer.write("};\n");
//         }
//      }
//      writeCloseGroup(writer);
//      writeCloseNamespace(writer, NAME_SPACE);
//   }
//   
//   /**
//    * Writes macros describing common pin functions for all pins
//    * e.g.<pre>
//    * #undef FIXED_ADC_FN
//    * #undef FIXED_GPIO_FN
//    * #undef FIXED_PORT_CLOCK_REG
//    * 
//    * #define FIXED_ADC_FN         0                    // Fixed ADC Multiplexing value
//    * #define FIXED_GPIO_FN        1                    // Fixed GPIO Multiplexing value
//    * #define FIXED_PORT_CLOCK_REG SIM->SCGC5           // Fixed PORT Clock
//    * </pre>
//    * 
//    * @param headerFile Header file to write to
//    * 
//    * @throws Exception 
//    */
//   private void writePinDefines(BufferedWriter headerFile) throws Exception {
//      writeBanner(headerFile, "Common Mux settings for PCR");
//      writeMacroUnDefinition(headerFile, "FIXED_ADC_FN");
//      writeMacroUnDefinition(headerFile, "FIXED_GPIO_FN");
//      writeMacroUnDefinition(headerFile, "FIXED_PORT_CLOCK_REG");
//      if (adcFunctionMuxValueChanged) {
//         writeMacroDefinition(headerFile, "ADC_FN_CHANGES", "", " Indicates ADC Multiplexing varies with pin");
//      }
//      else {
//         writeMacroDefinition(headerFile, "FIXED_ADC_FN", Integer.toString(adcFunctionMuxValue), " Fixed ADC Multiplexing value");
//      }
//      if (gpioFunctionMuxValueChanged) {
//         writeMacroDefinition(headerFile, "GPIO_FN_CHANGES", "", " Indicates GPIO Multiplexing varies with pin");
//      }
//      else {
//         writeMacroDefinition(headerFile, "FIXED_GPIO_FN", Integer.toString(gpioFunctionMuxValue), " Fixed GPIO Multiplexing value");
//      }
//      if (portClockRegisterChanged) {
//         writeMacroDefinition(headerFile, "PORT_CLOCK_REG_CHANGES", "", " Indicates PORT Clock varies with pin");
//      }
//      else {
//         writeMacroDefinition(headerFile, "FIXED_PORT_CLOCK_REG", portClockRegisterValue, " Fixed PORT Clock");
//      }
//      headerFile.write("\n");
//   }

   /**
    * Writes XML describing how peripheral functions are mapped to a pin
    * e.g.<pre>
    *   &lt;pin name=="PTD7"&gt;
    *      &lt;mux sel=="mux1" name=="GPIOD_7" /&gt;
    *      &lt;mux sel=="mux2" name=="CMT_IRO" /&gt;
    *      &lt;mux sel=="mux3" name=="UART0_TX" /&gt;
    *      &lt;mux sel=="mux4" name=="FTM0_CH7" /&gt;
    *      &lt;mux sel=="mux6" name=="FTM0_FLT1" /&gt;
    *      &lt;reset sel=="Disabled" /&gt;
    *      &lt;default sel=="mux1" /&gt;
    *   &lt;/pin&gt;
    * </pre>
    *  
    * @param documentUtilities   Where to write
    * @param pinInformation      Peripheral function to write definitions for
    * 
    * @throws Exception 
    */
   private void writePinMapping(XmlDocumentUtilities documentUtilities, PinInformation pinInformation) throws Exception {
      documentUtilities.openTag("pin");

      Map<MuxSelection, MappingInfo>  mappingInfo  = fDeviceInfo.getFunctions(pinInformation);

      MuxSelection[] sortedSelectionIndexes = mappingInfo.keySet().toArray(new MuxSelection[mappingInfo.keySet().size()]);
      Arrays.sort(sortedSelectionIndexes);

      MuxSelection defaultSelection = MuxSelection.reset;

      // Construct list of alternatives
      StringBuffer alternativeHint = new StringBuffer();
      for (MuxSelection selection:sortedSelectionIndexes) {
         if (selection == MuxSelection.disabled) {
            continue;
         }
         if ((selection == MuxSelection.reset) && (sortedSelectionIndexes.length>1)) {
            continue;
         }
         if (selection == MuxSelection.fixed) {
            defaultSelection = MuxSelection.fixed;
         }
         if (selection == pinInformation.getDefaultValue()) {
            defaultSelection = selection;
         }
         MappingInfo mInfo = mappingInfo.get(selection);
         StringBuffer name = new StringBuffer();
         name.append(mInfo.getFunctionList());
//         if ((pinInformation.getDefaultValue() != null) && 
//             (mInfo.functions == pinInformation.getDefaultValue().functions)) {
//            defaultSelection = selection;
//         }
         if (alternativeHint.length() != 0) {
            alternativeHint.append(", ");
         }
         alternativeHint.append(name);
      }
      documentUtilities.writeAttribute("name", pinInformation.getName());
      if (defaultSelection == MuxSelection.fixed) {
         documentUtilities.writeAttribute("isFixed", "true");
      }
      String       resetFunction  = null;
      MuxSelection resetSelection = MuxSelection.disabled;
      for (MuxSelection selection:sortedSelectionIndexes) {
         MappingInfo mInfo = mappingInfo.get(selection);
         StringBuffer name = new StringBuffer();
         name.append(mInfo.getFunctionList());
         
         for (PeripheralFunction fn:mInfo.functions) {
            if (selection == MuxSelection.reset) {
               resetFunction = fn.getName();
               continue;
            }
            if (fn.getName().equalsIgnoreCase(resetFunction)) {
               resetSelection = selection;
            }
            documentUtilities.openTag("mux");
            documentUtilities.writeAttribute("sel", selection.name());
            documentUtilities.writeAttribute("function", fn.getName());
            documentUtilities.closeTag();
         }
      }
      if (defaultSelection != MuxSelection.fixed) {
         if (resetFunction == null) {
            throw new RuntimeException("No reset value given for ");
         }
         if (resetSelection == MuxSelection.reset) {
            
         }
         documentUtilities.openTag("reset");
         documentUtilities.writeAttribute("sel", resetSelection.name());
         documentUtilities.closeTag();

         documentUtilities.openTag("default");
         if (defaultSelection == MuxSelection.reset) {
            defaultSelection = resetSelection;
         }
         documentUtilities.writeAttribute("sel", defaultSelection.name());
         documentUtilities.closeTag();
      }
      documentUtilities.closeTag();
   }

   /**
    * Writes pin-mapping selection code for all peripheral functions
    *  
    * @param documentUtilities  Where to write result
    * 
    * @throws Exception 
    */
   private void writePins(XmlDocumentUtilities documentUtilities) throws Exception {

      documentUtilities.openTag("pins");
      
      HashMap<String,ArrayList<PinInformation>> categories = new HashMap<String,ArrayList<PinInformation>>();
      class Pair {
         public final String namePattern;
         public final String titlePattern;
         
         Pair(String n, String t) {
            namePattern    = n;
            titlePattern   = t;
         }
      };
      final String UNMATCHED_NAME = "Miscellaneous Pins";
      Pair[] pinPatterns = {
            new Pair("XXXX",          UNMATCHED_NAME), 
            new Pair("PT([A-Z]).*",   "Port $1 Pins"), 
      };
      ArrayList<String> categoryTitles = new ArrayList<String>();
      for (String name:fDeviceInfo.getPins().keySet()) {
         String categoryTitle = UNMATCHED_NAME;
         for (Pair pair:pinPatterns) {
            Pattern p = Pattern.compile(pair.namePattern);
            Matcher m = p.matcher(name);
            if (m.matches()) {
               categoryTitle = m.replaceAll(pair.titlePattern);
               break;
            }
         }
         ArrayList<PinInformation> category = categories.get(categoryTitle);
         if (category == null) {
            category = new ArrayList<PinInformation>();
            categories.put(categoryTitle, category);
            categoryTitles.add(categoryTitle);
         }
         category.add(fDeviceInfo.findPin(name));
      }
      for (String p:categoryTitles) {
         ArrayList<PinInformation> category = categories.get(p);
         if (category != null) {
            for (PinInformation pinInformation:category) {
               writePinMapping(documentUtilities, pinInformation);
            }
         }
      }
      documentUtilities.closeTag();
   }

//   static class ConstantAttribute implements WizardAttribute {
//
//      @Override
//      public String getAttributeString() {
//         return "<constant>";
//      }
//      
//   };
//   
//   /** A constant attribute for convenience */
//   static final ConstantAttribute   constantAttribute      = new ConstantAttribute();
////   static final ConstantAttribute[] constantAttributeArray = {constantAttribute};
//   
//   /**
//    * Gets pin name with appended list of aliases
//    * 
//    * @param pinInformation
//    * @return name with aliases e.g. <b><i>PTE0 (Alias:D14)</b></i>
//    */
//   private String getPinNameWithAlias(PinInformation pinInformation) {
//      String pinName = pinInformation.getName();
//      String aliases = Aliases.getAliasList(pinInformation);
//      if (aliases != null) {
//         pinName += " (Alias:"+aliases+")";
//      }
//      return pinName;
//   }
//   
//   /**
//    * Writes code to select which pin a peripheral function is mapped to
//    *  
//    * @param documentUtilities     Header file to write result
//    * @param function   The function to process
//    * 
//    * @throws Exception
//    */
//   private void writePeripheralSignalMapping(DocumentUtilities documentUtilities, PeripheralFunction function) throws Exception {
//      if (!function.isIncluded()) {
//         return;
//      }
//      documentUtilities.openTag("peripheral");
//      ArrayList<MappingInfo> mappingInfos = MappingInfo.getPins(function);
//      Collections.sort(mappingInfos, new Comparator<MappingInfo>() {
//
//         @Override
//         public int compare(MappingInfo o1, MappingInfo o2) {
//            return o1.mux.value - o2.mux.value;
//         }
//      });
//      boolean noChoices =  ((mappingInfos == null) || (mappingInfos.size() == 0) ||
//            ((mappingInfos.size() == 1) && (mappingInfos.get(0).mux == MuxSelection.fixed)));
//
//      //      boolean debug = false;
//      //      if (function.getName().startsWith("JTAG_TDI")) {
//      //         System.err.println("writePeripheralSignalMapping(): " + function.getName());
//      //         debug = true;
//      //      }
//      // Create list of choices as string and determine default selection (if any)
//      int defaultSelection = 0;
//      //      int resetSelection = -1;
//      String choices = null;
//      if (mappingInfos != null) {
//         //         Collections.sort(mappingInfos, new Comparator<MappingInfo>() {
//         //            @Override
//         //            public int compare(MappingInfo o1, MappingInfo o2) {
//         //               return PinInformation.portNameComparator.compare(o1.pin.getName(), o2.pin.getName());
//         //            }
//         //         });
//         int selection = 0;
//         if (!noChoices) {
//            selection++;
//         }
//         for (MappingInfo mappingInfo:mappingInfos) {
//            if (mappingInfo.mux == MuxSelection.disabled) {
//               continue;
//            }
//            if ((mappingInfo.mux == MuxSelection.reset) && (mappingInfo.pin.getDefaultValue() == null)) {
//               if (defaultSelection == 0) {
//                  defaultSelection = selection;
//               }
//               //               continue;
//            }
//            if (mappingInfo.mux == MuxSelection.fixed) {
//               defaultSelection = selection;
//            }
//            if (mappingInfo.pin.getDefaultValue() != null) {
//               if (mappingInfo.pin.getDefaultValue().indexOf(function) >= 0) {
//                  defaultSelection = selection;
//               }
//            }
//            if (mappingInfo.mux != MuxSelection.reset) {
//               if (choices == null) {
//                  choices = mappingInfo.pin.getName();
//               }
//               else {
//                  choices += ", " + mappingInfo.pin.getName();
//               }
//            }
//            selection++;
//         }
//      }
//      if (choices != null) {
//         choices = " [" + choices + "]";
//      }
//      documentUtilities.writeAttribute("name", function.getName());
//
////      WizardAttribute[] attributes = {new NameAttribute(function.getName()+"_PIN_SEL"), noChoices?constantAttribute:null};
////      writeWizardOptionSelectionPreamble(documentUtilities, 
////            "Pin Mapping for " + function.getName() + " signal",
////            0,
////            attributes,
////            String.format("%s", function.getName()),
////            String.format("Shows which pin %s is mapped to", function.getName()),
////            choices);
////
////      int selection = 0;
////      if (!noChoices) {
////         writeWizardOptionSelectionEnty(documentUtilities, Integer.toString(selection++), "Disabled");
////      }
//////      if ((mappingInfos == null) || (mappingInfos.size() == 0)) {
////         writeWizardOptionSelectionEnty(documentUtilities, Integer.toString(-1), function.getName());
////         writeMacroDefinition(documentUtilities, function.getName()+"_PIN_SEL", Integer.toString(-1));
////      }
////      else {
////         for (MappingInfo mappingInfo:mappingInfos) {
////            if (mappingInfo.mux == MuxSelection.disabled) {
////               continue;
////            }
////            String pinName = getPinNameWithAlias(mappingInfo.pin);
////            if (mappingInfo.mux == MuxSelection.reset) {
////               pinName += " (reset default)";
////               //            continue;
////            }
////            String seletionTag = mappingInfo.getFunctionList();
////            if (mappingInfo.mux == MuxSelection.reset) {
////               seletionTag += " (reset default)";
////            }
////            WizardAttribute[] functionAttributes = {new SelectionAttribute(mappingInfo.pin.getName()+"_SIG_SEL", seletionTag)};
////            writeWizardOptionSelectionEnty(documentUtilities, Integer.toString(selection++), pinName, functionAttributes);
////         }
////
////         writeWizardOptionSelectionEnty(documentUtilities, Integer.toString(defaultSelection), "Default", null);
////         writeMacroDefinition(documentUtilities, function.getName()+"_PIN_SEL", Integer.toString(defaultSelection));
////
////      }
//      documentUtilities.closeTag();
//   }
//
//   /**
//    * Writes code to control what pin peripheral functions are mapped to
//    *  
//    * @param writer  Header file to write result
//    * 
//    * @throws Exception 
//    */
//   private void writePeripheralSignalMappings(DocumentUtilities documentUtilities) throws Exception {
//      documentUtilities.openTag("peripherals");
//      
//      HashMap<String,ArrayList<PeripheralFunction>> categories = new HashMap<String,ArrayList<PeripheralFunction>>();
//      class Pair {
//         public final String namePattern;
//         public final String titlePattern;
//         
//         Pair(String p, String t) {
//            namePattern = p;
//            titlePattern   = t;
//         }
//      };
//      final String UNMATCHED_NAME = "Miscellaneous";
//      Pair[] functionPatterns = {
//            new Pair("(ADC\\d+).*",             "Analogue to Digital ($1)"), 
//            new Pair("(VREF\\d*).*",            "Voltage Reference ($1)"), 
//            new Pair("(A?CMP\\d+).*",           "Analogue Comparator ($1)"), 
//            new Pair("(FTM\\d+).*",             "FlexTimer ($1)"), 
//            new Pair("(TPM\\d+).*",             "Timer ($1)"), 
//            new Pair("(LCD_P)?(\\d+).*",        "Liquid Crystal Display"), 
//            new Pair("(GPIO[A-Z]+).*",          "General Purpose I/O ($1)"), 
//            new Pair("(I2C\\d+).*",             "Inter-Integrated Circuit ($1)"), 
//            new Pair("(I2S\\d+).*",             "Integrated Interchip Sound ($1)"), 
//            new Pair("(LLWU\\d*).*",            "Low-Leakage Wake-up Unit ($1)"), 
//            new Pair("(SPI\\d+).*",             "Serial Peripheral Interface ($1)"), 
//            new Pair("(TSI\\d+).*",             "Touch Sense Interface ($1)"), 
//            new Pair("(LPTMR|LPTIM)(\\d+)*.*",  "Low Power Timer ($1)"), 
//            new Pair("(UART\\d+).*",            "Universal Asynchronous Rx/Tx ($1)"), 
//            new Pair("(PXBAR).*",               "($1)"), 
//            new Pair("(QT).*",                  "($1)"), 
//            new Pair("(SCI\\d+).*",             "Serial Communication Interface ($1)"), 
//            new Pair("(SDAD)(M|P)\\d+.*",       "Sigma-delta ADC ($1)"), 
//            new Pair("(LPUART\\d+).*",          "Low Power UART ($1)"), 
//            new Pair("(DAC\\d*).*",             "Digital to Analogue ($1)"), 
//            new Pair("(PDB\\d*).*",             "Programmable Delay Block ($1)"), 
//            new Pair("(CAN\\d*).*",             "CAN Bus ($1)"), 
//            new Pair("(ENET\\d*).*",            "Ethernet ($1)"), 
//            new Pair("(MII\\d*).*",             "Ethernet ($1)"), 
//            new Pair("(RMII\\d*).*",            "Ethernet ($1)"), 
//            new Pair("(SDHC\\d*).*",            "Secured Digital Host Controller ($1)"), 
//            new Pair("(CMT\\d*).*",             "Carrier Modulator Transmitter ($1)"), 
//            new Pair("(EWM).*",                 "External Watchdog Monitor ($1)"), 
//            new Pair("E?XTAL.*",                "Clock and Timing"),
//            new Pair("(JTAG|SWD|NMI|TRACE|RESET).*",  "Debug and Control"),
//            new Pair("(FB_).*",                 "Flexbus"),
//            new Pair("(FXIO\\d+).*",            "Flexbus ($1)"),
//            new Pair(".*(USB).*",               "Universal Serial Bus"), 
//            new Pair(".*(CLK|EXTRG).*",         "Clock and Timing"),
//      };
//      
//      ArrayList<String> categoryTitles = new ArrayList<String>();
//
//      // Add catch-all "Miscellaneous" category
//      categoryTitles.add(UNMATCHED_NAME);
//      categories.put(UNMATCHED_NAME, new ArrayList<PeripheralFunction>());
//      
//      ArrayList<String> peripheralNames = PeripheralFunction.getPeripheralFunctionsAsList();
//      for (String name:peripheralNames) {
//         PeripheralFunction peripheralFunction = PeripheralFunction.find(name);
//         if (!peripheralFunction.isIncluded()) {
//            continue;
//         }
//         String categoryTitle = UNMATCHED_NAME;
//         for (Pair pair:functionPatterns) {
//            Pattern p = Pattern.compile(pair.namePattern);
//            Matcher m = p.matcher(name);
//            if (m.matches()) {
//               categoryTitle = m.replaceAll(pair.titlePattern);
//               break;
//            }
//         }
//         ArrayList<PeripheralFunction> category = categories.get(categoryTitle);
//         if (category == null) {
//            categoryTitles.add(categoryTitle);
//            category = new ArrayList<PeripheralFunction>();
//            categories.put(categoryTitle, category);
//         }
//         category.add(peripheralFunction);
//      }
//      for (String categoryTitle:categoryTitles) {
//         ArrayList<PeripheralFunction> category = categories.get(categoryTitle);
//         if (category.size()>0) {
//            for (PeripheralFunction peripheralFunction:category) {
//               writePeripheralSignalMapping(documentUtilities, peripheralFunction);
//            }
//         }
//      }
//      documentUtilities.closeTag();
//
//   }

//   /**
//    * Write timer configuration wizard information e.g.
//    * <pre>
//    * // &lth> Clock settings for FTM0
//    * //
//    * // FTM0_SC.CLKS ================================
//    * //   &lt;o> FTM0_SC.CLKS Clock source 
//    * //   &lt;i> Selects the clock source for the FTM0 module. [FTM0_SC.CLKS]
//    * //     &lt;0=> Disabled
//    * //     &lt;1=> System clock
//    * //     &lt;2=> Fixed frequency clock
//    * //     &lt;3=> External clock
//    * //     &lt;1=> Default
//    * 
//    * // FTM0_SC.PS ================================
//    * //   &lt;o1> FTM0_SC.PS Clock prescaler 
//    * //   &lt;i> Selects the prescaler for the FTM0 module. [FTM0_SC.PS]
//    * //     &lt;0=> Divide by 1
//    * //     &lt;1=> Divide by 2
//    * //     &lt;2=> Divide by 4
//    * //     &lt;3=> Divide by 8
//    * //     &lt;4=> Divide by 16
//    * //     &lt;5=> Divide by 32
//    * //     &lt;6=> Divide by 64
//    * //     &lt;7=> Divide by 128
//    * //     &lt;0=> Default
//    * namespace USBDM {
//    * constexpr uint32_t FTM0_SC = (FTM_SC_CLKS(0x1)|FTM_SC_PS(0x6));
//    * }
//    * </pre>
//    * @param headerFile    Where to write
//    * 
//    * @throws IOException
//    */
//   private void writeTimerWizard(BufferedWriter headerFile) throws IOException {
//      HashMap<String, PeripheralFunction> map;
//      map = PeripheralFunction.getFunctionsByBaseName("FTM");
//      if (map != null) {
//         HashSet<String> instances = new HashSet<String>();
//         for (String function:map.keySet()) {
//            instances.add(map.get(function).fPeripheral.fInstance);
//         }
//         String[] sortedInstances = instances.toArray(new String[instances.size()]);
//         Arrays.sort(sortedInstances);
//         for (String ftm:sortedInstances) {
//            if (!ftm.matches("\\d+")) {
//               continue;
//            }
//            writeWizardSectionOpen(headerFile, "Clock settings for FTM" + ftm);
//            writeWizardOptionSelectionPreamble(headerFile, 
//                  String.format("FTM%s_SC.CLKS ================================\n//", ftm), 
//                  0,
//                  null,
//                  String.format("FTM%s_SC.CLKS Clock source", ftm),
//                  String.format("Selects the clock source for the FTM%s module. [FTM%s_SC.CLKS]", ftm, ftm));
//            writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
//            writeWizardOptionSelectionEnty(headerFile, "1", "System clock");
//            writeWizardOptionSelectionEnty(headerFile, "2", "Fixed frequency clock");
//            writeWizardOptionSelectionEnty(headerFile, "3", "External clock");
//            writeWizardDefaultSelectionEnty(headerFile, "1");
//            writeWizardOptionSelectionPreamble(headerFile, 
//                  String.format("FTM%s_SC.PS ================================\n//",ftm),
//                  1,
//                  null,
//                  String.format("FTM%s_SC.PS Clock prescaler", ftm),
//                  String.format("Selects the prescaler for the FTM%s module. [FTM%s_SC.PS]", ftm, ftm));
//            writeWizardOptionSelectionEnty(headerFile, "0", "Divide by 1");
//            writeWizardOptionSelectionEnty(headerFile, "1", "Divide by 2");
//            writeWizardOptionSelectionEnty(headerFile, "2", "Divide by 4");
//            writeWizardOptionSelectionEnty(headerFile, "3", "Divide by 8");
//            writeWizardOptionSelectionEnty(headerFile, "4", "Divide by 16");
//            writeWizardOptionSelectionEnty(headerFile, "5", "Divide by 32");
//            writeWizardOptionSelectionEnty(headerFile, "6", "Divide by 64");
//            writeWizardOptionSelectionEnty(headerFile, "7", "Divide by 128");
//            writeWizardDefaultSelectionEnty(headerFile, "0");
//            writeOpenNamespace(headerFile, NAME_SPACE);
//            writeConstexpr(headerFile, 16, "FTM"+ftm+"_SC", "(FTM_SC_CLKS(0x1)|FTM_SC_PS(0x0))");
//            writeCloseNamespace(headerFile);
//            headerFile.write("\n");
//            writeWizardSectionClose(headerFile);
//            //      headerFile.write( String.format(optionSectionClose));
//         }
//      }
//      map = PeripheralFunction.getFunctionsByBaseName("TPM");
//      if (map != null) {
//         HashSet<String> instances = new HashSet<String>();
//         for (String function:map.keySet()) {
//            instances.add(map.get(function).fPeripheral.fInstance);
//         }
//         String[] sortedInstances = instances.toArray(new String[instances.size()]);
//         Arrays.sort(sortedInstances);
//         for (String ftm:sortedInstances) {
//            if (!ftm.matches("\\d+")) {
//               continue;
//            }
//            writeWizardSectionOpen(headerFile, "Clock settings for TPM" + ftm);
//            writeWizardOptionSelectionPreamble(headerFile, 
//                  String.format("TPM%s_SC.CMOD ================================\n//", ftm),
//                  0,
//                  null,
//                  String.format("TPM%s_SC.CMOD Clock source",ftm),
//                  String.format("Selects the clock source for the TPM%s module. [TPM%s_SC.CMOD]", ftm, ftm));
//            writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
//            writeWizardOptionSelectionEnty(headerFile, "1", "Internal clock");
//            writeWizardOptionSelectionEnty(headerFile, "2", "External clock");
//            writeWizardOptionSelectionEnty(headerFile, "3", "Reserved");
//            writeWizardDefaultSelectionEnty(headerFile, "1");
//            writeWizardOptionSelectionPreamble(headerFile, 
//                  String.format("TPM%s_SC.PS ================================\n//", ftm),
//                  1,
//                  null,
//                  String.format("TPM%s_SC.PS Clock prescaler", ftm),
//                  String.format("Selects the prescaler for the TPM%s module. [TPM%s_SC.PS]", ftm, ftm));
//            writeWizardOptionSelectionEnty(headerFile, "0", "Divide by 1");
//            writeWizardOptionSelectionEnty(headerFile, "1", "Divide by 2");
//            writeWizardOptionSelectionEnty(headerFile, "2", "Divide by 4");
//            writeWizardOptionSelectionEnty(headerFile, "3", "Divide by 8");
//            writeWizardOptionSelectionEnty(headerFile, "4", "Divide by 16");
//            writeWizardOptionSelectionEnty(headerFile, "5", "Divide by 32");
//            writeWizardOptionSelectionEnty(headerFile, "6", "Divide by 64");
//            writeWizardOptionSelectionEnty(headerFile, "7", "Divide by 128");
//            writeWizardDefaultSelectionEnty(headerFile, "0");
//            writeOpenNamespace(headerFile, NAME_SPACE);
//            writeConstexpr(headerFile, 16, "TPM"+ftm+"_SC", "(TPM_SC_CMOD(0x1)|TPM_SC_PS(0x0))");
//            writeCloseNamespace(headerFile);
//            headerFile.write("\n");
//            writeWizardSectionClose(headerFile);
//         }
//      }
//   }
//
//   /**
//    * Writes GPIO options e.g.
//    * 
//    * <pre>
//    * // Inline port functions
//    * //   &lt;q> Force inline port functions
//    * //   &lt;i> This option forces some small GPIO functions to be inlined
//    * //   &lt;i> This increases speed but may also increase code size
//    * //     &lt;0=> Disabled
//    * //     &lt;1=> Enabled
//    * 
//    * #define DO_INLINE_GPIO   0
//    * 
//    * </pre>
//    * 
//    * @param headerFile    Where to write
//    * 
//    * @throws IOException
//    */
//   private void writeGpioWizard(BufferedWriter headerFile) throws IOException {
////      writeWizardSectionOpen(headerFile, "GPIO Options");
////      writeWizardBinaryOptionSelectionPreamble(headerFile, 
////            String.format("Inline port functions\n//"), 
////            0,
////            false,
////            String.format("Force inline port functions"),
////            String.format("Forces some small GPIO functions to be inlined\n"+
////                          "This increases speed but may also increase code size"));
////      writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
////      writeWizardOptionSelectionEnty(headerFile, "1", "Enabled");
////      writeMacroDefinition(headerFile, "DO_INLINE_GPIO", "0");
//      
////      writeWizardBinaryOptionSelectionPreamble(headerFile, 
////            String.format("Use USBDM namespace\n//"), 
////            0,
////            false,
////            String.format("Place CPP objects in the USBDM namespace"),
////            String.format("This will require us of \"using namespace USBDM\" directive"));
////      writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
////      writeWizardOptionSelectionEnty(headerFile, "1", "Enabled");
////      writeMacroDefinition(headerFile, NAMESPACES_GUARD_STRING, "0");
////      headerFile.write("\n");
////      writeWizardSectionClose(headerFile);
//   }

//   /**
//    * Writes all clock macros e.g.
//    * <pre>
//    * #define ADC0_CLOCK_REG       SIM->SCGC6          
//    * #define ADC0_CLOCK_MASK      SIM_SCGC6_ADC0_MASK 
//    * </pre>
//    * 
//    * @param writer  Where to write
//    * 
//    * @throws Exception
//    */
//   private void writeClockMacros(BufferedWriter writer) throws Exception {
//      writeBanner(writer, "Peripheral clock macros");
//      ArrayList<String> peripheralNames = Peripheral.getList();
//      for (String name:peripheralNames) {
//         Peripheral peripheral = Peripheral.getPeripheral(name);
//         if (peripheral.fClockReg == null) {
//            continue;
//         }
//         if (peripheral.fName.matches("PORT[A-Z]")) {
//            if (portClockRegisterValue == null) {
//               portClockRegisterValue = peripheral.fClockReg;
//            }
//            else if (!portClockRegisterValue.equals(peripheral.fClockReg)) {
//               throw new Exception(
//                  String.format("Multiple port clock registers existing=%s, new=%s", portClockRegisterValue, peripheral.fClockReg));
//            }
//         }
//         writeMacroDefinition(writer, peripheral.fName+"_CLOCK_REG",  peripheral.fClockReg);
//         writeMacroDefinition(writer, peripheral.fName+"_CLOCK_MASK", peripheral.fClockMask);
//      }
//      writeMacroDefinition(writer, "PORT_CLOCK_REG", portClockRegisterValue);
//      writer.write("\n");
//      
////      /* 
////       * XXX - Write debug information
////       */
////      writer.write("/*\n * Clock Information Summary\n");
////      for (String name:Peripheral.getList()) {
////         Peripheral peripheral = Peripheral.getPeripheral(name);
////         if (peripheral.fClockReg == null) {
////            continue;
////         }
////         if (peripheral.fName.matches("PORT[A-Z]")) {
////            if (portClockRegisterValue == null) {
////               portClockRegisterValue = peripheral.fClockReg;
////            }
////            else if (!portClockRegisterValue.equals(peripheral.fClockReg)) {
////               throw new Exception(
////                     String.format("Multiple port clock registers existing=%s, new=%s", portClockRegisterValue, peripheral.fClockReg));
////            }
////         }
////         writer.write(String.format(" * %-10s %-12s %-10s\n", peripheral.fName,  peripheral.fClockReg, peripheral.fClockMask));
////      }
////      writer.write(" */\n\n");
//
//   }

private void writePackages(XmlDocumentUtilities documentUtilities) throws IOException {
   documentUtilities.openTag("packages");
   
   for (String packageName:fDeviceInfo.getDevicePackages().keySet()) {
      documentUtilities.openTag("package");
      documentUtilities.writeAttribute("name", packageName);
      DevicePackage pkg = fDeviceInfo.findDevicePackage(packageName);
      for (String pinName:pkg.getPins().keySet()) {
         documentUtilities.openTag("placement");
         String location = pkg.getLocation(pinName);
         documentUtilities.writeAttribute("pin", pinName);
         documentUtilities.writeAttribute("location", location);
         documentUtilities.closeTag();
      }
      documentUtilities.closeTag();
   }
   
   documentUtilities.closeTag();
}
   //
//   /**
//    * Write file validators e.g.
//    * 
//    * <pre>
//    *    //================================
//    *    // Validators
//    *    // &lt;validate=<b><i>net.sourceforge.usbdm.annotationEditor.validators.PinMappingValidator</b></i>&gt;
//    * </pre>
//    * 
//    * @param writer        Where to write
//    * @param validators    Validators to write
//    * 
//    * @throws IOException
//    */
//   private void writeValidators(BufferedWriter writer, ValidatorAttribute[] validators) throws IOException {
//      final String format = 
//         "//================================\n"
//         + "// Validators\n";
//      
//      writer.write(format);
//      
//      for (ValidatorAttribute validatorAttribute: validators) {
//         if (validatorAttribute != null) {
//            writer.write("// " + validatorAttribute.getAttributeString() + "\n");
//         }
//      }
//      writer.write("\n");
//   }
//
//   @SuppressWarnings("unused")
//   private String makeOrExpression(String value, boolean[] values) {
//      StringBuffer sb = new StringBuffer();
//      boolean firstValue = true;
//      for (int index=0; index<values.length; index++) {
//         if (!values[index]) {
//            continue;
//         }
//         if (!firstValue) {
//            sb.append("||");
//         }
//         sb.append("("+value+"=="+Integer.toString(index)+")");
//         firstValue = false;
//      }
//      return sb.toString();
//   }
//   
//   /**
//    * Write an external declaration for a simple peripheral (GPIO,ADC,PWM) e.g.
//    * 
//    * <pre>
//    * <b>#if</b> <i>PTC18_SEL</i> == 1
//    * using <i>gpio_A5</i>  = const USBDM::<i>GpioC&lt;18&gt;</i>;
//    * <b>#endif</b>
//    * </pre>
//    * 
//    * @param template         Template information
//    * @param mappedFunction   Information about the pin and function being declared
//    * @param fnIndex          Index into list of functions mapped to pin
//    * @param gpioHeaderFile   Where to write
//    * 
//    * @throws Exception 
//    */
//   void writeExternDeclaration(PeripheralTemplateInformation template, MappingInfo mappedFunction, int fnIndex, BufferedWriter gpioHeaderFile) throws Exception {
//
//      String definition = template.fInstanceWriter.getDefinition(mappedFunction, fnIndex);
//      if (definition == null) {
//         return;
//      }
//      boolean guardWritten = false;
//
//      String signalName = template.fInstanceWriter.getInstanceName(mappedFunction, fnIndex);
////      if (guardWritten || macroAliases.add(signalName)) {
////         gpioHeaderFile.write(definition);
////      }
//      if (template.useAliases(null)) {
//         Aliases aliasList = Aliases.getAlias(mappedFunction.pin);
//         if (aliasList != null) {
//            for (String alias:aliasList.aliasList) {
//               String aliasName = template.fInstanceWriter.getAliasName(signalName, alias);
//               if (aliasName!= null) {
//                  String declaration = template.fInstanceWriter.getAlias(aliasName, mappedFunction, fnIndex);
//                  if (declaration != null) {
//                     if (!guardWritten) {
//                        guardWritten = writeFunctionSelectionGuardMacro(template, mappedFunction, gpioHeaderFile);
//                     }
//                     if (!macroAliases.add(aliasName)) {
//                        // Comment out repeated aliases
//                        gpioHeaderFile.write("//");
//                     }
//                     gpioHeaderFile.write(declaration);
//                  }
//               }
//            }
//         }
//      }
//      writeConditionalEnd(gpioHeaderFile, guardWritten);
//   }
   
   /**
    * Process pins
    */
   void processPins() {
      for (PeripheralTemplateInformation pinTemplate:fDeviceInfo.getTemplateList()) {
         for (String pinName:fDeviceInfo.getPins().keySet()) {
            PinInformation pinInfo = fDeviceInfo.findPin(pinName);
            Map<MuxSelection, MappingInfo> mappedFunctions = fDeviceInfo.getFunctions(pinInfo);
            if (mappedFunctions == null) {
               continue;
            }
            for (MuxSelection index:mappedFunctions.keySet()) {
               if (index == MuxSelection.reset) {
                  continue;
               }
               MappingInfo mappedFunction = mappedFunctions.get(index);
               for (PeripheralFunction function:mappedFunction.functions) {
                  if (pinTemplate.matches(function)) {
                     fDeviceInfo.addFunctionType(pinTemplate.getPeripheralName(), pinInfo);
                  }
               }
            }
         }
      }
   }
      
   /**
    * Write alls Peripheral Information Classes<br>
    * 
    * <pre>
    *  class Adc0Info {
    *     public:
    *        //! Hardware base pointer
    *        static constexpr uint32_t basePtr   = ADC0_BasePtr;
    * 
    *        //! Base value for PCR (excluding MUX value)
    *        static constexpr uint32_t pcrValue  = DEFAULT_PCR;
    * 
    *        //! Information for each pin of peripheral
    *        static constexpr PcrInfo  info[32] = {
    * 
    *   //         clockMask         pcrAddress      gpioAddress gpioBit muxValue
    *   /*  0 * /  { 0 },
    *   ...
    *   #if (ADC0_SE4b_PIN_SEL == 1)
    *    /*  4 * /  { PORTC_CLOCK_MASK, PORTC_BasePtr,  GPIOC_BasePtr,  2,  0 },
    *   #else
    *    /*  4 * /  { 0 },
    *   #endif
    *   ...
    *   };
    *   };
    * </pre>
    * @param documentUtilities Where to write
    * 
    * @throws Exception 
    */
   private void writePeripheralInformationTables(XmlDocumentUtilities documentUtilities) throws Exception {
      documentUtilities.openTag("peripherals");
      for (PeripheralTemplateInformation pinTemplate:fDeviceInfo.getTemplateList()) {
         pinTemplate.writePeripheralInformation(documentUtilities);
      }
      documentUtilities.closeTag();
   }

//   /**
//    * Write GPIO Header file.<br>
//    * This mostly contains the extern declarations for peripherals
//    * 
//    * <pre>
//    * <b>#if</b> <i>PTC18_SEL</i> == 1
//    * using <i>gpio_A5</i>  = const USBDM::<i>GpioC&lt;18&gt;</i>;
//    * <b>#endif</b>
//    * </pre>
//    * @param gpioHeaderFile
//    * 
//    * @throws Exception 
//    */
//   private void writeDeclarations(BufferedWriter gpioHeaderFile) throws Exception {
//      
//      gpioHeaderFile.write("\n");
//      writeOpenNamespace(gpioHeaderFile, NAME_SPACE);
//      for (PeripheralTemplateInformation pinTemplate:PeripheralTemplateInformation.getList()) {
//         if (!pinTemplate.classIsUsed()) {
//            continue;
//         }
//         boolean groupDone = false;
//         for (String pinName:PinInformation.getPinNames()) {
//            PinInformation pinInfo = PinInformation.find(pinName);
//            HashMap<MuxSelection, MappingInfo> mappedFunctions = MappingInfo.getFunctions(pinInfo);
//            if (mappedFunctions == null) {
//               continue;
//            }
//            if (!pinTemplate.useAliases(pinInfo)) {
//               continue;
//            }
//            for (MuxSelection index:mappedFunctions.keySet()) {
//               if (index == MuxSelection.reset) {
//                  continue;
//               }
//               MappingInfo mappedFunction = mappedFunctions.get(index);
//               for (int fnIndex=0; fnIndex<mappedFunction.functions.size(); fnIndex++) {
//                  PeripheralFunction function = mappedFunction.functions.get(fnIndex);
//                  if (pinTemplate.matches(function)) {
//                     if (!groupDone) {
//                        writeStartGroup(gpioHeaderFile, pinTemplate);
//                        groupDone = true;
//                        String t = pinTemplate.fInstanceWriter.getTemplate();
//                        if (t != null) {
//                           gpioHeaderFile.write(t);
//                        }
//                     }
//                     writeExternDeclaration(pinTemplate, mappedFunction, fnIndex, gpioHeaderFile);
//                  }
//               }
//            }
//         }
//         if (groupDone) {
//            writeCloseGroup(gpioHeaderFile);
//         }
//      }
//      writeConditionalStart(gpioHeaderFile, "DO_MAP_PINS_ON_RESET>0");
//      writeDocBanner(gpioHeaderFile, "Used to configure pin-mapping before 1st use of peripherals");
//      gpioHeaderFile.write("extern void usbdm_PinMapping();\n");
//      writeConditionalEnd(gpioHeaderFile);
//      writeCloseNamespace(gpioHeaderFile, NAME_SPACE);
////      writeHeaderFilePostamble(gpioHeaderFile, gpioBaseFileName+".h");
//   }
//
//   /**
//    * Write Pin Mapping function to CPP file
//    * 
//    * @param cppFile    File to write to
//    * 
//    * @throws IOException
//    */
//   private void writePinMappingFunction(BufferedWriter cppFile) throws IOException {
//      
//      writeConditionalStart(cppFile, "DO_MAP_PINS_ON_RESET>0");
//      cppFile.write(
//         "struct PinInit {\n"+
//         "   uint32_t pcrValue;\n"+
//         "   uint32_t volatile *pcr;\n"+
//         "};\n\n"+
//         "static constexpr PinInit pinInit[] = {\n"
//      );
//
//      for (String pinName:PinInformation.getPinNames()) {
//         Pattern p = Pattern.compile("PT([A-Z]+)([0-9]+)");
//         Matcher m = p.matcher(pinName);
//         if (m.matches()) {
//            String instance = m.replaceAll("$1");
//            String signal   = m.replaceAll("$2");
//            writeConditionalStart(cppFile, String.format("%s_SIG_SEL>=0", pinName, pinName));
//            cppFile.write(String.format("   { PORT_PCR_MUX(%s_SIG_SEL)|%s::DEFAULT_PCR, &PORT%s->PCR[%s]},\n", pinName, NAME_SPACE, instance, signal));
//            writeConditionalEnd(cppFile);
//         }
//      }
//      cppFile.write("};\n\n");
//      
//      cppFile.write(
//         "/**\n" + 
//         " * Used to configure pin-mapping before 1st use of peripherals\n" + 
//         " */\n" + 
//         "void usbdm_PinMapping() {\n"
//      );
//      
//      boolean firstExpression = true;
//      String currentBasename = null;
//      String  instance = "X";
//      int conditionCounter = 0;
//      for (String pinName:PinInformation.getPinNames()) {
//         Pattern p = Pattern.compile("(PT([A-Z]))[0-9]+");
//         Matcher m = p.matcher(pinName);
//         if (m.matches()) {
//            String basename = m.replaceAll("$1");
//            if (!basename.equals(currentBasename)) {
//               if (!firstExpression) {
//                  cppFile.write(String.format("\n\n   SIM->FIXED_PORT_CLOCK_REG |= PORT%s_CLOCK_MASK;\n", instance));
//                  writeConditionalEnd(cppFile);
//               }
//               currentBasename = basename;
//               cppFile.write("#if ");
//               firstExpression = false;
//               instance = m.replaceAll("$2");
//            }
//            else {
//               cppFile.write(" || ");
//               if (++conditionCounter>=4) {
//                  cppFile.write("\\\n    ");
//                  conditionCounter = 0;
//               }
//            }
//            cppFile.write(String.format("(%s_SIG_SEL>=0)", pinName));
//         }
//      }
//      if (!firstExpression) {
//         cppFile.write(String.format("\n   SIM->FIXED_PORT_CLOCK_REG |= PORT%s_CLOCK_MASK;\n", instance));
//         writeConditionalEnd(cppFile);
//      }
//  
//      cppFile.write(
//         "\n"+
//         "   for (const PinInit *p=pinInit; p<(pinInit+(sizeof(pinInit)/sizeof(pinInit[0]))); p++) {\n"+   
//         "      *(p->pcr) = p->pcrValue;\n"+ 
//         "   }\n"
//      );
//      cppFile.write("}\n");
//      writeConditionalEnd(cppFile);
//   }
   
//   /**
//    * Write conditional macro guard for function declaration or definition
//    * <pre>
//    * e.g. #<b>if</b> (PTD5_SIG_SEL == 0)
//    * or   #<b>elif</b> (PTD5_SIG_SEL == 0)
//    * </pre>
//    * 
//    * @param pinTemplate
//    * @param mappedFunction
//    * @param file
//    * @param guardWritten     If true, indicates that an elif clause should be written
//    * 
//    * @return Indicates if guard was written (and hence closing macro needs to be written)
//    * 
//    * @throws IOException
//    */
//   private boolean writeFunctionSelectionGuardMacro(PeripheralTemplateInformation pinTemplate, MappingInfo mappedFunction, BufferedWriter file, boolean guardWritten) throws IOException {
//      final String format = "%s == %s";
//      String pinName = mappedFunction.pin.getName();
//
//      if (mappedFunction.mux == MuxSelection.fixed) {
//         // Don't guard fixed selections
//         return false;
//      }
//      if (!pinTemplate.fInstanceWriter.useGuard()) {
//         // Don't use guard
//         return false;
//      }
//      writeConditional(file, String.format(format, pinName+"_SIG_SEL", Integer.toString(mappedFunction.mux.value)), guardWritten);
//      return true;
//   }
//   
//   /**
//    * Write conditional macro guard for function declaration or definition
//    * <pre>
//    * e.g. #<b>if</b> (PTD5_SIG_SEL == 0)
//    * </pre>
//    * 
//    * @param pinTemplate
//    * @param mappedFunction
//    * @param file
//    * 
//    * @return Indicates if guard was written (and hence closing macro needs to be written)
//    * 
//    * @throws IOException
//    */
//   private boolean writeFunctionSelectionGuardMacro(PeripheralTemplateInformation pinTemplate, MappingInfo mappedFunction, BufferedWriter file) throws IOException {
//      return writeFunctionSelectionGuardMacro(pinTemplate, mappedFunction, file, false);
//   }
//   /**                    
//    * Write CPP file      
//    *                     
//    * @param cppFile      
//    * @throws Exception 
//    */                    
//   private void writeGpioCppFile(BufferedWriter cppFile) throws Exception {
//    DocumentUtilities.writeCppFilePreamble(
//            cppFile, 
//            gpioBaseFileName+".cpp", 
//            gpioCppFileName, 
//            "Pin declarations for "+deviceName+", generated from "+sourceName+"\n" +
//            "Devices   " + deviceNames.toString() + "\n" +
//            "Reference " + referenceManual.toString());
//      writeHeaderFileInclude(cppFile, "gpio.h");
//      cppFile.write("\n");
//
//      writeOpenNamespace(cppFile, NAME_SPACE);
//      writePinMappingFunction(cppFile);
//      writeCppFilePostAmple();
//      writeCloseNamespace(cppFile, NAME_SPACE);
//   }
//
   
   public static void initialiseTemplates(DeviceInfo factory) {
      dmaInfoList = new ArrayList<DmaInfo>();
      
//      macroAliases = new HashSet<String>();
      
      /*
       * Set up templates
       */
      for (char suffix='A'; suffix<='G'; suffix++) {
         factory.createPeripheralTemplateInformation(
               "Gpio"+suffix, "PORT"+suffix, 
               "^\\s*(GPIO)("+suffix+")_(\\d+)\\s*$",
               new WriterForDigitalIO(factory.getDeviceFamily()));
      }
//      for (char suffix='A'; suffix<='G'; suffix++) {
//         new FunctionTemplateInformation(
//               "Gpio"+suffix, "PORT"+suffix, "Port_Group",  "Port Definitions",               
//               "Information required to manipulate PORT PCRs & associated GPIOs", 
//               null,
//               new WriterForPort(deviceIsMKE));
//      }
      
      if (factory.getDeviceFamily() == DeviceFamily.mk) {
         for (char suffix='0'; suffix<='3'; suffix++) {
            factory.createPeripheralTemplateInformation(
                  "Adc"+suffix, "ADC"+suffix,
                  "(ADC)("+suffix+")_(SE\\d+)b?",
                  new WriterForAnalogueIO(factory.getDeviceFamily()));
            factory.createPeripheralTemplateInformation(
                  "Adc"+suffix+"a", "ADC"+suffix,
                  "(ADC)("+suffix+")_(SE\\d+)a",
                  new WriterForAnalogueIO(factory.getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            factory.createPeripheralTemplateInformation(
                  "Cmp"+suffix, "CMP"+suffix,  
                  "(CMP)("+suffix+")_(IN\\d)",
                  new WriterForCmp(factory.getDeviceFamily()));
         }
         factory.createPeripheralTemplateInformation(
               "DmaMux0", "DMAMUX0",  
               null,
               new WriterForDmaMux(factory.getDeviceFamily()));
         for (char suffix='0'; suffix<='3'; suffix++) {
            factory.createPeripheralTemplateInformation(
                  "Ftm"+suffix, "FTM"+suffix, 
                  "(FTM)("+suffix+")_(CH\\d+|QD_PH[A|B]|FLT\\d|CLKIN\\d)",
                  new WriterForPwmIO_FTM(factory.getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            factory.createPeripheralTemplateInformation(
                  "I2c"+suffix, "I2C"+suffix,  
                  "(I2C)("+suffix+")_(SCL|SDA)",
                  new WriterForI2c(factory.getDeviceFamily()));
         }
         factory.createPeripheralTemplateInformation(
               "Lptmr0", "LPTMR0",  
               "(LPTMR)(0)_(ALT\\d+)",
               new WriterForLptmr(factory.getDeviceFamily()));
         factory.createPeripheralTemplateInformation(
               "Pit", "PIT",  
               "(PIT)()(\\d+)",
               new WriterForPit(factory.getDeviceFamily()));
         factory.createPeripheralTemplateInformation(
               "Llwu", "LLWU",  
               "(LLWU)()_(P\\d+)",
               new WriterForLlwu(factory.getDeviceFamily()));
         for (char suffix='0'; suffix<='3'; suffix++) {
            factory.createPeripheralTemplateInformation(
                  "Spi"+suffix, "SPI"+suffix,  
                  "(SPI)("+suffix+")_(SCK|SIN|SOUT|MISO|MOSI|PCS\\d+)",
                  new WriterForSpi(factory.getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            factory.createPeripheralTemplateInformation(
                  "Tpm"+suffix, "TPM"+suffix,  
                  "(TPM)("+suffix+")_(CH\\d+|QD_PH[A|B])",
                  new WriterForPwmIO_TPM(factory.getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            factory.createPeripheralTemplateInformation(
                  "Tsi"+suffix, "TSI"+suffix,  
                  "(TSI)("+suffix+")_(CH\\d+)",
                  new WriterForTsi(factory.getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='5'; suffix++) {
            factory.createPeripheralTemplateInformation(
                  "Uart"+suffix, "UART"+suffix,  
                  "(UART)("+suffix+")_(TX|RX|CTS_b|RTS_b|COL_b)",
                  new WriterForUart(factory.getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='5'; suffix++) {
            factory.createPeripheralTemplateInformation(
                  "Lpuart"+suffix, "LPUART"+suffix,  
                  "(LPUART)("+suffix+")_(TX|RX|CTS_b|RTS_b)",
                  new WriterForLpuart(factory.getDeviceFamily()));
         }
         factory.createPeripheralTemplateInformation(
               "Vref", "VREF",  
               "(VREF)()_(OUT)",
               new WriterForVref(factory.getDeviceFamily()));
         
         factory.createMiscellaneousPeripheralTemplateInformation(factory.getDeviceFamily());
      }
   }
   
   /**
    * Writes pin mapping header file
    * 
    * @param writer Header file to write to
    * 
    * @throws Exception
    */
   public void writeXmlFile(BufferedWriter writer, DeviceInfo deviceInfomation) throws Exception {
      XmlDocumentUtilities documentUtilities = new XmlDocumentUtilities(writer);
      documentUtilities.writeXmlFilePreamble(
            xmlFileName, 
            DeviceInfo.DTD_FILE, 
            "Generated from "+sourceName);

      documentUtilities.openTag("root");
      documentUtilities.writeAttribute("version", DeviceInfo.VERSION);
      
      documentUtilities.openTag("family");
      documentUtilities.writeAttribute("name", deviceInfomation.getDeviceName());
      for (String key:fDeviceInfo.getDevices().keySet()) {
         DeviceInformation deviceInfo = fDeviceInfo.findDevice(key);
         documentUtilities.openTag("device");
         documentUtilities.writeAttribute("name",     deviceInfo.getName());
         documentUtilities.writeAttribute("manual",   deviceInfo.getManual());
         documentUtilities.writeAttribute("package",  deviceInfo.getPackage().getName());
         documentUtilities.closeTag();
      }
      documentUtilities.closeTag();
      
      writePins(documentUtilities);
      writePackages(documentUtilities);
      writePeripheralInformationTables(documentUtilities);
      
//      writePeripheralSignalMappings(documentUtilities);
//
//      writePinDefines(writer);
//      writeClockMacros(writer);

//
////      writeHeaderFileInclude(writer, "gpio_defs.h");
//      
//      writeDeclarations(writer);
//      
//      writeDmaMuxInfo(writer);
      
//      writeHeaderFilePostamble(writer, pinMappingBaseFileName+".h");
      documentUtilities.closeTag();
   }

   private void writeCppHeaderFile(BufferedWriter cppFile, DeviceInfo deviceInfo) {
      // TODO Auto-generated method stub
      
   }
   
   /**
    * Process file
    * 
    * @param filePath
    * @throws IOException 
    * @throws Exception
    */
   public void writeXMLFile(Path cppFilePath, DeviceInfo deviceInfo) throws IOException {

      fDeviceInfo = deviceInfo;
      fXmlFilename = cppFilePath.getFileName().toString();
      
      BufferedWriter cppFile = Files.newBufferedWriter(cppFilePath, StandardCharsets.UTF_8);
      writeCppHeaderFile(cppFile, deviceInfo);
      cppFile.close();
   }

}
