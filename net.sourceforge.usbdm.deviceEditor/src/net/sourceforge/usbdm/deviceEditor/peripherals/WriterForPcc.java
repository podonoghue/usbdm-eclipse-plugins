package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.Map;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of PMC
 */
public class WriterForPcc extends PeripheralWithState {

   public WriterForPcc(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Peripheral Clock Control";
   }
   
   protected Map<String, String> addTemplatesToSymbolMap(Map<String, String> map) {
      return super.addTemplatesToSymbolMap(map);
   }
   
}