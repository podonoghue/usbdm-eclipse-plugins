package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of LLWU
 */
public class WriterForLlwu extends PeripheralWithState {

   public WriterForLlwu(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can create type declarations for signals belonging to this peripheral
      super.setCanCreateSignalType(true);
   }

   @Override
   public String getTitle() {
      return "Low-leakage Wake-up Unit";
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p = Pattern.compile("P(\\d+)");
      Matcher m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      throw new RuntimeException("Signal does not match expected pattern " + function.getSignalName());
   }
   
   static final String PIN_FORMAT   = "   %-50s = %-13s %s\n";
   
   /**
    * Generate set of enum symbols for comparator inputs that are mapped to pins.
    * 
    * <pre>
    *   // Mapped pins
    *   LlwuPin_Pta4         = LlwuPin_3,    ///< Mapped pin PTA4
    *   LlwuPin_Ptb0         = LlwuPin_5,    ///< Mapped pin PTB0
    *   LlwuPin_Ptc1         = LlwuPin_6,    ///< Mapped pin PTC1
    * </pre> 
    * 
    * @param documentUtilities
    * @throws IOException
    */
   @Override
   protected void writeDeclarations() {
      
      super.writeDeclarations();
      
      String enumName    = getClassName()+"Pin";
      String commentRoot = "///< ";
      ArrayList<InfoTable> signalTables = getSignalTables();
      HashSet<String> usedIdentifiers = new HashSet<String>();
      
      StringBuffer sb = new StringBuffer();
      for (InfoTable signalTable:signalTables) {
         int index = -1;
         for (Signal signal:signalTable.table) {
            index++;
            if (signal == null) {
               continue;
            }
            if (signal == Signal.DISABLED_SIGNAL) {
               continue;
            }
            MappingInfo mappingInfo = signal.getFirstMappedPinInformation();
            Pin pin = mappingInfo.getPin();
            String pinName = enumName+"_"+prettyPinName(pin.getName());
            String mapName = enumName+"_"+index;
            if (pin == Pin.UNASSIGNED_PIN) {
               continue;
            }
            String comment = pin.getName();
            String location = pin.getLocation();
            if ((location != null) && !location.isBlank()) {
               comment = comment+" ("+location+")";
            }
            if (mappingInfo.getMux() == MuxSelection.fixed) {
               // Fixed pin mapping
               comment = commentRoot+"Fixed pin  "+comment;
            }
            else {
               comment = commentRoot+"Mapped pin "+comment;
            }
            if (comment.isBlank() ) {
               continue;
            }
            // Found fixed or mapped pin
            sb.append(String.format(PIN_FORMAT, pinName, mapName+',', comment));
            
            String cIdentifier = signal.getCodeIdentifier();
            if (!cIdentifier.isBlank()) {
               cIdentifier = makeCTypeIdentifier(enumName+"_"+cIdentifier);
               String userComment = signal.getUserDescription();
               if (!userComment.isBlank()) {
                  comment = "///< " + userComment;
               }
               boolean inUse = !usedIdentifiers.add(cIdentifier);
               if (inUse) {
                  cIdentifier = "// "+cIdentifier; 
               }
               sb.append(String.format(PIN_FORMAT, cIdentifier, mapName+",", comment));
            }
         }
      }
      StringVariable llwuPinInputsVar = new StringVariable("Input Pin Mapping", makeKey("InputPinMapping"));
      llwuPinInputsVar.setValue(sb.toString());
      llwuPinInputsVar.setDerived(true);
      fDeviceInfo.addOrReplaceVariable(llwuPinInputsVar.getKey(), llwuPinInputsVar);

      String devices = getParam("device_list");
      if (devices != null) {
         enumName    = getClassName()+"Peripheral";
         sb = new StringBuffer();
         String[] deviceList = devices.split(";");
         int peripheralCount = 0;
         for (String deviceNames:deviceList) {
            if (!deviceNames.isBlank()) {
               for (String deviceName:deviceNames.split("/")) {
                  String mapName    = enumName+"_"+peripheralCount;
                  String moduleName = makeCTypeIdentifier(enumName+"_"+prettyPinName(deviceName));
                  boolean inUse = !usedIdentifiers.add(moduleName);
                  if (inUse) {
                     moduleName = "// "+moduleName; 
                  }
                  sb.append(String.format(PIN_FORMAT,  moduleName, mapName+",", ""));
                  
                  Peripheral peripheral = fDeviceInfo.getPeripherals().get(deviceName);
                  if (peripheral != null) {
                     String identifier = peripheral.getCodeIdentifier();
                     if (!identifier.isBlank()) {
                        identifier = makeCTypeIdentifier(enumName+"_"+identifier);
                        inUse = !usedIdentifiers.add(identifier);
                        if (inUse) {
                           identifier = "// "+identifier; 
                        }
                        String description = peripheral.getUserDescription();
                        if (!description.isBlank()) {
                           description = "///< "+description;
                        }
                        sb.append(String.format(PIN_FORMAT,  identifier, mapName+",", description));
                     }
                  }
               
                  
               }
            }
            peripheralCount++;
         }
         StringVariable llwuModuleInputsVar = new StringVariable("Input Pin Mapping", makeKey("InputModuleMapping"));
         llwuModuleInputsVar.setValue(sb.toString());
         llwuModuleInputsVar.setDerived(true);
         fDeviceInfo.addOrReplaceVariable(llwuModuleInputsVar.getKey(), llwuModuleInputsVar);
      }
   }

}