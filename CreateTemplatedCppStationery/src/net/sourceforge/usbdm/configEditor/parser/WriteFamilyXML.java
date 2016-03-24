package net.sourceforge.usbdm.configEditor.parser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.configEditor.information.DeviceInfo;
import net.sourceforge.usbdm.configEditor.information.DeviceInformation;
import net.sourceforge.usbdm.configEditor.information.DevicePackage;
import net.sourceforge.usbdm.configEditor.information.MappingInfo;
import net.sourceforge.usbdm.configEditor.information.MuxSelection;
import net.sourceforge.usbdm.configEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.configEditor.information.PeripheralTemplateInformation;
import net.sourceforge.usbdm.configEditor.information.PinInformation;
import net.sourceforge.usbdm.configEditor.xmlParser.XmlDocumentUtilities;

public class WriteFamilyXML {

   private DeviceInfo fDeviceInfo;

   /**
    * Writes XML describing how peripheral functions are mapped to a pin
    * e.g.<pre>
    *   &lt;pin name=="PTD7"&gt;
    *      &lt;mux sel="mux1" function="GPIOD_7" /&gt;
    *      &lt;mux sel="mux2" function="CMT_IRO" /&gt;
    *      &lt;reset sel="Disabled" /&gt;
    *      &lt;default sel="mux1" /&gt;
    *   &lt;/pin&gt;
    * </pre>
    *  
    * @param documentUtilities   Where to write
    * @param pinInformation      Peripheral function to write definitions for
    * 
    * @throws IOException 
    */
   private void writePinMapping(XmlDocumentUtilities documentUtilities, PinInformation pinInformation) throws IOException {
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
    * Writes XML describing how peripheral functions are mapped to all pins
    * e.g.<pre>
    * &ltpins&gt;
    *   &lt;pin name=="PTD7"&gt;
    *      &lt;mux sel="mux1" function="GPIOD_7" /&gt;
    *      ...
    *   &lt;/pin&gt;
    *   ...
    * &lt/pins&gt;
    * </pre>
    *  
    * @param documentUtilities   Where to write
    * @throws IOException 
    */
   private void writePins(XmlDocumentUtilities documentUtilities) throws IOException {

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

   /**
    * Writes XML describing how pins are mapped to package locations
    * 
    * e.g.<pre>
    * &ltpackages&gt;
    *    &ltpackage name="BGA_121"&gt;
    *       &lt;placement pin="ADC0_DM0"      location="K2" /&gt;
    *       &lt;placement pin="ADC0_DP0"      location="K1" /&gt;
    *       ...
    *    &lt/package&gt;
    *    ...
    * &lt/packages&gt;
    * </pre>
    *  
    * @param documentUtilities   Where to write
    * @throws IOException 
    */
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
   //   /**
   //    * Process pins
   //    */
   //   static void processPins() {
   //      for (PeripheralTemplateInformation pinTemplate:fDeviceInfo.getTemplateList()) {
   //         for (String pinName:fDeviceInfo.getPins().keySet()) {
   //            PinInformation pinInfo = fDeviceInfo.findPin(pinName);
   //            Map<MuxSelection, MappingInfo> mappedFunctions = fDeviceInfo.getFunctions(pinInfo);
   //            if (mappedFunctions == null) {
   //               continue;
   //            }
   //            for (MuxSelection index:mappedFunctions.keySet()) {
   //               if (index == MuxSelection.reset) {
   //                  continue;
   //               }
   //               MappingInfo mappedFunction = mappedFunctions.get(index);
   //               for (PeripheralFunction function:mappedFunction.functions) {
   //                  if (pinTemplate.matches(function)) {
   //                     fDeviceInfo.addFunctionType(pinTemplate.getPeripheralName(), pinInfo);
   //                  }
   //               }
   //            }
   //         }
   //      }
   //   }

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
    * @throws IOException 
    * 
    * @throws Exception 
    */
   private void writePeripheralInformationTables(XmlDocumentUtilities documentUtilities) throws IOException {
      documentUtilities.openTag("peripherals");
      for (PeripheralTemplateInformation pinTemplate:fDeviceInfo.getTemplateList()) {
         pinTemplate.writePeripheralInformation(documentUtilities);
      }
      documentUtilities.closeTag();
   }

   /**
    * Writes XML file
    * 
    * @param writer Header file to write to
    * @throws IOException 
    * 
    * @throws Exception
    */
   public void writeXmlFile(Path xmlFilePath, DeviceInfo deviceInfomation) throws IOException {
      String fXmlFilename = xmlFilePath.getFileName().toString();
      BufferedWriter writer = Files.newBufferedWriter(xmlFilePath, StandardCharsets.UTF_8);
      XmlDocumentUtilities documentUtilities = new XmlDocumentUtilities(writer);
      documentUtilities.writeXmlFilePreamble(
            fXmlFilename, 
            DeviceInfo.DTD_FILE, 
            "Generated from "+ deviceInfomation.getDeviceName()+".csv");

      documentUtilities.openTag("root");
      documentUtilities.writeAttribute("version", DeviceInfo.VERSION);

      documentUtilities.openTag("family");
      documentUtilities.writeAttribute("name", deviceInfomation.getDeviceName());
      for (String key:fDeviceInfo.getDevices().keySet()) {
         DeviceInformation deviceInformation = fDeviceInfo.findDevice(key);
         documentUtilities.openTag("device");
         documentUtilities.writeAttribute("name",     deviceInformation.getName());
         documentUtilities.writeAttribute("manual",   deviceInformation.getManual());
         documentUtilities.writeAttribute("package",  deviceInformation.getPackage().getName());
         documentUtilities.closeTag();
      }
      documentUtilities.closeTag();

      writePins(documentUtilities);
      writePackages(documentUtilities);
      writePeripheralInformationTables(documentUtilities);

      documentUtilities.closeTag();
      writer.close();
   }

   /**
    * Process file
    * 
    * @param filePath
    * @throws IOException 
    * @throws Exception
    */
   public void writeXMLFile(Path xmlFilePath, DeviceInfo deviceInfo) throws IOException {

      fDeviceInfo = deviceInfo;
      
      writeXmlFile(xmlFilePath, deviceInfo);
   }

}
