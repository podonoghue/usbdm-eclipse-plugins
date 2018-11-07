package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of VREF
 */
public class WriterForTrgmux extends PeripheralWithState {

   public WriterForTrgmux(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Trigger Multiplexor";
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p = Pattern.compile("IN(\\d+)");
      Matcher m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      p = Pattern.compile("OUT(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return 16+Integer.parseInt(m.group(1));
      }
      throw new RuntimeException("Function "+function+", Signal " + function.getSignalName() + " does not match expected pattern");
   }
   
   @Override
   protected void addSignalToTable(Signal function) {
      int signalIndex = getSignalIndex(function);
      if (fInfoTable == null) {
         throw new RuntimeException("Illegal function " + function.toString());
      }
      if (signalIndex>=fInfoTable.table.size()) {
         fInfoTable.table.setSize(signalIndex+1);
      }
      if ((fInfoTable.table.get(signalIndex) != null) && 
            (fInfoTable.table.get(signalIndex) != function)) {
         throw new RuntimeException("Multiple functions mapped to index new = " + function + ", old = " + fInfoTable.table.get(signalIndex));
      }
      fInfoTable.table.setElementAt(function, signalIndex);
   }

   @Override
   public ArrayList<InfoTable> getSignalTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fInfoTable);
      return rv;
   }
}