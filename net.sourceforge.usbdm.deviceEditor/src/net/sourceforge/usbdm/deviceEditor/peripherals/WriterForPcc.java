package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;

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
   
   protected ISubstitutionMap addTemplatesToSymbolMap(ISubstitutionMap map) {
      return super.addTemplatesToSymbolMap(map);
   }
   
}