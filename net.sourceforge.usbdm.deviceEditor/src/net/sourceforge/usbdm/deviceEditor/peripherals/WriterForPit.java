package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of PIT
 */
public class WriterForPit extends PeripheralWithState {

   public WriterForPit(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Programmable Interrupt Timer";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"CH0", "CH1", "CH2", "CH3", "OUT"};
      return getSignalIndex(function, signalNames);
   }

   @Override
   protected void writeDeclarations() {
      
      super.writeDeclarations();
      
      for (int index=0; index<fInfoTable.table.size(); index++) {
         Signal signal = fInfoTable.table.get(index);
         if (signal == null) {
            continue;
         }
         MappingInfo pinMapping = signal.getFirstMappedPinInformation();
         Pin pin = pinMapping.getPin();
         String ident = pin.getSecondaryOrPrimaryCodeIdentifier();
         if (ident.isBlank()) {
            continue;
         }
         String declaration = String.format("const %s<%d>", getClassBaseName()+getInstance()+"::"+"Channel", index);
         
         writeVariableDeclaration("", pin.getPinDescription(), ident, declaration, pin.getLocation());
      }
   }

   @Override
   public boolean isPcrTableNeeded() {
      return false;
   }
   
}