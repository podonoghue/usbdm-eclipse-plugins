package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.editor.BaseLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.ModifierEditorInterface;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Settings;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Cluster;
import net.sourceforge.usbdm.peripheralDatabase.Field;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripheralDatabase.Register;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

/**
 * Class encapsulating the code for writing an instance of GPIO
 */
public class WriterForGpio extends PeripheralWithState {

   /** Key used to save/restore identifier used for code generation */
   private final String POLARITY_KEY  = "$peripheral$"+getName()+"_polarity";

   int fPolarity = 0;

   public WriterForGpio(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);

      // Can't create instances of this peripheral
      fCanCreateInstance = false;

      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType = true;

      // Can create instances for signals belonging to this peripheral
      fCanCreateSignalInstance = true;
   }

   @Override
   public String getTitle() {
      return"Digital Input/Output";
   }

   class GpioPinInformation {

      private final ArrayList<Integer> fListOfBits    = new ArrayList<Integer>();
      private final ArrayList<Pin>     fListOfPins    = new ArrayList<Pin>();
      private final ArrayList<Signal>  fListOfSignals = new ArrayList<Signal>();
      private final String  fDescription;
      private boolean fIsMixedPolarity    = false;
      private boolean fBitsAreConsecutive = true;
      private int     fLastBitAdded;
      private int     fPolarity;
      private boolean fCreateInstance;

      /**
       * Constructor. Creates entry for first (only) bit.
       * 
       * @param bitNum
       * @param isActiveLow
       */
      public GpioPinInformation(int bitNum, String description, Signal signal, Pin pin) {
         fListOfBits.add(bitNum);
         fListOfPins.add(pin);
         fListOfSignals.add(signal);
         fPolarity          = isActiveLow(signal)?(1<<bitNum):0;
         fLastBitAdded      = bitNum;
         fDescription       = description;
         fCreateInstance    = signal.getCreateInstance();
      }

      /**
       * Add new bit.
       * 
       * @param bitNum
       * @param isActiveLow
       */
      public void addBit(int bitNum, Signal signal, Pin pin) {
         fListOfBits.add(bitNum);
         fListOfPins.add(pin);
         fListOfSignals.add(signal);
         if (bitNum != (fLastBitAdded+1)) {
            fBitsAreConsecutive = false;
         }
         fLastBitAdded  = bitNum;
         // Only create instance for field if ALL bits marked
         fCreateInstance = fCreateInstance && signal.getCreateInstance();
         fIsMixedPolarity =
               fIsMixedPolarity ||
               (((fPolarity == 0) && isActiveLow(signal)) ||
                     ((fPolarity != 0) && !isActiveLow(signal)));
         fPolarity     |= isActiveLow(signal)?(1<<bitNum):0;
      }

      /**
       * Get array of bits associated with this identifier
       * 
       * @return Array
       */
      public final ArrayList<Integer> getListOfBits() {
         return fListOfBits;
      }

      /**
       * Get list of pins associated with each bit
       * 
       * @return
       */
      public final ArrayList<Pin> getPins() {
         return fListOfPins;
      }

      /**
       * Get list of signals associated with each bit
       * 
       * @return
       */
      public final ArrayList<Signal> getSignals() {
         return fListOfSignals;
      }

      /**
       * Check polarity of the given bit.
       * 
       * @return true is active low
       */
      public boolean isActiveLowBit(int bitNum) {
         return (fPolarity & (1<<bitNum)) != 0;
      }

      /**
       * Get polarity of field
       * 
       * @return Bitmask showing polarity of each bit in field
       */
      public int getFieldPolarity() {
         return fPolarity>>fListOfBits.get(0);
      }

      /**
       * Indicates if should be created as an instance
       * 
       * @return
       */
      public boolean getCreateInstance() {
         return fCreateInstance;
      }

      /**
       * Indicates if there is an inconsistency in the bit polarities
       * 
       * @return
       */
      public boolean isMixedPolarity() {
         return fIsMixedPolarity;
      }

      /**
       * Indicates if the bits are consecutive
       * 
       * @return
       */
      public boolean areBitConsecutive() {
         return fBitsAreConsecutive;
      }

      /**
       * Get field description (obtained from first pin added).
       * 
       * @return
       */
      public String getDescription() {
         return fDescription;
      }
   }

   @Override
   protected void writeDeclarations() {

      if (!getCodeIdentifier().isBlank()) {
         writeDefaultPeripheralDeclaration("Port"+getInstance());
      }

      // Information about each unique identifier in GPIO
      HashMap<String, GpioPinInformation> variablesToCreate = new HashMap<String, GpioPinInformation>();

      // Collect the pins into fields and individual bits based on code identifier
      for (int infoTableIndex=0; infoTableIndex<fInfoTable.table.size(); infoTableIndex++) {
         Signal signal = fInfoTable.table.get(infoTableIndex);
         if (signal == null) {
            continue;
         }
         MappingInfo pinMapping = signal.getFirstMappedPinInformation();
         if (pinMapping == MappingInfo.UNASSIGNED_MAPPING) {
            continue;
         }
         Pin pin = pinMapping.getPin();

         String[] descriptions = signal.getUserDescription().split("/", -2);
         String[] identifiers  = signal.getCodeIdentifier().split("/", -2);
         for (int variableIndex=0; variableIndex<identifiers.length; variableIndex++) {
            String identifier  = identifiers[variableIndex];
            if (identifier.isBlank()) {
               // Discard empty identifiers
               continue;
            }
            String description = "";
            if (descriptions.length>variableIndex) {
               description = descriptions[variableIndex];
            }
            identifier = makeCVariableIdentifier(identifier);
            GpioPinInformation gpioPinInformation = variablesToCreate.get(identifier);

            if (gpioPinInformation == null) {
               gpioPinInformation = new GpioPinInformation(infoTableIndex, description, signal, pin);
               variablesToCreate.put(identifier, gpioPinInformation);
            }
            else {
               gpioPinInformation.addBit(infoTableIndex, signal, pin);
            }
         }
      }
      
      StringVariable gpioPatternVar      = (StringVariable) safeGetVariable("/SYSTEM/$gpioPattern");
      final String gpioPattern;
      if (gpioPatternVar != null) {
         gpioPattern = gpioPatternVar.getValueAsString();
      }
      else {
         gpioPattern = "Gpio%i<%b,%p>";
      }
      
      StringVariable gpioFieldPatternVar = (StringVariable) safeGetVariable("/SYSTEM/$gpioFieldPattern");
      final String gpioFieldPattern;
      if (gpioFieldPatternVar != null) {
         gpioFieldPattern = gpioFieldPatternVar.getValueAsString();
      }
      else {
         gpioFieldPattern = "Gpio%iField<%l,%r,%p>";
      }
      
      // Process the identifiers
      for (String mainIdentifier:variablesToCreate.keySet()) {

         GpioPinInformation gpioPinInformation = variablesToCreate.get(mainIdentifier);
         final ArrayList<Integer> bitNums = gpioPinInformation.getListOfBits();
         final ArrayList<Pin>     pins    = gpioPinInformation.getPins();
         final ArrayList<Signal>  signals = gpioPinInformation.getSignals();
         if (bitNums.size()==1) {
            // Do simple Gpio
            Pin    pin    = pins.get(0);
            Signal signal = signals.get(0);
            String trailingComment  = pin.getNameWithLocation();
            String polarity         = isActiveLow(signal)?"ActiveLow":"ActiveHigh";
            String pinDescription   = gpioPinInformation.getDescription();

            /*
             * %i = port instance e.g."A"
             * %b = bit number
             * %p = polarity
             * e.g. Gpio%i<%b,%p>
             */
            String type = expandTypePattern(gpioPattern, pin, 0, polarity);
            String constType = "const "+ type;
            if (signal.getCreateInstance()) {
               writeVariableDeclaration("", pinDescription, mainIdentifier, constType, trailingComment);
            }
            else {
               writeTypeDeclaration("", pinDescription, mainIdentifier, type, trailingComment);
            }
         }
         else {
            // Do GpioField
            String comment = "";
            String error = "";
            if (!gpioPinInformation.areBitConsecutive()) {
               error = "Bits are not consecutive in field - check Configure.usbdmProject";
               comment = "<== Error: Missing bits";
            }
            String fieldDescription = gpioPinInformation.getDescription();
            if (!fieldDescription.isBlank()) {
               fieldDescription = fieldDescription + " (Bit Field)";
            }
            boolean doComma = false;
            String trailingComment = "";
            for (Pin pin:pins) {
               if (doComma) {
                  trailingComment += ", ";
               }
               doComma = true;
               trailingComment += pin.getNameWithLocation();
            }
            trailingComment = trailingComment + comment;
            final int fieldPolarity = gpioPinInformation.getFieldPolarity();
            String polarity= "";
            if (fieldPolarity != 0) {
               if (gpioPinInformation.isMixedPolarity()) {
                  // Use explicit bit-mask
                  polarity = "Polarity(0b" + Integer.toBinaryString(fieldPolarity)+")";
               }
               else {
                  polarity = "ActiveLow";
               }
            }
            else {
               polarity = "ActiveHigh";
            }
            /*
             * %i = port instance e.g."A"
             * %l = left bit number
             * %r = right bit number
             * %p = polarity
             * e.g. Gpio%iField<%l,%r,%p>
             */
            String type = gpioFieldPattern;
            type = type.replace("%i", getInstance());                            // port instance e.g."A"
            type = type.replace("%l", bitNums.get(bitNums.size()-1).toString()); // left bit number
            type = type.replace("%r", bitNums.get(0).toString());                // right bit number
            type = type.replace("%p", polarity);                                 // polarity
            String constType = "const "+ type;
            if (gpioPinInformation.getCreateInstance()) {
               writeVariableDeclaration(error, fieldDescription, mainIdentifier, constType, trailingComment);
            }
            else {
               writeTypeDeclaration(error, fieldDescription, mainIdentifier, type, trailingComment);
            }
         }
      }
   }

   @Override
   public int getSignalIndex(Signal signal) {
      // No tables for GPIO
      return Integer.parseInt(signal.getSignalName());
   }

   @Override
   public void modifyVectorTable(VectorTable vectorTable) {
      try {
         for (IrqVariable var : irqVariables) {
            modifyVectorTable(vectorTable, var, "Port");
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Indicates whether the signal is active-low
    * 
    * @param signal Signal to check
    * 
    * @return  true if Active-low
    */
   public boolean isActiveLow(Signal signal) {
      int index = fInfoTable.table.indexOf(signal);
      if (index<0) {
         return false;
      }
      return (fPolarity&(1<<index)) != 0;
   }

   /**
    * Set signal as active-low
    * 
    * @param signal Signal to modify
    * @param value  true to set signal active-low, false otherwise
    * 
    * @return True is value changed
    */
   public boolean setActiveLow(Signal signal, boolean value) {
      int index = fInfoTable.table.indexOf(signal);
      if (index<0) {
         return false;
      }
      boolean currentValue = (fPolarity & (1<<index)) != 0;
      if (currentValue == value) {
         return false;
      }
      fPolarity ^= (1<<index);
      setDirty(true);
      return true;
   }

   @Override
   public void saveSettings(Settings settings) {
      super.saveSettings(settings);
      if (fPolarity != 0) {
         settings.put(POLARITY_KEY, Integer.toString(fPolarity));
      }
   }

   @Override
   public void loadSettings(Settings settings) {
      super.loadSettings(settings);
      String value = settings.get(POLARITY_KEY);
      if (value != null) {
         fPolarity = Integer.parseInt(value);
      }
   }

   /**
    * Editor support for GPIO polarity
    */
   public class ModifierEditingSupport implements ModifierEditorInterface {

      @Override
      public boolean canEdit(SignalModel model) {
         return model.getSignal().getMappedPin() != Pin.UNASSIGNED_PIN;
      }

      @Override
      public CellEditor getCellEditor(TreeViewer viewer) {
         return new CheckboxCellEditor(viewer.getTree());
      }

      @Override
      public Object getValue(SignalModel model) {
         if (model.getSignal().getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }
         return isActiveLow(model.getSignal());
      }

      @Override
      public String getText(SignalModel model) {
         if (model.getSignal().getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }
         return isActiveLow(model.getSignal())?"ActiveLow":"ActiveHigh";
      }

      @Override
      public boolean setValue(SignalModel model, Object value) {
         return setActiveLow(model.getSignal(), (Boolean)value);
      }

      @Override
      public Image getImage(SignalModel model) {
       return isActiveLow(model.getSignal())?BaseLabelProvider.checkedImage:BaseLabelProvider.uncheckedImage;
      }
      
      @Override
      public String getModifierHint(SignalModel model) {
         return "Polarity of Gpio or bit within GpioField";
      }
   }

   private ModifierEditingSupport modifierEditingSupport =  new ModifierEditingSupport();
   
   @Override
   public ModifierEditorInterface getModifierEditor() {
      return modifierEditingSupport;
   }

   @Override
   public int getPriority() {
      return 800;
   }

   String registersToRecord[] = {"PCR"};
   
   @Override
   public void extractHardwareInformation(Peripheral dbPortPeripheral) {
      try {
         boolean pcrFound = false;
         for(Cluster cl:dbPortPeripheral.getRegisters()) {
            if (!(cl instanceof Register)) {
               continue;
            }
            Register reg = (Register) cl;
            if (reg.getName().startsWith("PCR")) {
               pcrFound = true;
               for (Field field:reg.getFields()) {
                  String key = "/PCR/"+field.getName().toLowerCase()+"_present";
                  addOrIgnoreParam(key, null);
               }
            } else if (reg.getName().equalsIgnoreCase("DFER")) {
               String key = "dfer_register_present";
               addOrIgnoreParam("/PCR/"+key, null);
               addOrIgnoreParam(key, null);
               break;
            }
         }
         addOrIgnoreParam("/"+getName()+"/_present", null);
         if (pcrFound) {
            String key = "/PCR/_present";
            addOrIgnoreParam(key, null);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
}