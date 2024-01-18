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
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp.HardwareDeclarationInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of CMP
 */
public class WriterForCmp extends PeripheralWithState {

   // Number of inputs each comparator multiplexor has
   final int NUMBER_OF_INPUTS = 8;
   
   public WriterForCmp(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType     = true;
      fCanCreateSignalInstance = true;
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

   static final String PIN_FORMAT   = "%-50s = %-20s %s\n";
   
   /**
    * Generate set of enum symbols and Types for CMP inputs that are mapped to pins.
    * 
    * <pre>
    *   // In cmp.h
    *   Input_Ptc6                = 6,       ///< Mapped pin PTC6 (p51)
    *   Input_MyComparatorInput   = 6,       ///< Mapped pin PTC6 (p51)
    * 
    *   // In hardware.h
    *   /// User comment
    *   typedef Cmp0::Pin<Cmp0::Input_Ptc6>  MyComparatorInput;    // PTC6 (p51)
    * </pre>
    * 
    * @param documentUtilities
    * @throws IOException
    */
   @Override
   protected void writeDeclarations(HardwareDeclarationInfo hardwareDeclarationInfo) {
      
      super.writeDeclarations(hardwareDeclarationInfo);
      
      String enumNameP   = getClassName()+"InputPlus_";
      String enumNameM   = getClassName()+"InputMinus_";
      String enumNameE   = getClassName()+"InputEnable_";
      
      String commentRoot = "///< ";
      ArrayList<InfoTable> signalTables = getSignalTables();
      HashSet<String> usedIdentifiers = new HashSet<String>();

      StringBuffer inputsStringBuilder = new StringBuffer();
      inputsStringBuilder.append("\n// Pin mapping for "+getName()+"\n");
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
            if (!signal.isEnabled()) {
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
            String instanceIfUsed = ""; //getInstance();
            String trailingComment  = pin.getNameWithLocation();
            String description = signal.getUserDescription();
            String pinNameP  = enumNameP+prettyPinName(pin.getName());
            String pinNameM  = enumNameM+prettyPinName(pin.getName());
            String pinEnable = enumNameE+prettyPinName(pin.getName());
            String cIdentifier = makeCTypeIdentifier(signal.getCodeIdentifier());
            String mapNameP = getClassBaseName()+instanceIfUsed+"InputPlus_"+index;
            String mapNameM = getClassBaseName()+instanceIfUsed+"InputMinus_"+index;
            String mapPinEnable = getClassBaseName()+instanceIfUsed+"InputEnable_"+index;
            
            if (!cIdentifier.isBlank()) {
               String typeM = String.format("%s<%s>", getClassBaseName()+getInstance()+"::"+"Pin", pinNameM);
               String constTypeM = "const "+ typeM;
               if (signal.getCreateInstance()) {
                  writeVariableDeclaration(hardwareDeclarationInfo, "", description, cIdentifier, constTypeM, trailingComment);
               }
               else {
                  writeTypeDeclaration(hardwareDeclarationInfo, "", description, cIdentifier, typeM, trailingComment);
               }
            }
            boolean inUse = !usedIdentifiers.add(pinNameP);
            pinNameP = "constexpr "+getClassBaseName()+instanceIfUsed+"InputPlus   "+ pinNameP;
            if (inUse) {
               pinNameP = "// "+pinNameP;
            }
            inUse = !usedIdentifiers.add(pinNameM);
            pinNameM = "constexpr "+getClassBaseName()+instanceIfUsed+"InputMinus  "+ pinNameM;
            if (inUse) {
               pinNameM = "// "+pinNameM;
            }
            inUse = !usedIdentifiers.add(pinEnable);
            pinEnable = "constexpr "+getClassBaseName()+instanceIfUsed+"InputEnable "+ pinEnable;
            if (inUse) {
               pinEnable = "// "+pinEnable;
            }
            boolean doEnables = (safeGetVariable("/"+getName()+"/acmp_c2_acipe_present") != null);
            if (mappingInfo.getMux() == MuxSelection.fixed) {
               // Fixed pin mapping
               trailingComment = commentRoot+"Fixed pin  "+trailingComment;
               inputsStringBuilder.append(String.format(PIN_FORMAT, pinNameP, mapNameP+";", trailingComment));
               inputsStringBuilder.append(String.format(PIN_FORMAT, pinNameM, mapNameM+";", trailingComment));
               if (doEnables) {
                  inputsStringBuilder.append(String.format(PIN_FORMAT, pinEnable, mapPinEnable+";", trailingComment));
               }
//               if (!inputIdentifierP.isBlank()) {
//                  inUse = !usedIdentifiers.add(inputIdentifierP);
//                  if (inUse) {
//                     inputIdentifierP = "// "+inputIdentifierP;
//                  }
//                  inUse = !usedIdentifiers.add(inputIdentifierM);
//                  if (inUse) {
//                     inputIdentifierM = "// "+inputIdentifierM;
//                  }
//                  inputsStringBuilder.append(String.format(PIN_FORMAT, inputIdentifierP, mapNameP+";", trailingComment));
//                  inputsStringBuilder.append(String.format(PIN_FORMAT, inputIdentifierM, mapNameM+";", trailingComment));
//               }
            }
            else if (mappingInfo.isSelected()) {
               trailingComment = commentRoot+"Mapped pin "+trailingComment;
               inputsStringBuilder.append(String.format(PIN_FORMAT, pinNameP, mapNameP+";", trailingComment));
               inputsStringBuilder.append(String.format(PIN_FORMAT, pinNameM, mapNameM+";", trailingComment));
               if (doEnables) {
                  inputsStringBuilder.append(String.format(PIN_FORMAT, pinEnable, mapPinEnable+";", trailingComment));
               }
//               if (!inputIdentifierP.isBlank()) {
//                  inUse = !usedIdentifiers.add(inputIdentifierP);
//                  if (inUse) {
//                     inputIdentifierP = "// "+inputIdentifierP;
//                  }
//                  inUse = !usedIdentifiers.add(inputIdentifierM);
//                  if (inUse) {
//                     inputIdentifierM = "// "+inputIdentifierM;
//                  }
//                  inputsStringBuilder.append(String.format(PIN_FORMAT, inputIdentifierP, mapNameP+";", trailingComment));
//                  inputsStringBuilder.append(String.format(PIN_FORMAT, inputIdentifierM, mapNameM+";", trailingComment));
//               }
            }
         }
         String mappings = inputsStringBuilder.toString();
         
         // Replace or add to existing input mapping variable as needed
         String key = makeKey("/"+getBaseName()+"/InputMapping");
         if (!getInstance().equals("0")) {
            // Keep existing and add to it
            Variable existingMapping = fDeviceInfo.safeGetVariable(key);
            if (existingMapping != null) {
               mappings = existingMapping.getValueAsString() + mappings;
            }
         }
         fDeviceInfo.addOrUpdateStringVariable("InputMapping", makeKey(key), mappings, true);
      }
   }

}