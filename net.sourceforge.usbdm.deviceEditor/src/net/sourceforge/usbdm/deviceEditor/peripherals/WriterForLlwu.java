package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of LLWU
 */
public class WriterForLlwu extends PeripheralWithState {

   public WriterForLlwu(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
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
   
   static final String PIN_FORMAT   = "   %-20s = %-13s %s\n";
   
   void writeInputEnum(DocumentUtilities documentUtilities) throws IOException {
      String enumName    = getClassName()+"Pin";
      String commentRoot = "//!< ";
      ArrayList<InfoTable> signalTables = getSignalTables();
      
      StringBuffer sb = new StringBuffer();
      for (InfoTable signalTable:signalTables) {
         int index = -1;
         for (Signal signal:signalTable.table) {
            index++;
            if (signal == null) {
               continue;
            }
            MappingInfo mappingInfo = signal.getMappedPin();
            String originalPinName = mappingInfo.getPin().getName();
            String pinName = enumName+"_"+prettyPinName(originalPinName);
            String mapName = enumName+"_"+index;
            do {
               if (!mappingInfo.getPin().isAvailableInPackage()) {
                  // Discard unmapped signals on this package 
                  continue;
               }
               if (mappingInfo.getMux() == MuxSelection.unassigned) {
                  // Reset selection - ignore
                  continue;
               }
               if (mappingInfo.getMux() == MuxSelection.fixed) {
                  // Fixed pin mapping
                  sb.append(String.format(PIN_FORMAT, pinName, mapName+',', commentRoot+"Fixed pin  "+mappingInfo.getPin().getName()));
                  continue;
               }
               if (mappingInfo.isSelected()) {
                  sb.append(String.format(PIN_FORMAT, pinName, mapName+',', commentRoot+"Mapped pin "+mappingInfo.getPin().getName()));

               }
            } while(false);
         }
         StringVariable llwuInputsVar = new StringVariable("InputMapping", makeKey("InputMapping"));
         llwuInputsVar.setValue(sb.toString());
         llwuInputsVar.setDerived(true);
         fDeviceInfo.addOrReplaceVariable(llwuInputsVar.getKey(), llwuInputsVar);
      }
   }
   
   @Override
   public void writeNamespaceInfo(DocumentUtilities documentUtilities) throws IOException {
      super.writeNamespaceInfo(documentUtilities);
      
      if (!needPCRTable()) {
         return;
      }
      writeInputEnum(documentUtilities);
   }

}