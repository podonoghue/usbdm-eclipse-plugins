package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;

/**
 * Class encapsulating the code for writing an instance of RTC
 */
public class WriterForRtc extends PeripheralWithState {

   public WriterForRtc(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Real Time Clock";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"XTAL32K?", "EXTAL32K?", "CLKOUT", "CLKIN", "WAKEUP_b"};
      return getSignalIndex(function, signalNames);
   }

   @Override
   public int getPriority() {
      return 900;
   }
   
   @Override
   public void validateMappedPins() {
      super.validateMappedPins();
      if (fStatus != null) {
         return;
      }
      // Warn if EXTAL32 or XTAL32 are not mapped
      validateMappedPins(new int[]{0,1}, getSignalTables().get(0).table);
   }

   @Override
   public void extractHardwareInformation(Peripheral dbPortPeripheral) {
      extractAllRegisterFields(dbPortPeripheral);
   }
}