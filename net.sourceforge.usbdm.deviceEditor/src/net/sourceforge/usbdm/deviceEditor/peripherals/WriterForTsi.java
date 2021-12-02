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
 * Class encapsulating the code for writing an instance of TSI
 */
public class WriterForTsi extends PeripheralWithState {
   // Number of inputs each comparator multiplexor has
   static final int NUMBER_OF_INPUTS = 15;
   static final String PIN_FORMAT   = "   %-25s = %-8s %s\n";


   public WriterForTsi(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can create type declarations for signals belonging to this peripheral (actually enums)
      super.setCanCreateSignalType(true);
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
   protected void writeDeclarations() {

      super.writeDeclarations();
      
      String enumName    = "TsiInput_";
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
            
            String comment = pin.getName();
            String location = pin.getLocation();
            if ((location != null) && !location.isBlank()) {
               comment = comment+" ("+location+")";
            }
            
            String pinName = enumName+prettyPinName(pin.getName());
            String cIdentifier = signal.getCodeIdentifier().trim();
            String inputIdentifier = "";
            String mapName = "TsiInput_"+index;
            if (!cIdentifier.isBlank()) {
               cIdentifier     = makeCTypeIdentifier(cIdentifier);
               inputIdentifier =  enumName+cIdentifier;
               String type = String.format("const %s<%s>", getClassBaseName()+getInstance()+"::"+"Pin", pinName);
               writeTypeDeclaration("", signal.getUserDescription(), cIdentifier, type, comment);
            }
            if (mappingInfo.getMux() == MuxSelection.fixed) {
               // Fixed pin mapping
               comment = commentRoot+"Fixed pin  "+comment;
               boolean inUse = !usedIdentifiers.add(pinName);
               if (inUse) {
                  pinName = "// "+pinName; 
               }
               inputsStringBuilder.append(String.format(PIN_FORMAT, pinName, mapName+",", comment));
               if (!inputIdentifier.isBlank()) {
                  inUse = !usedIdentifiers.add(inputIdentifier);
                  if (inUse) {
                     inputIdentifier = "// "+inputIdentifier; 
                  }
                  inputsStringBuilder.append(String.format(PIN_FORMAT, inputIdentifier, mapName+",", comment));
               }
            }
            else if (mappingInfo.isSelected()) {
               comment = commentRoot+"Mapped pin "+comment;
               boolean inUse = !usedIdentifiers.add(pinName);
               if (inUse) {
                  pinName = "// "+pinName; 
               }
               inputsStringBuilder.append(String.format(PIN_FORMAT, pinName, mapName+",", comment));
               if (!inputIdentifier.isBlank()) {
                  inUse = !usedIdentifiers.add(inputIdentifier);
                  if (inUse) {
                     inputIdentifier = "// "+inputIdentifier; 
                  }
                  inputsStringBuilder.append(String.format(PIN_FORMAT, inputIdentifier, mapName+",", comment));
               }
            }
         }
         StringVariable cmpInputsVar = new StringVariable("InputMapping", makeKey("InputMapping"));
         cmpInputsVar.setValue(inputsStringBuilder.toString());
         cmpInputsVar.setDerived(true);
         fDeviceInfo.addOrReplaceVariable(cmpInputsVar.getKey(), cmpInputsVar);
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