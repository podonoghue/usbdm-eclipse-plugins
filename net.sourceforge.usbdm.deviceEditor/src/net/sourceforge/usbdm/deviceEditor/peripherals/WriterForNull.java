package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForNull extends Peripheral {

   public WriterForNull(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Not sensible to make a null type
      fCanCreateType = false;

      // Not real hardware
      setSynthetic();

      System.err.println("NULL peripheral - " + getName());
   }

   @Override
   public void writeInfoClass(DocumentUtilities writer) throws IOException {
   }

   @Override
   public String getTitle() {
      return "Unknown";
   }

   @Override
   public int getSignalIndex(Signal function) {
      System.err.println("WriterForNull: Illegal signal index "+function.getName());
      return -1;
   }

   @Override
   public void addSignal(Signal signal) {
      System.err.println("WriterForNull: Adding signal "+signal.getName());
   }

   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
   }

}