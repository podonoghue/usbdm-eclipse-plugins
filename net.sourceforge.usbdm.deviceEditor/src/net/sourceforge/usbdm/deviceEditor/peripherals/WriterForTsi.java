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
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp.HardwareDeclarationInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of TSI
 */
public class WriterForTsi extends PeripheralWithState {
   // Number of inputs each comparator multiplexor has
   static final int NUMBER_OF_INPUTS = 15;
   static final String PIN_FORMAT    = "constexpr %s %-25s = %-14s %s\n";


   public WriterForTsi(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType = true;
   }

   /**
    * Replaces simple patterns in format<br>
    *    <li>%i = index in Info table i.e. pin # or similar
    *    <li>%p = associated pin e.g. Ptb0
    *    <br>
    * Example:<br>
    * <pre>
    *     constexpr TsiInput TsiInput_%p             = TsiInput_%i;
    * </pre>
    * <pre>
    *     constexpr TsiInput TsiInput_Ptb0             = TsiInput_0;
    * </pre>
    * 
    * @param index   Index in info table
    * @param format  Format string for substitutions
    * 
    * @return Formatted enum
    */
   String formatEnum(String format, int index, Pin pin) {
      format = format.replace("%i", Integer.toString(index));
      format = format.replace("%p", prettyPinName(pin.getName()));
      String[] parts = format.split("=");
      if (parts.length==0) {
         return format;
      }
      if (parts.length>2) {
         return "Illegal format string in Tsi";
      }
      return String.format("%-30s, = %-30s", parts[0].trim(), parts[1].trim());
   }
   
   /**
    * Generate set of enum symbols and Types for TSI inputs that are mapped to pins.
    * 
    * <pre>
    *   // In tsi.h
    *   TsiInput_Ptb18      = TsiInput_11, ///< Mapped pin PTB18 (p41)
    *   TsiInput_Electrode1 = TsiInput_11, ///< Mapped pin PTB18 (p41)
    * 
    *   // In hardware.h
    *   /// User comment
    *   typedef const Tsi0::Pin&lt;TsiInput_Ptb18&gt;  Electrode1;  // PTB18 (p41)
    * </pre>
    * 
    * @param documentUtilities
    * @throws IOException
    */
   @Override
   protected void writeDeclarations(HardwareDeclarationInfo hardwareDeclarationInfo) {

      super.writeDeclarations(hardwareDeclarationInfo);
      
      StringVariable tsiEnumPatternVar = (StringVariable) safeGetVariable("/TSI/tsiEnumType");
      final String enumName;
      if (tsiEnumPatternVar != null) {
         enumName = tsiEnumPatternVar.getValueAsString();
      }
      else {
         enumName = "TsiChannel";
      }
      String commentRoot = "///< ";
      ArrayList<InfoTable> signalTables = getSignalTables();
      HashSet<String> usedIdentifiers = new HashSet<String>();

      StringBuffer inputsStringBuilder       = new StringBuffer();
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
            String pinName = enumName+"_"+prettyPinName(pin.getName());
            String inputIdentifier = "";
            String mapName = enumName+"_"+index;
            
            
            String cIdentifier = makeCTypeIdentifier(signal.getCodeIdentifier().trim());
            if (!cIdentifier.isBlank()) {
               inputIdentifier =  enumName+"_"+cIdentifier;
               String type = String.format("%s<%s>", getClassBaseName()+getInstance()+"::"+"Pin", pinName);
               writeTypeDeclaration(hardwareDeclarationInfo, "", signal.getUserDescription(), cIdentifier, type, trailingComment);
            }
            if (mappingInfo.getMux() == MuxSelection.fixed) {
               // Fixed pin mapping
               trailingComment = commentRoot+"Fixed pin  "+trailingComment;
               boolean inUse = !usedIdentifiers.add(pinName);
               if (inUse) {
                  pinName = "// "+pinName;
               }
               inputsStringBuilder.append(String.format(PIN_FORMAT, enumName, pinName, mapName+";", trailingComment));
               if (!inputIdentifier.isBlank()) {
                  inUse = !usedIdentifiers.add(inputIdentifier);
                  if (inUse) {
                     inputIdentifier = "// "+inputIdentifier;
                  }
                  inputsStringBuilder.append(String.format(PIN_FORMAT, enumName, inputIdentifier, mapName+";", trailingComment));
               }
            }
            else if (mappingInfo.isSelected()) {
               trailingComment = commentRoot+"Mapped pin "+trailingComment;
               boolean inUse = !usedIdentifiers.add(pinName);
               if (inUse) {
                  pinName = "// "+pinName;
               }
               inputsStringBuilder.append(String.format(PIN_FORMAT, enumName, pinName, mapName+";", trailingComment));
               if (!inputIdentifier.isBlank()) {
                  inUse = !usedIdentifiers.add(inputIdentifier);
                  if (inUse) {
                     inputIdentifier = "// "+inputIdentifier;
                  }
                  inputsStringBuilder.append(String.format(PIN_FORMAT, enumName, inputIdentifier, mapName+";", trailingComment));
               }
            }
//            if (mappingInfo.getMux() == MuxSelection.fixed) {
//               // Fixed pin mapping
//               trailingComment = commentRoot+"Fixed pin  "+trailingComment;
//               boolean inUse = !usedIdentifiers.add(pinName);
//               if (inUse) {
//                  pinName = "// "+pinName;
//               }
//               inputsStringBuilder.append(String.format(PIN_FORMAT, pinName, mapName+";", trailingComment));
//               if (!inputIdentifier.isBlank()) {
//                  inUse = !usedIdentifiers.add(inputIdentifier);
//                  if (inUse) {
//                     inputIdentifier = "// "+inputIdentifier;
//                  }
//                  inputsStringBuilder.append(String.format(PIN_FORMAT, inputIdentifier, mapName+";", trailingComment));
//               }
//            }
//            else if (mappingInfo.isSelected()) {
//               trailingComment = commentRoot+"Mapped pin "+trailingComment;
//               boolean inUse = !usedIdentifiers.add(pinName);
//               if (inUse) {
//                  pinName = "// "+pinName;
//               }
//               inputsStringBuilder.append(String.format(PIN_FORMAT, pinName, mapName+";", trailingComment));
//               if (!inputIdentifier.isBlank()) {
//                  inUse = !usedIdentifiers.add(inputIdentifier);
//                  if (inUse) {
//                     inputIdentifier = "// "+inputIdentifier;
//                  }
//                  inputsStringBuilder.append(String.format(PIN_FORMAT, inputIdentifier, mapName+";", trailingComment));
//               }
//            }
         }
         // Create or replace Input Mapping variable as needed
         fDeviceInfo.addOrUpdateStringVariable("Input Mapping", makeKey("InputMapping"), inputsStringBuilder.toString(), true);
      }
   }

   @Override
   public String getTitle() {
      return "Touch Sense Interface";
   }
   
   @Override
   public int getSignalIndex(Signal function) {
      Pattern p = Pattern.compile("CH(\\d+)");
      Matcher m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      throw new RuntimeException("Signal does not match expected pattern " + function.getSignalName());
   }
}