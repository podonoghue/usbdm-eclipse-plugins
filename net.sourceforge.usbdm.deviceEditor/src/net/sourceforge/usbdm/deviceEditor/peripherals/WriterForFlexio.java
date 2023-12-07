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
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp.HardwareDeclarationInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of FlexIO
 */
public class WriterForFlexio extends PeripheralWithState {

   public WriterForFlexio(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Flexible I/O";
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p;
      Matcher m;
      
      int offset = 0;
      p = Pattern.compile("D(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 32;

      final String signalNames[] = {};
      return offset+super.getSignalIndex(function, signalNames);
   }
   

   static final String PIN_FORMAT   = "   %-25s = %-8s %s\n";
   
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
   protected void writeDeclarations(HardwareDeclarationInfo hardwareDeclarationInfo) {
      
      super.writeDeclarations(hardwareDeclarationInfo);
      
      String enumName    = "FlexioPinSel_";
      String commentRoot = "///< ";
      ArrayList<InfoTable> signalTables = getSignalTables();
      HashSet<String> usedIdentifiers = new HashSet<String>();

      StringBuffer inputsStringBuilder       = new StringBuffer();
      for (InfoTable signalTable:signalTables) {
         int index = -1;
         for (Signal signal:signalTable.table) {
            index++;
            if (signal == null) {
               continue;
            }
            MappingInfo mappingInfo = signal.getFirstMappedPinInformation();
            if (mappingInfo == MappingInfo.UNASSIGNED_MAPPING) {
               continue;
            }
            Pin pin = mappingInfo.getPin();
            if (pin == Pin.UNASSIGNED_PIN) {
               continue;
            }
            if (!pin.isAvailableInPackage()) {
               continue;
            }
            String trailingComment  = pin.getNameWithLocation();
            String pinName = enumName+prettyPinName(pin.getName());
            String cIdentifier = makeCTypeIdentifier(signal.getCodeIdentifier());
            String inputIdentifier = "";
            if (!cIdentifier.isBlank()) {
               inputIdentifier =  enumName+cIdentifier;
//               String type = String.format("const %s<%d>", getClassBaseName()+getInstance()+"::"+"Pin", index);
//               writeVariableDeclaration("", signal.getUserDescription(), cIdentifier, type, pin.getLocation());
            }
            String mapName = "FlexioPinSel_"+index;
            if (mappingInfo.getMux() == MuxSelection.fixed) {
               // Fixed pin mapping
               trailingComment = commentRoot+"Fixed pin  "+trailingComment;
               boolean inUse = !usedIdentifiers.add(pinName);
               if (inUse) {
                  pinName = "// "+pinName;
               }
               inputsStringBuilder.append(String.format(PIN_FORMAT, pinName, mapName+",", trailingComment));
               if (!inputIdentifier.isBlank()) {
                  inUse = !usedIdentifiers.add(inputIdentifier);
                  if (inUse) {
                     inputIdentifier = "// "+inputIdentifier;
                  }
                  inputsStringBuilder.append(String.format(PIN_FORMAT, inputIdentifier, mapName+",", trailingComment));
               }
            }
            else if (mappingInfo.isSelected()) {
               trailingComment = commentRoot+"Mapped pin "+trailingComment;
               boolean inUse = !usedIdentifiers.add(pinName);
               if (inUse) {
                  pinName = "// "+pinName;
               }
               inputsStringBuilder.append(String.format(PIN_FORMAT, pinName, mapName+",", trailingComment));
               if (!inputIdentifier.isBlank()) {
                  inUse = !usedIdentifiers.add(inputIdentifier);
                  if (inUse) {
                     inputIdentifier = "// "+inputIdentifier;
                  }
                  inputsStringBuilder.append(String.format(PIN_FORMAT, inputIdentifier, mapName+",", trailingComment));
               }
            }
         }
         // Create or replace Pin List variable as needed
         fDeviceInfo.addOrUpdateStringVariable("PinList", makeKey("PinList"), inputsStringBuilder.toString(), true);
      }
   }
   
}