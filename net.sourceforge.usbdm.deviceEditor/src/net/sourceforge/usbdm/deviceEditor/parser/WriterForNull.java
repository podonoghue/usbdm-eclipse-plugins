package net.sourceforge.usbdm.deviceEditor.parser;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInformation;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForNull extends WriterBase {

   public WriterForNull(DeviceInfo deviceInfo, Peripheral peripheral) {
      super(deviceInfo, peripheral);
   }

   @Override
   public void writeInfoClass(DeviceInformation deviceInformation, DocumentUtilities writer) throws IOException {
   }

   @Override
   public String getTitle() {
      return null;
   }

   @Override
   public String getGroupBriefDescription() {
      return null;
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   @Override
   public String getAliasName(String signalName, String alias) {
      return null;
   }

   @Override
   public int getFunctionIndex(PeripheralFunction function) {
      return -1;
   }

}