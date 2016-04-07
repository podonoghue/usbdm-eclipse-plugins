package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForNull extends Peripheral {

   public WriterForNull(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public void writeInfoClass(DocumentUtilities writer) throws IOException {
   }

   @Override
   public String getTitle() {
      return "Misc";
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   @Override
   public int getFunctionIndex(Signal function) {
      return -1;
   }

}