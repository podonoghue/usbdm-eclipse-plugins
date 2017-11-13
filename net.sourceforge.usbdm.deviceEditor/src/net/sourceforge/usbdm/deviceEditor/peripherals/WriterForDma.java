package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForDma extends PeripheralWithState {

   public WriterForDma(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Direct Memory Access (DMA)";
   }

   @Override
   public void writeExtraInfo(DocumentUtilities documentUtilities) throws IOException {
   }

   @Override
   public void writeNamespaceInfo(DocumentUtilities documentUtilities) throws IOException {
      super.writeNamespaceInfo(documentUtilities);
   }

}