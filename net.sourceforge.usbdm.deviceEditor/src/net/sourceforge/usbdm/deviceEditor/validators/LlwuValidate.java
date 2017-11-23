package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral.InfoTable;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to validate LLWU settings
 */
public class LlwuValidate extends PeripheralValidator {
   boolean donePinNames = false;
   
   public LlwuValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Converts a string to Capital-case by removing spaces and capitalising the next character
    * 
    * @param name Name to convert
    * 
    * @return Converted name e.g. "HELLO   there" => "HelloThere"
    */
   private String capitalCase(String name) {
      if (name == null) {
         return null;
      }
      StringBuilder sb = new StringBuilder();
      boolean skippingSpace = true;
      for (int index=0; index<name.length(); index++) {
         char ch = name.charAt(index);
         if (!Character.isAlphabetic(ch) && !Character.isDigit(ch) ){
            ch = ' ';
         }
         if (Character.isWhitespace(ch)) {
            skippingSpace = true;
            continue;
         }
         if (skippingSpace) {
            ch = Character.toUpperCase(ch);
         }
         else {
            ch = Character.toLowerCase(ch);
         }
         sb.append(ch);
         skippingSpace = false;
      }
      return sb.toString();
   }
   
   /**
    * Extract pin names and create LLWU pin and peripheral C enum tables.<br>
    * The description for pins is also annotated with the pin number found or (Reserved)<br>
    * 
    * Tables are added to the following Peripheral Variables:
    *  <li>LlwuPins
    *  <li>LlwuPeripherals
    *  
    */
   private void doPinNames() {
      final String RESERVED = "Reserved";

      if (donePinNames) {
         return;
      }
      donePinNames = true;
      StringBuilder sb = new StringBuilder();
      sb.append(
            "/**\n"+
                  " * LLWU pin sources\n"+
                  " */\n"+
                  "enum LlwuPin : uint32_t {\n"
            );
      InfoTable pinTable = getPeripheral().getSignalTables().get(0);
      for (int index=0; index<32; index++) {
         String         choiceName = "llwu_pe"+((index/4)+1)+"_wupe"+index;
         ChoiceVariable choiceVar  = safeGetChoiceVariable(choiceName);
         if (choiceVar == null) {
            continue;
         }
         String llwuPinName;
         if (index>=pinTable.table.size()) {
            // Pin not in table (doesn't exist)
            choiceVar.enable(false);
            llwuPinName = RESERVED;
         }
         else {
            // Look up possible pin mapping in table
            Signal signal = pinTable.table.elementAt(index);
            Pin mappablePin = null;
            if (signal != null) {
               TreeSet<MappingInfo> pinMappings = signal.getPinMapping();
               for (MappingInfo pinMapping:pinMappings) {
                  if (pinMapping.getMux() == MuxSelection.mux1) {
                     mappablePin = pinMapping.getPin();
                  }
               }
            }
            if (mappablePin == null) {
               // No mappable pin
               choiceVar.enable(false);
               llwuPinName = RESERVED;
            }
            else {
               // Mappable pin
               choiceVar.enable(true);
               llwuPinName = mappablePin.getName();
            }
         }
         if (llwuPinName != RESERVED) {
            String llwuPinLine = String.format(
                  "   LlwuPin_%-15s = %2d, //!< Wake-up pin LLWU_P%d\n", 
                  capitalCase(llwuPinName), index, index); 
            sb.append(llwuPinLine);
         }
         choiceVar.setDescription(choiceVar.getDescription() + " - "+llwuPinName);
      }
      sb.append("};\n\n");
      StringVariable llwuPinsVar = new StringVariable("LlwuPins", getPeripheral().makeKey("LlwuPins"));
      llwuPinsVar.setValue(sb.toString());
      llwuPinsVar.setDerived(true);
      getPeripheral().addVariable(llwuPinsVar);

      sb = new StringBuilder();
      sb.append(
            "/**\n"+
                  " * LLWU peripheral sources\n"+
                  " */\n"+
                  "enum LlwuPeripheral : uint32_t {\n"
            );
      for (int index=0; index<=7; index++) {
         String         choiceName = "llwu_me_wume"+index;
         BooleanVariable choiceVar  = safeGetBooleanVariable(choiceName);
         String llwuPeripheralName;
         if (choiceVar != null) {
            llwuPeripheralName = choiceVar.getDescription();
            String llwuPeripheralLine = String.format(
                  "   LlwuPeripheral_%-15s = (1<<%d), //!< Wake-up peripheral LLWU_M%dIF\n", 
                  capitalCase(llwuPeripheralName), index, index
                  );
            sb.append(llwuPeripheralLine);
         }
      }
      sb.append("};\n\n");
      StringVariable llwuPeripheralsVar = new StringVariable("LlwuPeripherals", getPeripheral().makeKey("LlwuPeripherals"));
      llwuPeripheralsVar.setValue(sb.toString());
      llwuPeripheralsVar.setDerived(true);
      getPeripheral().addVariable(llwuPeripheralsVar);
   }
   
   /**
    * Class to validate LLWU settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      doPinNames();
      
      for (int index=0; index<8; index++) {
         // Filter variables
         ChoiceVariable   llwu_filt_filteVar     = safeGetChoiceVariable("llwu_filt"+index+"_filte");
         LongVariable     llwu_filt_filtselVar   = safeGetLongVariable("llwu_filt"+index+"_filtsel");
         if ((llwu_filt_filteVar != null) && ((variable == null) || (variable == llwu_filt_filteVar))) {
            llwu_filt_filtselVar.enable(llwu_filt_filteVar.getValueAsLong() != 0);
         }
      }
//      // Warn if Rx and Tx signals not mapped
//      validateMappedPins(new int[]{0,1}, getPeripheral().getSignalTables().get(0).table);
   }
   
   @Override
   protected void createDependencies() throws Exception {
      // No external dependencies
   }
}