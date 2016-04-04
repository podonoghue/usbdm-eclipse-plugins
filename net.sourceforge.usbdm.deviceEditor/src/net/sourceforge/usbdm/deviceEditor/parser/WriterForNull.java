package net.sourceforge.usbdm.deviceEditor.parser;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralTemplateInformation;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForNull extends Peripheral {

   public WriterForNull(String basename, String instance, PeripheralTemplateInformation template, DeviceInfo deviceInfo) {
      super(basename, instance, template, deviceInfo);
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
   public int getFunctionIndex(PeripheralFunction function) {
      return -1;
   }

}