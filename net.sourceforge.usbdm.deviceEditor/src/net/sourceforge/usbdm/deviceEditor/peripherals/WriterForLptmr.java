package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp.HardwareDeclarationInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of LPTMR
 */
public class WriterForLptmr extends PeripheralWithState {

   public WriterForLptmr(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);

      // Can create type instances of this peripheral
      fCanCreateInstance = true;
      
      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType = true;

      // Can create instances for signals belonging to this peripheral
      fCanCreateSignalInstance = true;
   }

   @Override
   public String getTitle() {
      return "Low Power Timer";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"ALT0", "ALT1", "ALT2", "ALT3"};
      return getSignalIndex(function, signalNames);
   }

   @Override
   protected void writeDeclarations(HardwareDeclarationInfo hardwareDeclarationInfo) {
      super.writeDeclarations(hardwareDeclarationInfo);
      writeSignalPcrDeclarations(hardwareDeclarationInfo);
   }

}