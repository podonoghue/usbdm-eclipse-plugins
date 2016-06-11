package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.HashMap;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForShared extends Peripheral {

   public WriterForShared(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Shared Resources";
   }

   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      if (needPCRTable()) {
         pinMappingHeaderFile.write(getPcrDefinition());
      }
   }

   @Override
   public void writeInfoClass(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoClass(pinMappingHeaderFile);
   }

   @Override
   public String getGroupName() {
      return getBaseName().toUpperCase()+"_Group";
   }

   private HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
   private int fIndex = 0;
   
   @Override
   public int getSignalIndex(Signal signal) {
      Integer index = indexMap.get(signal.getName());
      if (index == null) {
         index = fIndex++;
         indexMap.put(signal.getName(), index);
      }
      return index;
   }
}