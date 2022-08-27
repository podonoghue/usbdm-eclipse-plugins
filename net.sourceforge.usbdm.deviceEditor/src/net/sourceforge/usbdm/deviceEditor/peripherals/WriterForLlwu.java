package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import net.sourceforge.usbdm.deviceEditor.editor.BaseLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.ModifierEditorInterface;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Settings;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.ChoiceData;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of LLWU
 */
public class WriterForLlwu extends PeripheralWithState implements Customiser {

   /** Key used to save/restore identifier used for code generation */
   private final String PIN_MODE_KEY  = "$peripheral$"+getName()+"_pinMode";
   
   /** Contains list of peripherals that may be LLWU sources **/
   private String[] peripherals = null;

   public enum LlwuPinMode {
      LlwuPinMode_Disabled   ("Disabled",    0b00),   //!< Wake-up by pin change disabled
      LlwuPinMode_RisingEdge ("RisingEdge",  0b01),   //!< Wake-up on pin rising edge
      LlwuPinMode_FallingEdge("FallingEdge", 0b10),   //!< Wake-up on pin falling edge
      LlwuPinMode_EitherEdge ("EitherEdge",  0b11);   //!< Wake-up on pin rising or falling edge
      
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
      fCanCreateSignalInstance = true;
      
      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType = true;
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
   
   static final String PIN_FORMAT   = "      %-20s = %-13s %s\n";
   
   /**
    * Generate set of enum symbols for llwu inputs that are mapped to pins.
    * 
    * <pre>
    *   // Mapped pins
    *   LlwuPin_Pta4         = LlwuPin_3,    ///< Mapped pin PTA4
    *   LlwuPin_Ptb0         = LlwuPin_5,    ///< Mapped pin PTB0
    *   LlwuPin_Ptc1         = LlwuPin_6,    ///< Mapped pin PTC1
    * </pre> 
    * 
    * @param documentUtilities
    * @throws Exception 
    * @throws IOException
    */
   @Override
   protected void writeDeclarations() {
      
      super.writeDeclarations();
      
      String enumName    = getClassName()+"Pin";
      String commentRoot = "///< ";
      HashSet<String> usedEnumIdentifiers = new HashSet<String>();
      
      // Generate enums for llwu pin inputs (signals)
      StringBuffer sb = new StringBuffer();
      InfoTable signalTable = getUniqueSignalTable();
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
         if (!pin.isAvailableInPackage()) {
            continue;
         }
         // Found fixed or mapped pin
         String trailingComment  = pin.getNameWithLocation();
         String cIdentifier      = makeCTypeIdentifier(signal.getCodeIdentifier());
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
            String type = String.format("%s<%s>", getClassBaseName()+getInstance()+"::"+"Pin", pinName);
            String constType = "const "+ type;
            if (signal.getCreateInstance()) {
               writeVariableDeclaration("", description, cIdentifier, constType, getPinMode(signal).toString(), pin.getNameWithLocation());
            }
            else {
               writeTypeDeclaration("", description, cIdentifier, type, pin.getNameWithLocation());
            }
            String enumIdentifier = makeCTypeIdentifier(enumName+"_"+cIdentifier);
            boolean inUse = !usedEnumIdentifiers.add(enumIdentifier);
            if (inUse) {
               // Repeated value comment out
               enumIdentifier = "// "+enumIdentifier; 
            }
            sb.append(String.format(PIN_FORMAT, enumIdentifier, mapName+",", trailingComment));
         }
      }
      // Create or replace Input Pin Mapping variable as needed
      fDeviceInfo.addOrUpdateStringVariable("Input Pin Mapping", makeKey("InputPinMapping"), sb.toString(), true);

      // Generate enums for llwu modules (peripherals) from configuration parameter
      enumName    = getClassName()+"Peripheral";

      if (peripherals != null) {
         // Find size to allow alignment in columns
         int maximumNameLength = 5;
         for (String deviceNames:peripherals) {
            if ((deviceNames != null) && !deviceNames.isBlank()) {
               for (String deviceName:deviceNames.split("/")) {
                  String moduleName = makeCTypeIdentifier(enumName+"_"+prettyPinName(deviceName));
                  maximumNameLength = Math.max(maximumNameLength, moduleName.length()+3);
               }
            }
         }
         
         sb = new StringBuffer();
         int peripheralCount = 0;
         for (String deviceNames:peripherals) {
            if ((deviceNames != null) && !deviceNames.isBlank()) {
               for (String deviceName:deviceNames.split("/")) {
                  String mapName    = enumName+"_"+peripheralCount;
                  String moduleName = makeCTypeIdentifier(enumName+"_"+prettyPinName(deviceName));
                  boolean inUse = !usedEnumIdentifiers.add(moduleName);
                  if (inUse) {
                     moduleName = "// "+moduleName; 
                  }
                  sb.append(String.format("      %"+(-maximumNameLength)+"s = %-13s %s\n",  moduleName, mapName+",", ""));
                  
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
         fDeviceInfo.addOrUpdateStringVariable("Input Pin Mapping", makeKey("InputModuleMapping"), sb.toString(), true);
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
    * 
    * @return True if value changed 
    */
   public boolean setPinMode(Signal signal, LlwuPinMode mode) {
      int index = fInfoTable.table.indexOf(signal);
      if (index<0) {
         return false;
      }
      if (pinMode[index] == mode) {
         return false;
      }
      pinMode[index] = mode;
      setDirty(true);
      return true;
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
   
   static class ChoiceCellEditor extends ComboBoxCellEditor {
      
      private static final String[] llwuPinModes = {
            LlwuPinMode.LlwuPinMode_Disabled.getName(),
            LlwuPinMode.LlwuPinMode_RisingEdge.getName(),
            LlwuPinMode.LlwuPinMode_FallingEdge.getName(),
            LlwuPinMode.LlwuPinMode_EitherEdge.getName(),
      };
      
      public ChoiceCellEditor(Composite tree) {
         super(tree, llwuPinModes, SWT.READ_ONLY);

         setActivationStyle(
               ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION |
               ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
         setValueValid(true);
      }
   }

   /**
    * Editor support for LLWU sensitivity
    */
   public class ModifierEditingSupport implements ModifierEditorInterface {

      @Override
      public boolean canEdit(SignalModel model) {
         return model.getSignal().getMappedPin() != Pin.UNASSIGNED_PIN;
      }

      @Override
      public CellEditor getCellEditor(TreeViewer viewer) {
         return new ChoiceCellEditor(viewer.getTree());
      }

      @Override
      public Object getValue(SignalModel model) {
         if (model.getSignal().getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }
         return getPinMode(model.getSignal()).getValue();
      }

      @Override
      public String getText(SignalModel model) {
         Signal signal = model.getSignal();
         if (signal.getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }
         if (signal.getCreateInstance()) {
            return getPinMode(model.getSignal()).getName();
         }
         return null;
      }
      
      @Override
      public boolean setValue(SignalModel model, Object value) {
         return setPinMode(model.getSignal(), LlwuPinMode.convertFromInt((int)value));
      }

      @Override
      public Image getImage(SignalModel model) {
         Signal signal = model.getSignal();
         if (signal.getCreateInstance()) {
            switch(getPinMode(signal)) {
            case LlwuPinMode_Disabled:    return BaseLabelProvider.disabledImage;
            case LlwuPinMode_EitherEdge:  return BaseLabelProvider.upDownArrowImage;
            case LlwuPinMode_FallingEdge: return BaseLabelProvider.downArrowImage;
            case LlwuPinMode_RisingEdge:  return BaseLabelProvider.upArrowImage;
            }
         }
         return null;
      }
      
      @Override
      public String getModifierHint(SignalModel model) {
         return "Sensitivity of LLWU input";
      }

   }

   private ModifierEditingSupport modifierEditingSupport =  new ModifierEditingSupport();
   
   @Override
   public ModifierEditorInterface getModifierEditor() {
      return modifierEditingSupport;
   }

   @Override
   public void modifyPeripheral() throws Exception {
      // Update list of devices
      Variable devicesListVar = safeGetVariable(makeKey("/SIM/llwu_device_list"));
      if (devicesListVar == null) {
         devicesListVar = safeGetVariable(makeKey("device_list"));
      }
      if (devicesListVar != null) {
         String devices = devicesListVar.getValueAsString();
         if (!devices.isBlank() && !devices.equalsIgnoreCase("none")) {
            peripherals = devices.split(";");
            for (int index=0; index<peripherals.length; index++) {
               String peripheralDescription = "Peripheral "+peripherals[index];
               if (peripherals[index].isBlank()) {
                  // No device for this slot
                  peripherals[index] = null;
                  peripheralDescription = "";
               }
               Variable meEntry = safeGetVariable(makeKey("llwu_me_wume"+index));
               if (meEntry != null) {
                  meEntry.setDescription(peripheralDescription);
                  if (peripheralDescription.isBlank()) {
                     meEntry.setHidden(true);
                  }
               }
            }
         }
      }
      
      // Update list of pins
      ArrayList<ChoiceData> choiceData = new ArrayList<ChoiceData>();
      InfoTable signalTable = getUniqueSignalTable();
      for (int index = 0; index<=31; index++) {
         Variable peEntry = safeGetVariable(makeKey("llwu_pe"+((index/4)+1)+"_wupe"+index));
//         ((VariableWithChoices)peEntry).getField(PIN_FORMAT)
         if (peEntry == null) {
            continue;
            // XXX Fix me!
         }
         if (index<signalTable.table.size()) {
            // peEntry MUST exists and needs to be modified or cleared
            Signal signal = signalTable.table.get(index);
            String pinName = null;
            String enumName = null;
            if ((signal != null) && (signal != Signal.DISABLED_SIGNAL)) {
               final StringBuilder nameCollector = new StringBuilder();
               TreeSet<MappingInfo> mapping = signal.getPinMapping();
               mapping.forEach(new Consumer<MappingInfo>() {

                  @Override
                  public void accept(MappingInfo mapInfo) {
                     Pin pin = mapInfo.getPin();
                     if ((pin != Pin.UNASSIGNED_PIN) && pin.isAvailableInPackage()) {
                        if (nameCollector.length() > 0) {
                           nameCollector.append("/" + pin.getName());
                        }
                        nameCollector.append(pin.getName());
                     }
                  }
               });
               if (!nameCollector.toString().isEmpty()) {
                  pinName  = "Pin " + nameCollector.toString();
                  enumName = nameCollector.toString().replace("/", "_");
               }
            }
            if (pinName == null) {
               // Input not used
               peEntry.setHidden(true);
               // Can't remove variable as already generated template includes reference 
               // removeVariable(peEntry);
            }
            else {
               // Input associated with pin
               peEntry.setDescription(pinName);
               peEntry.setTypeName("LlwuPinMode");
               enumName = prettyPinName(enumName);
               ChoiceData entry = new ChoiceData(pinName, Integer.toString(index), enumName, "", "");
               choiceData.add(entry);
            }
         }
         else {
            // peEntry MAY exists and needs to be cleared if so
            if (peEntry != null) {
               peEntry.setHidden(true);
            }
         }
      }
      // Choice used if FILT is disabled
      choiceData.add(new ChoiceData("Disabled", "0", "0", "0", null));
      for(int index=0; index<6; index++) {
         ChoiceVariable filter = (ChoiceVariable) safeGetVariable(makeKey("llwu_filt"+index+"_filtsel"));
         if (filter != null) {
            filter.setTypeName("LlwuPin");
            filter.setData(choiceData);
            filter.setValue(choiceData.get(0).getName());
//            filter.setDisabledValue(choiceData.get(0).getName());
         }
      }
      fMenuData.prune();
   }

   @Override
   public void extractHardwareInformation(net.sourceforge.usbdm.peripheralDatabase.Peripheral dbPortPeripheral) {
      extractAllRegisterNames(dbPortPeripheral);
   }
   
}
