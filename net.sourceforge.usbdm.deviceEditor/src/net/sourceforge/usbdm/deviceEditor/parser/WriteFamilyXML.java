package net.sourceforge.usbdm.deviceEditor.parser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceVariantInformation;
import net.sourceforge.usbdm.deviceEditor.information.DevicePackage;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.deviceEditor.information.PinInformation;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;

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

      Map<MuxSelection, MappingInfo>  mappingInfo  = pinInformation.getMappedFunctions();

      Set<MuxSelection> sortedSelectionIndexes = mappingInfo.keySet();

      MuxSelection defaultSelection = MuxSelection.reset;

      // Construct list of alternatives
      StringBuffer alternativeHint = new StringBuffer();
      for (MuxSelection selection:mappingInfo.keySet()) {
         //         if (selection == MuxSelection.disabled) {
         //            // Ignore disabled entries
         //            continue;
         //         }
         //         if ((selection == MuxSelection.reset) && (sortedSelectionIndexes.size()>1)) {
         //            continue;
         //         }
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
      for (MuxSelection selection:sortedSelectionIndexes) {
         MappingInfo mInfo = mappingInfo.get(selection);
         StringBuffer name = new StringBuffer();
         name.append(mInfo.getFunctionList());

         for (PeripheralFunction fn:mInfo.getFunctions()) {
            documentUtilities.openTag("mux");
            documentUtilities.writeAttribute("sel", selection.name());
            documentUtilities.writeAttribute("function", fn.getName());
            documentUtilities.closeTag();
         }
      }
      documentUtilities.openTag("reset");
      documentUtilities.writeAttribute("sel", pinInformation.getResetValue().name());
      documentUtilities.closeTag();

      documentUtilities.openTag("default");
      documentUtilities.writeAttribute("sel", pinInformation.getDefaultValue().name());
      documentUtilities.closeTag();
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

   void writePeripherals(XmlDocumentUtilities documentUtilities) throws IOException {
      documentUtilities.openTag("peripherals");
      for (String key:fDeviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(key);
         peripheral.writeXmlInformation(documentUtilities);
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


   /**
    * Writes XML file
    * 
    * @param xmlFilePath      Path to write XML to
    * @param deviceInfomation Data to write
    * 
    * @throws IOException
    */
   public void writeXmlFile(Path xmlFilePath, DeviceInfo deviceInfomation) throws IOException {
      fDeviceInfo = deviceInfomation;

      String fXmlFilename = xmlFilePath.getFileName().toString();
      BufferedWriter writer = Files.newBufferedWriter(xmlFilePath, StandardCharsets.UTF_8);
      XmlDocumentUtilities documentUtilities = new XmlDocumentUtilities(writer);
      documentUtilities.writeXmlFilePreamble(
            fXmlFilename, 
            DeviceInfo.DTD_FILE, 
            "Generated from "+ deviceInfomation.getDeviceVariantName()+".csv");

      documentUtilities.openTag("root");
      documentUtilities.writeAttribute("version", DeviceInfo.VERSION);

      documentUtilities.openTag("family");
      documentUtilities.writeAttribute("name", deviceInfomation.getDeviceVariantName());
      for (String key:fDeviceInfo.getDeviceVariants().keySet()) {
         DeviceVariantInformation deviceInformation = fDeviceInfo.findVariant(key);
         documentUtilities.openTag("device");
         documentUtilities.writeAttribute("name",     deviceInformation.getName());
         documentUtilities.writeAttribute("manual",   deviceInformation.getManual());
         documentUtilities.writeAttribute("package",  deviceInformation.getPackage().getName());
         documentUtilities.closeTag();
      }
      documentUtilities.closeTag();

      writePins(documentUtilities);
      writePackages(documentUtilities);
      writePeripherals(documentUtilities);
      
      documentUtilities.closeTag();
      writer.close();
   }

}
