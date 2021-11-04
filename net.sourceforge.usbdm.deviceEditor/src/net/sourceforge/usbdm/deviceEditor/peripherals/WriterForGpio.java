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
   }

   @Override
   public String getTitle() {
      return"Digital Input/Output";
   }

   class GpioPinInformation {
      
      private final ArrayList<Integer> fListOfBits  = new ArrayList<Integer>();
      private final ArrayList<Pin>     fListOfPins  = new ArrayList<Pin>();
      private final String  fDescription;
      private boolean fConflictedPolarity    = false;
      private boolean fBitsAreConsecutive    = true;
      private int     fLastBitAdded;
      private int     fPolarity;
      
      /**
       * Constructor. Creates entry for first (only) bit.
       * 
       * @param bitNum
       * @param isActiveLow
       */
      public GpioPinInformation(int bitNum, String description, Pin pin) {
         fListOfBits.add(bitNum);
         fListOfPins.add(pin);
         fPolarity          = pin.isActiveLow()?(1<<bitNum):0;
         fLastBitAdded      = bitNum;
         fDescription       = description;
      }
      
      /**
       * Add new bit.
       * 
       * @param bitNum
       * @param isActiveLow
       */
      public void addBit(int bitNum, Pin pin) {
         fListOfBits.add(bitNum);
         fListOfPins.add(pin);
         if (bitNum != (fLastBitAdded+1)) {
            fBitsAreConsecutive = false;
         }
         fLastBitAdded  = bitNum;
         fConflictedPolarity = 
               fConflictedPolarity || 
               (((fPolarity == 0) && pin.isActiveLow()) || 
               ((fPolarity != 0) && !pin.isActiveLow()));
         fPolarity     |= pin.isActiveLow()?(1<<bitNum):0;
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
       * Get list of pins associated with each bit
       * 
       * @return
       */
      public final ArrayList<Pin> getPins() {
         return fListOfPins;
      }
      
      /**
       * Indicates if there is an inconsistency in the bit polarities
       * 
       * @return
       */
      public boolean isMixedPolarity() {
         return fConflictedPolarity;
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
   
   /**
    * Get main text from description string
    * 
    * @param text
    * 
    * @return
    */
   String getMainText(String text) {
      String[] texts = text.split(";");
      return texts[0];
   }
   
   /**
    * Get minor text from description string
    * 
    * @param text
    * 
    * @return
    */
   String getMinorText(String text) {
      String[] texts = text.split(";");
      if (texts.length>0) {
         return texts[1];
      }
      return texts[0];
   }
   
   @Override
   protected void writeDeclarations() {
      
      if (!getCodeIdentifier().isBlank()) {
         writeDefaultPeripheralDeclaration("Port"+getInstance());
      }
      
      // Information about each unique identifier in GPIO
      HashMap<String, GpioPinInformation> variablesToCreate = new HashMap<String, GpioPinInformation>();

      // Collect the pins into fields and individual bits based on primary code identifier 
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
         
         String[] descriptions = pin.getUserDescription().split("/", -2);
         String[] identifiers  = pin.getCodeIdentifier().split("/", -2);
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
               gpioPinInformation = new GpioPinInformation(infoTableIndex, description, pin);
               variablesToCreate.put(identifier, gpioPinInformation);
            }
            else {
               gpioPinInformation.addBit(infoTableIndex, pin);
            }
         }
      }
      
      // Process the identifiers
      for (String mainIdentifier:variablesToCreate.keySet()) {
         
         GpioPinInformation gpioPinInformation = variablesToCreate.get(mainIdentifier);
         String comment = "";
         String error = "";
         if (!gpioPinInformation.areBitConsecutive()) {
            error = "Bits are not consecutive in field - check Configure.usbdmProject";
            comment = "<== Error: Missing bits";
         }
         
         final ArrayList<Integer> bitNums = gpioPinInformation.getListOfBits();
         final ArrayList<Pin>     pins    = gpioPinInformation.getPins();
         if (bitNums.size()==1) {
            // Do simple Gpio
            int bitNum = bitNums.get(0);
            Pin pin = pins.get(0);
            String pinTrailingComment = pin.getLocation() + comment;
            String polarity           = pin.isActiveLow()?", ActiveLow":"";
            String pinDescription     = gpioPinInformation.getDescription();
            
            String type = String.format("const %s<%d%s>", getClassBaseName()+getInstance(), bitNum, polarity);
            writeVariableDeclaration(error, pinDescription, mainIdentifier, type, pinTrailingComment);
         }
         else {
            // Do GpioField
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

            writeVariableDeclaration(error, fieldDescription, mainIdentifier, type, fieldComment.toString());
         }
      }
   }
   
   @Override
   public boolean isPcrTableNeeded() {
      return false;
   }

//   @Override
//   public String getPcrDefinition() {
//      return String.format(
//            "   //! Value for PCR (including MUX value)\n"+
//            "   static constexpr uint32_t %s  = GPIO_DEFAULT_PCR;\n\n", DEFAULT_PCR_VALUE_NAME
//            );
//   }

   @Override
   public int getSignalIndex(Signal function) {
      // No tables for GPIO
      return Integer.parseInt(function.getSignalName());
//      Pattern p = Pattern.compile("(\\d+).*");
//      Matcher m = p.matcher(function.getSignalName());
//      if (!m.matches()) {
//         throw new RuntimeException("Function "+function+", Signal " + function.getSignalName() + " does not match expected pattern");
//      }
//      int signalIndex = Integer.parseInt(m.group(1));
//      return signalIndex;
   }

   @Override
   public void modifyVectorTable(VectorTable vectorTable) {
      try {
         for (IrqVariable var : irqVariables) {
            modifyVectorTable(vectorTable, var, "Port");
         }
      } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
   
}