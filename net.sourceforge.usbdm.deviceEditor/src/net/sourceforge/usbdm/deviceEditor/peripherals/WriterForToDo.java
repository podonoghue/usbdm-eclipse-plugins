package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.HashMap;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of Unknow!!
 */
/**
 * @author podonoghue
 *
 */
public class WriterForToDo extends Peripheral {

   public WriterForToDo(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Not real hardware
      setSynthetic();
   }

   @Override
   public String getTitle() {
      return "(Incomplete)";
   }

   @Override
   public String getGroupName() {
      return getBaseName().toUpperCase()+"_TODO_Group";
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

   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
   }
}