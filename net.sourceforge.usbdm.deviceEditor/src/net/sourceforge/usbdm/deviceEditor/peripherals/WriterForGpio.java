package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

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
      
      private ArrayList<Integer> fListOfBits  = new ArrayList<Integer>();
      private ArrayList<Pin>     fListOfPins  = new ArrayList<Pin>();
      private boolean fActiveLow             = false;
      private boolean fConflictedPolarity    = false;
      private boolean fBitsAreConsecutive    = true;
      private int     fLastBitAdded          = 0;
      
      /**
       * Constructor. Creates entry for first (only) bit.
       * 
       * @param bitNum
       * @param isActiveLow
       */
      public GpioPinInformation(int bitNum, Pin pin) {
         fListOfBits.add(bitNum);
         fListOfPins.add(pin);
         fActiveLow     = pin.isActiveLow();
         fLastBitAdded  = bitNum;
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
         fLastBitAdded = bitNum;
         if (fActiveLow != pin.isActiveLow()) {
            fConflictedPolarity = true;
         }
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
       * Polarity of the first bit added.
       * 
       * @return
       */
      public boolean isActiveLow() {
         return fActiveLow;
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
      public boolean isConflictedPolarity() {
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
   void writeDeclarations(DocumentUtilities writer, Set<String> usedNames) throws IOException {
      
      // Information about each unique identifier in GPIO
      HashMap<String, GpioPinInformation> identifiers = new HashMap<String, GpioPinInformation>();

      // Collect the pins into fields and individual bits based on code identifier 
      for (int index=0; index<fInfoTable.table.size(); index++) {
         Signal signal = fInfoTable.table.get(index);
         if (signal == null) {
            continue;
         }
         MappingInfo pinMapping = signal.getFirstMappedPinInformation();
         Pin pin = pinMapping.getPin();
         String ident = pin.getCodeIdentifier();
         if (ident.isBlank()) {
            continue;
         }
         // Only use stem for collecting
         ident = ident.split(";")[0];
         GpioPinInformation gpioPinInformation = identifiers.get(ident);
         
         if (gpioPinInformation == null) {
            gpioPinInformation = new GpioPinInformation(index, pin);
            identifiers.put(ident, gpioPinInformation);
         }
         else {
            gpioPinInformation.addBit(index, pin);
         }
      }
      
      // Process the identifiers
      for (String mainIdentifier :identifiers.keySet()) {
         
         GpioPinInformation gpioPinInformation = identifiers.get(mainIdentifier);
         String comment = "";
         String error = "";
         if (gpioPinInformation.isConflictedPolarity()) {
            error = "Inconsistent polarity of pins in field - check Configure.usbdmProject";
            comment = "<== Inconsistent polarity";
         }
         if (!gpioPinInformation.areBitConsecutive()) {
            error = "Bits are not consecutive in field - check Configure.usbdmProject";
            comment = "<== Missing bits";
         }
         
         String polarity = gpioPinInformation.isActiveLow()?", "+DeviceInfo.NAME_SPACE_USBDM_LIBRARY+"::ActiveLow":"";

         mainIdentifier = makeCIdentifier(mainIdentifier);
         boolean repeatedIdent = !usedNames.add(mainIdentifier);
         String type;
         final ArrayList<Integer> bitNums = gpioPinInformation.getListOfBits();
         final ArrayList<Pin>     pins    = gpioPinInformation.getPins();
         
         String mainDescription = pins.get(0).getPinUseDescription().split(";")[0].trim();
         
         if (bitNums.size()==1) {
            // Do Gpio
            type = String.format("const %s::%s<%d%s>", DeviceInfo.NAME_SPACE_USBDM_LIBRARY, getClassBaseName()+getInstance(), bitNums.get(0), polarity);
            writeDeclaration(writer, error, mainDescription, repeatedIdent, mainIdentifier, type, comment);
         }
         else {
            // Do GpioField
            type = String.format("const %s::%s<%d, %d%s>", DeviceInfo.NAME_SPACE_USBDM_LIBRARY, getClassBaseName()+getInstance()+"Field", bitNums.get(bitNums.size()-1), bitNums.get(0), polarity);
            
            String fieldDescription = mainDescription;
            if (!fieldDescription.isBlank()) {
               fieldDescription = fieldDescription + " (Bit Field)";
            }
            writeDeclaration(writer, error, fieldDescription, repeatedIdent, mainIdentifier, type, comment);
            
            // Do individual bits in bit-field
            // Only done if named
            for (int index=0; index<bitNums.size(); index++) {
               int bitNum = bitNums.get(index);
               Pin pin = pins.get(index);
               
               String[] bitIdentifiers = pin.getCodeIdentifier().split(";");
               String   bitIdentifier  = (bitIdentifiers.length>1)?bitIdentifiers[1].trim():"";
               
               if (bitIdentifier.isBlank()) {
                  if (gpioPinInformation.isConflictedPolarity()) {
                     // Force auto-generation of bits
                     bitIdentifier = "*";
                  }
                  else {
                     // No identifier for bit - don't generate individual Gpio
                     continue;
                  }
               }

               String[] descriptions = pin.getPinUseDescription().split(";");
               String   description  = ((descriptions.length>1)?descriptions[1]:descriptions[0]).trim();
               description = description + " (" + mainIdentifier + " bit #" + index + ")";
               
               String bitPolarity = pin.isActiveLow()?", "+DeviceInfo.NAME_SPACE_USBDM_LIBRARY+"::ActiveLow":"";

               type = String.format("const %s::%s<%d%s>", DeviceInfo.NAME_SPACE_USBDM_LIBRARY, getClassBaseName()+getInstance(), bitNum, bitPolarity);
               if (bitIdentifier.startsWith("*")) {
                  // Use common identifier with suffix
                  writeDeclaration(writer, description, repeatedIdent, mainIdentifier+"_"+(index), type, comment);
               }
               else {
                  bitIdentifier = makeCIdentifier(bitIdentifier);
                  writeDeclaration(writer, description, !usedNames.add(bitIdentifier), bitIdentifier, type, comment);
               }
            }
         }
      }
   }
   
   @Override
   public boolean needPCRTable() {
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