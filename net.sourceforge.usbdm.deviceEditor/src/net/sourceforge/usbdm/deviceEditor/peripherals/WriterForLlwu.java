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
import net.sourceforge.usbdm.deviceEditor.information.Settings;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of LLWU
 */
public class WriterForLlwu extends PeripheralWithState {

   /** Key used to save/restore identifier used for code generation */
   private final String PIN_MODE_KEY  = "$peripheral$"+getName()+"_pinMode";

   public enum LlwuPinMode {
      LlwuPinMode_Disabled   ("Disabled",    0b00),      //!< Wake-up by pin change disabled
      LlwuPinMode_RisingEdge ("RisingEdge",  0b01),    //!< Wake-up on pin rising edge
      LlwuPinMode_FallingEdge("FallingEdge", 0b10),   //!< Wake-up on pin falling edge
      LlwuPinMode_EitherEdge ("EitherEdge",  0b11);    //!< Wake-up on pin rising or falling edge
      
      private final int      fValue;
      private final String   fName;
      
      private LlwuPinMode(String name, int value) {
         fName  = name;
         fValue = value;
      }
      
      public String getName() {
         return fName;
      }
      
      public int getValue() {
         return fValue;
      }
      
      static public LlwuPinMode convertFromInt(int value) {
         if ((value<0)||(value>0b11)) {
            return LlwuPinMode_Disabled;
         }
         return values()[value];
      }
   };
   
   LlwuPinMode pinMode[] = new LlwuPinMode[32];
   
   public WriterForLlwu(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can create instances for signals belonging to this peripheral
      super.setCanCreateSignalInstance(true);
      
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
      HashSet<String> usedEnumIdentifiers = new HashSet<String>();
      
      // Generate enums for llwu pin inputs (signals)
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
            if (pin == Pin.UNASSIGNED_PIN) {
               continue;
            }

            // Found fixed or mapped pin
            String trailingComment  = pin.getNameWithLocation();
            String cIdentifier      = signal.getCodeIdentifier();
            String pinName = enumName+"_"+prettyPinName(pin.getName());
            String mapName = enumName+"_"+index;
            if (mappingInfo.getMux() == MuxSelection.fixed) {
               // Fixed pin mapping
               trailingComment = "Fixed pin  "+trailingComment;
            }
            else {
               trailingComment = "Mapped pin "+trailingComment;
            }
            trailingComment = commentRoot + trailingComment;
            
            // Default enum e.g.  "LlwuPin_Pta13 = LlwuPin_4, ///< Mapped pin PTA13(p29)"
            sb.append(String.format(PIN_FORMAT, pinName, mapName+',', trailingComment));
            
            if (!cIdentifier.isBlank()) {
               String description = signal.getUserDescription();
               String type = String.format("const %s<%s>", getClassBaseName()+getInstance()+"::"+"Pin", pinName);
               if (signal.getCreateInstance()) {
                  writeVariableDeclaration("", description, cIdentifier, type, getPinMode(signal).toString(), pin.getNameWithLocation());
               }
               else {
                  writeTypeDeclaration("", description, cIdentifier, type, pin.getNameWithLocation());
               }
               String enumIdentifier      = makeCTypeIdentifier(enumName+"_"+cIdentifier);
               boolean inUse = !usedEnumIdentifiers.add(enumIdentifier);
               if (inUse) {
                  // Repeated value comment out
                  enumIdentifier = "// "+enumIdentifier; 
               }
               sb.append(String.format(PIN_FORMAT, enumIdentifier, mapName+",", trailingComment));
            }
         }
      }
      StringVariable llwuPinInputsVar = new StringVariable("Input Pin Mapping", makeKey("InputPinMapping"));
      llwuPinInputsVar.setValue(sb.toString());
      llwuPinInputsVar.setDerived(true);
      fDeviceInfo.addOrReplaceVariable(llwuPinInputsVar.getKey(), llwuPinInputsVar);

      // Generate enums for llwu modules (peripherals) from configuration parameter
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
                  boolean inUse = !usedEnumIdentifiers.add(moduleName);
                  if (inUse) {
                     moduleName = "// "+moduleName; 
                  }
                  sb.append(String.format(PIN_FORMAT,  moduleName, mapName+",", ""));
                  
                  Peripheral peripheral = fDeviceInfo.getPeripherals().get(deviceName);
                  if (peripheral != null) {
                     String identifier = peripheral.getCodeIdentifier();
                     if (!identifier.isBlank()) {
                        identifier = makeCTypeIdentifier(enumName+"_"+identifier);
                        inUse = !usedEnumIdentifiers.add(identifier);
                        if (inUse) {
                           identifier = "// "+identifier; 
                        }
                        String description = peripheral.getUserDescription();
                        if (description.isBlank()) {
                           description = peripheral.getDescription();
                        }
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
   /**
    * Indicates mode for input
    * 
    * @param signal Signal to check
    * 
    * @return  Value reflecting sensitivity of pin
    */
   public LlwuPinMode getPinMode(Signal signal) {
      int index = fInfoTable.table.indexOf(signal);
      if (index<0) {
         return LlwuPinMode.LlwuPinMode_Disabled;
      }
      LlwuPinMode mode = pinMode[index];
      if (mode == null) {
         mode = LlwuPinMode.LlwuPinMode_Disabled;
      }
      return mode;
   }

   /**
    * Set mode of input
    * 
    * @param signal Signal to modify
    * @param value  Value controlling sensitivity of pin
    */
   public void setPinMode(Signal signal, LlwuPinMode mode) {
      int index = fInfoTable.table.indexOf(signal);
      if (index<0) {
         return;
      }
      setDirty(pinMode[index] != mode);
      pinMode[index] = mode;
   }

   @Override
   public void saveSettings(Settings settings) {
      super.saveSettings(settings);
      for (int index=0; index<pinMode.length; index++) {
         if (pinMode[index] == null) {
            continue;
         }
         if (pinMode[index] == LlwuPinMode.LlwuPinMode_Disabled) {
            continue;
         }
         settings.put(PIN_MODE_KEY+index, pinMode[index].toString());
      }
   }

   @Override
   public void loadSettings(Settings settings) {
      super.loadSettings(settings);
      for (int index=0; index<pinMode.length; index++) {
         String value = settings.get(PIN_MODE_KEY+index);
         if (value != null) {
            pinMode[index] = LlwuPinMode.valueOf(value);
         }
      }
   }

}