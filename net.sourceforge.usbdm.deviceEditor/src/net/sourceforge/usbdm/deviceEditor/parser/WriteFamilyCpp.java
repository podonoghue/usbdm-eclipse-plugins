package net.sourceforge.usbdm.deviceEditor.parser;

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
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInformation;
import net.sourceforge.usbdm.deviceEditor.information.DmaInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralTemplateInformation;
import net.sourceforge.usbdm.deviceEditor.information.PinInformation;

public class WriteFamilyCpp {

   private DeviceInfo fDeviceInfo;

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

   /** Current device */
   private DeviceInformation fDeviceInformation;

   /*
    * Macros =============================================================================================
    */
   HashSet<String> macros = new HashSet<String>();
   
   /**
    * Records MACROs used
    * 
    * @param MACRO to record
    * 
    * @return true=> new (acceptable) MACRO
    */
   public boolean addMacroAlias(String macro) {
      if (macros.contains(macro)) {
         return false;
      }
      macros.add(macro);
      return true;
   }

   private static class NameAttribute implements WizardAttribute {
      private String fName;

      NameAttribute(String name) {
         fName = name;
      }

      @Override
      public String getAttributeString() {
         return "<name=" + fName + ">";
      }
   }

   private static class ValidatorAttribute implements WizardAttribute {
      private String fValidatorId;

      ValidatorAttribute(String validatorId) {
         fValidatorId = validatorId;
      }

      @Override
      public String getAttributeString() {
         return "<validate=" + fValidatorId + ">";
      }
   }

   private static class SelectionAttribute implements WizardAttribute {
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
    * Writes enumeration describing DMA slot use
    * 
    * e.g.<pre>
    * enum {
    *    DMA0_SLOT_Disabled                   = 0,
    *    DMA0_SLOT_UART0_Receive              = 2,
    *    DMA0_SLOT_UART0_Transmit             = 3,
    *    ...
    * };
    * </pre>
    * @param writer
    * @throws IOException
    */
   private void writeDmaMuxInfo(DocumentUtilities writer) throws IOException {
      if (fDeviceInfo.getDmaList().size() == 0) {
         return;
      }
      writer.write("\n");
      writer.writeOpenNamespace(DeviceInfo.NAME_SPACE);
      writer.writeStartGroup("DMA_Group", "Direct Memory Access (DMA)", "Support for DMA operations");
      for (int instance=0; instance<4; instance++) {
         boolean noneWritten = true;
         for (DmaInfo item:fDeviceInfo.getDmaList()) {
            if (item.dmaInstance == instance) {
               if (noneWritten) {
                  writer.write("enum {\n");
                  noneWritten = false;
               }
               writer.write(String.format("   %-35s  = %d,\n", "DMA"+item.dmaInstance+"_SLOT_"+item.dmaSource, item.dmaChannelNumber));
            }
         }
         if (!noneWritten) {
            writer.write("};\n");
         }
      }
      writer.writeCloseGroup();
      writer.writeCloseNamespace();
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
    * @param headerFile Header file to write to
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
    * Writes code to select which peripheral signal is mapped to a pin
    * e.g.<pre>
    * // <b><i>PTD1</b></i> Pin Mapping
    * //   &lt;o&gt; <b><i>PTD1</b></i> (Alias:<b><i>D13</b></i>) [<b><i>ADC0_SE5b, GPIOD_1, SPI0_SCK</b></i>] &lt;name=<b><i>PTD1_SIG_SEL</b></i>&gt;
    * //   &lt;i&gt; Selects which peripheral signal is mapped to <b><i>PTD1</b></i> pin
    * //     &lt;0=&gt; <b><i>ADC0_SE5b</b></i>&lt;selection=<b><i>ADC0_SE5b_PIN_SEL,PTD1</b></i>&gt;
    * //     &lt;1=&gt; <b><i>GPIOD_1</b></i>&lt;selection=<b><i>GPIOD_1_PIN_SEL,PTD1</b></i>&gt;
    * //     &lt;2=&gt; <b><i>SPI0_SCK</b></i>&lt;selection=<b><i>SPI0_SCK_PIN_SEL,PTD1</b></i>&gt;
    * //     &lt;0=&gt; <b><i>Default</b></i>
    * #define <b><i>PTD1_SIG_SEL</b></i> 0                   
    * </pre>
    *  
    * @param pinInformation  Peripheral function to write definitions for
    * @param writer          Where to write
    * @param categoryTitle 
    * @throws IOException 
    */
   private void writePinMapping(PinInformation pinInformation, DocumentUtilities writer, String categoryTitle) throws IOException {

      String pinName = getPinNameWithLocation(pinInformation);
      if (pinName == null) {
         // Not mapped in this device
         return;
      }

      Map<MuxSelection, MappingInfo>  mappingInfo  = pinInformation.getMappedFunctions();

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
         MappingInfo mInfo = mappingInfo.get(selection);
         StringBuffer name = new StringBuffer();
         name.append(mInfo.getFunctionList());
         if (selection == pinInformation.getDefaultValue()) {
            defaultSelection = selection;
         }
         if (alternativeHint.length() != 0) {
            alternativeHint.append(", ");
         }
         alternativeHint.append(name);
      }
      WizardAttribute[] attributes = {new NameAttribute(pinInformation.getName()+"_SIG_SEL"), (sortedSelectionIndexes.length <= 1)?constantAttribute:null};
      String hint;
      if (sortedSelectionIndexes.length <= 1) {
         hint = String.format("%s has no pin-mapping hardware", pinInformation.getName());
      }
      else {
         hint = String.format("Selects which peripheral signal is mapped to %s pin", pinInformation.getName());
      }
      writer.writeWizardOptionSelectionPreamble(
            "Signal mapping for " + pinInformation.getName() + " pin",
            0,
            attributes,
            pinName,
            //            String.format("%s%s", pinInformation.getName(), alias),
            hint,
            alternativeHint.toString());
      for (MuxSelection selection:sortedSelectionIndexes) {
         MappingInfo mInfo = mappingInfo.get(selection);
         StringBuffer name = new StringBuffer();
         ArrayList<SelectionAttribute> selectionAttribute = new ArrayList<SelectionAttribute>();
         name.append(mInfo.getFunctionList());
         for (PeripheralFunction fn:mInfo.getFunctions()) {
            String targetName = getPinNameWithLocation(pinInformation);
            if (selection == MuxSelection.reset) {
               targetName += " (reset default)";
            }
            if (fn.isIncluded()) {
               selectionAttribute.add(new SelectionAttribute(fn.getName()+"_PIN_SEL", targetName));
            }
         }
         if (sortedSelectionIndexes.length <= 1) {
            name.append(" (fixed)");
         }
         else if (selection == MuxSelection.reset) {
            name.append(" (reset default)");
         }
         writer.writeWizardOptionSelectionEnty(
               
               Integer.toString(selection.value), 
               name.toString(), 
               selectionAttribute.toArray(new SelectionAttribute[selectionAttribute.size()]));
      }
      if (sortedSelectionIndexes.length >= 2) {
         writer.writeWizardDefaultSelectionEnty(Integer.toString(defaultSelection.value));
      }
      writer.writeMacroDefinition(pinInformation.getName()+"_SIG_SEL", Integer.toString(defaultSelection.value));
      writer.write("\n");
   }

   /**
    * Writes pin-mapping selection code for all peripheral functions
    *  
    * @param writer  Header file to write result
    * @throws IOException 
    * 
    * @throws Exception 
    */
   private void writePinMappings(DocumentUtilities writer) throws IOException  {
      WizardAttribute[] attributes = {new NameAttribute("MAP_BY_PIN")};

      writer.writeWizardConditionalSectionOpen(
            "Pin peripheral signal mapping", 
            0, 
            attributes, 
            "Mapping by Pin", 
            "This allows the mapping of peripheral functions to pins\n"+
            "to be controlled by individual pin");
      writer.writeWizardOptionSelectionEnty("0", "Disabled");
      writer.writeWizardOptionSelectionEnty("1", "Enabled");
      writer.writeMacroDefinition("MAP_BY_PIN_ENABLED", "1");
      writer.write("\n");
      HashMap<String,ArrayList<PinInformation>> categories = new HashMap<String,ArrayList<PinInformation>>();
      class Pair {
         public final String namePattern;
         public final String titlePattern;

         Pair(String n, String t) {
            namePattern    = n;
            titlePattern   = t;
         }
      };
      // Group the pins into Miscellaneous and Port groups
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
      for (String categoryTitle:categoryTitles) {
         ArrayList<PinInformation> category = categories.get(categoryTitle);
         if (category != null) {
            writer.writeWizardSectionOpen(categoryTitle);
            for (PinInformation pinInformation:category) {
               writePinMapping(pinInformation, writer, categoryTitle);
            }
            writer.writeWizardSectionClose();
         }
      }
      writer.writeWizardConditionalSectionClose();
   }

   private static class ConstantAttribute implements WizardAttribute {

      @Override
      public String getAttributeString() {
         return "<constant>";
      }
   };

   /** A constant attribute for convenience */
   private static final ConstantAttribute   constantAttribute      = new ConstantAttribute();

   /**
    * Gets pin name with appended location<br>
    * If no location a null is returned
    * 
    * @param pinInformation
    * @return name with aliases e.g. <b><i>PTE0 (Alias:D14)</b></i>
    */
   private String getPinNameWithLocation(PinInformation pinInformation) {
      String pinName = pinInformation.getName();

      String location = fDeviceInformation.getPackage().getLocation(pinInformation.getName());
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
    * Writes code to select which pin a peripheral function is mapped to
    *  
    * @param documentUtilities     Header file to write result
    * @param function   The function to process
    * @throws IOException 
    * 
    * @throws Exception
    */
   private void writePeripheralSignalMapping(DocumentUtilities writer, PeripheralFunction function) throws IOException {
      if (!function.isIncluded()) {
         return;
      }
      ArrayList<MappingInfo> mappingInfos = fDeviceInfo.getPins(function);
      Collections.sort(mappingInfos, new Comparator<MappingInfo>() {

         @Override
         public int compare(MappingInfo o1, MappingInfo o2) {
            return o1.getMux().value - o2.getMux().value;
         }
      });
      if ((mappingInfos == null) || (mappingInfos.size() == 0)) {
         throw new RuntimeException("Signal must be mapped to at least one pin");
      }
      boolean peripheralMappedInThisPackage = false;
      for (MappingInfo mappingInfo:mappingInfos) {
         if (fDeviceInformation.getPackage().getLocation(mappingInfo.getPin()) != null) {
            peripheralMappedInThisPackage = true;
            break;
         }
      }
      if (!peripheralMappedInThisPackage) {
         return;
      }
      boolean noChoices =  ((mappingInfos.size() == 1) && (mappingInfos.get(0).getMux() == MuxSelection.fixed));

      // Create list of choices as string and determine default selection (if any)
      int defaultSelection = 0;
      String choices = null;
      if (mappingInfos != null) {
         int selection = 0;
         if (!noChoices) {
            selection++;
         }
         for (MappingInfo mappingInfo:mappingInfos) {
            if (fDeviceInformation.getPackage().getLocation(mappingInfo.getPin().getName()) == null) {
               continue;
            }
            if (mappingInfo.getMux() == MuxSelection.disabled) {
               continue;
            }
            if ((mappingInfo.getMux() == MuxSelection.reset) && (mappingInfo.getPin().getDefaultValue() == null)) {
               if (defaultSelection == 0) {
                  defaultSelection = selection;
               }
               //               continue;
            }
            if (mappingInfo.getMux() == MuxSelection.fixed) {
               defaultSelection = selection;
            }
            if (mappingInfo.getPin().getDefaultValue() == mappingInfo.getMux()) {
               defaultSelection = selection;
            }
            if (mappingInfo.getMux() != MuxSelection.reset) {
               if (choices == null) {
                  choices = mappingInfo.getPin().getName();
               }
               else {
                  choices += ", " + mappingInfo.getPin().getName();
               }
            }
            selection++;
         }
      }
      if (choices != null) {
         choices = " [" + choices + "]";
      }

      WizardAttribute[] attributes = {new NameAttribute(function.getName()+"_PIN_SEL"), noChoices?constantAttribute:null};
      writer.writeWizardOptionSelectionPreamble(
            "Pin Mapping for " + function.getName() + " signal",
            0,
            attributes,
            String.format("%s", function.getName()),
            String.format("Shows which pin %s is mapped to", function.getName()),
            choices);

      int selection = 0;
      if (!noChoices) {
         writer.writeWizardOptionSelectionEnty(Integer.toString(selection++), "Disabled");
      }
      if ((mappingInfos == null) || (mappingInfos.size() == 0)) {
         writer.writeWizardOptionSelectionEnty(Integer.toString(-1), function.getName());
         writer.writeMacroDefinition(function.getName()+"_PIN_SEL", Integer.toString(-1));
      }
      else {
         for (MappingInfo mappingInfo:mappingInfos) {
            if (mappingInfo.getMux() == MuxSelection.disabled) {
               continue;
            }
            String pinName = getPinNameWithLocation(mappingInfo.getPin());
            if (pinName == null) {
               continue;
            }
            if (mappingInfo.getMux() == MuxSelection.reset) {
               pinName += " (reset default)";
               //            continue;
            }
            String seletionTag = mappingInfo.getFunctionList();
            if (mappingInfo.getMux() == MuxSelection.reset) {
               seletionTag += " (reset default)";
            }
            WizardAttribute[] functionAttributes = {new SelectionAttribute(mappingInfo.getPin().getName()+"_SIG_SEL", seletionTag)};
            writer.writeWizardOptionSelectionEnty(Integer.toString(selection++), pinName, functionAttributes);
         }

         writer.writeWizardOptionSelectionEnty(Integer.toString(defaultSelection), "Default", null);
         writer.writeMacroDefinition(function.getName()+"_PIN_SEL", Integer.toString(defaultSelection));
      }
      writer.write("\n");
   }

   /**
    * Writes code to control what pin peripheral functions are mapped to
    *  
    * @param writer  Header file to write result
    * @throws IOException 
    * 
    * @throws Exception 
    */
   private void writePeripheralSignalMappings(DocumentUtilities writer) throws IOException {
      WizardAttribute[] attributes = {new NameAttribute("MAP_BY_FUNCTION"), new ConstantAttribute()};

      writer.writeWizardConditionalSectionOpen(
            "Pin peripheral signal mapping", 
            0, 
            attributes, 
            "Mapping by Peripheral Function", 
            "This allows the mapping of peripheral functions to pins\n"+
                  "to be controlled by peripheral function.\n" +
            "This option is active when Mapping by Pin is disabled");
      writer.writeWizardOptionSelectionEnty("0", "Disabled");
      writer.writeWizardOptionSelectionEnty("1", "Enabled");
      writer.writeMacroDefinition("MAP_BY_FUNCTION_ENABLED", "0");
      writer.write("\n");

      HashMap<String,ArrayList<PeripheralFunction>> categories = new HashMap<String,ArrayList<PeripheralFunction>>();
      class Pair {
         public final String namePattern;
         public final String titlePattern;

         Pair(String p, String t) {
            namePattern = p;
            titlePattern   = t;
         }
      };
      final String UNMATCHED_NAME = "Miscellaneous";
      Pair[] functionPatterns = {
            new Pair("(ADC\\d+).*",             "Analogue to Digital ($1)"), 
            new Pair("(VREF\\d*).*",            "Voltage Reference ($1)"), 
            new Pair("(A?CMP\\d+).*",           "Analogue Comparator ($1)"), 
            new Pair("(FTM\\d+).*",             "FlexTimer ($1)"), 
            new Pair("(TPM\\d+).*",             "Timer ($1)"), 
            new Pair("(LCD_P)?(\\d+).*",        "Liquid Crystal Display"), 
            new Pair("(GPIO[A-Z]+).*",          "General Purpose I/O ($1)"), 
            new Pair("(I2C\\d+).*",             "Inter-Integrated Circuit ($1)"), 
            new Pair("(I2S\\d+).*",             "Integrated Interchip Sound ($1)"), 
            new Pair("(LLWU\\d*).*",            "Low-Leakage Wake-up Unit ($1)"), 
            new Pair("(SPI\\d+).*",             "Serial Peripheral Interface ($1)"), 
            new Pair("(TSI\\d+).*",             "Touch Sense Interface ($1)"), 
            new Pair("(LPTMR|LPTIM)(\\d+)*.*",  "Low Power Timer ($1)"), 
            new Pair("(UART\\d+).*",            "Universal Asynchronous Rx/Tx ($1)"), 
            new Pair("(PXBAR).*",               "($1)"), 
            new Pair("(QT).*",                  "($1)"), 
            new Pair("(SCI\\d+).*",             "Serial Communication Interface ($1)"), 
            new Pair("(SDAD)(M|P)\\d+.*",       "Sigma-delta ADC ($1)"), 
            new Pair("(LPUART\\d+).*",          "Low Power UART ($1)"), 
            new Pair("(DAC\\d*).*",             "Digital to Analogue ($1)"), 
            new Pair("(PDB\\d*).*",             "Programmable Delay Block ($1)"), 
            new Pair("(CAN\\d*).*",             "CAN Bus ($1)"), 
            new Pair("(ENET\\d*).*",            "Ethernet ($1)"), 
            new Pair("(MII\\d*).*",             "Ethernet ($1)"), 
            new Pair("(RMII\\d*).*",            "Ethernet ($1)"), 
            new Pair("(SDHC\\d*).*",            "Secured Digital Host Controller ($1)"), 
            new Pair("(CMT\\d*).*",             "Carrier Modulator Transmitter ($1)"), 
            new Pair("(EWM).*",                 "External Watchdog Monitor ($1)"), 
            new Pair("E?XTAL.*",                "Clock and Timing"),
            new Pair("(JTAG|SWD|NMI|TRACE|RESET).*",  "Debug and Control"),
            new Pair("(FB_).*",                 "Flexbus"),
            new Pair("(FXIO\\d+).*",            "Flexbus ($1)"),
            new Pair(".*(USB).*",               "Universal Serial Bus"), 
            new Pair(".*(CLK|EXTRG).*",         "Clock and Timing"),
      };

      ArrayList<String> categoryTitles = new ArrayList<String>();

      // Add catch-all "Miscellaneous" category
      categoryTitles.add(UNMATCHED_NAME);
      categories.put(UNMATCHED_NAME, new ArrayList<PeripheralFunction>());

      for (String name:fDeviceInfo.getPeripheralFunctions().keySet()) {
         PeripheralFunction peripheralFunction = fDeviceInfo.findPeripheralFunction(name);
         if (!peripheralFunction.isIncluded()) {
            continue;
         }
         String categoryTitle = UNMATCHED_NAME;
         for (Pair pair:functionPatterns) {
            Pattern p = Pattern.compile(pair.namePattern);
            Matcher m = p.matcher(name);
            if (m.matches()) {
               categoryTitle = m.replaceAll(pair.titlePattern);
               break;
            }
         }
         ArrayList<PeripheralFunction> category = categories.get(categoryTitle);
         if (category == null) {
            categoryTitles.add(categoryTitle);
            category = new ArrayList<PeripheralFunction>();
            categories.put(categoryTitle, category);
         }
         category.add(peripheralFunction);
      }
      for (String categoryTitle:categoryTitles) {
         ArrayList<PeripheralFunction> category = categories.get(categoryTitle);
         if (category.size()>0) {
            writer.writeWizardSectionOpen(categoryTitle);
            for (PeripheralFunction peripheralFunction:category) {
               writePeripheralSignalMapping(writer, peripheralFunction);
            }
            writer.writeWizardSectionClose();
         }
      }
      writer.writeWizardConditionalSectionClose();
   }

   /**
    * Write configuration wizard information e.g.
    * <pre>
    * // &lth> Clock settings for FTM0
    * //
    * // FTM0_SC.CLKS ================================
    * //   &lt;o> FTM0_SC.CLKS Clock source 
    * //   &lt;i> Selects the clock source for the FTM0 module. [FTM0_SC.CLKS]
    * //     &lt;0=> Disabled
    * //     &lt;1=> System clock
    * //     &lt;2=> Fixed frequency clock
    * //     &lt;3=> External clock
    * //     &lt;1=> Default
    * 
    * // FTM0_SC.PS ================================
    * //   &lt;o1> FTM0_SC.PS Clock prescaler 
    * //   &lt;i> Selects the prescaler for the FTM0 module. [FTM0_SC.PS]
    * //     &lt;0=> Divide by 1
    * //     &lt;1=> Divide by 2
    * //     &lt;2=> Divide by 4
    * //     &lt;3=> Divide by 8
    * //     &lt;4=> Divide by 16
    * //     &lt;5=> Divide by 32
    * //     &lt;6=> Divide by 64
    * //     &lt;7=> Divide by 128
    * //     &lt;0=> Default
    * namespace USBDM {
    * constexpr uint32_t FTM0_SC = (FTM_SC_CLKS(0x1)|FTM_SC_PS(0x6));
    * }
    * </pre>
    * @param headerFile    Where to write
    * 
    * @throws IOException
    */
   private void writeDeviceWizards(DocumentUtilities writer) throws IOException {
      for (String key:fDeviceInfo.getPeripheralNames()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(key);
         peripheral.writeWizard(writer);
      }
   }

   /**
    * Writes Pin Function options e.g.
    * 
    * <pre>
    * // Pin mapping Options
    * //
    * //   &lt;q&gt; Map pins 
    * //   &lt;i&gt; Selects whether pin mappings are done when individual
    * //   &lt;i&gt; peripherals are configured or during reset initialisation.
    * //     &lt;0=&gt; Pins mapped on demand
    * //     &lt;1=&gt; Pin mapping on reset
    * #define DO_MAP_PINS_ON_RESET 0
    * </pre>
    * 
    * @param headerFile    Where to write
    * 
    * @throws IOException
    */
   private void writePinMappingOptions(DocumentUtilities writer) throws IOException {
      writer.writeWizardBinaryOptionSelectionPreamble(
            String.format("Pin mapping Options\n//"), 
            0,
            false,
            String.format("Map pins"),
            String.format("Selects whether pin mappings are done when individual\n" +
                  "peripherals are configured or during reset initialisation." ));
      writer.writeWizardOptionSelectionEnty("0", "Pins mapped on demand");
      writer.writeWizardOptionSelectionEnty("1", "Pin mapping on reset");
      writer.writeMacroDefinition("DO_MAP_PINS_ON_RESET", "0");
      writer.write("\n");
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
//      for (String peripheralName:fDeviceInfo.getPeripherals().keySet()) {
//         Peripheral peripheral = fDeviceInfo.findPeripheral(peripheralName);
//         if (peripheral.getClockReg() == null) {
//            continue;
//         }
//         if (peripheral.getName().matches("PORT[A-Z]")) {
//            if (portClockRegisterValue == null) {
//               portClockRegisterValue = peripheral.getClockReg();
//            }
//            else if (!portClockRegisterValue.equals(peripheral.getClockReg())) {
//               throw new RuntimeException(
//                     String.format("Multiple port clock registers existing=%s, new=%s", portClockRegisterValue, peripheral.getClockReg()));
//            }
//         }
//         writer.writeMacroDefinition(peripheral.getName()+"_CLOCK_REG",  peripheral.getClockReg());
//         writer.writeMacroDefinition(peripheral.getName()+"_CLOCK_MASK", peripheral.getClockMask());
//      }
      writer.writeMacroDefinition("PORT_CLOCK_REG", portClockRegisterValue);
      writer.write("\n");
   }

   /**
    * Write file validators e.g.
    * 
    * <pre>
    *    //================================
    *    // Validators
    *    // &lt;validate=<b><i>net.sourceforge.usbdm.annotationEditor.validators.PinMappingValidator</b></i>&gt;
    * </pre>
    * 
    * @param writer        Where to write
    * @param validators    Validators to write
    * 
    * @throws IOException
    */
   private void writeValidators(DocumentUtilities writer, ValidatorAttribute[] validators) throws IOException {
      final String format = 
            "//================================\n"
                  + "// Validators\n";

      writer.write(format);

      for (ValidatorAttribute validatorAttribute: validators) {
         if (validatorAttribute != null) {
            writer.write("// " + validatorAttribute.getAttributeString() + "\n");
         }
      }
      writer.write("\n");
   }
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
   /**
    * Write an external declaration for a simple peripheral (GPIO,ADC,PWM) e.g.
    * 
    * <pre>
    * <b>#if</b> <i>PTC18_SEL</i> == 1
    * using <i>gpio_A5</i>  = const USBDM::<i>GpioC&lt;18&gt;</i>;
    * <b>#endif</b>
    * </pre>
    * 
    * @param writer         Template information
    * @param mappedFunction   Information about the pin and function being declared
    * @param fnIndex          Index into list of functions mapped to pin
    * @param pWriter   Where to write
    * 
    * @throws Exception 
    */
   private void writeExternDeclaration(WriterBase writer, MappingInfo mappedFunction, int fnIndex, DocumentUtilities pWriter) throws IOException {
      String definition = writer.getDefinition(mappedFunction, fnIndex);
      if (definition == null) {
         return;
      }
      boolean guardWritten = false;

      String signalName = writer.getInstanceName(mappedFunction, fnIndex);
      if (writer.useAliases(mappedFunction.getPin())) {
         String locations = fDeviceInformation.getPackage().getLocation(mappedFunction.getPin());
         if (locations != null) {
            for (String location:locations.split("/")) {
               if (!location.equalsIgnoreCase(mappedFunction.getPin().getName())) {
                  String aliasName = writer.getAliasName(signalName, location);
                  if (aliasName!= null) {
                     String declaration = writer.getAlias(aliasName, mappedFunction, fnIndex);
                     if (declaration != null) {
                        if (writer.useGuard() && !guardWritten) {
                           guardWritten = writeFunctionSelectionGuardMacro(writer, mappedFunction, pWriter, guardWritten);
                        }
                        if (!addMacroAlias(aliasName)) {
                           // Comment out repeated aliases
                           pWriter.write("//");
                        }
                        pWriter.write(declaration);
                     }
                  }
               }
            }
         }
      }
      pWriter.writeConditionalEnd(guardWritten);
   }

   /**
    * Write all Peripheral Information Classes<br>
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
    * @param pinMappingHeaderFile Where to write
    * 
    * @throws IOException 
    */
   private void writePeripheralInformationClasses(DocumentUtilities writer) throws IOException {
      writer.writeOpenNamespace(DeviceInfo.NAME_SPACE);
      writer.writeBanner("Peripheral Pin Tables");

      writer.writeStartGroup( 
            "PeripheralPinTables", 
            "Peripheral Information Classes", 
            "Provides instance specific information about a peripheral");

      for (String key:fDeviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(key);
         peripheral.writeInfoClass(fDeviceInformation, writer);
      }
      writer.writeCloseGroup();
      writer.writeCloseNamespace();
      writer.write("\n");
   }

   /**
    * Write GPIO Header file.<br>
    * This mostly contains the extern declarations for peripherals
    * 
    * <pre>
    * <b>#if</b> <i>PTC18_SEL</i> == 1
    * using <i>gpio_A5</i>  = const USBDM::<i>GpioC&lt;18&gt;</i>;
    * <b>#endif</b>
    * </pre>
    * 
    * @param gpioHeaderFile Where to write
    * 
    * @throws Exception 
    */
   private void writeDeclarations(DocumentUtilities writer) throws IOException {

      writer.write("\n");
      writer.writeOpenNamespace(DeviceInfo.NAME_SPACE);

      PeripheralTemplateInformation peripheralTemplateInformation = null;
      boolean groupDone = false;
      for (String key:fDeviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(key);
         if (peripheralTemplateInformation != peripheral.getPeripheralTemplate()) {
            peripheralTemplateInformation = peripheral.getPeripheralTemplate();
            if (groupDone) {
               // Terminate previous group
               writer.writeCloseGroup();
            }
            groupDone = false;
         }
         String declaration = peripheral.getDeclarations();
         if (declaration != null) {
            writer.writeStartGroup(peripheral.getWriter());
            groupDone = true;
            writer.write(declaration);
         }
         for (String pinName:fDeviceInfo.getPins().keySet()) {
            PinInformation pin = fDeviceInfo.getPins().get(pinName);
            Map<MuxSelection, MappingInfo> mappedFunctions = pin.getMappedFunctions();
            if (mappedFunctions == null) {
               continue;
            }
            for (MuxSelection index:mappedFunctions.keySet()) {
               if (index == MuxSelection.reset) {
                  continue;
               }
               MappingInfo mappedFunction = mappedFunctions.get(index);
               for (int fnIndex=0; fnIndex<mappedFunction.getFunctions().size(); fnIndex++) {
                  PeripheralFunction function = mappedFunction.getFunctions().get(fnIndex);
                  if (function.getPeripheral() == peripheral) {
                  //                     if (!groupDone) {
                  //                        writer.writeStartGroup(gpioHeaderFile, peripheralTemplateInformation);
                  //                        groupDone = true;
                  //                        String t = peripheralTemplateInformation.getInstanceWriter().getTemplate();
                  //                        if (t != null) {
                  //                           writer.write(t);
                  //                        }
                     writeExternDeclaration(peripheral.getWriter(), mappedFunction, fnIndex, writer);
                  }
               }
            }
         }
      }
      if (groupDone) {
         // Terminate last group
         writer.writeCloseGroup();
      }

      writer.writeConditionalStart("DO_MAP_PINS_ON_RESET>0");
      writer.writeDocBanner("Used to configure pin-mapping before 1st use of peripherals");
      writer.write("extern void usbdm_PinMapping();\n");
      writer.writeConditionalEnd();
      writer.writeCloseNamespace();

      //      for (PeripheralTemplateInformation pinTemplate:fDeviceInfo.getFunctionTemplateList()) {
      //         boolean groupDone = false;
      //         for (String pinName:fDeviceInfo.getPins().keySet()) {
      //            PinInformation pinInfo = fDeviceInfo.findPin(pinName);
      //            Map<MuxSelection, MappingInfo> mappedFunctions = pinInfo.getMappedFunctions();
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
      //               for (int fnIndex=0; fnIndex<mappedFunction.getFunctions().size(); fnIndex++) {
      //                  PeripheralFunction function = mappedFunction.getFunctions().get(fnIndex);
      //                  if (pinTemplate.matches(function)) {
      //                     if (!groupDone) {
      //                        writer.writeStartGroup(gpioHeaderFile, pinTemplate);
      //                        groupDone = true;
      //                        String t = pinTemplate.getInstanceWriter().getTemplate();
      //                        if (t != null) {
      //                           writer.write(t);
      //                        }
      //                     }
      //                     writeExternDeclaration(pinTemplate, mappedFunction, fnIndex, gpioHeaderFile);
      //                  }
      //               }
      //            }
      //         }
      //         if (groupDone) {
      //            writer.writeCloseGroup(gpioHeaderFile);
      //         }
      //      }
   }

   /**
    * Write Pin Mapping function to CPP file
    * 
    * @param cppFile    File to write to
    * 
    * @throws IOException
    */
   private void writePinMappingFunction(DocumentUtilities writer) throws IOException {

      writer.writeConditionalStart("DO_MAP_PINS_ON_RESET>0");
      writer.write(
            "struct PinInit {\n"+
                  "   uint32_t pcrValue;\n"+
                  "   uint32_t volatile *pcr;\n"+
                  "};\n\n"+
                  "static constexpr PinInit pinInit[] = {\n"
            );

      for (String pinName:fDeviceInfo.getPins().keySet()) {
         if (fDeviceInformation.getPackage().getLocation(pinName) == null) {
            // Discard pin that is not available on this package
            continue;
         }
         Pattern p = Pattern.compile("PT([A-Z]+)([0-9]+)");
         Matcher m = p.matcher(pinName);
         if (m.matches()) {
            String instance = m.replaceAll("$1");
            String signal   = m.replaceAll("$2");
            writer.writeConditionalStart(String.format("%s_SIG_SEL>=0", pinName, pinName));
            writer.write(String.format("   { PORT_PCR_MUX(%s_SIG_SEL)|%s::DEFAULT_PCR, &PORT%s->PCR[%s]},\n", pinName, DeviceInfo.NAME_SPACE, instance, signal));
            writer.writeConditionalEnd();
         }
      }
      writer.write("};\n\n");

      writer.write(
            "/**\n" + 
                  " * Used to configure pin-mapping before 1st use of peripherals\n" + 
                  " */\n" + 
                  "void usbdm_PinMapping() {\n"
            );

      boolean firstExpression = true;
      String currentBasename = null;
      String  instance = "X";
      int conditionCounter = 0;
      for (String pinName:fDeviceInfo.getPins().keySet()) {
         if (fDeviceInformation.getPackage().getLocation(pinName) == null) {
            // Discard pin that is not available on this package
            continue;
         }
         Pattern p = Pattern.compile("(PT([A-Z]))[0-9]+");
         Matcher m = p.matcher(pinName);
         if (m.matches()) {
            String basename = m.replaceAll("$1");
            if (!basename.equals(currentBasename)) {
               if (!firstExpression) {
                  writer.write(String.format("\n\n   SIM->FIXED_PORT_CLOCK_REG |= PORT%s_CLOCK_MASK;\n", instance));
                  writer.writeConditionalEnd();
               }
               currentBasename = basename;
               writer.write("#if ");
               firstExpression = false;
               instance = m.replaceAll("$2");
            }
            else {
               writer.write(" || ");
               if (++conditionCounter>=4) {
                  writer.write("\\\n    ");
                  conditionCounter = 0;
               }
            }
            writer.write(String.format("(%s_SIG_SEL>=0)", pinName));
         }
      }
      if (!firstExpression) {
         writer.write(String.format("\n   SIM->FIXED_PORT_CLOCK_REG |= PORT%s_CLOCK_MASK;\n", instance));
         writer.writeConditionalEnd();
      }

      writer.write(
            "\n"+
                  "   for (const PinInit *p=pinInit; p<(pinInit+(sizeof(pinInit)/sizeof(pinInit[0]))); p++) {\n"+   
                  "      *(p->pcr) = p->pcrValue;\n"+ 
                  "   }\n"
            );
      writer.write("}\n");
      writer.writeConditionalEnd();
   }

   /**
    * Write conditional macro guard for function declaration or definition
    * <pre>
    * e.g. #<b>if</b> (PTD5_SIG_SEL == 0)
    * or   #<b>elif</b> (PTD5_SIG_SEL == 0)
    * </pre>
    * 
    * @param writer
    * @param mappedFunction
    * @param file
    * @param guardWritten     If true, indicates that an elif clause should be written
    * 
    * @return Indicates if guard was written (and hence closing macro needs to be written)
    * 
    * @throws IOException
    */
   private boolean writeFunctionSelectionGuardMacro(WriterBase writer, MappingInfo mappedFunction, DocumentUtilities pWriter, boolean guardWritten) throws IOException {
      final String format = "%s == %s";
      String pinName = mappedFunction.getPin().getName();

      if (mappedFunction.getMux() == MuxSelection.fixed) {
         // Don't guard fixed selections
         return false;
      }
//      if (!pinTemplate.getInstanceWriter().useGuard()) {
//         // Don't use guard
//         return false;
//      }
      pWriter.writeConditional(String.format(format, pinName+"_SIG_SEL", Integer.toString(mappedFunction.getMux().value)), guardWritten);
      return true;
   }

   /**
    * Writes pin mapping header file
    * 
    * @param headerFile Header file to write to
    * 
    * @throws IOException 
    */
   private void writePinMappingHeaderFile(Path filePath) throws IOException {
      
      macros = new HashSet<String>();

      String headerFilename = pinMappingBaseFileName+"-"+fDeviceInformation.getName() + ".h";

      Path cppFilePath = filePath.resolve("Project_Headers").resolve(headerFilename);
      BufferedWriter headerFile = Files.newBufferedWriter(cppFilePath, StandardCharsets.UTF_8);
      DocumentUtilities writer = new DocumentUtilities(headerFile);
      
      writer.writeHeaderFilePreamble(
            pinMappingBaseFileName+".h", headerFilename, 
            DeviceInfo.VERSION, 
            "Pin declarations for "+fDeviceInfo.getDeviceName()+", generated from "+fDeviceInfo.getSourceFilename());

      writer.writeSystemHeaderFileInclude("stddef.h");
      writer.writeHeaderFileInclude("derivative.h");
      headerFile.write("\n");

      writer.writeWizardMarker(headerFile);
      ValidatorAttribute[] attributes = {
            new ValidatorAttribute("net.sourceforge.usbdm.annotationEditor.validators.PinMappingValidator")
      };
      writeValidators(writer, attributes);

      writeDeviceWizards(writer);
      writePinMappingOptions(writer);
      writePinMappings(writer);
      writePeripheralSignalMappings(writer);
      writer.writeEndWizardMarker();

      writePinDefines(writer);
      writeClockMacros(writer);
      writePeripheralInformationClasses(writer);

      writer.writeHeaderFileInclude("gpio_defs.h");

      writeDeclarations(writer);

      writeDmaMuxInfo(writer);

      writer.writeHeaderFilePostamble(pinMappingBaseFileName+".h");

      writer.close();
   }

   /**                    
    * Write CPP file      
    *                     
    * @param cppFile      
    * 
    * @throws IOException 
    */                    
   private void writePinMappingCppFile(Path filePath) throws IOException {
      String fCppFilename = gpioBaseFileName+"-"+fDeviceInformation.getName() + ".cpp";

      Path cppFilePath = filePath.resolve("Sources").resolve(fCppFilename);
      BufferedWriter cppFile = Files.newBufferedWriter(cppFilePath, StandardCharsets.UTF_8);
      DocumentUtilities writer = new DocumentUtilities(cppFile);
      
      writer.writeCppFilePreamble(
            gpioBaseFileName+".cpp", fCppFilename, 
            DeviceInfo.VERSION, 
            "Pin declarations for "+fDeviceInfo.getDeviceName()+", generated from "+fDeviceInfo.getSourceFilename());


      writer.writeHeaderFileInclude("gpio.h");
      writer.write("\n");

      writer.writeOpenNamespace(DeviceInfo.NAME_SPACE);
      writePinMappingFunction(writer);
      writer.writeCppFilePostAmble();
      writer.writeCloseNamespace();
      writer.close();
   }

   /**
    * Process file
    * 
    * @param  directory    Parent director
    * @param  deviceInfo   Device information to print to CPP files  
    * 
    * @throws IOException 
    */
   public void writeCppFiles(Path directory, DeviceInfo deviceInfo, String deviceName) throws IOException {

      fDeviceInfo = deviceInfo;
      fDeviceInformation = fDeviceInfo.findDevice(deviceName);
      writePinMappingHeaderFile(directory);
      writePinMappingCppFile(directory);
   }
   /**
    * Process file
    * 
    * @param  directory    Parent director
    * @param  deviceInfo   Device information to print to CPP files  
    * 
    * @throws IOException 
    */
   public void writeCppFiles(Path directory, DeviceInfo deviceInfo) throws IOException {

      fDeviceInfo = deviceInfo;

      for (String key:fDeviceInfo.getDevices().keySet()) {
         writeCppFiles(directory, deviceInfo, key);
      }
   }
}
