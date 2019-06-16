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
 * Class encapsulating the code for writing an instance of CMP
 */
public class WriterForCmp extends PeripheralWithState {

   final int NUMBER_OF_INPUTS = 8;
   
   public WriterForCmp(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
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

   static final String PIN_FORMAT   = "   %-20s = %-13s %s\n";
   
   void writeInputEnum(DocumentUtilities documentUtilities) throws IOException {
      String enumName    = getClassName()+"Input";
      String commentRoot = "//!< ";
      ArrayList<InfoTable> signalTables = getSignalTables();
      
      StringBuffer sb = new StringBuffer();
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
            MappingInfo mappingInfo = signal.getMappedPin();
            String pinName = enumName+"_"+prettyPinName(mappingInfo.getPin().getName());
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
         StringVariable cmpInputsVar = new StringVariable("InputMapping", makeKey("InputMapping"));
         cmpInputsVar.setValue(sb.toString());
         cmpInputsVar.setDerived(true);
         fDeviceInfo.addOrReplaceVariable(cmpInputsVar.getKey(), cmpInputsVar);
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