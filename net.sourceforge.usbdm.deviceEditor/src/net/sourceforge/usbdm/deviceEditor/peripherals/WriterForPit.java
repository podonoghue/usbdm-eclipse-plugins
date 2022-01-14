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
      fCanCreateInstance = true;

      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType = true;

      // Can create instances for signals belonging to this peripheral
      fCanCreateSignalInstance = true;
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
         String trailingComment  = pin.getNameWithLocation();
         String description = signal.getUserDescription();
         String type = String.format("const %s<%d>", getClassBaseName()+getInstance()+"::"+"Channel", index);
         String constType = "const "+ type;
         if (signal.getCreateInstance()) {
            writeVariableDeclaration("", description, cIdentifier, constType, trailingComment);
         }
         else {
            writeTypeDeclaration("", description, cIdentifier, type, trailingComment);
         }
      }
   }

   @Override
   public boolean isPcrTableNeeded() {
      return false;
   }

}