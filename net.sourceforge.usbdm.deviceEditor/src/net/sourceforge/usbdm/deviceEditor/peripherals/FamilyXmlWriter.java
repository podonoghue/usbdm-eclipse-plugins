package net.sourceforge.usbdm.deviceEditor.peripherals;

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
import net.sourceforge.usbdm.deviceEditor.information.DevicePackage;
import net.sourceforge.usbdm.deviceEditor.information.DeviceVariantInformation;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.parsers.XmlDocumentUtilities;
import net.sourceforge.usbdm.jni.UsbdmException;

public class FamilyXmlWriter {
   
   /** The parsed information */
   private final DeviceInfo fDeviceInfo;

   /**
    * 
    * @param deviceInfomation
    * 
    * @throws IOException
    * @throws UsbdmException
    */
   public FamilyXmlWriter(DeviceInfo deviceInfomation) throws IOException, UsbdmException {
      fDeviceInfo = deviceInfomation;
   }
   
   @SuppressWarnings("unused")
   private void writeSignals(XmlDocumentUtilities documentUtilities) throws IOException {
      documentUtilities.openTag("signals");
      for (String signalName:fDeviceInfo.getSignals().keySet()) {
         Signal signal = fDeviceInfo.getSignals().get(signalName);
         documentUtilities.openTag("signal");
         documentUtilities.writeAttribute("name", signal.getName());
         documentUtilities.writeAttribute("peripheral", signal.getPeripheral().getName());
         documentUtilities.closeTag();
      }
      documentUtilities.closeTag();
   }
   
   /**
    * Writes XML describing how peripheral signals are mapped to a pin
    * e.g.<pre>
    *   &lt;pin name=="PTD7"&gt;
    *      &lt;mux sel="mux1" signal="GPIOD_7" /&gt;
    *      &lt;mux sel="mux2" signal="CMT_IRO" /&gt;
    *      &lt;reset sel="Disabled" /&gt;
    *      &lt;default sel="mux1" /&gt;
    *   &lt;/pin&gt;
    * </pre>
    * 
    * @param documentUtilities   Where to write
    * @param pin                 Pin to write definitions for
    * 
    * @throws IOException
    */
   private void writePin(XmlDocumentUtilities documentUtilities, Pin pin) throws IOException {
      documentUtilities.openTag("pin");

      Map<MuxSelection, MappingInfo>  mappingInfo  = pin.getMappableSignals();

      Set<MuxSelection> sortedSelectionIndexes = mappingInfo.keySet();

      boolean isFixed = false;

      // Construct list of alternatives
      StringBuffer alternativeHint = new StringBuffer();
      for (MuxSelection selection:mappingInfo.keySet()) {
         if (selection == MuxSelection.fixed) {
            isFixed = true;
         }
         MappingInfo mInfo = mappingInfo.get(selection);
         StringBuffer name = new StringBuffer();
         name.append(mInfo.getSignalNames());
         if (alternativeHint.length() != 0) {
            alternativeHint.append(", ");
         }
         alternativeHint.append(name);
      }
      documentUtilities.writeAttribute("name", pin.getName());
      if (isFixed) {
         documentUtilities.writeAttribute("isFixed", "true");
      }
      for (MuxSelection selection:sortedSelectionIndexes) {
         MappingInfo mInfo = mappingInfo.get(selection);
         StringBuffer name = new StringBuffer();
         name.append(mInfo.getSignalNames());

         for (Signal fn:mInfo.getSignals()) {
            documentUtilities.openTag("mux");
            documentUtilities.writeAttribute("sel", selection.name());
            documentUtilities.writeAttribute("signal", fn.getName());
            documentUtilities.closeTag();
         }
      }
      documentUtilities.openTag("reset");
      documentUtilities.writeAttribute("sel", pin.getResetValue().name());
      documentUtilities.closeTag();

//      documentUtilities.openTag("default");
//      documentUtilities.writeAttribute("sel", pin.getDefaultValue().name());
//      documentUtilities.closeTag();
      documentUtilities.closeTag();
   }

   /**
    * Writes XML describing how peripheral signals are mapped to all pins
    * e.g.<pre>
    * &ltpins&gt;
    *   &lt;pin name=="PTD7"&gt;
    *      &lt;mux sel="mux1" signal="GPIOD_7" /&gt;
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

      HashMap<String,ArrayList<Pin>> categories = new HashMap<String,ArrayList<Pin>>();
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
         ArrayList<Pin> category = categories.get(categoryTitle);
         if (category == null) {
            category = new ArrayList<Pin>();
            categories.put(categoryTitle, category);
            categoryTitles.add(categoryTitle);
         }
         category.add(fDeviceInfo.findPin(name));
      }
      for (String p:categoryTitles) {
         ArrayList<Pin> category = categories.get(p);
         if (category != null) {
            for (Pin pinInformation:category) {
               writePin(documentUtilities, pinInformation);
            }
         }
      }
      documentUtilities.closeTag();
   }

   /**
    * Write XML for peripherals
    * 
    * @param documentUtilities
    * @throws IOException
    * @throws UsbdmException
    */
   void writePeripherals(XmlDocumentUtilities documentUtilities) throws IOException, UsbdmException {
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
    * @throws Exception
    */
   public void writeXmlFile(Path xmlFilePath) throws Exception {
      String xmlFilename = xmlFilePath.getFileName().toString();
      BufferedWriter writer = Files.newBufferedWriter(xmlFilePath, StandardCharsets.UTF_8);
      XmlDocumentUtilities documentUtilities = new XmlDocumentUtilities(writer);
      documentUtilities.writeXmlFilePreamble(
            xmlFilename,
            DeviceInfo.DTD_FILE,
            "Generated from "+ fDeviceInfo.getSourceFilename());

      documentUtilities.openTag("root");
      documentUtilities.writeAttribute("version", DeviceInfo.VERSION);

      documentUtilities.openTag("family");
      documentUtilities.writeAttribute("name", fDeviceInfo.getDeviceSubFamily());
      
//      documentUtilities.writeParam(null,"/_deviceFamily","StringVariable",fDeviceInfo.getDeviceSubFamily());
      
      for (String key:fDeviceInfo.getDeviceVariants().keySet()) {
         DeviceVariantInformation deviceInformation = fDeviceInfo.findVariant(key);
         deviceInformation.writeXml(documentUtilities);
      }
      documentUtilities.closeTag();

      writePeripherals(documentUtilities);
//      writeSignals(documentUtilities);
      writePins(documentUtilities);
      writePackages(documentUtilities);
      
      documentUtilities.closeTag();
      writer.close();
   }
}
