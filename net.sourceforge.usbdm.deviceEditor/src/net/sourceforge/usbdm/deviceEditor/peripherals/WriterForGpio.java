package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

/**
 * Class encapsulating the code for writing an instance of GPIO
 */
public class WriterForGpio extends PeripheralWithState {

   public WriterForGpio(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can't create instances of this peripheral 
      super.setCanCreateInstance(false);
      
      // Can create type declarations for signals belonging to this peripheral
      super.setCanCreateSignalType(true);
      
      // Can create instances for signals belonging to this peripheral
      super.setCanCreateSignalInstance(true);
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
         fPolarity          = signal.isActiveLow()?(1<<bitNum):0;
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
               (((fPolarity == 0) && signal.isActiveLow()) || 
               ((fPolarity != 0) && !signal.isActiveLow()));
         fPolarity     |= signal.isActiveLow()?(1<<bitNum):0;
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
      public boolean isCreateInstance() {
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
      
      // Process the identifiers
      for (String mainIdentifier:variablesToCreate.keySet()) {
         
         GpioPinInformation gpioPinInformation = variablesToCreate.get(mainIdentifier);
         final ArrayList<Integer> bitNums = gpioPinInformation.getListOfBits();
         final ArrayList<Pin>     pins    = gpioPinInformation.getPins();
         final ArrayList<Signal>  signals = gpioPinInformation.getSignals();
         if (bitNums.size()==1) {
            // Do simple Gpio
            int bitNum = bitNums.get(0);
            Pin    pin    = pins.get(0);
            Signal signal = signals.get(0);
            String pinTrailingComment = pin.getLocation();
            String polarity           = signal.isActiveLow()?", ActiveLow":"";
            String pinDescription     = gpioPinInformation.getDescription();
            
            String type = String.format("const %s<%d%s>", getClassBaseName()+getInstance(), bitNum, polarity);
            if (signal.getCreateInstance()) {
               writeVariableDeclaration("", pinDescription, mainIdentifier, type, pinTrailingComment);
            }
            else {
               writeTypeDeclaration("", pinDescription, mainIdentifier, type, pinTrailingComment);
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
            String fieldComment = pins.get(0).getLocation() + "-" + pins.get(pins.size()-1).getLocation();
            fieldComment = fieldComment + comment;
            final int fieldPolarity = gpioPinInformation.getFieldPolarity(); 
            String polarity= "";
            if (fieldPolarity != 0) {
               if (gpioPinInformation.isMixedPolarity()) {
                  // Use explicit bit-mask
                  polarity = ", 0b" + Integer.toBinaryString(fieldPolarity);
               }
               else {
                  polarity = ", ActiveLow";
               }
            }
            String type = String.format("const %s<%d, %d%s>", getClassBaseName()+getInstance()+"Field", bitNums.get(bitNums.size()-1), bitNums.get(0), polarity);
            
            if (gpioPinInformation.isCreateInstance()) {
               writeVariableDeclaration(error, fieldDescription, mainIdentifier, type, fieldComment.toString());
            }
            else {
               writeTypeDeclaration(error, fieldDescription, mainIdentifier, type, fieldComment.toString());
            }
         }
      }
   }
   
   @Override
   public boolean isPcrTableNeeded() {
      return false;
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
   
}