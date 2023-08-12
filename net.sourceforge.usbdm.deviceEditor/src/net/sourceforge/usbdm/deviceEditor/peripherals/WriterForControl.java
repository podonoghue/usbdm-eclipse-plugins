package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of Control
 */
/**
 * @author podonoghue
 *
 */
public class WriterForControl extends PeripheralWithState {

   public WriterForControl(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      setSynthetic();
      
      // Can create type declarations for signals belonging to this peripheral (PCR)
      fcanCreateSignalType = true;
   }

   @Override
   protected void writeSignalPcrDeclarations() {
      // TODO Auto-generated method stub
      super.writeSignalPcrDeclarations();
   }
   
   @Override
   protected void writeDeclarations() {
      String pattern = null;

      // Check for explicit pattern form peripheral XML
      StringVariable patternVar = (StringVariable) safeGetVariable("/SYSTEM/$gpioPattern");
      if (patternVar != null) {
         pattern = patternVar.getValueAsString();
         // Suppressed output
         if (pattern.isBlank()) {
            return;
         }
      }
      ArrayList<InfoTable> InfoTables = getSignalTables();
      for (InfoTable infoTable:InfoTables) {
         for (int infoTableIndex=0; infoTableIndex<infoTable.table.size(); infoTableIndex++) {

            Signal signal = infoTable.table.get(infoTableIndex);
            if (signal == null) {
               continue;
            }
            MappingInfo pinMapping = signal.getFirstMappedPinInformation();
            if (pinMapping == MappingInfo.UNASSIGNED_MAPPING) {
               continue;
            }
            Pin pin = pinMapping.getPin();
            String trailingComment  = pin.getNameWithLocation();
            String cIdentifier = makeCTypeIdentifier(signal.getCodeIdentifier());
            if (!cIdentifier.isBlank()) {
               String type = expandTypePattern(pattern, "PcrTable_T<%cInfo,%t>", pin, infoTableIndex, "ActiveHigh");
               writeTypeDeclaration("", signal.getUserDescription(), cIdentifier, type, trailingComment);
            }
         }
      }
   }
   
   @Override
   public String getTitle() {
      return "Control";
   }

   @Override
   public void writeInfoConstants(final DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      super.writeDefaultPinInstances(pinMappingHeaderFile);
   }
   
   @Override
   public String getGroupName() {
      return "Control_Group";
   }

   private HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
   private int fIndex = 0;
   
   @Override
   public int getSignalIndex(Signal function) {
      Integer index = indexMap.get(function.getName());
      if (index == null) {
         index = fIndex++;
         indexMap.put(function.getName(), index);
      }
      return index;
      
//      return super.getSignalIndex(function, signalNames);
   }

}