package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of PIT
 */
public class WriterForPit extends PeripheralWithState {

   public WriterForPit(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can create instances for signals belonging to this peripheral
      super.setCanCreateInstance(true);

      // Can create type declarations for signals belonging to this peripheral
      super.setCanCreateSignalType(true);

      // Can create instances for signals belonging to this peripheral
      super.setCanCreateSignalInstance(true);
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
         String cIdentifier = signal.getCodeIdentifier();
         if (cIdentifier.isBlank()) {
            continue;
         }
         Pin pin = signal.getFirstMappedPinInformation().getPin();
         if (pin == Pin.UNASSIGNED_PIN) {
            continue;
         }
         String description = signal.getUserDescription();
         String declaration = String.format("const %s<%d>", getClassBaseName()+getInstance()+"::"+"Channel", index);
         
         if (signal.getCreateInstance()) {
            writeVariableDeclaration("", description, cIdentifier, declaration, pin.getLocation());
         }
         else {
            writeTypeDeclaration("", description, cIdentifier, declaration, pin.getLocation());
         }
      }
   }

   @Override
   public boolean isPcrTableNeeded() {
      return false;
   }

}