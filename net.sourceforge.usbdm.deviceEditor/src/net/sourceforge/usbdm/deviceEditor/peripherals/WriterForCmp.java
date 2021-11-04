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
 * Class encapsulating the code for writing an instance of CMP
 */
public class WriterForCmp extends PeripheralWithState {

   // Number of inputs each comparator multiplexor has
   final int NUMBER_OF_INPUTS = 8;
   
   public WriterForCmp(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Analogue Comparator";
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p = Pattern.compile("IN(\\d+)");
      Matcher m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      final String signalNames[] = {"OUT","RRT"};
      return NUMBER_OF_INPUTS+super.getSignalIndex(function, signalNames);
   }

   static final String PIN_FORMAT   = "      %-25s = %-8s %s\n";
   
   /**
    * Generate set of enum symbols for comparator inputs that are mapped to pins.
    * 
    * <pre>
    *       Input_Ptc7       = 1,       ///< Mapped pin PTC7
    *       Input_VrefOut    = 5,       ///< Fixed pin  VREF_OUT
    *       Input_Bandgap    = 6,       ///< Fixed pin  BANDGAP
    *       Input_CmpDac     = 7,       ///< Fixed pin  CMP_DAC
    * </pre> 
    * 
    * @param documentUtilities
    * @throws IOException
    */
   @Override
   protected void writeDeclarations() {
      super.writeDeclarations();
      
      String enumName    = "Input";
      String commentRoot = "///< ";
      ArrayList<InfoTable> signalTables = getSignalTables();
      HashSet<String> usedIdentifiers = new HashSet<String>();

      StringBuffer sb = new StringBuffer();
      for (InfoTable signalTable:signalTables) {
         int index = -1;
         for (Signal signal:signalTable.table) {
            index++;
            if (index >= NUMBER_OF_INPUTS) {
               break;
            }
            if (signal == null) {
               continue;
            }
            MappingInfo mappingInfo = signal.getFirstMappedPinInformation();
            Pin pin = mappingInfo.getPin();
            String pinName = enumName+"_"+prettyPinName(pin.getName());
            String userPinName = pin.getSecondaryOrPrimaryCodeIdentifier().trim();
            if (!userPinName.isBlank()) {
               userPinName =  enumName+"_"+makeCTypeIdentifier(userPinName);
            }
            int mapName = index;
            do {
               if (!pin.isAvailableInPackage()) {
                  // Discard unmapped signals on this package 
                  continue;
               }
               if (mappingInfo.getMux() == MuxSelection.unassigned) {
                  // Reset selection - ignore
                  continue;
               }
               if (mappingInfo.getMux() == MuxSelection.fixed) {
                  // Fixed pin mapping
                  String comment = commentRoot+"Fixed pin  "+pin.getName();
                  boolean inUse = !usedIdentifiers.add(pinName);
                  if (inUse) {
                     pinName = "// "+pinName; 
                  }
                  sb.append(String.format(PIN_FORMAT, pinName, mapName+",", comment));
                  if (!userPinName.isBlank()) {
                     inUse = !usedIdentifiers.add(userPinName);
                     if (inUse) {
                        userPinName = "// "+userPinName; 
                     }
                     sb.append(String.format(PIN_FORMAT, userPinName, mapName+",", comment));
                  }
                  continue;
               }
               if (mappingInfo.isSelected()) {
                  String comment = commentRoot+"Mapped pin "+pin.getName();
                  boolean inUse = !usedIdentifiers.add(pinName);
                  if (inUse) {
                     pinName = "// "+pinName; 
                  }
                  sb.append(String.format(PIN_FORMAT, pinName, mapName+",", comment));
                  if (!userPinName.isBlank()) {
                     inUse = !usedIdentifiers.add(userPinName);
                     if (inUse) {
                        userPinName = "// "+userPinName; 
                     }
                     sb.append(String.format(PIN_FORMAT, userPinName, mapName+",", comment));
                  }
               }
            } while(false);
         }
         StringVariable cmpInputsVar = new StringVariable("InputMapping", makeKey("InputMapping"));
         cmpInputsVar.setValue(sb.toString());
         cmpInputsVar.setDerived(true);
         fDeviceInfo.addOrReplaceVariable(cmpInputsVar.getKey(), cmpInputsVar);
      }
   }

}